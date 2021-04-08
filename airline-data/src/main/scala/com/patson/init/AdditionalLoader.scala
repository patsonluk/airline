package com.patson.init

import com.patson.model.Airport
import scala.io.Source
import com.patson.model.City
import com.patson.model.Country
import scala.collection.mutable.ListBuffer

object AdditionalLoader {
  def loadRemovalAirportIatas() : List[String] = {
    val removalAirportSource = scala.io.Source.fromFile("removal-airports.csv").getLines()
    val result = removalAirportSource.filter(!_.startsWith("#")).map(_.trim)

    result.toList
  }

  def loadAdditionalAirports() : List[Airport] = {
    val additionalAirportSource = scala.io.Source.fromFile("additional-airports.csv").getLines()
    val additionalAirports = ListBuffer[Airport]()
    additionalAirportSource.foreach { line =>
      if (!line.startsWith("#")) {
        val tokens = line.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).transform { token =>
//        val tokens = line.trim().split(",", -1).transform { token =>
          if (token.startsWith("\"") && token.endsWith("\"")) { 
            token.substring(1, token.length() - 1) 
          } else {
            token
          }
        }
        //iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, zone : String, var size : Int, var power : Long, var population : Long, var slots : Int, var id : Int = 0
        val airport = Airport(iata = tokens(0), icao = tokens(1), name = tokens(2), latitude = tokens(3).toDouble, longitude = tokens(4).toDouble, countryCode = tokens(5), city = tokens(6), zone = tokens(7), size = tokens(8).toInt, power = 0, population = 0, slots = 0)
        additionalAirports += airport
      }
    }
    
    println("Additional Airports!!: ")
    additionalAirports.foreach(println)
    additionalAirports.toList
  }

  def loadSpecialAirportNames() : List[String] = {
    val specialAirportNameSource = scala.io.Source.fromFile("special-airport-names.csv").getLines()
    val specialAirportNames = ListBuffer[String]()
    specialAirportNameSource.foreach { line =>
      if (!line.startsWith("#")) {
        val airportName = line.trim()
        if (airportName.length > 0) {
          specialAirportNames += airportName.toLowerCase
        }
      }
    }
    specialAirportNames.toList
  }

  def loadAdditionalCities(incomeInfo : Map[String, Int]) : List[City] = {
    val additionalCitySource = scala.io.Source.fromFile("additional-cities.csv").getLines()
    val additionalCities = ListBuffer[City]()
    additionalCitySource.foreach { line =>
      if (!line.startsWith("#") && !line.trim().isEmpty()) {
        
        val tokens = line.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).transform { token =>
//        val tokens = line.trim().split(",", -1).transform { token =>
          if (token.startsWith("\"") && token.endsWith("\"")) { 
            token.substring(1, token.length() - 1) 
          } else {
            token
          }
        }
        val city = City(name = tokens(0), latitude = tokens(1).toDouble, longitude = tokens(2).toDouble, countryCode = tokens(3), population = tokens(4).toInt, income = incomeInfo.get(tokens(3)).getOrElse(Country.DEFAULT_UNKNOWN_INCOME))
        additionalCities += city
      }
    }
    println("Additional Cities!!: ")
    additionalCities.toList.foreach(println)
    additionalCities.toList
  }
  
//  def getAirport() : Future[List[Airport]]= {
//    val airportSource = Source(scala.io.Source.fromFile("airports.csv").getLines())
//    val airportSource = Source(scala.io.Source.fromFile("short-airports.csv").getLines())
//    val pattern = Pattern.compile("(?:^|,)(?=[^\"]|(\")?)\"?((?(1)[^\"]*|[^,\"]*))\"?(?=,|$)");
//    pattern.matcher("test1,\"test2\",\"test3,test4\"").find
//    val splitFlow: Flow[String, Array[String]] = Flow[String].map {
//       inputLine =>   
//         val tokens = Nil
//         val matcher = pattern.matcher(inputLine)
//         while (matcher.find()) {
//           matcher.group(3) :: tokens 
//         }
//         tokens.toArray[String]        
//    }
    
//    val splitFlow: Flow[String, Array[String]] = Flow[String].map {
//      _.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).transform { 
//          token : String =>  
//            if (token.startsWith("\"") && token.endsWith("\"")) { 
//              token.substring(1, token.length() - 1) 
//            } else {
//              token
//            }
//      }.toArray
//    }
//
//    var headerLine = true
//    
//    val constructAirportFlow : Flow[Array[String], Airport] = Flow[Array[String]].map {
//      info => 
//        val airportSize = 
//          info(2) match {
//            case "small_airport" => 1
//            case "medium_airport" => 2
//            case "large_airport" => 3
//            case _ => 0
//          }
//        new Airport(info(13), info(12), info(3), info(4).toDouble, info(5).toDouble, info(8), info(10), zone = info(7), airportSize, 0, 0, 0) //2 - size, 3 - name, 4 - lat, 5 - long, 7 - zone, 8 - country, 10 - city, 12 - code1, 13- code2
//    }
//
//    val resultSink = Sink.fold(List[Airport]())((airportList, Airport : Airport) => (Airport :: airportList))
//    
//    val completeFlow = airportSource.via(splitFlow).via(constructAirportFlow).to(resultSink)
//    val materializedFlow = completeFlow.run()
//    materializedFlow.get(resultSink)
//  }
}