package com.patson.init

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Source, Sink}
import akka.stream.scaladsl.Flow
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

object GeoDataGenerator extends App {

  import actorSystem.dispatcher

  //implicit val materializer = FlowMaterializer()

  private val DEFAULT_UNKNOWN_INCOME = 1000
  
  mainFlow
  
  def mainFlow() {
    val getCityFuture = getCity(getIncomeInfo())
    
    val cities = Await.result(getCityFuture, Duration.Inf)  
        
    //make sure cities are saved first as we need the id for airport info
    try {
//      AirportSource.deleteAllAirports()
      CitySource.deleteAllCitites()
      CitySource.saveCities(cities)
    } catch {
      case e : Throwable => e.printStackTrace()
    }
    
    buildAirportData(getAirport(), getRunway(), cities)
//    println(calculateDistance(38.898556, -77.037852, 38.897147, -77.043934))
//    println(calculateLongitudeBoundary(38.898556, -77.037852, 0.526))
//    println(calculateLongitudeBoundary(38.898556, 77.037852, 0.526))
    
    actorSystem.shutdown()
  }
  

  def getCity(incomeInfo : Map[String, Int]): Future[List[City]] = {
    val citySource = Source(scala.io.Source.fromFile("cities1000.txt").getLines())
    val splitFlow: Flow[String, Array[String]] = Flow[String].map(_.split("\\t"))
    val parseFlow: Flow[Array[String], City] = Flow[Array[String]].filter { infoArray =>
      infoArray(6) == "P" && isCity(infoArray(7), infoArray(8)) && infoArray(14).toInt > 0  
    }.map {
      info =>
        {  
          if (incomeInfo.get(info(8)).isEmpty) {
            println(info(8) + " has no income info")
          }
          new City(info(1), info(4).toDouble, info(5).toDouble, info(8), info(14).toInt, incomeInfo.get(info(8)).getOrElse(DEFAULT_UNKNOWN_INCOME)) //1, 4, 5, 8 - country code, 14
        }
    }
    
    

    val resultSink = Sink.fold(List[City]())((cityList, City : City) => (City :: cityList))
    
    val completeFlow = citySource.via(splitFlow).via(parseFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
  }
  
  def isCity(placeCode : String, countryCode : String) : Boolean = {
    placeCode == "PPLC" || placeCode == "PPLA" || placeCode == "PPLA2" || placeCode == "PPLA3" || (placeCode == "PPL" && (countryCode == "AU" /*|| countryCode == "CA"*/))  
  }
  
  def getRunway() : Future[Map[String, List[Runway]]] = {
    val runwaySource = Source(scala.io.Source.fromFile("runways.csv").getLines())
    val splitFlow: Flow[String, Array[String]] = Flow[String].map {
      _.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).transform { 
          token : String =>  
            if (token.startsWith("\"") && token.endsWith("\"")) { 
              token.substring(1, token.length() - 1) 
            } else {
              token
            }
      }.toArray
    }

    var headerLine = true
    
    val asphaltPattern  = "(asp.*)".r
    val concretePattern = "(con.*|pem.*)".r
    val gravelPattern = "(gvl.*|.*gravel.*)".r
    val constructRunwayFlow : Flow[Array[String], Option[(String, Runway)]] = Flow[Array[String]].map {
      info => 
        val lighted = info(6) == "1"
        if (lighted) {
          try{
            val length = info(3).toInt
            val icao = info(2)
            val runway =
            info(5).toLowerCase() match { 
              case asphaltPattern(_) =>
                Some((icao, Runway(length, RunwayType.Asphalt))) 
              case concretePattern(_) => Some((icao, Runway(length, RunwayType.Concrete)))
              case gravelPattern(_) => Some((icao, Runway(length, RunwayType.Gravel)))
              case unknown  =>
                None
            }
            runway
          } catch {
            case _ : NumberFormatException => None
          }
        } else {
          None
        }
    }

    val resultSink = Sink.fold(Map[String, List[Runway]]()) { (foldMap, runwayEntry : Option[(String, Runway)]) =>
      runwayEntry match {
        case Some((icao, runway)) =>
          val existingList = foldMap.getOrElse(icao, List[Runway]())
          foldMap + Tuple2(icao, runway :: existingList)
        case None =>
          foldMap
      }
    }
    
    val completeFlow = runwaySource.via(splitFlow).via(constructRunwayFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
    
  }
  
  def getAirport() : Future[List[Airport]]= {
    val airportSource = Source(scala.io.Source.fromFile("airports.csv").getLines())
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
    
    val splitFlow: Flow[String, Array[String]] = Flow[String].map {
      _.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).transform { 
          token : String =>  
            if (token.startsWith("\"") && token.endsWith("\"")) { 
              token.substring(1, token.length() - 1) 
            } else {
              token
            }
      }.toArray
    }

    var headerLine = true
    
    val constructAirportFlow : Flow[Array[String], Airport] = Flow[Array[String]].map {
      info => 
        val airportSize = 
          info(2) match {
            case "small_airport" => 1
            case "medium_airport" => 2
            case "large_airport" => 3
            case _ => 0
          }
        new Airport(info(13), info(12), info(3), info(4).toDouble, info(5).toDouble, info(8), info(10), zone = info(7), airportSize, 0, 0, 0) //2 - size, 3 - name, 4 - lat, 5 - long, 7 - zone, 8 - country, 10 - city, 12 - code1, 13- code2
    }

    val resultSink = Sink.fold(List[Airport]())((airportList, Airport : Airport) => (Airport :: airportList))
    
    val completeFlow = airportSource.via(splitFlow).via(constructAirportFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
  }
  
  def getIncomeInfo() = {
     
      val codeMap = scala.collection.mutable.Map[String, String]() //from 3 Char code to 2 Char code
      val incomeMap = scala.collection.mutable.Map[String, Int]()
      scala.io.Source.fromFile("country-code.txt").getLines().map(_.split(",")).foreach { tokens => 
        codeMap.put(tokens(2), tokens(1))
        //println(tokens(2) + " => " + tokens(1))
      }
        
      scala.io.Source.fromFile("income-data.txt").getLines().map(_.split("\\t")).map{ tokens => 
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
          income = DEFAULT_UNKNOWN_INCOME
//          println("unknown: " + tokens(0))
        }
        (countryCode, income)
      }.foreach {
        case(countryCode, income) =>
          codeMap.get(countryCode).foreach(incomeMap.put(_, income))
      }
      
//      incomeMap.foreach(println)
      
      collection.immutable.HashMap() ++ incomeMap
  }
  
