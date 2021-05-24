package com.patson

import com.patson.model._
import com.patson.model.airplane._
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable.ListBuffer
 
class AirportSimulationSpec extends WordSpecLike with Matchers {
  val sampleLink = Link.fromId(1)
  val sampleConsumption = LinkConsumptionDetails(link = sampleLink, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, delayCompensation = 0, maintenanceCost = 0, loungeCost = 0, depreciation = 0, revenue = 0, profit = 0, satisfaction = 0, cycle = 0)
  
//  "getTargetLoyalty".must {
//    "get target loyalty based on average quality link consumption if volume is huge".in {
//       assert(AirportSimulation.getTargetLoyalty(List.empty, 1000000) == 0) //0
//
//       val link1 = sampleLink.copy()
//       val link2 = sampleLink.copy()
//       val link3 = sampleLink.copy()
//
//       link1.soldSeats = LinkClassValues.getInstance(100000, 100000, 100000)
//       link1.setQuality(50)
//       link2.setQuality(100)
//       link3.soldSeats = LinkClassValues.getInstance(100000, 100000, 100000)
//       link3.setQuality(0)
//
//
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link1),
//                                                      sampleConsumption.copy(link = link2),
//                                                      sampleConsumption.copy(link = link3)), 1000000) == 25)
//
//
//    }
//    "get target loyalty based on passenger volume if quality is high".in {
//       val link = sampleLink.copy()
//       link.setQuality(100)
//
//       link.soldSeats = LinkClassValues.getInstance(10, 0, 0)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 1)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 20)
//
//       link.soldSeats = LinkClassValues.getInstance(100, 0, 0)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 20)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 40)
//
//       link.soldSeats = LinkClassValues.getInstance(1000, 0, 0)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 40)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 60)
//
//       link.soldSeats = LinkClassValues.getInstance(10000, 0, 0)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 60)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 80)
//
//       link.soldSeats = LinkClassValues.getInstance(15000, 0, 0)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) > 80)
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) < 100)
//
//       link.soldSeats = LinkClassValues.getInstance(1000000 / 52, 0, 0) //least amount of passenger to get full loyalty
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100)
//
//       link.soldSeats = LinkClassValues.getInstance(100000, 0, 0) //hit ceiling
//       assert(AirportSimulation.getTargetLoyalty(List(sampleConsumption.copy(link = link)), 1000000) == 100)
//    }
  "getPenalty".must {
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
        link.setTestingAssignedAirplanes(Map(airplane -> frequency))
        consumptions.append(LinkConsumptionDetails(link = link, fuelCost = 0, crewCost = 0, airportFees = 0, inflightCost = 0, delayCompensation = 0, maintenanceCost = 0, loungeCost = 0, depreciation = 0, revenue = 0, profit = 0, satisfaction = 0, cycle = 0))
      }
      
      val consumption = 
       //no penalty
       assert(AirportSimulation.getPenalty(consumptions.toList) == 0)
       
       val singleMinorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.minorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       val singleMinorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.minorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       val singleMajorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.majorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleMajorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.majorDelayCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleCancellationSmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.cancellationCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val singleCancellationLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.cancellationCount = 1
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMinorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.minorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       val allMinorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.minorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       val allMajorDelaySmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.majorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMajorDelayLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.majorDelayCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allCancellationSmallAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == smallAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.cancellationCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allCancellationLargeAirplaneConsumptions = consumptions.map { consumption =>
         if (consumption.link.asInstanceOf[Link].getAssignedModel().get == largeAirplaneModel) {
           consumption.copy(link = {
             val newLink = consumption.link.asInstanceOf[Link].copy()
             newLink.cancellationCount = consumption.link.frequency
             newLink
           })
         } else {
           consumption
         }
       }
       
       val allMinorDelayConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.asInstanceOf[Link].copy()
           newLink.minorDelayCount = consumption.link.frequency
           newLink
         })
       }
       
       val allMajorDelayConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.asInstanceOf[Link].copy()
           newLink.majorDelayCount = consumption.link.frequency
           newLink
         })
       }
       
       val allCancellationConsumptions = consumptions.map { consumption =>
         consumption.copy(link = {
           val newLink = consumption.link.asInstanceOf[Link].copy()
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
//  "getNewLoyalty".must {
//    "increment loyalty correctly".in {
//       val population = 1000000
//       val weeklyPassenger = 1000000 / 52
//
//       val link = sampleLink.copy()
//       link.soldSeats = LinkClassValues.getInstance(weeklyPassenger, 0, 0)
//       link.setQuality(100)
//       val consumptions = List(sampleConsumption.copy(link = link))
//
//       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //cannot increase any further
//       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY - 0.01, AirlineAppeal.MAX_LOYALTY) == AirlineAppeal.MAX_LOYALTY) //increaes to max
//       assert(AirportSimulation.getNewLoyalty(0, AirportSimulation.LOYALTY_INCREMENT_BY_FLIGHTS + 1) == AirportSimulation.LOYALTY_INCREMENT_BY_FLIGHTS) //increase by AirportSimulation.LOYALTY_INCREMENT only
//    }
//    "decrement loyalty correctly".in {
//       assert(AirportSimulation.getNewLoyalty(0, 0) == 0) //cannot decrease any further
//       assert(AirportSimulation.getNewLoyalty(0.5, 0) == 0) //decrease to 0
//       assert(AirportSimulation.getNewLoyalty(AirlineAppeal.MAX_LOYALTY, 0) == AirlineAppeal.MAX_LOYALTY - AirportSimulation.LOYALTY_DECREMENT_BY_FLIGHTS)
//    }
//  }

  "computeLoyalists".must {
    val airport1 = Airport("", "", "Test Airport 1", 0, 0 , "", "", "", size = 1, power = 10000 * 10L, population = 10000L, 0, 0, id = 1)
    val airport2 = Airport("", "", "Test Airport 2", 0, 0 , "", "", "", size = 1, power = 100000 * 10L, population = 100000L, 0, 0, id = 2)
    val airport3 = Airport("", "", "Test Airport 3", 0, 0 , "", "", "", size = 1, power = 100000 * 10L, population = 100000L, 0, 0, id = 3)
    val airline1 = Airline.fromId(1)
    val airline2 = Airline.fromId(2)
    val airline3 = Airline.fromId(3)
    val airline4 = Airline.fromId(4)
    val airline1Link1 = Link(airport1, airport2, airline1, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1)
    val badAirline1Link1 = LinkConsideration(airline1Link1, 100000, ECONOMY, false, 0)
    val goodAirline1Link1 = LinkConsideration(airline1Link1, 0, ECONOMY, false, 0)
    val airline2Link2 = Link(airport2, airport3, airline2, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 2)
    val badAirline2Link2 = LinkConsideration(airline2Link2, 100000, ECONOMY, false, 0)
    val goodAirline2Link2 = LinkConsideration(airline2Link2, 0, ECONOMY, false, 0)
    val badRoute = Route(List(badAirline1Link1, badAirline2Link2), 0)
    val goodRoute = Route(List(goodAirline1Link1, goodAirline2Link2), 0)
    val passengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, 0, 1, 1), PassengerType.BUSINESS)
    val allAirports = List(airport1, airport2, airport3)
    "Do nothing if there's no consumption".in {
      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(allAirports, Map.empty, Map(1 -> List(Loyalist(airport1, airline1, 5)), 2 -> List(Loyalist(airport1, airline2, 50))))
      assert(updatingLoyalists.isEmpty)
      assert(deletingLoyalist.isEmpty)
    }

    "Do nothing if there's bad links but no existing loyalists".in {
      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(allAirports, Map((passengerGroup, airport3, badRoute) -> 100), Map.empty)
      assert(updatingLoyalists.isEmpty)
      assert(deletingLoyalist.isEmpty)
    }

//    "Delete all loyalists if there's bad links but a few remaining loyalists".in {
//      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
//        allAirports,
//        Map((passengerGroup, airport3, badRoute) -> 100),
//        Map(1 -> List(Loyalist(airport1, airline1, 1), Loyalist(airport1, airline2, 2))))
//      assert(updatingLoyalists.isEmpty)
//      assert(deletingLoyalist.length == 2)
//      assert(deletingLoyalist.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).isDefined)
//      assert(deletingLoyalist.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).isDefined)
//    }

//    "Delete some loyalists if there's bad links but have remaining loyalists".in {
//      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
//        allAirports,
//        Map((passengerGroup, airport3, badRoute) -> 100),
//        Map(1 -> List(Loyalist(airport1, airline1, 200), Loyalist(airport1, airline2, 1000))))
//      assert(updatingLoyalists.length == 2)
//      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount == 200 - (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
//      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).get.amount == 1000 - (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
//
//      assert(deletingLoyalist.isEmpty)
//    }

    "Gain loyalists if there's good links but a no existing loyalists".in {
      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
        allAirports,
        Map((passengerGroup, airport3, goodRoute) -> 100),
        Map.empty)
      assert(updatingLoyalists.length == 2)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount == (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).get.amount == (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)

      assert(deletingLoyalist.isEmpty)
    }

    "Gain loyalists if there's good links but are existing loyalists, flip loyalists from other airlines".in {
      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
        allAirports,
        Map((passengerGroup, airport3, goodRoute) -> 100),
        Map(1 -> List(Loyalist(airport1, airline3, 10000))))
      assert(updatingLoyalists.length == 3)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount == (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).get.amount == (100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
      //this does not make too much sense cause essentially it's the same pax, but for sim purpose this is okay. Otherwise we would need decimal points
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline3.id).get.amount == 10000 - 2 * ((100 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt))

      assert(deletingLoyalist.isEmpty)
    }

    "Gain loyalists if there's good links but are existing loyalists, flip loyalists from other airlines - the one with MORE loyalists get bigger loss".in {
      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
        allAirports,
        Map((passengerGroup, airport3, goodRoute) -> 1000),
        Map(1 -> List(Loyalist(airport1, airline3, 3000), Loyalist(airport1, airline4, 7000))))
      assert(updatingLoyalists.length == 4)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount == (1000 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).get.amount == (1000 * AirportSimulation.MAX_LOYALIST_FLIP_RATIO / 2).toInt)

      val airline3Delta = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline3.id).get.amount - 3000
      val airline4Delta = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline4.id).get.amount - 7000
      assert(airline3Delta < 0)
      assert(airline4Delta < 0)
      assert(airline3Delta > airline4Delta) //airline 4 should lose more

      assert(deletingLoyalist.isEmpty)
    }

    "Only Gain in loyalists to max pop".in {
      val (updatingLoyalists, deletingLoyalists) = AirportSimulation.computeLoyalists(
        allAirports,
        Map((passengerGroup, airport2, Route(List(goodAirline1Link1), 0)) -> 1000),
        Map(1 -> List(Loyalist(airport1, airline1, airport1.population.toInt - 50))))
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount >= airport1.population.toInt - 50)
      assert(updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount <= airport1.population.toInt)
    }

    "No Gain in loyalists if there's only one airline and it's already at max".in {
      val (updatingLoyalists, deletingLoyalists) = AirportSimulation.computeLoyalists(
        allAirports,
        Map((passengerGroup, airport2, Route(List(goodAirline1Link1), 0)) -> 1000),
        Map(1 -> List(Loyalist(airport1, airline1, airport1.population.toInt))))
      assert(updatingLoyalists.isEmpty)
    }


    "Around net zero if both airlines have similar parameters (4 airlines)".in {
      val linkConsideration1 = goodAirline1Link1
      val airport1 = Airport("", "", "Test Airport 1", 0, 0 , "", "", "", size = 1, power = 10000 * 4000000L, population = 4000000L, 0, 0, id = 1)
      val passengerGroup = PassengerGroup(airport1, AppealPreference(airport1, ECONOMY, 0, 1, 1), PassengerType.BUSINESS)
      val linkConsideration2 = linkConsideration1.copy(link = Link(airport1, airport2, airline2, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1))
      val linkConsideration3 = linkConsideration1.copy(link = Link(airport1, airport2, airline3, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1))
      val linkConsideration4 = linkConsideration1.copy(link = Link(airport1, airport2, airline4, LinkClassValues.getInstance(), 1000, LinkClassValues.getInstance(), 0, 0, 0, FlightType.SHORT_HAUL_DOMESTIC, 0, 1))

      val (updatingLoyalists, deletingLoyalist) = AirportSimulation.computeLoyalists(
        allAirports,
        Map(
          (passengerGroup, airport2, Route(List(linkConsideration1), 0)) -> 1000000,
          (passengerGroup, airport2, Route(List(linkConsideration2), 0)) -> 1000000,
          (passengerGroup, airport2, Route(List(linkConsideration3), 0)) -> 1000000,
          (passengerGroup, airport2, Route(List(linkConsideration4), 0)) -> 1000000
        ),
        Map(1 -> List(Loyalist(airport1, airline1, 1000000), Loyalist(airport1, airline2, 1000000), Loyalist(airport1, airline3, 1000000), Loyalist(airport1, airline4, 1000000))))
      val airline1Loyalist = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline1.id).get.amount
      val airline2Loyalist = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline2.id).get.amount
      val airline3Loyalist = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline3.id).get.amount
      val airline4Loyalist = updatingLoyalists.find(loyalist => loyalist.airport.id == airport1.id && loyalist.airline.id == airline4.id).get.amount
      assert(airline1Loyalist + airline2Loyalist  + airline3Loyalist + airline4Loyalist == 4000000)
      println(s"$airline1Loyalist vs $airline2Loyalist vs $airline3Loyalist vs $airline4Loyalist")
      assert(airline1Loyalist > 990000 && airline1Loyalist < 1010000) //around the same
      assert(airline2Loyalist > 990000 && airline1Loyalist < 1010000) //around the same
      assert(airline3Loyalist > 990000 && airline1Loyalist < 1010000) //around the same
      assert(airline4Loyalist > 990000 && airline1Loyalist < 1010000) //around the same


      assert(deletingLoyalist.isEmpty)
    }
  }
}
