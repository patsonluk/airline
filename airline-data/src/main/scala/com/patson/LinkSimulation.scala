package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane.Airplane
import scala.util.Random

object LinkSimulation {
  private val FUEL_UNIT_COST = 0.08//for now...
  private val CREW_UNIT_COST = 12 //for now...
  
  private[this] val VIP_COUNT = 5
  
  def linkSimulation(cycle: Int, links : List[Link]) : List[LinkConsumptionDetails] = {
    //val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)'
    val demand = DemandGenerator.computeDemand()
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

    simulateLinkError(links)
    
    val consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int] = PassengerSimulation.passengerConsume(demand, links)
    //generate statistic 
    println("Generating stats")
    val linkStatistics = generateLinkStatistics(consumptionResult, cycle)
    println("Saving generated stats to DB")
    LinkStatisticsSource.deleteLinkStatisticsBeforeCycle(cycle - 5)
    LinkStatisticsSource.saveLinkStatistics(linkStatistics)
    
    //generate country market share
    println("Generating country market share")
    val countryMarketShares = generateCountryMarketShares(consumptionResult)
    println("Saving country market share to DB")
    CountrySource.saveMarketShares(countryMarketShares)
    
    //save all consumptions
    println("Saving " + consumptionResult.size +  " consumptions")
    ConsumptionHistorySource.updateConsumptions(consumptionResult)
    println("Saved all consumptions")
    //generate link history
//    println("Generating link history")
//    val linkHistory = generateLinkHistory(consumptionResult)
//    println("Saving " + linkHistory.size + " generated history to DB")
//    LinkHistorySource.updateLinkHistory(linkHistory)
    
//    println("Generating VIP")
//    val vipRoutes = generateVipRoutes(consumptionResult)
//    RouteHistorySource.deleteVipRouteBeforeCycle(cycle)
//    RouteHistorySource.saveVipRoutes(vipRoutes, cycle)
    
