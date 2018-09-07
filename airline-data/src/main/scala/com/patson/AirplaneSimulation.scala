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
import com.patson.model.airplane._
import scala.collection.mutable.Map
import akka.actor.Actor
import akka.actor.Props
import java.util.concurrent.TimeUnit


import com.patson.model.CashFlowType.CashFlowType

object AirplaneSimulation {
  def airplaneSimulation(cycle: Int, links : List[Link]) : List[Airplane] = {
    println("starting airplane simulation")
    println("loading all airplanes")
    //do 2nd hand market adjustment
    secondHandAirplaneSimulate(cycle)
    
    //do decay
    val allAirplanes = AirplaneSource.loadAirplanesWithAssignedLinkByCriteria(List.empty)
    
    println("finished loading all airplanes")
    
    val updatingAirplanesListBuffer = ListBuffer[Airplane]()
    allAirplanes.groupBy { _._1.owner }.foreach {
      case (owner, airplanes) => {
        AirlineSource.loadAirlineById(owner.id, true) match {
          case Some(airline) =>
            val readyAirplanes = airplanes.filter(_._1.isReady(cycle))
            updatingAirplanesListBuffer ++= decayAirplanesByAirline(readyAirplanes, airline)
          case None => println("airline " + owner.id + " has airplanes but the airline cannot be loaded!")//invalid airline?
        }
      }
    }
    
    var updatingAirplanes = updatingAirplanesListBuffer.toList 
    AirplaneSource.updateAirplanes(updatingAirplanes)
    println("Finished updating all airplanes")
    
    println("Start renewing airplanes")
    updatingAirplanes = renewAirplanes(updatingAirplanes)
    println("Finished renewing airplanes")
    
    println("Start retiring airplanes")
    removeAgingAirplaneFromLinks(links, updatingAirplanes) //need to pass the airplanes here as the airplanes in the `links` are not updated yet
    retireAgingAirplanes(updatingAirplanes.toList)
    println("Finished retiring airplanes")
    
    updatingAirplanes.toList
  }
  
  def secondHandAirplaneSimulate(cycle : Int) = {
    var secondHandAirplanes = AirplaneSource.loadAirplanesCriteria(List(("is_sold", true)))
    secondHandAirplanes = secondHandAirplanes.map(airplane => airplane.copy(dealerRatio = airplane.dealerRatio - DEALER_RATIO_DROP_RATE))
    
    val updatingAirplanes = ListBuffer[Airplane]()
    val removingAirplanes = ListBuffer[Airplane]()
    secondHandAirplanes.foreach { airplane =>
      airplane.dealerRatio = airplane.dealerRatio - DEALER_RATIO_DROP_RATE
      if (airplane.dealerRatio >= DEALER_RATIO_LOWER_THERSHOLD) {
        updatingAirplanes.append(airplane)
      } else {
        removingAirplanes.append(airplane)
      }
    }
    AirplaneSource.updateAirplanes(updatingAirplanes.toList)
    removingAirplanes.foreach { airplane =>
      AirplaneSource.deleteAirplanesByCriteria(List(("id", airplane.id), ("is_sold", true))) //need to be careful here, make sure it is still in 2nd hand market
    }
    
  }
  
