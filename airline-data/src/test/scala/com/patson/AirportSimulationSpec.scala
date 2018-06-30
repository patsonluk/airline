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
  val sampleLink = Link.fromId(1)
  val sampleConsumption = LinkConsumptionDetails(link = sampleLink, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, delayCompensation = 0, maintenanceCost = 0, depreciation = 0, revenue = 0, profit = 0,  cycle = 0)
  
  "getTargetLoyalty".must {
    "get target loyalty based on average quality link consumption if volume is huge".in {
       assert(AirportSimulation.getTargetLoyalty(List.empty, 1000000) == 0) //0
       
       val link1 = sampleLink.copy()
       val link2 = sampleLink.copy()
       val link3 = sampleLink.copy()
       
       link1.soldSeats = LinkClassValues.getInstance(100000, 100000, 100000)
       link1.setQuality(50)
       link2.setQuality(100)
       link3.soldSeats = LinkClassValues.getInstance(100000, 100000, 100000)
       link3.setQuality(0)
       
       
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link1),
                                                      sampleConsumption.copy(link = link2),
                                                      sampleConsumption.copy(link = link3)), 1000000) == 25)
                               
                               
    }
    "get target loyalty based on passenger volume if quality is high".in {
       val link = sampleLink.copy()
       link.setQuality(100)
       
       link.soldSeats = LinkClassValues.getInstance(10, 0, 0)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 1)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 20)
       
       link.soldSeats = LinkClassValues.getInstance(100, 0, 0)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 20)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 40)
       
       link.soldSeats = LinkClassValues.getInstance(1000, 0, 0)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 40)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 60)
       
       link.soldSeats = LinkClassValues.getInstance(10000, 0, 0)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 60)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 80)
       
       link.soldSeats = LinkClassValues.getInstance(15000, 0, 0)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 80)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 100)
       
       link.soldSeats = LinkClassValues.getInstance(1000000 / 52, 0, 0) //least amount of passenger to get full loyalty
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100)
       
       link.soldSeats = LinkClassValues.getInstance(100000, 0, 0) //hit ceiling
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100)
    }
    "get target loyalty with delay/cancellation penalty".in {
       val link = sampleLink.copy()
       link.setQuality(100)
       
       //no penalty
       link.soldSeats = LinkClassValues.getInstance(100000, 0, 0) //hit ceiling
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100)
       
       link.minorDelayCount = 1
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100 - 1 * AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link), sampleConsumption.copy(link = link)), 1000000) == 100 - 2 * AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       
       link.minorDelayCount = 3
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link), sampleConsumption.copy(link = link)), 1000000) == 100 - 6 * AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       
       link.majorDelayCount = 2
       link.cancellationCount = 3
       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100 - 3 * AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY - 2 * AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY - 3 * AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
    }
  }
  "getNewLoyalty".must {
    "increment loyalty correctly".in {
       val population = 1000000
       val weeklyPassenger = 1000000 / 52
       
       val link = sampleLink.copy()
       link.soldSeats = LinkClassValues.getInstance(weeklyPassenger, 0, 0)
       link.setQuality(100)
       val consumptions = List(sampleConsumption.copy(link = link))
       
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