  def buildAirportData(airportFuture : Future[List[Airport]], runwayFuture : Future[Map[String, List[Runway]]], citites: List[City]) {
    val combinedFuture = Future.sequence(Seq(airportFuture, runwayFuture))
    combinedFuture.onComplete { 
      case Success(results) =>
        val rawAirportResult : List[Airport] = results(0).asInstanceOf[List[Airport]]
        val runwayResult : Map[String, List[Runway]] = results(1).asInstanceOf[Map[String, List[Runway]]]
        
        println(rawAirportResult.size + " airports")
        println(runwayResult.size + " solid runways")
        println(citites.size + " cities")
        
        val airportResult = adjustAirportByRunway(rawAirportResult.filter { airport => 
             airport.iata != "" && airport.name.toLowerCase().contains(" airport") && airport.size > 0
          }, runwayResult)
        
        val airportsSortedByLongitude = airportResult.sortBy(_.longitude)
        val citiesSortedByLongitude = citites.sortBy(_.longitude)
        
        
        
        var counter = 0;
        var progressCount = 0;
        
        for (city <- citiesSortedByLongitude) {
          //calculate max and min longitude that we should kick off the calculation
          val boundaryLongitude = calculateLongitudeBoundary(city.latitude, city.longitude, 200)
          val potentialAirports = scala.collection.mutable.MutableList[(Airport, Double)]()
          for (airport <- airportsSortedByLongitude) {
            if (airport.size > 0 &&
                airport.countryCode == city.countryCode &&
                airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2) {
              val distance = Util.calculateDistance(city.latitude, city.longitude, airport.latitude, airport.longitude)
              val airportRadis = Computation.calculateAirportRadius(airport)
              if (airportRadis >= distance) {
                  //println(city.name + " => " + airport.name)
                 potentialAirports += Tuple2(airport, distance)
              }
            }
          }
          
          if (potentialAirports.size == 1) {
            potentialAirports(0)._1.addCityServed(city, 1)
          } else if (potentialAirports.size > 1) {
            //val sortedAirports = potentialAirports.sortBy(_._2).sortBy(- _._1.size)
            val dominateAirportSize : Int =  potentialAirports.filter(_._2 <= 50).map( _._1).reduceLeftOption { (largestAirport, airport) =>
              if (largestAirport.size < airport.size) airport else largestAirport
            }.fold(0)(_.size)
            
            val validAirports = if (dominateAirportSize >= 6) { potentialAirports.filter(_._1.size >= 2) } else potentialAirports //there's a super airport within 50km, then other airports can only get some share if it's size >= 3

//            val validAirports = potentialAirports            //give small airports a chance... for now
            
            val airportWeights = validAirports.foldRight(List[(Airport, Int)]()) {
              case (Tuple2(airport, distance), airportWeightList) => 
                val thisAirportWeight = (if (distance <= 25) 5 else if (distance <= 50) 3 else if (distance <= 100) 2 else 1) * airport.size * airport.size * airport.size
                (airport, thisAirportWeight) :: airportWeightList
            }.sortBy(_._2).takeRight(10) //take the largest 10
            
            val totalWeight = airportWeights.foldRight(0)(_ ._2+ _)
            
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
            case(foldLong, Tuple2(city, weight)) => foldLong + (city.population.toLong * weight).toLong * city.income
          }
          airport.population = airport.citiesServed.foldLeft(0.toLong) {
            case(foldLong, Tuple2(city, weight)) => foldLong + (city.population.toLong * weight).toLong
          }
          
          //calculate slots
          val slots = airport.size match {
            case 1 => 50
            case 2 => 100
            case 3 => 450
            case 4 => 700
            case 5 => 1200
            case 6 => 1700
            case 7 => 2200
            case size : Int if size >= 8 => 2800
            case _ => 0
          }
          airport.slots = slots
          
          airport
        }.sortBy { _.power }
        
        //Meta.resetDatabase
        
        AirportSource.deleteAllAirports()
        AirportSource.saveAirports(airports)
        
        
        //Goal : map all city to some airport
        //Big airport = 200 km radius
        //Medium airport = 100 km radius
        //Small airport = 50 km radius
        
        //1. iterate thru city for airports within reaFch. the country code SHOULD BE the same. Pick serving airport with precedence of: 1. Size, 2. Distance
        //2. for city that has no match, find the closest airport with country code match (what if a city still does not match?)
        //3. now airport should have a list of cities it serves, calculate the total pop

        
        
      case Failure(failure) => println()
    }
    Await.result(combinedFuture, Duration.Inf)
    
    //patch features
    AirportFeaturePatcher.patchFeatures()
  }
  
  
  
  
  def calculateLongitudeBoundary(latInDegree : Double, lonInDegree: Double, maxDistance : Double) = {
    val lat = Math.toRadians(latInDegree)
    val lon = Math.toRadians(lonInDegree)
    
    val resultLon = Math.acos((Math.cos(maxDistance/6371) - Math.sin(lat) * Math.sin(lat)) / (Math.cos(lat) * Math.cos(lat))) + lon
    val resultLonInDegree = resultLon.toDegrees
    if (resultLonInDegree > lonInDegree) { 
      (2 * lonInDegree - resultLonInDegree, resultLonInDegree)
    } else {
      (resultLonInDegree, 2 * lonInDegree - resultLonInDegree)
    }
    //val d = Math.acos(Math.sin(lat) * Math.sin(lat) + Math.cos(lat) * Math.cos(lat) * Math.cos(unknown - lon)) * 6371 //=ACOS(SIN(Lat1)*SIN(Lat2)+COS(Lat1)*COS(Lat2)*COS(Lon2-Lon1))*6371

  }

