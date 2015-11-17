

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
  
  
  val FUEL_UNIT_COST = 2 //for now...
  val AIRLINE_FIXED_COST = 10000 //for now...
  val CREW_UNIT_COST = 10 //for now...
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
      airportSimulation(cycle, links) 
      linkSimulation(cycle, links)  
  }
  
  def airportSimulation(cycle: Int, links : List[Link]) = {
    val airportWithLinks = Map[Airport, Set[Airline]]()
    links.foreach { link =>
      val airlinesOnThisAirport = airportWithLinks.getOrElseUpdate(link.from, Set[Airline]())
      airlinesOnThisAirport.add(link.airline)
    }
    val incrementWithLink = 0.2
    
    //add awareness based on links
    airportWithLinks.foreach {
      case(airport, airlinesWithLinks) =>
        airlinesWithLinks.foreach { airline =>  
          val existingAwareness = airport.airlineAppeals.get(airline).map { _.awareness }.getOrElse(0.0)
          val newAwareness = 
            if ((existingAwareness + incrementWithLink) >= AirlineAppeal.MAX_AWARENESS) {
              AirlineAppeal.MAX_AWARENESS   
            } else {
              (((existingAwareness + incrementWithLink) * 10).toInt).toDouble / 10
            }
          airport.setAirlineAwareness(airline, newAwareness)
        }
    }
    
    //update the awareness on these airport
    AirportSource.updateAirlineAppeal(airportWithLinks.keys.toList)
   
    
  }
  
  
  def linkSimulation(cycle: Int, links : List[Link]) = {
    val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

    val consumptionResult = PassengerSimulation.passengerConsume(demand, links)
    println("Consumption result : ")
    consumptionResult.foreach { 
      case(_, _, _, links) => println(links)
    }
    
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        val soldSeats = link.capacity - link.availableSeats
        val totalFuelBurn = link.assignedAirplanes.foldLeft(0)(_ + _.model.fuelBurn) //fuel burn actually similar to crew cost
        val fuelCost = totalFuelBurn * link.duration * FUEL_UNIT_COST * link.frequency
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