    println("Calculating profits by links")
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        computeLinkConsumptionDetail(link, cycle) :: foldList
    }
    
    LinkSource.deleteLinkConsumptionsByCycle(30)
    LinkSource.saveLinkConsumptions(linkConsumptionDetails)
    
    linkConsumptionDetails
  }
  
  val minorDelayNormalThreshold = 0.4  // so it's around 24% at 40% condition (multiplier at 0.6) to run into minor delay OR worse
  val majorDelayNormalThreshold = 0.1 // so it's around 6% at 40% condition (multiplier at 0.6) to run into major delay OR worse    
  val cancellationNormalThreshold = 0.03 // so it's around 1.8% at 40% condition (multiplier at 0.6) to run into cancellation
  val minorDelayBadThreshold = 0.5  // so it's around 40% at 20% condition (multiplier at 0.8) to run into minor delay OR worse
  val majorDelayBadThreshold = 0.2 // so it's around 16% at 20% condition (multiplier at 0.8) to run into major delay OR worse    
  val cancellationBadThreshold = 0.1 // so it's around 8% at 20% condition (multiplier at 0.8) to run into cancellation
  val minorDelayCriticalThreshold = 1  // so it's around 100% at 0% condition (multiplier at 1) to run into minor delay OR worse
  val majorDelayCriticalThreshold = 0.5 // so it's around 50% at 0% condition (multiplier at 1) to run into major delay OR worse    
  val cancellationCriticalThreshold = 0.3 // so it's around 30% at 0% condition (multiplier at 1) to run into cancellation
  
  def simulateLinkError(links : List[Link]) = {
    links.foreach {
      link => {
        var i = 0
        for ( i <- 0 until link.frequency) {
          var airplaneCount = link.getAssignedAirplanes().length
          if (airplaneCount > 0) {
            val airplane = link.getAssignedAirplanes()(i % airplaneCount)           //round robin
            val errorValue = Random.nextDouble()
            val conditionMultipler = (Airplane.MAX_CONDITION - airplane.condition).toDouble / Airplane.MAX_CONDITION
            var minorDelayThreshold : Double = 0
            var majorDelayThreshold : Double = 0
            var cancellationThreshold : Double = 0
            if (airplane.condition > Airplane.BAD_CONDITION) { //small chance of delay and cancellation
              if (errorValue < cancellationNormalThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayNormalThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayNormalThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }
            } else if (airplane.condition > Airplane.CRITICAL_CONDITION) {
              if (errorValue < cancellationBadThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayBadThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayBadThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }
            } else { 
              if (errorValue < cancellationCriticalThreshold * conditionMultipler) {
                link.cancellationCount = link.cancellationCount + 1
              } else if (errorValue < majorDelayCriticalThreshold * conditionMultipler) {
                link.majorDelayCount = link.majorDelayCount + 1
              } else if (errorValue < minorDelayCriticalThreshold * conditionMultipler) {
                link.minorDelayCount = link.minorDelayCount + 1
              }         
            }
          }
        }
      }
      if (link.cancellationCount > 0) {
        link.addCancelledSeats(link.capacityPerFlight() * link.cancellationCount)
      }
    }
  }
  
  def computeLinkConsumptionDetail(link : Link, cycle : Int) : LinkConsumptionDetails = {
    
    val loadFactor = link.getTotalSoldSeats.toDouble / link.getTotalCapacity
    
    //val totalFuelBurn = link //fuel burn actually similar to crew cost
    val fuelCost = link.getAssignedModel() match {
      case Some(model) =>
        (if (link.duration <= 90) {
          val ascendTime, descendTime = (link.duration / 2)
          (model.fuelBurn * 10 * ascendTime + model.fuelBurn * descendTime) * FUEL_UNIT_COST * link.frequency 
        } else {
          (model.fuelBurn * 10 * 45 + model.fuelBurn * (link.duration - 30)) * FUEL_UNIT_COST * link.frequency //first 60 minutes huge burn, then cruising at 1/4 the cost
        } * (0.7 + 0.3 * loadFactor)).toInt //at 0 LF, 70% fuel cost
      case None => 0
    }

    val maintenanceCost = (link.getAssignedAirplanes.foldLeft(0)(_ + _.model.maintenanceCost) * link.airline.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY).toInt
    val airportFees = link.getAssignedModel() match {
      case Some(model) =>
        val airline = link.airline
        (link.from.slotFee(model, airline) + link.to.slotFee(model, airline) + link.from.landingFee(model) + link.to.landingFee(model)) * link.frequency
      case None => 0 
    }
    
    val depreciation = link.getAssignedAirplanes().foldLeft(0)(_ + _.depreciationRate)
    
    var inflightCost, crewCost, revenue = 0 
    link.capacity.map.keys.foreach { linkClass =>
      val capacity = link.capacity(linkClass)
      val soldSeats = link.soldSeats(linkClass)
      
      inflightCost += (linkClass.resourceMultiplier * (10 + link.rawQuality * link.duration / 60 / 10) * soldSeats * 2).toInt //10 hours, on top quality flight, cost is 100 per passenger + $10 basic cost . Roundtrip X 2
      crewCost += (linkClass.resourceMultiplier * capacity * link.duration / 60 * CREW_UNIT_COST).toInt 
      revenue += soldSeats * link.price(linkClass)
    }
    
    // delays incur extra cost
    var delayCompensation = Computation.computeCompensation(link)
    
    val profit = revenue - fuelCost - maintenanceCost - crewCost - airportFees - inflightCost - delayCompensation - depreciation

    //val result = LinkConsumptionDetails(link.id, link.price, link.capacity, link.soldSeats, link.computedQuality, fuelCost, crewCost, airportFees, inflightCost, delayCompensation = delayCompensation, maintenanceCost, depreciation = depreciation, revenue, profit, link.cancellationCount, linklink.from.id, link.to.id, link.airline.id, link.distance, cycle)
    val result = LinkConsumptionDetails(link, fuelCost, crewCost, airportFees, inflightCost, delayCompensation = delayCompensation, maintenanceCost, depreciation = depreciation, revenue, profit, cycle)
    //println("model : " + link.getAssignedModel().get + " profit : " + result.profit + " result: " + result)
    result
  }
  
  def generateLinkStatistics(consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int], cycle : Int) : List[LinkStatistics] = {
    val statistics = Map[LinkStatisticsKey, Int]()
    consumptionResult.foreach {
      case ((_, _, route), passengerCount) =>
        for (i <- 0 until route.links.size) {
          val link = route.links(i) 
          val airline = link.link.airline
          val key = 
            if (i == 0) {
              if (route.links.size == 1) {
                LinkStatisticsKey(link.from, link.to, true, true, airline)  
              } else {
                LinkStatisticsKey(link.from, link.to, true, false, airline)
              }
            } else if (i == route.links.size -1) { //last one in list
              LinkStatisticsKey(link.from, link.to, false, true, airline)
            } else { //in the middle
              LinkStatisticsKey(link.from, link.to, false, false, airline)
            }
          val newPassengerCount = statistics.getOrElse(key, 0) + passengerCount
          statistics.put(key, newPassengerCount)
        }
    }
    
    statistics.map { 
      case (linkStatisticsKey, passenger) =>
        LinkStatistics(linkStatisticsKey, passenger, cycle)
    }.toList
    
  }
  
  def generateCountryMarketShares(consumptionResult: scala.collection.immutable.Map[(PassengerGroup, Airport, Route), Int]) : List[CountryMarketShare] = {
    val countryAirlinePassengers = Map[String, Map[Int, Long]]()
    consumptionResult.foreach {
      case ((_, _, route), passengerCount) =>
        for (i <- 0 until route.links.size) {
          val link = route.links(i) 
          val airline = link.link.airline
          val country = link.from.countryCode
          val airlinePassengers = countryAirlinePassengers.getOrElseUpdate(country, Map[Int, Long]())
          val currentSum : Long = airlinePassengers.getOrElse(airline.id, 0L)
          airlinePassengers.put(airline.id, currentSum + passengerCount)
        }
    }
    
    
    
    countryAirlinePassengers.map {
      case ((countryCode, airlinePassengers)) => { 
        CountryMarketShare(countryCode, airlinePassengers.toMap)
      }
    }.toList
    
  }
}