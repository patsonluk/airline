package com.patson

import com.patson.model._
import com.patson.model.airplane._
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable.ListBuffer
 
class AirportSimulationSpec extends WordSpecLike with Matchers {
  val sampleLink = Link.fromId(1)
  val sampleConsumption = LinkConsumptionDetails(link = sampleLink, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, delayCompensation = 0, maintenanceCost = 0, loungeCost = 0, depreciation = 0, revenue = 0, profit = 0,  cycle = 0)
  
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
    "get penalty with delay/cancellation".in {
      val smallAirplaneModel = Model.modelByName("Bombardier CS100")
      val largeAirplaneModel = Model.modelByName("Boeing 747-400")
      
      val smallAirplane = Airplane(smallAirplaneModel, Airline.fromId(1), 0, purchasedCycle = 0, 100, AirplaneSimulation.computeDepreciationRate(smallAirplaneModel, Airplane.MAX_CONDITION.toDouble / smallAirplaneModel.lifespan), smallAirplaneModel.price)
      val largeAirplane = Airplane(largeAirplaneModel, Airline.fromId(1), 0, purchasedCycle = 0, 100, AirplaneSimulation.computeDepreciationRate(largeAirplaneModel, Airplane.MAX_CONDITION.toDouble / largeAirplaneModel.lifespan), largeAirplaneModel.price)
      
      val consumptions = ListBuffer[LinkConsumptionDetails]()
      List(smallAirplane, largeAirplane).foreach { airplane =>
        val distance = airplane.model.range / 2
        val duration = Computation.calculateDuration(airplane.model, distance)
        val price = Pricing.computeStandardPrice(distance, FlightType.LONG_HAUL_INTERCONTINENTAL, ECONOMY)
      
        val frequency = Computation.calculateMaxFrequency(airplane.model, distance)
        val capacity = frequency * airplane.model.capacity
        val fromAirport = Airport.fromId(1)
        val toAirport = Airport.fromId(2)
        val link = Link(fromAirport, toAirport, Airline.fromId(1), LinkClassValues.getInstanceByMap(Map(ECONOMY -> price)), distance, LinkClassValues.getInstanceByMap(Map(ECONOMY -> capacity)), rawQuality = 0, duration, frequency, Computation.getFlightType(fromAirport, toAirport, distance))
        link.setAssignedAirplanes(List(airplane))
        consumptions.append(LinkConsumptionDetails(link = link, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, delayCompensation = 0, maintenanceCost = 0, loungeCost = 0, depreciation = 0, revenue = 0, profit = 0,  cycle = 0))
      }
      
      val consumption = 
       //no penalty
       assert(AirportSimulation.getPenalty(consumptions.toList) == 0)
       
       val singleMinorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.minorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       val singleMinorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.minorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       val singleMajorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.majorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleMajorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.majorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleCancellationSmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.cancellationCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleCancellationLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.cancellationCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMinorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.minorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       val allMinorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.minorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       val allMajorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.majorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMajorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.majorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allCancellationSmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.cancellationCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allCancellationLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.copy()
             newLink.cancellationCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMinorDelayConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.copy()
           newLink.minorDelayCount = consumption.link.frequency
           newLink
         })
       }
       
       val allMajorDelayConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.copy()
           newLink.majorDelayCount = consumption.link.frequency
           newLink
         })
       }
       
       val allCancellationConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.copy()
           newLink.cancellationCount = consumption.link.frequency
           newLink
         })
       }
      
       assert(AirportSimulation.getPenalty(singleMinorDelaySmallAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleMinorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getPenalty(singleMinorDelayLargeAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleMinorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY)
       assert(AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY)
       assert(AirportSimulation.getPenalty(singleCancellationSmallAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleCancellationSmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
       assert(AirportSimulation.getPenalty(singleCancellationLargeAirplaneConsumptions.toList) > 0)
       assert(AirportSimulation.getPenalty(singleCancellationLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
       
       //compare severity
       assert(AirportSimulation.getPenalty(singleMinorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleCancellationSmallAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMinorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleCancellationLargeAirplaneConsumptions.toList))
       
       //compare plane size
       assert(AirportSimulation.getPenalty(singleMinorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleMinorDelayLargeAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleCancellationSmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(singleCancellationLargeAirplaneConsumptions.toList))
       
       
       //compare occurrence count
       assert(AirportSimulation.getPenalty(singleMinorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allMinorDelaySmallAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMajorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allMajorDelaySmallAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleCancellationSmallAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allCancellationSmallAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMinorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allMinorDelayLargeAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleMajorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allMajorDelayLargeAirplaneConsumptions.toList))
       assert(AirportSimulation.getPenalty(singleCancellationLargeAirplaneConsumptions.toList) < AirportSimulation.getPenalty(allCancellationLargeAirplaneConsumptions.toList))
       
       //compare all
       assert(AirportSimulation.getPenalty(allMinorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getPenalty(allMajorDelaySmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY)
       assert(AirportSimulation.getPenalty(allCancellationSmallAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
       
       assert(AirportSimulation.getPenalty(allMinorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getPenalty(allMajorDelayLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY)
       assert(AirportSimulation.getPenalty(allCancellationLargeAirplaneConsumptions.toList) < AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
       
       assert(AirportSimulation.getPenalty(allMinorDelayConsumptions.toList) == AirportSimulation.LOYALTY_DECREMENT_BY_MINOR_DELAY)
       assert(AirportSimulation.getPenalty(allMajorDelayConsumptions.toList) == AirportSimulation.LOYALTY_DECREMENT_BY_MAJOR_DELAY)
       assert(AirportSimulation.getPenalty(allCancellationConsumptions.toList) == AirportSimulation.LOYALTY_DECREMENT_BY_CANCELLATION)
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
