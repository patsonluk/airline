package com.patson.init

import com.patson.{Authentication, DemandGenerator, Util}
import com.patson.data._
import com.patson.data.airplane._
import com.patson.init.GeoDataGenerator.calculateLongitudeBoundary
import com.patson.model._
import com.patson.model.airplane._

import java.util.Calendar
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Set}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random


object GenericTransitGenerator {

  val TRANSIT_MANUAL_LINKS = Map(
    "HND" -> "NRT",
    "PKK" -> "PEK",
    "PKK" -> "TSN",
    "IST" -> "SAW",
    "EWR" -> "PHL",
    "EWR" -> "HVN",
    "JFK" -> "ISP",
    "ORH" -> "BOS",
    "PVD" -> "BOS",
    "PBI" -> "MIA",
    "PBI" -> "FLL",
    "LAX" -> "ONT",
    "SNA" -> "ONT",
    "STL" -> "BLV",
    "SEA" -> "PEI",
    "KOA" -> "ITO",
    "YKF" -> "YYZ",
    "YVR" -> "YXX",
    "MEX" -> "TLC",
    "VCP" -> "GRU",
    "VCP" -> "CGH",
//    "AMS" -> "BRU",
    "AMS" -> "EIN",
    "EIN" -> "MST",
    "EIN" -> "RTM",
    "DUS" -> "CGN",
    "HAM" -> "LBC",
    "TRF" -> "OSL",
    "ARN" -> "NYO",
    "LTN" -> "LGW",
    "CDG" -> "BVA",
    "BCN" -> "REU",
    "BRN" -> "ZRH",
    "BRN" -> "GVA",
    "BRN" -> "BSL",
    "ZRH" -> "BSL",
    "MEL" -> "AVV"
  )
  val ISLANDS = List(
    "MVY", "ACK", "ISP", "FRD", "ESD", "MKK", "LNY", "GST", "VQS", "CPX", "STT", "EIS", "AXA", "PLS", "GDT", "SVD", "HID", "APW", "PPT"
  )

  def main(args : Array[String]) : Unit = {
    generateGenericTransit()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }
  def generateGenericTransit(airportCount : Int = 4000, range : Int = 50) : Unit = {
    //purge existing generic transit
    LinkSource.deleteLinksByCriteria(List(("transport_type", TransportType.GENERIC_TRANSIT.id)))

    val airports = AirportSource.loadAllAirports(true).filter(_.size >= 2).sortBy { _.power }.takeRight(airportCount)

    var counter = 0;
    var progressCount = 0;

    val processed = mutable.HashSet[(Int, Int)]()
    val countryRelationships = CountrySource.getCountryMutualRelationships()
    for (airport <- airports) {
      //calculate max and min longitude that we should kick off the calculation
      val boundaryLongitude = calculateLongitudeBoundary(airport.latitude, airport.longitude, range)
      val airportsInRange = scala.collection.mutable.ListBuffer[(Airport, Double)]()
      for (targetAirport <- airports) {
        if (airport.id != targetAirport.id &&
            !processed.contains((targetAirport.id, airport.id)) && //check the swap pairs are not processed already to avoid duplicates
            !ISLANDS.contains(targetAirport.iata) &&
            airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2 &&
            countryRelationships.getOrElse((airport.countryCode, targetAirport.countryCode), 0) >= 2
        ) {
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude).toInt
          if (range >= distance) {
            airportsInRange += Tuple2(targetAirport, distance)
          }
        } else if (TRANSIT_MANUAL_LINKS.get(airport.iata).contains(targetAirport.iata)) {
          val distance = Util.calculateDistance(airport.latitude, airport.longitude, targetAirport.latitude, targetAirport.longitude).toInt
          airportsInRange += Tuple2(targetAirport, distance)
        }

        processed.add((airport.id, targetAirport.id))
      }




      airportsInRange.foreach { case (targetAirport, distance) =>
        val domesticAirportBonus = if(targetAirport.isDomesticAirport() || airport.isDomesticAirport()){
          40000
        } else {
          0
        }
        val minSize = Math.min(airport.size, targetAirport.size)
        val capacity = minSize * 12000 + 10000 + domesticAirportBonus //kinda random
        val genericTransit = GenericTransit(from = airport, to = targetAirport, distance = distance.toInt, capacity = LinkClassValues.getInstance(economy = capacity))
        LinkSource.saveLink(genericTransit)
        println(genericTransit)
      }

      val progressChunk = airports.size / 100
      counter += 1
      if (counter % progressChunk == 0) {
        progressCount += 1;
        print(".")
        if (progressCount % 10 == 0) {
          print(progressCount + "% ")
        }
      }
    }
  }
}