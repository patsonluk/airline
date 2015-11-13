

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
  
  
  val FUEL_UNIT_COST = 500 //for now...
  val AIRLINE_FIXED_COST = 10000 //for now...
//  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

//  import actorSystem.dispatcher

//  implicit val materializer = FlowMaterializer()
  
  mainFlow
  
  def mainFlow() = {
    import actorSystem.dispatcher
    val actor = actorSystem.actorOf(Props[MainSimulationActor])
    actorSystem.scheduler.schedule(Duration.Zero, Duration(1, TimeUnit.MINUTES), actor, Start)
  }
  
  def startWeeklyCycle(week : Int) {
    val demand = Await.result(DemandGenerator.computeDemand(), Duration.Inf)
    println("DONE with demand total demand: " + demand.foldLeft(0) {
      case(holder, (_, _, demandValue)) =>  
        holder + demandValue
    })

    val links = LinkSource.loadAllLinks()
    
    val consumptionResult = PassengerSimulation.passengerConsume(demand, links)
    
    
    val linkConsumptionDetails = links.foldRight(List[LinkConsumptionDetails]()) {
      (link, foldList) =>
        val soldSeats = link.capacity - link.availableSeats
        val totalFuelBurn = link.assignedAirplanes.foldLeft(0)(_ + _.model.fuelBurn) //fuel burn actually similar to crew cost
        val fuelCost = totalFuelBurn * FUEL_UNIT_COST
        val fixedCost = 0
        val crewCost = 0
        val revenue = soldSeats * link.price
        val profit = revenue - fuelCost - fixedCost - crewCost
        val consumption = LinkConsumptionDetails(link.id, link.price, link.capacity, soldSeats, fuelCost, crewCost, fixedCost, revenue, profit, link.from.id, link.to.id, link.airline.id, link.distance, week)
        println(consumption)
        consumption :: foldList
    }
    
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
      //  airlineProfit.put(airline, profit)
        airline.airlineInfo.balance += profit
        println(airline + " profit is: " + profit + " new balance is " + airline.airlineInfo.balance)
    }
    
    AirlineSource.updateAirlineInfo(allAirlines)
  }
  
  class MainSimulationActor extends Actor {
    var currentWeek = 0
    def receive = {
      case Start =>
        startWeeklyCycle(currentWeek)
        currentWeek += 1
    }
  }
  
  case class Start()
}