  val DEALER_RATIO_DROP_RATE = 0.0025
  val DEALER_RATIO_LOWER_THERSHOLD = Computation.SELL_RATE //at this ratio, the dealer would just scrap the airplane
  
  
  def renewAirplanes(airplanes : List[Airplane]) : List[Airplane] = {
    val renewalThresholdsByAirline : scala.collection.immutable.Map[Int, Int] = AirlineSource.loadAirplaneRenewals()
    val costsByAirline : Map[Int, (Long, Long, Long, Long)] = Map[Int, (Long, Long, Long, Long)]()
    val airlinesByid = AirlineSource.loadAllAirlines(false).map(airline => (airline.id, airline)).toMap
    val renewedAirplanes : ListBuffer[Airplane] = ListBuffer[Airplane]() 
    val secondHandAirplanes  = ListBuffer[Airplane]()
    
    val updatingAirplanes = airplanes.map { airplane => 
      renewalThresholdsByAirline.get(airplane.owner.id) match {
        case Some(threshold) =>
          if (airplane.condition < threshold ) {
             val airlineId = airplane.owner.id 
             val (existingCost, existingBuyPlane, existingSellPlane, existingCapitalLost) : (Long, Long, Long, Long) = costsByAirline.getOrElse(airlineId, (0, 0, 0, 0))
             val sellValue = Computation.calculateAirplaneSellValue(airplane)
             val renewCost = airplane.model.price - sellValue
             val newCost = existingCost + renewCost
             val newBuyPlane = existingBuyPlane + airplane.model.price
             val newSellPlane = existingSellPlane + sellValue 
             
             if (newCost <= airlinesByid(airplane.owner.id).getBalance()) {
               println("auto renewing " + airplane)
               val newCapitalLost = existingCapitalLost + (airplane.value - sellValue)
               costsByAirline.put(airlineId, (newCost, newBuyPlane, newSellPlane, newCapitalLost))
               if (airplane.condition >= Airplane.BAD_CONDITION) { //create a clone as the sold airplane
                 secondHandAirplanes.append(airplane.copy(isSold = true, dealerRatio = Airplane.DEFAULT_DEALER_RATIO, id = 0))
               }
               val renewedAirplane = airplane.copy(constructedCycle = MainSimulation.currentWeek, condition = Airplane.MAX_CONDITION, value = airplane.model.price)
               renewedAirplanes.append(renewedAirplane)
               renewedAirplane
             } else { //not enough fund
               airplane
             }
          } else {
            airplane
          }
        case None => airplane
      }
    }
    
    //now deduct money
    costsByAirline.foreach {
      case(airlineId, (cost, buyAirplane, sellAirplane, captialLoss)) => {
        println("Deducting " + cost + " from " + airlinesByid(airlineId) + " for renewal")
        AirlineSource.adjustAirlineBalance(airlineId, cost * -1)
        AirlineSource.saveTransaction(AirlineTransaction(airlineId, TransactionType.CAPITAL_GAIN, captialLoss * -1))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.SELL_AIRPLANE, sellAirplane))
        AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BUY_AIRPLANE, buyAirplane * -1))
      }
      
    }
    //save the 2nd hand airplanes
    AirplaneSource.saveAirplanes(secondHandAirplanes.toList)
    //save the renewed airplanes
    AirplaneSource.updateAirplanes(renewedAirplanes.toList)
      
    updatingAirplanes
  }
  
   def removeAgingAirplaneFromLinks(links : List[Link], airplanes : List[Airplane]) = {
    val updatingLinks = ListBuffer[Link]()
    val updatedAirplanesById = airplanes.map( airplane => (airplane.id, airplane)).toMap
    links.foreach {
      link => {
        val updatedAssignedAirplanes : List[Airplane] = link.getAssignedAirplanes().map( airplane => updatedAirplanesById.getOrElse(airplane.id, airplane)) //update the list of assigned airplanes
        
        val okAirplanes : List[Airplane] = updatedAssignedAirplanes.filter( _.condition > 0)
        
        val retiringAirplanesCount = updatedAssignedAirplanes.size - okAirplanes.size
        
        if (retiringAirplanesCount > 0) {
           println("retiring " + retiringAirplanesCount + " airplanes for link " + link)
           //now see if frequency should be reduced
           val maxFrequency = okAirplanes.foldLeft(0) {
             case (x, airplane) => x + Computation.calculateMaxFrequency(airplane.model, link.distance)
           }
           val updatingLink = 
             if (maxFrequency < link.frequency) {
               val capacityPerFlight = link.capacity / link.frequency
               link.copy(capacity = capacityPerFlight * maxFrequency, frequency = maxFrequency)
             } else {
               link
             }
          
           updatingLinks.append(updatingLink)
        }
      }
    }
    
    LinkSource.updateLinks(updatingLinks.toList)
  }
   
  def retireAgingAirplanes(airplanes : List[Airplane]) {
    airplanes.filter(_.condition <= 0).foreach { airplane =>
      println("Deleting airplane " + airplane)
      AirplaneSource.deleteAirplane(airplane.id)
    }
  }
  
  def computeDepreciationRate(model : Model, decayRate : Double) = {
    val depreciationRate = (model.price * (decayRate / 100)).toInt
    depreciationRate
  }
  
  def decayAirplanesByAirline(airplanesWithAssignedLink : List[(Airplane, Option[Link])], owner : Airline) : List[Airplane] = {
    val updatingAirplanes = ListBuffer[Airplane]()
    
    
    airplanesWithAssignedLink.foreach { 
      case(airplane, assignedLink) =>
        if (!owner.isGenerated) {
          val minDecay = Airplane.MAX_CONDITION.toDouble / airplane.model.lifespan //live the whole lifespan
          val maxDecay = minDecay * 2
          val baseDecayRate = maxDecay - (maxDecay - minDecay) * (owner.getMaintenanceQuality() / Airline.MAX_MAINTENANCE_QUALITY)
          var decayRate =
            if (assignedLink.isEmpty) { //not assigned to any links, decay slower
              baseDecayRate / 3 
            } else {
              baseDecayRate
            }
          if (decayRate > airplane.condition) {
            decayRate = airplane.condition
          }
          
          val newCondition = airplane.condition - decayRate
          val depreciationRate = computeDepreciationRate(airplane.model, decayRate)
          val newValue = airplane.value - depreciationRate
          
          updatingAirplanes.append(airplane.copy(condition = newCondition, depreciationRate = depreciationRate, value = newValue))
        }
    }
    updatingAirplanes.toList
  }
}