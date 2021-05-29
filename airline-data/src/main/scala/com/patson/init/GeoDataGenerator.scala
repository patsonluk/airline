package com.patson.init

import java.nio.file.Paths

import akka.NotUsed

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.stream.IOResult

import scala.collection.mutable.ListBuffer
import scala.sys.process.ProcessImpl
//import akka.stream.scaladsl.{FileIO, Flow, Framing, RunnableGraph, Sink, Source}
import scala.io.Source
import akka.util.ByteString

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import com.patson.data.Constants._
import com.patson.model.City
import com.patson.model.Airport
import com.patson.data.AirportSource
import com.patson.data.CitySource
import com.patson.Util
import com.patson.model.Runway
import com.patson.model.RunwayType
import com.patson.model.Computation

import scala.collection.mutable.ArrayBuffer
import com.patson.model.Country
import com.patson.data.CountrySource

object GeoDataGenerator extends App {

  import actorSystem.dispatcher

  //implicit val materializer = FlowMaterializer()

  mainFlow

  def mainFlow() {
    val incomeInfo = getIncomeInfo()
    val getCityFuture = getCity(incomeInfo)

    var cities = Await.result(getCityFuture, Duration.Inf)
    cities = cities ++ AdditionalLoader.loadAdditionalCities(incomeInfo)

    //make sure cities are saved first as we need the id for airport info
    try {
      //      AirportSource.deleteAllAirports()
      CitySource.deleteAllCitites()
      CitySource.saveCities(cities)
    } catch {
      case e : Throwable => e.printStackTrace()
    }

    val airports = buildAirportData(getAirport(), getRunway(), cities)

    AirportAnimationPatcher.patchAirportAnimations()

    buildCountryData(airports)

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def getCity(incomeInfo : Map[String, Int]) : Future[List[City]] = {
    //val citySource : Source[String, NotUsed] = Source(scala.io.Source.fromFile("cities1000.txt").getLines())
    Future {
      val result = ListBuffer[City]()
      for (line : String <- Source.fromFile("cities500.txt").getLines) {
        val infoArray = line.split("\\t")
        if (infoArray(6) == "P" && isCity(infoArray(7), infoArray(8), infoArray(14).toInt) && infoArray(14).toInt > 0) { //then a valid target
          if (incomeInfo.get(infoArray(8)).isEmpty) {
            println(infoArray(8) + " has no income info")
          }
          result += new City(infoArray(1), infoArray(4).toDouble, infoArray(5).toDouble, infoArray(8), infoArray(14).toInt, incomeInfo.get(infoArray(8)).getOrElse(Country.DEFAULT_UNKNOWN_INCOME)) //1, 4, 5, 8 - country code, 14
        }
      }
      result.toList
    }
  }

  def isCity(placeCode : String, countryCode : String, population : Int) : Boolean = {
    placeCode == "PPLC" || placeCode == "PPLA" || placeCode == "PPLA2" || placeCode == "PPLA3" || (placeCode == "PPL" && (population >= 100000 || !countryCode.equals("US")))
  }

  def getRunway() : Future[Map[Int, List[Runway]]] = {
    Future {
      val result = scala.collection.mutable.HashMap[Int, collection.mutable.ListBuffer[Runway]]()
      val asphaltPattern = "(asp.*)".r
      val concretePattern = "(con.*|pem.*)".r
      val gravelPattern = "(gvl.*|.*gravel.*)".r
      for (line : String <- Source.fromFile("runways.csv").getLines) {
        val info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map { token =>
          if (token.startsWith("\"") && token.endsWith("\"")) {
            token.substring(1, token.length() - 1)
          } else {
            token
          }
        }


        val lighted = info(6) == "1"

        try {
          var length = (info(3).toInt * 0.3048).toInt
          if (length % 10 == 9) { //somehow the data is off my 1 meter
            length += 1
          }
          val csvAirportId = info(1).toInt
          var codeTokens = ListBuffer[String](info(8).trim, info(14).trim)
          codeTokens = codeTokens.filterNot(token => "XX".equals(token) || "".equals(token))
          val code = codeTokens.mkString("/")

          val runwayOption =
            info(5).toLowerCase() match {
              case asphaltPattern(_) =>
                Some(Runway(length, code, RunwayType.Asphalt, lighted))
              case concretePattern(_) => Some(Runway(length, code, RunwayType.Concrete, lighted))
              case gravelPattern(_) => Some(Runway(length, code, RunwayType.Gravel, lighted))
              case _ => Some(Runway(length, code, RunwayType.Unknown, lighted))
            }
          runwayOption.foreach {
            case (runway) =>
              val list = result.getOrElseUpdate(csvAirportId, ListBuffer[Runway]())
              list += runway
          }
        } catch {
          case _ : NumberFormatException => None
        }

        //
        //        if (infoArray(6) == "P" && isCity(infoArray(7), infoArray(8)) && infoArray(14).toInt > 0) { //then a valid target
        //          if (incomeInfo.get(infoArray(8)).isEmpty) {
        //            println(infoArray(8) + " has no income info")
        //          }
        //          result += new City(infoArray(1), infoArray(4).toDouble, infoArray(5).toDouble, infoArray(8), infoArray(14).toInt, incomeInfo.get(infoArray(8)).getOrElse(Country.DEFAULT_UNKNOWN_INCOME)) //1, 4, 5, 8 - country code, 14
        //        }
      }
      result.view.mapValues(_.toList).toMap
    }
  }

  def getAirport() : Future[List[CsvAirport]] = {
    Future {
      val result = ListBuffer[CsvAirport]()
      for (line : String <- Source.fromFile("airports.csv").getLines) {
        val info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map { token =>
          if (token.startsWith("\"") && token.endsWith("\"")) {
            token.substring(1, token.length() - 1)
          } else {
            token
          }
        }

        val airportSize =
          info(2) match {
            case "small_airport" => 1
            case "medium_airport" => 2
            case "large_airport" => 3
            case _ => 0
          }
        //0 - csvId, 2 - size, 3 - name, 4 - lat, 5 - long, 7 - zone, 8 - country, 10 - city, 11 - scheduled service, 12 - code1, 13- code2
        result += CsvAirport(airport = new Airport(info(13), info(12), info(3), info(4).toDouble, info(5).toDouble, info(8), info(10), zone = info(7), airportSize, 0, 0, 0),
          csvAirportId = info(0).toInt, scheduledService = "yes".equals(info(11)))

      }
      result.toList
    }
  }

  def getIncomeInfo() = {

    val codeMap = scala.collection.mutable.Map[String, String]() //from 3 Char code to 2 Char code
    val incomeMap = scala.collection.mutable.Map[String, Int]()
    scala.io.Source.fromFile("country-code.txt").getLines().map(_.split(",")).foreach { tokens =>
      codeMap.put(tokens(2), tokens(1))
      //println(tokens(2) + " => " + tokens(1))
    }

    scala.io.Source.fromFile("income-data.txt").getLines().map(_.split("\\t")).map { tokens =>
      val countryCode = tokens(1)
      var income = 0
      var index = 12
      while (income == 0 && index >= 2) { //from column 12 scan back to column 2
        //find the first income from right to left(latest)
        if (tokens(index) != "..") {
          income = tokens(index).toDouble.toInt
        }
        index -= 1
      }
      if (income == 0) {
        income = Country.DEFAULT_UNKNOWN_INCOME
        //          println("unknown: " + tokens(0))
      }
      (countryCode, income)
    }.foreach {
      case (countryCode, income) =>
        codeMap.get(countryCode).foreach(incomeMap.put(_, income))
    }

    //      incomeMap.foreach(println)

    collection.immutable.HashMap() ++ incomeMap
  }

  def buildAirportData(airportFuture : Future[List[CsvAirport]], runwayFuture : Future[Map[Int, List[Runway]]], citites : List[City]) : List[Airport] = {
    val combinedFuture = Future.sequence(Seq(airportFuture, runwayFuture))
    val results = Await.result(combinedFuture, Duration.Inf)

    val rawAirportResult : List[CsvAirport] = results(0).asInstanceOf[List[CsvAirport]]
    val runwayResult : Map[Int, List[Runway]] = results(1).asInstanceOf[Map[Int, List[Runway]]]

    println(rawAirportResult.size + " airports")
    println(runwayResult.size + " solid runways")
    println(citites.size + " cities")
    val airports = generateAirportData(rawAirportResult, runwayResult, citites)

    //Meta.resetDatabase

    AirportSource.deleteAllAirports()
    AirportSource.saveAirports(airports)

    //patch features
    AirportFeaturePatcher.patchFeatures()
    IsolatedAirportPatcher.patchIsolatedAirports()

    airports
  }



  def generateAirportData(rawAirportResult : List[CsvAirport], runwayResult : Map[Int, List[Runway]], cities : List[City]) : List[Airport] = {
    val specialAirportNames = AdditionalLoader.loadSpecialAirportNames()
    val removalAirportIatas = AdditionalLoader.loadRemovalAirportIatas()

    println(s"Removal iatas")
    removalAirportIatas.foreach(println)

    setAirportRunwayDetails(rawAirportResult, runwayResult)

    var airportResult = adjustAirportByRunway(rawAirportResult.filter { case(CsvAirport(airport, _, scheduledService)) =>
      airport.iata != "" && scheduledService && airport.size > 0 && !removalAirportIatas.contains(airport.iata)
    }, runwayResult) //

    airportResult = adjustAirportSize(airportResult)

    val additionalAirports : List[Airport] = AdditionalLoader.loadAdditionalAirports()

    airportResult = airportResult ++ additionalAirports

    val airportsSortedByLongitude = airportResult.sortBy(_.longitude)
    val citiesSortedByLongitude = cities.sortBy(_.longitude)

    var counter = 0;
    var progressCount = 0;

    for (city <- citiesSortedByLongitude) {
      //calculate max and min longitude that we should kick off the calculation
      val boundaryLongitude = calculateLongitudeBoundary(city.latitude, city.longitude, 300)
      val potentialAirports = scala.collection.mutable.ListBuffer[(Airport, Double)]()
      for (airport <- airportsSortedByLongitude) {
        if (airport.size > 0 &&
          airport.countryCode == city.countryCode &&
          airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2) {
          val distance = Util.calculateDistance(city.latitude, city.longitude, airport.latitude, airport.longitude)
          if (airport.airportRadius >= distance) {
            //println(city.name + " => " + airport.name)
            potentialAirports += Tuple2(airport, distance)
          }
        }
      }

      if (potentialAirports.size == 1) {
        potentialAirports(0)._1.addCityServed(city, 1)
      } else if (potentialAirports.size > 1) {
        //val sortedAirports = potentialAirports.sortBy(_._2).sortBy(- _._1.size)
        val dominateAirportSize : Int = potentialAirports.filter(_._2 <= 50).map(_._1).reduceLeftOption { (largestAirport, airport) =>
          if (largestAirport.size < airport.size) airport else largestAirport
        }.fold(0)(_.size)

        val validAirports = if (dominateAirportSize >= 6) {
          potentialAirports.filter(_._1.size >= 2)
        } else potentialAirports //there's a super airport within 50km, then other airports can only get some share if it's size >= 3

        //            val validAirports = potentialAirports            //give small airports a chance... for now

        val airportWeights = validAirports.foldRight(List[(Airport, Int)]()) {
          case (Tuple2(airport, distance), airportWeightList) =>
            val thisAirportWeight = (if (distance <= 25) 30 else if (distance <= 50) 20 else if (distance <= 100) 8 else if (distance <= 200) 2 else 1) * airport.size * airport.size
            (airport, thisAirportWeight) :: airportWeightList
        }.sortBy(_._2).takeRight(10) //take the largest 10

        val totalWeight = airportWeights.foldRight(0)(_._2 + _)

        airportWeights.foreach {
          case Tuple2(airport, weight) => airport.addCityServed(city, weight.toDouble / totalWeight)
        }
      }

      val progressChunk = citiesSortedByLongitude.size / 100
      counter += 1
      if (counter % progressChunk == 0) {
        progressCount += 1;
        print(".")
        if (progressCount % 10 == 0) {
          print(progressCount + "% ")
        }
      }
    }

    val airports = airportResult.map { airport =>
      airport.power = airport.citiesServed.foldLeft(0.toLong) {
        case (foldLong, Tuple2(city, weight)) => foldLong + (city.population.toLong * weight).toLong * city.income
      }
      airport.population = airport.citiesServed.foldLeft(0.toLong) {
        case (foldLong, Tuple2(city, weight)) => foldLong + (city.population.toLong * weight).toLong
      }

      //calculate slots
      //https://en.wikipedia.org/wiki/List_of_busiest_airports_by_aircraft_movements#2017_statistics (take 30% discount assumimg those are freight service)
      val slots = airport.size match {
        case 1 => 200
        case 2 => 500
        case 3 => 1000
        case 4 => 2000
        case 5 => 2500
        case 6 => 3000
        case 7 => 4000
        case 8 => 6000
        case 9 => 8000
        case size : Int if size >= 10 => 12000
        case _ => 0
      }
      airport.slots = slots

      airport
    }.sortBy {
      _.power
    }

    airports
  }


  def calculateLongitudeBoundary(latInDegree : Double, lonInDegree : Double, maxDistance : Double) = {
    val lat = Math.toRadians(latInDegree)
    val lon = Math.toRadians(lonInDegree)

    val resultLon = Math.acos((Math.cos(maxDistance / 6371) - Math.sin(lat) * Math.sin(lat)) / (Math.cos(lat) * Math.cos(lat))) + lon
    val resultLonInDegree = resultLon.toDegrees
    if (resultLonInDegree > lonInDegree) {
      (2 * lonInDegree - resultLonInDegree, resultLonInDegree)
    } else {
      (resultLonInDegree, 2 * lonInDegree - resultLonInDegree)
    }
    //val d = Math.acos(Math.sin(lat) * Math.sin(lat) + Math.cos(lat) * Math.cos(lat) * Math.cos(unknown - lon)) * 6371 //=ACOS(SIN(Lat1)*SIN(Lat2)+COS(Lat1)*COS(Lat2)*COS(Lon2-Lon1))*6371

  }

  case class CsvAirport(airport : Airport, csvAirportId : Int, scheduledService : Boolean)

  def setAirportRunwayDetails(csvAirports : List[CsvAirport], runwaysByCsvId : Map[Int, List[Runway]]) : Unit = {
    csvAirports.foreach {
      case (CsvAirport(airport, csvId, _)) =>
        runwaysByCsvId.get(csvId) match {
        case Some(runways) =>
          if (runways.length > 0) {
            airport.setRunways(runways)
            println(s"$airport has max runway ${airport.runwayLength}")
          } else {
            println(s"$airport has no runway")
          }
        case None =>
          println(s"$airport has no runway")
      }
    }
  }

  def adjustAirportByRunway(rawAirportResult : List[CsvAirport], runwayResult : Map[Int, List[Runway]]) : List[Airport] = {
    rawAirportResult.map {
      case (CsvAirport(rawAirport, csvAirportId, _)) =>
        val increment : Int = runwayResult.get(csvAirportId) match {
          case Some(runways) =>
            var longRunway = 0
            var veryLongRunway = 0
            var megaRunway = 0
            runways.filter(_.lighted).foreach { runway => //only count lighted runways
              if (runway.length >= 10000 * 0.3048) { //old logic (for example 10000, was in feet) while runway.length is in meter now
                megaRunway += 1
              } else if (runway.length >= 9000 * 0.3048) {
                veryLongRunway += 1
              } else if (runway.length >= 7000 * 0.3048) {
                longRunway += 1
              }
            }

            if (megaRunway > 0) {
              println(rawAirport.name)
              3
            } else if (veryLongRunway > 0) {
              if (veryLongRunway > 1) { //2 very long runway
                2
              } else if (longRunway > 0) { //1 very long 1+ long
                2
              } else {
                1
              }
            } else if (longRunway >= 1) {
              1
            } else {
              0
            }
          case None => 0 //no change
        }
        rawAirport.size = rawAirport.size + increment

        rawAirport
    }
  }

  def adjustAirportSize(airports : List[Airport]) : List[Airport] = {
    airports.foreach { airport =>
      AirportSizeAdjust.sizeList.get(airport.iata).foreach { newSize =>
        airport.size = newSize
      }
    }
    airports
  }

  def buildCountryData(airports : Seq[Airport]) {
    val airportsByCountry : Map[String, Seq[Airport]] = airports.groupBy { airport => airport.countryCode }

    val countryCodeToNameMap = scala.io.Source.fromFile("country-code.txt").getLines().map(_.split(",")).map { tokens =>
      val countryCode = tokens(1)
      val name = tokens(0)
      (countryCode, name)
    }.toMap


    val codeMap = scala.io.Source.fromFile("country-code.txt").getLines().map(_.split(",")).map { tokens =>
      (tokens(2), tokens(1))
    }.toMap

    val opennessMap : Map[String, Int] = scala.io.Source.fromFile("openness.csv").getLines().map(_.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)).map { tokens =>
      val trimmedTokens = tokens.map { token : String =>
        if (token.startsWith("\"") && token.endsWith("\"")) {
          token.substring(1, token.length() - 1)
        } else {
          token
        }
      }

      val countryCode3 = trimmedTokens(1)
      val opennessRanking : Option[Int] =
        trimmedTokens.drop(4).reverse.find { token => !token.isEmpty() } match {
          case Some(rankingString) =>
            try {
              Some(rankingString.toInt)
            } catch {
              case _ : NumberFormatException => None //ok just ignore
            }
          case None => None
        }
      codeMap.get(countryCode3) match {
        case Some(countryCode2) =>
          val opennessValue = opennessRanking.fold(0) { opennessRankingValue =>
            if (opennessRankingValue > 200) {
              0
            } else {
              (200 - opennessRankingValue) / 20 + 1
            }
          }
          Some((countryCode2, opennessValue))
        case None =>
          //println("cannot find matching country code for " + countryCode3)
          None
      }
    }.flatten.toMap


    val countries = ArrayBuffer[Country]()
    airportsByCountry.foreach {
      case (countryCode, airports) =>
        val totalAirportPopulation = airports.map {
          _.population
        }.sum
        val averageIncome = if (totalAirportPopulation == 0) {
          0
        } else {
          airports.map {
            _.power
          }.sum / totalAirportPopulation
        }
        val openness =
          countries += Country(countryCode, countryCodeToNameMap(countryCode), totalAirportPopulation.toInt, averageIncome.toInt, opennessMap.getOrElse(countryCode, 0))
    }

    CountrySource.purgeAllCountries()
    println("Truncated all countries")
    CountrySource.saveCountries(countries.toList)
    println(s"Saved ${countries.length} countries")

    CountryMutualRelationshipGenerator.mainFlow()
  }
}