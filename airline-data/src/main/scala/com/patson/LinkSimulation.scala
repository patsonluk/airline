package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LinkSimulation {
  private val FUEL_UNIT_COST = 0.08//for now...
  private val CREW_UNIT_COST = 12 //for now...
  
  private[this] val VIP_COUNT = 5
  
  def linkSimulation(cycle: Int) : List[LinkConsumptionDetails] = {
    println("Loading all links")
    val links = LinkSource.loadAllLinks(LinkSource.FULL_LOAD)
    println("Finished loading all links")
    val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

    val consumptionResult: List[(PassengerGroup, Airport, Int, Route)] = PassengerSimulation.passengerConsume(demand, links)
    //generate statistic 
    println("Generating stats")
    val linkStatistics = generateLinkStatistics(consumptionResult, cycle)
    println("Saving generated stats to DB")
    LinkStatisticsSource.deleteLinkStatisticsBeforeCycle(cycle - 5)
    LinkStatisticsSource.saveLinkStatistics(linkStatistics)
    
    //save all consumptions
    println("Saving all consumptions")
    ConsumptionHistorySource.updateConsumptions(consumptionResult)
    println("Saved all consumptions")
    //generate link history
//    println("Generating link history")
//    val linkHistory = generateLinkHistory(consumptionResult)
//    println("Saving " + linkHistory.size + " generated history to DB")
//    LinkHistorySource.updateLinkHistory(linkHistory)
    
    println("Generating VIP")
    val vipRoutes = generateVipRoutes(consumptionResult)
    RouteHistorySource.deleteVipRouteBeforeCycle(cycle)
    RouteHistorySource.saveVipRoutes(vipRoutes, cycle)
    
    println("Calculating profits by links")
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        computeLinkConsumptionDetail(link, cycle) :: foldList
    }
    
    LinkSource.deleteLinkConsumptionsByCycle(30)
    LinkSource.saveLinkConsumptions(linkConsumptionDetails)
    
    linkConsumptionDetails
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
      val soldSeats = capacity - link.availableSeats(linkClass)
      
      inflightCost += (linkClass.resourceMultiplier * (10 + link.rawQuality * link.duration / 60 / 10) * soldSeats * 2).toInt //10 hours, on top quality flight, cost is 100 per passenger + $10 basic cost . Roundtrip X 2
      crewCost += (linkClass.resourceMultiplier * capacity * link.duration / 60 * CREW_UNIT_COST).toInt 
      revenue += soldSeats * link.price(linkClass)
    }
    
    val profit = revenue - fuelCost - maintenanceCost - crewCost - airportFees - inflightCost - depreciation

    val result = LinkConsumptionDetails(link.id, link.price, link.capacity, link.soldSeats, link.computedQuality, fuelCost, crewCost, airportFees, inflightCost, maintenanceCost, depreciation, revenue, profit, link.from.id, link.to.id, link.airline.id, link.distance, cycle)
    //println("model : " + link.getAssignedModel().get + " profit : " + result.profit + " result: " + result)
    result
  }
  
  def generateLinkStatistics(consumptionResult: List[(PassengerGroup, Airport, Int, Route)], cycle : Int) : List[LinkStatistics] = {
    val statistics = Map[LinkStatisticsKey, Int]()
    consumptionResult.foreach {
      case (_, _, passengerCount, route) =>
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
  
  def generateLinkHistory(consumptionResult: List[(PassengerGroup, Airport, Int, Route)]) : List[LinkHistory] = {
    val linkHistoryMap = Map[(Int), Map[(Int, Airport, Airport, Airline, Boolean), Int]]() // [(watchedLink), [(linkId, fromAiport, toAirport, airline, watchedLinkInverted), totalPassengers]]
    
    val watchedLinkIds : List[Int] = LinkHistorySource.loadAllWatchedLinkIds()
    
    consumptionResult.foreach {
      case (_, _, passengerCount, route) =>
        val watchedLinks = route.links.map { linkWithCost => linkWithCost.link.id }.intersect(watchedLinkIds)
        watchedLinks.foreach { watchedLinkId =>
          val watchedLink = route.links.find( _.link.id == watchedLinkId).get
          val relatedLinksMap = linkHistoryMap.getOrElseUpdate(watchedLinkId, Map[(Int, Airport, Airport, Airline, Boolean), Int]()) //can't use link/linkWithCost directly as linkWithCost has directions
          route.links.foreach { linkInRoute =>
            val linkKey = (linkInRoute.link.id, linkInRoute.from, linkInRoute.to, linkInRoute.link.airline, watchedLink.inverted)
            val existingPassengersForThisLink = relatedLinksMap.getOrElse(linkKey, 0)
            relatedLinksMap.put(linkKey, existingPassengersForThisLink + passengerCount)
          }
        }
    }
    
    linkHistoryMap.map {
      case(watchedLinkId, relatedLinksMap) =>
        val relatedLinks = Set[RelatedLink]()
        val invertedRelatedLinks = Set[RelatedLink]()
        relatedLinksMap.foreach { 
          case((linkId, fromAirport, toAirport, airline, inverted), passenger) =>
            if (!inverted) {
              relatedLinks += RelatedLink(linkId, fromAirport, toAirport, airline, passenger)
            } else {
              invertedRelatedLinks += RelatedLink(linkId, fromAirport, toAirport, airline, passenger)
            }
        }
        LinkHistory(watchedLinkId, relatedLinks.toSet, invertedRelatedLinks.toSet)
    }.toList
  }
  
  
  
  def generateVipRoutes(consumptionResult: List[(PassengerGroup, Airport, Int, Route)]) : List[Route] = {
    //simply take the first VIP_COUNT records for now
    (if (consumptionResult.length < VIP_COUNT) {
      consumptionResult
    } else {
      consumptionResult.take(VIP_COUNT)
    }).map{ 
        case (_, _, _, route) => route
      }
  }
}