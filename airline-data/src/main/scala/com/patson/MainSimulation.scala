

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
  val AWARENESS_INCREMENT_WITH_LINKS = 0.3
  val LOYALTY_DECAY = 0.01
  val MAX_LOYALTY_ADJUSTMENT = 0.5
  
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
    //decay awareness and loyalty
    allAirports.foreach { airport =>
      val updatedAppeals = airport.airlineAppeals.map { 
        case(airline, AirlineAppeal(loyalty, awareness)) =>
          val newLoyalty = if (loyalty - LOYALTY_DECAY <= 0) 0 else loyalty - LOYALTY_DECAY
          val newAwareness = if (awareness - AWARENESS_DECAY <= 0) 0 else awareness - AWARENESS_DECAY
          (airline, AirlineAppeal(newLoyalty, newAwareness))
      }.filter { 
        case (airline, AirlineAppeal(newLoyalty, newAwareness)) => newLoyalty > 0 || newAwareness > 0
      }
      airport.airlineAppeals.clear()
      airport.airlineAppeals ++= updatedAppeals
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
        val existingAwareness = airport.airlineAppeals.get(airline).map { _.awareness }.getOrElse(0.0)
        val newAwareness = 
          if ((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) >= AirlineAppeal.MAX_AWARENESS) {
            AirlineAppeal.MAX_AWARENESS   
          } else {
            (((existingAwareness + AWARENESS_INCREMENT_WITH_LINKS) * 10).toInt).toDouble / 10
          }
        airport.setAirlineAwareness(airline, newAwareness)
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

    val consumptionResult = PassengerSimulation.passengerConsume(demand, links)
//    println("Consumption result : ")
//    consumptionResult.foreach { 
//      case(_, _, _, links) => println(links)
//    }
    
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        val soldSeats = link.capacity - link.availableSeats
        val totalFuelBurn = link.assignedAirplanes.foldLeft(0)(_ + _.model.fuelBurn) //fuel burn actually similar to crew cost
        val fuelCost = (
          if (link.duration <= 60) {
            totalFuelBurn * 10 * link.duration * FUEL_UNIT_COST * link.frequency
          } else {
            (totalFuelBurn * 10 * 60 + totalFuelBurn * (link.duration - 60)) * FUEL_UNIT_COST * link.frequency //first 60 minutes huge burn, then crusing at 1/10 the cost
          }).toInt
        val fixedCost = 1000
        val crewCost = link.capacity * link.duration / 60 * CREW_UNIT_COST 
        val revenue = soldSeats * link.price
        val profit = revenue - fuelCost - fixedCost - crewCost
        val consumption = LinkConsumptionDetails(link.id, link.price, link.capacity, soldSeats, fuelCost, crewCost, fixedCost, revenue, profit, link.from.id, link.to.id, link.airline.id, link.distance, cycle)
        consumption :: foldList
    }
    
    LinkSource.deleteLinkConsumptionsByCycle(10)
    LinkSource.saveLinkConsumptions(linkConsumptionDetails)
    
    val linkConsumptionDetailsByAirline = Map[Int, ListBuffer[LinkConsumptionDetails]]()
    linkConsumptionDetails.foreach { linkConsumption =>
      val consumptionsForThisAirline = linkConsumptionDetailsByAirline.getOrElseUpdate(linkConsumption.airlineId, ListBuffer())
      consumptionsForThisAirline.append(linkConsumption)
    }
    
    //compute profit
    //val airlineProfit = Map[Airline, Long]()
    
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
          
        println(airline + " profit is: " + profit + " new balance is " + airline.airlineInfo.balance)
    }
    
    
    //update the loyalty on airports based on link consumption
    println("start updating loyalty")
    val airportSoldLinks = Map[(Airport, Airline), Set[Link]]()
    
    links.filter { link => link.capacity > link.availableSeats }.foreach { link =>
      airportSoldLinks.getOrElseUpdate((link.to, link.airline), Set[Link]()).add(link) 
      airportSoldLinks.getOrElseUpdate((link.from, link.airline), Set[Link]()).add(link)
    }
    
    val updatingAirports = Set[Airport]()
    airportSoldLinks.foreach {
      case ((airport, airline), links) =>
        val totalTransportedPassengers = links.foldLeft(0) { (foldInt, link) => foldInt + (link.capacity - link.availableSeats) } 
        val totalQualityProduct = links.foldLeft(0L) { (foldLong, link) => foldLong + (link.capacity - link.availableSeats) * link.computedQuality }
        val averageQuality = totalQualityProduct.toDouble / totalTransportedPassengers
        
        var loyaltyAdjustment = totalTransportedPassengers / 200.0 / (airport.size * airport.size) //on a size 1 airport, 200 transported passengers is enough to move the loyalty by 1, on size 7 it's 7 * 7 * 1000)
        if (loyaltyAdjustment > MAX_LOYALTY_ADJUSTMENT) {
          loyaltyAdjustment = MAX_LOYALTY_ADJUSTMENT
        }
        val existingLoyalty = airport.getAirlineLoyalty(airline) 
        if (existingLoyalty < averageQuality) {
           airport.setAirlineLoyalty(airline, existingLoyalty + loyaltyAdjustment) 
        } else {
           airport.setAirlineLoyalty(airline, existingLoyalty - loyaltyAdjustment)
        }
        updatingAirports.add(airport)
        println("loyalty updating from " + existingLoyalty + " to " + airport.getAirlineLoyalty(airline))
    }
    AirportSource.updateAirlineAppeal(updatingAirports.toList)
    
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
}

