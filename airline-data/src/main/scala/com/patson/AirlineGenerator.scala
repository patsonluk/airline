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
import com.patson.model.Airport
import com.patson.data.AirportSource
import scala.collection.immutable.Map
import com.patson.model.FlightPreference
import com.patson.model.SimplePreference
import com.patson.data.AirlineSource
import com.patson.model.Airline
import com.patson.data.Constants._
import java.sql.PreparedStatement
import com.patson.data.Meta

object AirlineGenerator extends App {

//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
//
//  import actorSystem.dispatcher
//
//  implicit val materializer = FlowMaterializer()

 
  mainFlow
  
  def mainFlow() = {
    val airlines = generateAirlines(3)
    val airports = AirportSource.loadAllAirports()
    
    generateLoyalty(airlines, airports)
    
    println("DONE Creating airlines")
    
    actorSystem.shutdown()
  }
  
  def generateAirlines(count: Int) = {
    AirlineSource.deleteAllAirlines()
    
    val airlines = ListBuffer[Airline]()
    for (i <- 1 to count) {
      airlines.append(Airline("Airline " + i))
    }
    AirlineSource.saveAirlines(airlines.toList)
  }

  def generateLoyalty(airlines: List[Airline], airports: List[Airport]) = {
    var counter = 0;
    airlines.foreach { airline => 
      counter += 1  
      airports.foreach {  airport =>
        airport.setAirlineLoyalty(airline, counter) 
      }
    }
    
    AirportSource.updateLoyalty(airports)
  }
}