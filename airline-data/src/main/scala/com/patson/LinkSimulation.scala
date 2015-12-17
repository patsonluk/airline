package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LinkSimulation {
  private val FUEL_UNIT_COST = 0.12 //for now...
  private val CREW_UNIT_COST = 15 //for now...
  
  private val MAX_LOYALTY_ADJUSTMENT = 0.5
  private[this] val VIP_COUNT = 5
  
  def linkSimulation(cycle: Int) : Map[Int, ListBuffer[LinkConsumptionDetails]] = {
    val links = LinkSource.loadAllLinks(true)
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
    
    //generate link history
    val linkHistory = generateLinkHistory(consumptionResult)
    println("Saving generated history to DB")
    LinkHistorySource.updateLinkHistory(linkHistory)
    
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
    
    val linkConsumptionDetailsByAirline = Map[Int, ListBuffer[LinkConsumptionDetails]]()
    linkConsumptionDetails.foreach { linkConsumption =>
      val consumptionsForThisAirline = linkConsumptionDetailsByAirline.getOrElseUpdate(linkConsumption.airlineId, ListBuffer())
      consumptionsForThisAirline.append(linkConsumption)
    }
    
    //update the loyalty on airports based on link consumption
    println("start updating loyalty")
    val airportSoldLinks = Map[(Int, Int), Set[Link]]() //Map[(airportId, airlineId), links] //cannot use Airport instance directly as they are not the same instance 
    
    links.filter { link => link.capacity > link.availableSeats }.foreach { link =>
      airportSoldLinks.getOrElseUpdate((link.to.id, link.airline.id), Set[Link]()).add(link) 
      airportSoldLinks.getOrElseUpdate((link.from.id, link.airline.id), Set[Link]()).add(link)
    }
    
    val updatingAirports = Map[Int, Airport]()
    airportSoldLinks.foreach {
      case ((airportId, airlineId), links) =>
        val airport = updatingAirports.getOrElseUpdate(airportId, AirportSource.loadAirportById(airportId, true).get)
          
        val totalTransportedPassengers = links.foldLeft(0) { (foldInt, link) => foldInt + (link.capacity - link.availableSeats) } 
        val totalQualityProduct = links.foldLeft(0L) { (foldLong, link) => foldLong + (link.capacity - link.availableSeats) * link.computedQuality }
        val averageQuality = totalQualityProduct.toDouble / totalTransportedPassengers
        
        var loyaltyAdjustment = totalTransportedPassengers / 200.0 / (airport.size * airport.size) //on a size 1 airport, 200 transported passengers is enough to move the loyalty by 1, on size 7 it's 7 * 7 * 1000)
        if (loyaltyAdjustment > MAX_LOYALTY_ADJUSTMENT) {
          loyaltyAdjustment = MAX_LOYALTY_ADJUSTMENT
        }
        val existingLoyalty = airport.getAirlineLoyalty(airlineId)
        if (existingLoyalty < averageQuality) {
           airport.setAirlineLoyalty(airlineId, existingLoyalty + loyaltyAdjustment) 
        } else {
           airport.setAirlineLoyalty(airlineId, existingLoyalty - loyaltyAdjustment)
        }
        println("airport " + airport.name + " airline " + airlineId + " loyalty updating from " + existingLoyalty + " to " + airport.getAirlineLoyalty(airlineId))
    }
    AirportSource.updateAirlineAppeal(updatingAirports.values.toList)
    
    linkConsumptionDetailsByAirline
  }
  
  def computeLinkConsumptionDetail(link : Link, cycle : Int) : LinkConsumptionDetails = {
    val soldSeats = link.capacity - link.availableSeats
    val loadFactor = soldSeats.toDouble / link.capacity
    //val totalFuelBurn = link //fuel burn actually similar to crew cost
    val fuelCost = link.getAssignedModel() match {
      case Some(model) =>
        (if (link.duration <= 30) {
          model.fuelBurn * 10 * link.duration * FUEL_UNIT_COST * link.frequency
        } else {
          (model.fuelBurn * 10 * 30 + model.fuelBurn * (link.duration - 30)) * FUEL_UNIT_COST * link.frequency //first 60 minutes huge burn, then cruising at 1/4 the cost
        } * (0.5 + 0.5 * loadFactor)).toInt //at 0 LF, 50% fuel cost
      case None => 0
    }
       //at 0 LF, reduce fuel consumption by 70%
    val fixedCost = link.getAssignedAirplanes.foldLeft(0)(_ + _.model.maintenanceCost)
    val airportFees = link.getAssignedModel() match {
      case Some(model) => (link.from.slotFee(model) + link.to.slotFee(model) + link.from.landingFee(model) + link.to.landingFee(model)) * link.frequency
      case None => 0 
    }
    val inflightCost = (10 + link.rawQuality * link.duration / 60 / 10) * soldSeats * 2 //10 hours, on top quality flight, cost is 100 per passenger + $10 basic cost . Roundtrip X 2
    val crewCost = link.capacity * link.duration / 60 * CREW_UNIT_COST 
    val revenue = soldSeats * link.price
    val profit = revenue - fuelCost - fixedCost - crewCost - airportFees - inflightCost

    val result = LinkConsumptionDetails(link.id, link.price, link.capacity, soldSeats, fuelCost, crewCost, airportFees, inflightCost, fixedCost, revenue, profit, link.from.id, link.to.id, link.airline.id, link.distance, cycle)
    println("profit : " + result.profit + " result: " + result)
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