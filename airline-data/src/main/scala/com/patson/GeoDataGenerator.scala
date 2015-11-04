package com.patson

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import akka.actor.ActorSystem
import akka.util.ByteString
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ OnCompleteSink, Source, Sink }
import akka.stream.scaladsl.Flow
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import java.util.regex.Pattern
import java.sql.Connection
import java.sql.DriverManager
import com.patson.Constants._
import com.patson.model.CityInfo
import com.patson.model.AirportInfo

object GeoDataGenerator extends App {

  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

  import actorSystem.dispatcher

  implicit val materializer = FlowMaterializer()

  private val DEFAULT_UNKNOWN_INCOME = 100
  
  mainFlow
  
  def mainFlow() {
//    val cityList = Await.result(getCityInfo(), Duration(1, TimeUnit.MINUTES))
//    cityList.foreach{ println }
//    println(cityList.size)
    
//    val airportList = Await.result(getAirportInfo(), Duration(1, TimeUnit.MINUTES))
//    airportList.foreach{ println }
//    println(airportList.size)
    
    buildAirportData(getAirportInfo(), getCityInfo(getIncomeInfo()))
//    println(calculateDistance(38.898556, -77.037852, 38.897147, -77.043934))
//    println(calculateLongitudeBoundary(38.898556, -77.037852, 0.526))
//    println(calculateLongitudeBoundary(38.898556, 77.037852, 0.526))
    
    actorSystem.shutdown()
  }
  

