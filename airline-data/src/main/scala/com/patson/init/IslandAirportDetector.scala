package com.patson.init

import com.patson.Util
import com.patson.data.AirportSource
import com.patson.model.Airport
import play.api.libs.json.{JsArray, JsObject, Json}

import java.net.{HttpURLConnection, URL, URLEncoder}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.{Codec, Source}

/**
 * One-time script that calls Google Maps Directions API to detect airports with no pure road
 * access and saves the result as `island_airport` in the DB.
 *
 * Usage:
 *   IslandAirportDetector <GOOGLE_MAPS_API_KEY> [override-file.csv]
 *
 * Override CSV format (header line required):
 *   iata,is_island
 *   HNL,false
 *   APK,true
 *
 * Detection strategy per airport:
 *   1. Find the single closest airport with size >= MIN_TARGET_SIZE (a major hub).
 *   2. Call Directions API (driving, with alternatives=true).
 *   3. If ZERO_RESULTS -> island airport.
 *   4. If every returned route contains a ferry step -> island airport.
 *   5. If at least one route is purely driving (no ferry) -> NOT an island airport.
 *
 * Ferry detection: a step is considered a ferry if its travel_mode != "DRIVING" OR its
 * html_instructions contains "ferry" (case-insensitive). Both checks together cover all
 * known Directions API encodings of car-ferry crossings.
 */