  def adjustAirportByRunway(rawAirportResult: List[Airport], runwayResult: Map[String, List[Runway]]) : List[Airport]= {
    val MAX_AIRPORT_SIZE = 9
    rawAirportResult.map { rawAirport =>
      if (rawAirport.size == 3) { //have to at least to be a big airport for adjustment 
        runwayResult.get(rawAirport.icao) match {
          case Some(runways) =>
            var longRunway = 0 
            var veryLongRunway = 0
            var megaRunway = 0
            runways.foreach { runway =>
               if (runway.length >= 10000) {
                 megaRunway += 1
               } else if (runway.length >= 9000) {
                 veryLongRunway += 1
               } else if (runway.length >= 7000) {
                 longRunway += 1
               }
            }
             
           if (megaRunway > 0) {
             println(rawAirport.name)
             val size = 5 + megaRunway //at least size 6, max out at 9
             rawAirport.size = if (size > MAX_AIRPORT_SIZE) MAX_AIRPORT_SIZE else size 
           } else if (veryLongRunway > 0) {
             if (veryLongRunway > 1) { //2 very long runway
               rawAirport.size = 5 //size 5
             } else if (longRunway > 0) { //1 very long 1 long
               rawAirport.size = 5 //size 5
             } else {
               rawAirport.size = 4 //size 4
             }
           } else if (longRunway > 1) {
             rawAirport.size = 4//size 4
           }
          rawAirport
          case None => rawAirport //no change
        }
      } else {
        rawAirport //no change
      } 
    }
  }
  
  
  
  
 
}