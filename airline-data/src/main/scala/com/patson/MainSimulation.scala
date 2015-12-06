

package com.patson

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import scala.util.Random
import scala.concurrent.Future
import com.patson.data._
import com.patson.model._
import scala.collection.mutable.Map
import akka.actor.Actor
import akka.actor.Props
import java.util.concurrent.TimeUnit

object MainSimulation extends App {
  
  
  val FUEL_UNIT_COST = 0.2 //for now...
  val AIRLINE_FIXED_COST = 10000 //for now...
  val CREW_UNIT_COST = 10 //for now...
  
  val AWARENESS_DECAY = 0.1
  val AWARENESS_INCREMENT_WITH_LINKS = 0.2
  val AWARENESS_INCREMENT_WITH_HQ = 0.3
  val AWARENESS_INCREMENT_WITH_BASE = 0.1
  val LOYALTY_DECAY = 0.01
  val LOYALTY_AUTO_INCREMENT_WITH_HQ = 0.05
  val LOYALTY_AUTO_INCREMENT_WITH_BASE = 0.02
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ = 30 //how much loyalty will increment to just because of being a HQ
  val LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE = 15 //how much loyalty will increment to just because of being a HQ
  
  val MAX_LOYALTY_ADJUSTMENT = 0.5
  
  private[this] val VIP_COUNT = 5 
  
//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    import actorSystem.dispatcher
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(1, TimeUnit.MINUTES), actor, Start)
  }
  
  def startCycle(cycle : Int) = {
      val links = LinkSource.loadAllLinks(true)
      airportSimulation(cycle) 
      linkSimulation(cycle)  
  }
  
  def airportSimulation(cycle: Int) = {
    
    //do decay
    val allAirports = AirportSource.loadAllAirports(true)
    
    
    val basesByAirport : Map[Int, List[AirlineBase]] = AirlineSource.loadAirlineBasesByCriteria(List.empty).foldLeft(Map[Int, List[AirlineBase]]()) { (foldMap, airlineBase) =>
      val bases = foldMap.getOrElse(airlineBase.airport.id, List[AirlineBase]())
      foldMap + (airlineBase.airport.id -> (airlineBase :: bases))
    }
    
    //decay awareness and loyalty
    allAirports.foreach { airport =>
      airport.getAirlineAppeals().foreach { 
        case(airline, AirlineAppeal(loyalty, awareness)) =>
          //decay    
          val newLoyalty = if (loyalty - LOYALTY_DECAY <= 0) 0 else loyalty - LOYALTY_DECAY
          val newAwareness = if (awareness - AWARENESS_DECAY <= 0) 0 else awareness - AWARENESS_DECAY
          
          airport.setAirlineLoyalty(airline, newLoyalty)
          airport.setAirlineAwareness(airline, newAwareness)
      }
       //add base on bases
      basesByAirport.get(airport.id).foreach { _.foreach { base =>
          var newAwareness : Double = airport.getAirlineAwareness(base.airline.id)
          var newLoyalty : Double = airport.getAirlineLoyalty(base.airline.id)
          if (base.headquarter) {
            newAwareness += AWARENESS_INCREMENT_WITH_HQ
            if (newLoyalty < LOYALTY_AUTO_INCREMENT_MAX_WITH_HQ) {
              newLoyalty += LOYALTY_AUTO_INCREMENT_WITH_HQ
            }
          } else {
             newAwareness += AWARENESS_INCREMENT_WITH_BASE
             if (newLoyalty < LOYALTY_AUTO_INCREMENT_MAX_WITH_BASE) {
               newLoyalty += LOYALTY_AUTO_INCREMENT_WITH_BASE
             }
          }
          if (newAwareness > AirlineAppeal.MAX_AWARENESS) {
            newAwareness = AirlineAppeal.MAX_AWARENESS
          }
          if (newLoyalty > AirlineAppeal.MAX_LOYALTY) {
            newLoyalty = AirlineAppeal.MAX_LOYALTY
          }
          airport.setAirlineAwareness(base.airline.id, newAwareness)
          airport.setAirlineLoyalty(base.airline.id, newLoyalty)
        }
      }
    }
      
    AirportSource.updateAirlineAppeal(allAirports)
        
    //increment of awareness
    val links = LinkSource.loadAllLinks(true) 
    
    val airportWithLinks = Map[(Airport, Airline), Set[Link]]()
    links.foreach { link =>
      val airlinesFlyiesFromThisAirport = airportWithLinks.getOrElseUpdate((link.from, link.airline), Set[Link]())
      airlinesFlyiesFromThisAirport.add(link)
      
      val airlinesFlyiesToThisAirport = airportWithLinks.getOrElseUpdate((link.to, link.airline), Set[Link]())
      airlinesFlyiesToThisAirport.add(link)
    }
    
    val updatingAirports = Set[Airport]()
    //add awareness based on airline with some links to/from an airport
    airportWithLinks.keySet.foreach {
      case(airport, airline) =>
        val existingAwareness = airport.getAirlineAwareness(airline.id)
        val newAwareness = 
          if ((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) >= AirlineAppeal.MAX_AWARENESS) {
            AirlineAppeal.MAX_AWARENESS   
          } else {
            existingAwareness + AWARENESS_INCREMENT_WITH_LINKS
          }
        airport.setAirlineAwareness(airline.id, newAwareness)
        updatingAirports.add(airport)
    }
    
    AirportSource.updateAirlineAppeal(updatingAirports.toList);
  }
  
  def linkSimulation(cycle: Int) = {
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
    
    println("Generating VIP")
    val vipRoutes = generateVipRoutes(consumptionResult)
    RouteHistorySource.deleteVipRouteBeforeCycle(cycle)
    RouteHistorySource.saveVipRoutes(vipRoutes, cycle)
    
    println("Calculating profits by links")
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        computeLinkConsumptionDetail(link, cycle) :: foldList
    }
    
    LinkSource.deleteLinkConsumptionsByCycle(10)
    LinkSource.saveLinkConsumptions(linkConsumptionDetails)
    
    val linkConsumptionDetailsByAirline = Map[Int, ListBuffer[LinkConsumptionDetails]]()
    linkConsumptionDetails.foreach { linkConsumption =>
      val consumptionsForThisAirline = linkConsumptionDetailsByAirline.getOrElseUpdate(linkConsumption.airlineId, ListBuffer())
      consumptionsForThisAirline.append(linkConsumption)
    }
    
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    allAirlines.foreach { airline =>
        val profit = linkConsumptionDetailsByAirline.get(airline.id) match {
          case Some(linkConsumptions) => 
            linkConsumptions.foldLeft(0L)(_ + _.profit) - AIRLINE_FIXED_COST
          case None=>  
            0 - AIRLINE_FIXED_COST
              
        }
        AirlineSource.adjustAirlineBalance(airline.id, profit)
      //  airlineProfit.put(airline, profit)
          
        println(airline + " profit is: " + profit + " new balance is " + (airline.airlineInfo.balance + profit))
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
    
  }
  
  def computeLinkConsumptionDetail(link : Link, cycle : Int) : LinkConsumptionDetails = {
    val soldSeats = link.capacity - link.availableSeats
    val totalFuelBurn = link.getAssignedAirplanes.foldLeft(0)(_ + _.model.fuelBurn) //fuel burn actually similar to crew cost
    val fuelCost = (
      if (link.duration <= 60) {
        totalFuelBurn * 10 * link.duration * FUEL_UNIT_COST * link.frequency
      } else {
        (totalFuelBurn * 10 * 60 + totalFuelBurn * (link.duration - 60)) * FUEL_UNIT_COST * link.frequency //first 60 minutes huge burn, then cruising at 1/10 the cost
      }).toInt
    val fixedCost = 1000
    val crewCost = link.capacity * link.duration / 60 * CREW_UNIT_COST 
    val revenue = soldSeats * link.price
    val profit = revenue - fuelCost - fixedCost - crewCost

    LinkConsumptionDetails(link.id, link.price, link.capacity, soldSeats, fuelCost, crewCost, fixedCost, revenue, profit, link.from.id, link.to.id, link.airline.id, link.distance, cycle)  
  }
  
  class MainSimulationActor extends Actor {
    var currentWeek = CycleSource.loadCycle()
    def receive = {
      case Start =>
        startCycle(currentWeek)
        currentWeek += 1
        CycleSource.setCycle(currentWeek)
    }
  }
  
  case class Start()

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