object IslandAirportDetector {
  val MIN_TARGET_SIZE = 6    // target the nearest mega/hub airport as the reference point
  val API_DELAY_MS    = 120L // ~8 req/sec, well under the default 50 req/sec quota

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: IslandAirportDetector <GOOGLE_MAPS_API_KEY> [override-file.csv]")
      System.exit(1)
    }

    val apiKey = args(0)
    val overrides: Map[String, Boolean] =
      if (args.length > 1) loadOverrides(args(1)) else Map.empty

    if (overrides.nonEmpty) println(s"Loaded ${overrides.size} manual overrides")

    val airports = AirportSource.loadAllAirports(false).sortBy(a => (-a.size, -a.basePopulation))
    println(s"Loaded ${airports.size} airports")

    val majorAirports = airports.filter(_.size >= MIN_TARGET_SIZE)
    println(s"Major airports (size >= $MIN_TARGET_SIZE): ${majorAirports.size}")

    val results = mutable.Map[Int, Boolean]()
    var apiCallCount = 0
    var processed = 0

    airports.foreach { airport =>
      val isIsland = overrides.get(airport.iata.toUpperCase) match {
        case Some(v) =>
          println(s"[OVERRIDE] ${airport.iata} (${airport.name}) -> isIsland=$v")
          v
        case None =>
          val (island, calls) = detectIsIsland(airport, majorAirports, apiKey)
          apiCallCount += calls
          island
      }

      results(airport.id) = isIsland
      processed += 1

      if (processed % 200 == 0)
        println(s"--- Progress: $processed / ${airports.size}  |  API calls: $apiCallCount ---")
    }

    val islandCount = results.count(_._2)
    println(s"\nDetection complete: $islandCount island airports out of ${airports.size}.  API calls: $apiCallCount")

    val outputPath = "island-airports.csv"
    val writer = new java.io.PrintWriter(new java.io.File(outputPath))
    try {
      writer.println("iata,is_island")
      val airportById = airports.map(a => a.id -> a).toMap
      results.toSeq.sortBy(_._1).foreach { case (airportId, isIsland) =>
        val iata = airportById(airportId).iata
        writer.println(s"$iata,$isIsland")
      }
    } finally writer.close()

    println(s"Results written to $outputPath")

    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  /**
   * Returns (isIsland, numberOfApiCallsMade).
   */
  def detectIsIsland(airport: Airport, majorAirports: List[Airport], apiKey: String): (Boolean, Int) = {
    nearestMajorAirport(airport, majorAirports) match {
      case None =>
        println(s"[NO TARGET]  ${airport.iata} (${airport.name}) - no size>=$MIN_TARGET_SIZE airport exists -> island")
        (true, 0)
      case Some(target) =>
        Thread.sleep(API_DELAY_MS)
        val (hasDrivingRoute, drivingSummary) = checkPureGroundRoute("driving", airport, target, apiKey)

        val (hasGroundRoute, fullSummary, calls) =
          if (hasDrivingRoute) {
            (true, s"driving: $drivingSummary", 1)
          } else {
            Thread.sleep(API_DELAY_MS)
            val (hasTransitRoute, transitSummary) = checkPureGroundRoute("transit", airport, target, apiKey)
            (hasTransitRoute, s"driving: $drivingSummary | transit: $transitSummary", 2)
          }

        val tag = if (hasGroundRoute) "[OK]    " else "[ISLAND]"
        println(s"$tag ${airport.iata} (${airport.name}, ${airport.countryCode})" +
                s" -> ${target.iata} (${target.name}) | $fullSummary")
        (!hasGroundRoute, calls)
    }
  }

  def nearestMajorAirport(airport: Airport, majorAirports: List[Airport]): Option[Airport] =
    majorAirports
      .filter(_.id != airport.id)
      .minByOption { other =>
        Util.calculateDistance(airport.latitude, airport.longitude, other.latitude, other.longitude)
      }

  val MAX_RETRIES      = 20
  val RETRY_DELAY_MS   = 5000L  // 5 s between OVER_QUERY_LIMIT retries

  /**
   * Checks for a pure ground route (no ferry) using the given mode ("driving" or "transit").
   * For driving: every step must have travel_mode=DRIVING.
   * For transit: steps may be WALKING or TRANSIT, but no ferry vehicle type.
   * Returns (hasRoute, summaryString).
   */
  def airportQueryString(airport: Airport): String =
    URLEncoder.encode(s"${airport.name} ${airport.iata}", "UTF-8")

  def checkPureGroundRoute(mode: String,
                            from: Airport, to: Airport,
                            apiKey: String): (Boolean, String) = {
    val avoid = if (mode == "driving") "&avoid=ferries" else ""
    val url = s"https://maps.googleapis.com/maps/api/directions/json" +
              s"?origin=${airportQueryString(from)}" +
              s"&destination=${airportQueryString(to)}" +
              s"&mode=$mode" +
              s"&alternatives=true" +
              s"$avoid" +
              s"&key=$apiKey"

    var attempt = 0
    while (attempt <= MAX_RETRIES) {
      try {
        val raw    = httpGet(url)
        val json   = Json.parse(raw).as[JsObject]
        val status = (json \ "status").as[String]

        status match {
          case "OK" =>
            val routes = (json \ "routes").as[JsArray]
            val ferryFree = routes.value.exists(r => routeIsFerryFree(r, mode))
            val routeCount = routes.value.size
            val summary = s"status=OK routes=$routeCount ferry_free=$ferryFree"
            return (ferryFree, summary)

          case "ZERO_RESULTS" | "NOT_FOUND" =>
            return (false, s"status=$status url=$url")

          case "OVER_QUERY_LIMIT" =>
            attempt += 1
            if (attempt > MAX_RETRIES) {
              System.err.println(s"OVER_QUERY_LIMIT after $MAX_RETRIES retries — aborting.")
              System.exit(1)
            }
            println(s"  OVER_QUERY_LIMIT (attempt $attempt/$MAX_RETRIES), retrying in ${RETRY_DELAY_MS}ms...")
            Thread.sleep(RETRY_DELAY_MS)

          case "OVER_DAILY_LIMIT" =>
            System.err.println("OVER_DAILY_LIMIT — daily quota exhausted. Aborting.")
            System.exit(1)

          case "REQUEST_DENIED" =>
            val msg = (json \ "error_message").asOpt[String].getOrElse("no details")
            System.err.println(s"REQUEST_DENIED — $msg")
            System.err.println(s"Full response: ${json.toString()}")
            System.exit(1)

          case other =>
            System.err.println(s"Unexpected API status '$other' for ${from.iata}->${to.iata} mode=$mode. Aborting.")
            System.exit(1)
        }
      } catch {
        case e: Exception =>
          System.err.println(s"API error ${from.iata}->${to.iata} mode=$mode: ${e.getMessage}")
          System.exit(1)
      }
    }
    (false, "exhausted retries") // unreachable, but satisfies the compiler
  }

  def routeIsFerryFree(route: play.api.libs.json.JsValue, mode: String): Boolean = {
    val legs = (route \ "legs").as[JsArray]
    legs.value.forall { leg =>
      val steps = (leg \ "steps").as[JsArray]
      steps.value.forall(s => stepIsGroundOnly(s, mode))
    }
  }

  def stepIsGroundOnly(step: play.api.libs.json.JsValue, mode: String): Boolean = {
    val travelMode   = (step \ "travel_mode").asOpt[String].getOrElse("").toUpperCase
    val instructions = (step \ "html_instructions").asOpt[String].getOrElse("")
    if (instructions.toLowerCase.contains("ferry")) return false
    mode match {
      case "driving" =>
        travelMode == "DRIVING"
      case "transit" =>
        travelMode match {
          case "WALKING" => true
          case "TRANSIT" =>
            val vehicleType = (step \ "transit_details" \ "line" \ "vehicle" \ "type").asOpt[String].getOrElse("").toUpperCase
            vehicleType != "FERRY"
          case _ => false
        }
      case _ => false
    }
  }

  def loadOverrides(path: String): Map[String, Boolean] = {
    val src = Source.fromFile(path)(Codec.UTF8)
    try {
      src.getLines()
        .drop(1)
        .filter(_.trim.nonEmpty)
        .map { line =>
          val cols = line.split(",")
          cols(0).trim.toUpperCase -> cols(1).trim.toBoolean
        }.toMap
    } finally src.close()
  }

  def httpGet(url: String): String = {
    val conn = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    conn.setConnectTimeout(10000)
    conn.setReadTimeout(15000)
    conn.setRequestMethod("GET")
    val is = conn.getInputStream
    try Source.fromInputStream(is)(Codec.UTF8).mkString
    finally if (is != null) is.close()
  }
}
