package com.patson.init

import scala.collection.mutable.ListBuffer
import com.patson.model.Airport
import com.patson.data.AirportSource
import com.patson.data.AirlineSource
import com.patson.model.Airline
import com.patson.data.Constants._

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
      val newAirline = Airline("Airline " + i)
      newAirline.setBalance(5000000)
      airlines.append(newAirline)
    }
    AirlineSource.saveAirlines(airlines.toList)
  }

  def generateLoyalty(airlines: List[Airline], airports: List[Airport]) = {
    var counter = 0;
    airlines.foreach { airline => 
      counter += 1  
      airports.foreach {  airport =>
        airport.setAirlineLoyalty(airline.id, counter) 
        airport.setAirlineAwareness(airline.id, 10)
      }
    }
    
    AirportSource.updateAirlineAppeal(airports)
  }
}