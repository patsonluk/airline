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
import com.patson.data.LinkSource
 
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
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10, 0, 0), quality = 100)), 1000000) < 20)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100, 0, 0), quality = 100)), 1000000) > 20)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100, 0, 0), quality = 100)), 1000000) < 40)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000, 0, 0), quality = 100)), 1000000) > 40)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000, 0, 0), quality = 100)), 1000000) < 60)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10000, 0, 0), quality = 100)), 1000000) > 60)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(10000, 0, 0), quality = 100)), 1000000) < 80)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(15000, 0, 0), quality = 100)), 1000000) > 80)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(15000, 0, 0), quality = 100)), 1000000) < 100)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(1000000 / 52, 0, 0), quality = 100)), 1000000) == 100) //least amount of passenger to get full loyalty
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(100000, 0, 0), quality = 100)), 1000000) == 100) //hit ceiling
                               
                               
    }
  }
  "getNewLoyalty".must {
    "increment loyalty correctly".in {
       val population = 1000000
       val weeklyPassenger = 1000000 / 52
       
       val consumptions = List(sampleConsumption.copy(soldSeats = LinkClassValues.getInstance(weeklyPassenger, 0, 0), quality = 100))
       
       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //cannot increase any further 
       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY - 0.01, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //increaes to max
       assert(AirportSimulation.getNewLoyalty(0, AirportSimulation.LOYALTY_INCREMENT_BY_FLIGHTS + 1) == AirportSimulation.LOYALTY_INCREMENT_BY_FLIGHTS) //increase by AirportSimulation.LOYALTY_INCREMENT only
    }
    "decrement loyalty correctly".in {
       assert(AirportSimulation.getNewLoyalty(0, 0) == 0) //cannot decrease any further 
       assert(AirportSimulation.getNewLoyalty(0.5, 0) == 0) //decrease to 0
       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY, 0) == AirlineAppeal.MAX_LOYALTY - AirportSimulation.LOYALTY_DECREMENT_BY_FLIGHTS)
    }
  }
  
 
}
