package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirlineSimulation {
  private val AIRLINE_FIXED_COST = 0 //for now...
  private val REPUTATION_INCREMENT = 0.5 
  
  def airlineSimulation(linkResult : scala.collection.immutable.Map[Int, List[LinkConsumptionDetails]], cycle: Int) = {
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    allAirlines.foreach { airline =>
        var profit = 0L
        linkResult.get(airline.id) match {
          case Some(linkConsumptions) =>
            profit = linkConsumptions.foldLeft(0L)(_ + _.profit) - AIRLINE_FIXED_COST
            val totalPassengers = linkConsumptions.foldLeft(0)(_ + _.soldSeats.capacityMap.map(_._2).sum)
            
            var targetReputation = Math.log(totalPassengers / 1000) / Math.log(1.1)
            if (targetReputation > 100) {
              targetReputation = 100
            } else if (targetReputation < 10) {
              targetReputation = 10
            }
            
            val currentReputation = airline.getReputation() 
            if (currentReputation < targetReputation) {
              if (currentReputation + REPUTATION_INCREMENT >= targetReputation) {
                airline.setReputation(targetReputation)  
              } else {
                airline.setReputation(currentReputation + REPUTATION_INCREMENT)
              }
            }
          case None=>  
            profit = 0 - AIRLINE_FIXED_COST
        }
        airline.setBalance(airline.getBalance() + profit)
        
        AirlineSource.saveAirlineInfo(airline)
        
        println(airline + " profit is: " + profit + " new balance is " + airline.getBalance() + " reputation " +  airline.getReputation())
    }
  }
}