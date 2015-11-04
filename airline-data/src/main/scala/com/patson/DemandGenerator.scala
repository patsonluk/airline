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
import akka.stream.scaladsl.ForeachSink
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import com.patson.model.AirportInfo


object DemandGenerator extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()

  def computeDemandBetweenAirports(fromAirport : AirportInfo, toAirport : AirportInfo, totalWorldPower : Long) = {
    if (fromAirport == toAirport) {
      0
    } else {
      val passengerSupplyPerWeek =  (fromAirport.power / 30000 / 52).toInt //assuming 1 flight per year if PPP is 30k
      (passengerSupplyPerWeek * toAirport.power / totalWorldPower).toInt //TODO only proportional to target airport power for now
    }
  }
  
  mainFlow
  
  def mainFlow() = {
    Await.ready(computeDemand(DataSource.loadAirportData()), Duration.Inf)
    
    actorSystem.shutdown()
  }
  
  def computeDemand(airportInfo : List[AirportInfo]) = {
    val totalWorldPower = airportInfo.foldRight(0L)( _.power + _)
    
	  val airportSource = Source(airportInfo)
	  val computeFlow: Flow[AirportInfo, (AirportInfo, Map[AirportInfo, Int])] = Flow[AirportInfo].filter { _.power > 0 }.mapAsync { 
	    fromAirport : AirportInfo => {
	      Future {
	        val demandMap = airportInfo.foldLeft(Map[AirportInfo, Int]())((demandMap, toAirport) => {
	          val demand = computeDemandBetweenAirports(fromAirport, toAirport, totalWorldPower)
	          if (demand > 0) {
	            demandMap + Tuple2(toAirport, demand)
	          } else {
	            demandMap
	          }
	        }) 
	        (fromAirport, demandMap)
	      }
	    }
	  }
    //val resultSink = Sink.foreach { demandInfo : (AirportInfo, Map[AirportInfo, Int]) => println() }
    var counter = 0
    var progressCount = 0
    val progressChunk = airportInfo.length / 100
    
    val resultSink = Sink.fold(Map[AirportInfo, Map[AirportInfo, Int]]()) {
      (map, demandInfo : (AirportInfo, Map[AirportInfo, Int])) =>
        map + demandInfo 
    }
    
    val completeFlow = airportSource.via(computeFlow).to(resultSink)
    val materializedFlow = completeFlow.run()
    materializedFlow.get(resultSink)
  }
  
  
  

  def buildList(e : Any, list : List[Any]) : List[Any] = {
   buildList(e, e :: list) 
  }
  
  

//  case class AirportInfo(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, size : Int, power : Long) {
//    val citiesServed = scala.collection.mutable.MutableList[CityInfo]()
//    def addCityServed(city : CityInfo) {
//      citiesServed += city
//    }
//  }

}