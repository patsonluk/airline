package com.patson.init

import com.patson.model.Airport
import scala.io.Source
import com.patson.model.City
import com.patson.model.Country
import com.patson.model.Destination
import scala.collection.mutable.ListBuffer
import scala.util.Try
import com.patson.data.AirportSource

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
        val airport = Airport(iata = tokens(0), icao = tokens(1), name = tokens(2), latitude = tokens(3).toDouble, longitude = tokens(4).toDouble, countryCode = tokens(5), city = tokens(6), zone = tokens(7), size = tokens(8).toInt, basePopulation = 0, popMiddleIncome = 0, popElite = 0, baseIncome = 0)
        additionalAirports += airport
      }
    }
    
    println("Additional Airports!!: ")
    additionalAirports.foreach(println)
    additionalAirports.toList
  }

  def loadDestinations(): List[Destination] = {
    val destinationsSource = scala.io.Source.fromFile("destinations.csv").getLines()
    val destinations = ListBuffer[Destination]()
    var id = 0
    destinationsSource.foreach { line =>
      if (!line.startsWith("#") && !line.trim().isEmpty()) {
        import com.patson.model.DestinationType

        val tokens = line.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)
        id += 1
        val airportCode = tokens(2).toString
        val airport = AirportSource.loadAirportByIata(airportCode)
        val name = tokens(0)
        val destinationType = DestinationType.withNameSafe(tokens(6).trim).getOrElse(DestinationType.ELITE_DESTINATION)
        val description = tokens(5)
        val strength = Try(tokens(1).trim.toInt).getOrElse(0)
        val latitude = Try(tokens(7).trim.toDouble).getOrElse(0.0)
        val longitude = Try(tokens(8).trim.toDouble).getOrElse(0.0)
        val countryCode = tokens(4)

        airport match {
          case Some(airport) =>
            val destination = Destination(id, airport, name, destinationType, strength, description, latitude, longitude, countryCode)
            destinations += destination
          case None =>
            println(s"Invalid airport type: $airportCode. Skipping this entry.")
        }
      }
    }
    println("Loaded destinations!!")
//    destinations.toList.foreach(println)
    destinations.toList
  }

  def loadAdditionalCities() : List[City] = {
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
        val city = City(
          name = tokens(0),
          latitude = tokens(1).toDouble,
          longitude = tokens(2).toDouble,
          countryCode = tokens(3),
          population = tokens(4).toInt,
          income = if(tokens(5).length >= 1){
            tokens(5).toInt
          }  else {
            Country.DEFAULT_UNKNOWN_INCOME
          }
        )
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