  def getCityInfo(incomeInfo : Map[String, Int]): Future[List[CityInfo]] = {
    val citySource = Source(scala.io.Source.fromFile("cities1000.txt").getLines())
    val splitFlow: Flow[String, Array[String]] = Flow[String].map(_.split("\\t"))
    val parseFlow: Flow[Array[String], CityInfo] = Flow[Array[String]].filter { infoArray => infoArray(6) == "P" && (infoArray(7) == "PPLC" || infoArray(7) == "PPL") || infoArray(7) == "PPLA2" }.map {
      info =>
        {
          new CityInfo(info(1), info(4).toDouble, info(5).toDouble, info(8), info(14).toInt, (info(14).toLong * incomeInfo.get(info(8)).getOrElse(DEFAULT_UNKNOWN_INCOME))) //1, 4, 5, 8 - country code, 14
        }
    }

    val resultSink = Sink.fold(List[CityInfo]())((cityList, cityInfo : CityInfo) => (cityInfo :: cityList))
    
    val completeFlow = citySource.via(splitFlow).via(parseFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
  }
  
  def getAirportInfo() : Future[List[AirportInfo]]= {
    val airportSource = Source(scala.io.Source.fromFile("airports.csv").getLines())
    //val airportSource = Source(scala.io.Source.fromFile("short-airports.csv").getLines())
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
    
    val constructAirportInfoFlow : Flow[Array[String], AirportInfo] = Flow[Array[String]].map {
      info => 
        val airportSize = 
          info(2) match {
            case "small_airport" => 1
            case "medium_airport" => 2
            case "large_airport" => 3
            case _ => 0
          }
        new AirportInfo(info(13), info(12), info(3), info(4).toDouble, info(5).toDouble, info(8), info(10), airportSize) //2 - size, 3 - name, 4 - lat, 5 - long, 8 - country, 10 - city, 12 - code1, 13- code2
    }

    val resultSink = Sink.fold(List[AirportInfo]())((airportList, airportInfo : AirportInfo) => (airportInfo :: airportList))
    
    val completeFlow = airportSource.via(splitFlow).via(constructAirportInfoFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
  }
  
  def getIncomeInfo() = {
     
      val codeMap = scala.collection.mutable.Map[String, String]() //from 3 Char code to 2 Char code
      val incomeMap = scala.collection.mutable.Map[String, Int]()
      scala.io.Source.fromFile("country-code.txt").getLines().map(_.split(",")).foreach { tokens => 
        codeMap.put(tokens(2), tokens(1))
        println(tokens(2) + " => " + tokens(1))
      }
        
      scala.io.Source.fromFile("income-data.txt").getLines().map(_.split("\\t")).map( tokens => (tokens(1), tokens(12))).foreach {
        case(countryCode, incomeString) =>
          codeMap.get(countryCode).foreach(incomeMap.put(_, if (incomeString == "..") { DEFAULT_UNKNOWN_INCOME } else { incomeString.toDouble.toInt}))
      }
      incomeMap.foreach(println)
      
      collection.immutable.HashMap() ++ incomeMap
  }
  
  def buildAirportData(airportInfo : Future[List[AirportInfo]], cityInfo : Future[List[CityInfo]]) {
    val combinedFuture = Future.sequence(Seq(airportInfo, cityInfo))
    combinedFuture.onComplete { 
      case Success(results) =>
        val airportResult : List[AirportInfo] = results(0).asInstanceOf[List[AirportInfo]]
        val cityResult : List[CityInfo] = results(1).asInstanceOf[List[CityInfo]]
        
        println(airportResult.size + " airports")
        println(cityResult.size + " cities")
        
        val airportsSortedByLongitude = airportResult.sortBy(_.longitude)
        val citiesSortedByLongitude = cityResult.sortBy(_.longitude)
        
        
        
        var counter = 0;
        var progressCount = 0;
        
        for (city <- citiesSortedByLongitude) {
          //calculate max and min longitude that we should kick off the calculation
          val boundaryLongitude = calculateLongitudeBoundary(city.latitude, city.longitude, 200)
          val potentialAirports = scala.collection.mutable.MutableList[(AirportInfo, Double)]()
          for (airport <- airportsSortedByLongitude) {
            if (airport.size > 0 &&
                airport.countryCode == city.countryCode &&
                airport.longitude >= boundaryLongitude._1 && airport.longitude <= boundaryLongitude._2) {
              val distance = Util.calculateDistance(city.latitude, city.longitude, airport.latitude, airport.longitude)
              if ((airport.size == 1 && distance <= 50) || 
                  (airport.size == 2 && distance <= 100) ||
                  (airport.size == 3 && distance <= 200)) {
                  //println(city.name + " => " + airport.name)
                 potentialAirports += Tuple2(airport, distance)
              }
            }
          }
          
          if (potentialAirports.size == 1) {
            potentialAirports(0)._1.addCityServed(city)
          } else if (potentialAirports.size > 1) {
            val sortedAirports = potentialAirports.sortBy(_._2).sortBy(- _._1.size)
            sortedAirports(0)._1.addCityServed(city) 
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
        
        //open the hsqldb
        Class.forName(DB_DRIVER);
//        Class.forName("org.hsqldb.jdbcDriver");
        val connection = DriverManager.getConnection(DATABASE_CONNECTION, DATABASE_USER, "");
        //connection.setCatalog("airport")
        createSchema(connection)
        
        val preparedStatement = connection.prepareStatement("INSERT INTO airport(iata, icao, name, latitude, longitude, country_code, city, airport_size, power) VALUES(?,?,?,?,?,?,?,?,?)")
        
        var executeCounter = 0
        
        connection.setAutoCommit(false)
        airportResult.map(airportInfo => (airportInfo, airportInfo.citiesServed.foldLeft(0.toLong)( _ + _.power))).sortBy(_._2).foreach {
          case (airport, power) =>
            preparedStatement.setString(1, airport.iata)
            preparedStatement.setString(2, airport.icao)
            preparedStatement.setString(3, airport.name)
            preparedStatement.setDouble(4, airport.latitude)
            preparedStatement.setDouble(5, airport.longitude)
            preparedStatement.setString(6, airport.countryCode)
            preparedStatement.setString(7, airport.city)
            preparedStatement.setInt(8, airport.size)
            preparedStatement.setLong(9, power.toLong)
            if (preparedStatement.executeUpdate() != 1) {
              println(airport + " power " + airport.power)
            }
            
            executeCounter += 1
        }
        connection.commit()
        println(executeCounter)
        
        connection.close()
        
        
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
  }
  
  def createSchema(connection : Connection) {
//    val tables = connection.getMetaData.getTables(null, null, AIRPORT_SCHEMA_NAME, null)
//    if (!tables.next()) { //then table does not exist, create one
      connection.prepareStatement("DROP TABLE IF EXISTS " + AIRPORT_SCHEMA_NAME).execute()
      connection.prepareStatement("CREATE TABLE " + AIRPORT_SCHEMA_NAME + "( id INTEGER IDENTITY, iata VARCHAR(256), icao VARCHAR(256), name VARCHAR(256), latitude DOUBLE, longitude DOUBLE, country_code VARCHAR(256), city VARCHAR(256), airport_size INTEGER, power LONG)").execute()
//    }
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
  
  
  
  
 
}