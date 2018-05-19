package com.patson

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model._
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane._
 
class AirportSimulationSpec extends WordSpecLike with Matchers {
  val sampleConsumption = LinkConsumptionDetails(linkId = 1, price = LinkClassValues.getInstance(0, 0, 0), capacity = LinkClassValues.getInstance(0, 0, 0), soldSeats = LinkClassValues.getInstance(0, 0, 0), quality = 0, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, maintenanceCost = 0, depreciation = 0, revenue = 0, profit = 0, fromAirportId = 0, toAirportId = 0, airlineId = 0, distance = 0, cycle = 0)
  "getTargetLoyalty".must {
    "get target loyalty based on average quality link consumption if volume is huge".in {
       assert(AirportSimulation.getTargetLoyalty(List.empty, 1000000) == 0) //0
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100000, 100000, 100000), quality = 50),
                                                      sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(0, 0, 0), quality = 100),
                                                      sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100000, 100000, 100000), quality = 0)), 1000000) == 25)
                               
                               
    }
    "get target loyalty based on passenger volume if quality is high".in {
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10, 0, 0), quality = 100)), 1000000) > 1)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10, 0, 0), quality = 100)), 1000000) < 30)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100, 0, 0), quality = 100)), 1000000) > 30)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100, 0, 0), quality = 100)), 1000000) < 50)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000, 0, 0), quality = 100)), 1000000) > 50)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000, 0, 0), quality = 100)), 1000000) < 80)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10000, 0, 0), quality = 100)), 1000000) > 80)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10000, 0, 0), quality = 100)), 1000000) < 100)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000000 / 52, 0, 0), quality = 100)), 1000000) == 100) //least amount of passenger to get full loyalty
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100000, 0, 0), quality = 100)), 1000000) == 100) //hit ceiling
                               
                               
    }
  }
  "getNewLoyalty".must {
    "at MAX loyalty, to increment loyalty by 1 requires transporting everyone (1 X pop) once per year".in {
       val population = 1000000
       val weeklyPassenger = 1000000 / 52
       
       val consumptions = List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(weeklyPassenger, 0, 0), quality = 100))
       
       assert(AirportSimulation.getNewLoyalty(consumptions, population, AirlineAppeal.MAX_LOYALTY, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //cannot increase any further 
       assert(AirportSimulation.getNewLoyalty(consumptions, population, AirlineAppeal.MAX_LOYALTY - 1, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //increaes to max
       
       //less consumption will still brings increment but not 1
       val consumptions2 = List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(weeklyPassenger / 2, 0, 0), quality = 100))
       assert(AirportSimulation.getNewLoyalty(consumptions2, population, AirlineAppeal.MAX_LOYALTY - 1, AirlineAppeal.MAX_LOYALTY) < AirlineAppeal.MAX_LOYALTY) //cannot increase to max
       assert(AirportSimulation.getNewLoyalty(consumptions2, population, AirlineAppeal.MAX_LOYALTY - 1, AirlineAppeal.MAX_LOYALTY) > AirlineAppeal.MAX_LOYALTY - 1) //but still going up some
       
    }
    "at MIN loyalty, to increment loyalty by 1 requires transporting 10,000th (0.0001 X pop) per year  (100 passenger per year for 1 mil pop)".in {
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY, 0) == Airline.MAX_SERVICE_QUALITY - AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //get full decrement
       assert(AirlineSimulation.getNewQuality(0, 0) == 0) //no decrement
       assert(AirlineSimulation.getNewQuality(50, 0) > 50 - AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //slow down
       assert(AirlineSimulation.getNewQuality(50, 0) < 50) //but should still have decrement
       assert(AirlineSimulation.getNewQuality(1, 0) > 0) //slow down
       assert(AirlineSimulation.getNewQuality(1, 0) < 1) //but should still have decrement
    }
  }
  
 
}
