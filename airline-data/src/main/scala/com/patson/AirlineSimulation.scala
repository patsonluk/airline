package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirlineSimulation {
  private val AIRLINE_FIXED_COST = 0 //for now...
  private val REPUTATION_INCREMENT = 0.5 
  private val SERVICE_DELTA = 1
  
  def airlineSimulation(linkResult : scala.collection.immutable.Map[Int, List[LinkConsumptionDetails]], cycle: Int) = {
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    val allLinks = LinkSource.loadAllLinks(false).groupBy { _.airline.id }
    allAirlines.foreach { airline =>
        var airlineProfit = 0L
        linkResult.get(airline.id).foreach { linkConsumptions =>
          airlineProfit = linkConsumptions.foldLeft(0L)(_ + _.profit)
          val totalPassengers = linkConsumptions.foldLeft(0)(_ + _.soldSeats.total)
          
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
        }
        
        //calculate service quality
        allLinks.get(airline.id).foreach {  links =>
          
           val totalCapacity = links.map { _.capacity.total }.sum
           if (totalCapacity > 0) {
             var targetServiceQuality = airline.getServiceFunding() * 500 / totalCapacity
             if (targetServiceQuality > 100) {
               targetServiceQuality = 100
             }
             val currentServiceQuality = airline.getServiceQuality()
             if (currentServiceQuality < targetServiceQuality) {
                if (currentServiceQuality + SERVICE_DELTA >= targetServiceQuality) {
                  airline.setServiceQuality(targetServiceQuality)  
                } else {
                  airline.setServiceQuality(currentServiceQuality + SERVICE_DELTA)
                } 
              } else {
                if (currentServiceQuality - SERVICE_DELTA <= targetServiceQuality) {
                  airline.setServiceQuality(targetServiceQuality) 
                } else {
                  airline.setServiceQuality(currentServiceQuality - SERVICE_DELTA)
                }
              }
           } 
        }
        airlineProfit -= airline.getServiceFunding()
        
        airlineProfit -= AIRLINE_FIXED_COST
        airline.setBalance(airline.getBalance() + airlineProfit)
        
        AirlineSource.saveAirlineInfo(airline)
        
        println(airline + " profit is: " + airlineProfit + " new balance is " + airline.getBalance() + " reputation " +  airline.getReputation())
    }
  }
}