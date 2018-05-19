package com.patson

import com.patson.model._
import com.patson.data._
import scala.collection.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirlineSimulation {
  private val AIRLINE_FIXED_COST = 0 //for now...
  private val REPUTATION_INCREMENT = 0.5 
  private[patson] val MAX_SERVICE_QUALITY_INCREMENT : Double = 1
  
  def airlineSimulation(cycle: Int, linkResult : List[LinkConsumptionDetails]) = {
    //compute profit
    val allAirlines = AirlineSource.loadAllAirlines(true)
    val allLinks = LinkSource.loadAllLinks(LinkSource.ID_LOAD).groupBy { _.airline.id }
    val linkResultByAirline = linkResult.groupBy { _.airlineId }
    allAirlines.foreach { airline =>
        var airlineProfit = 0L
        linkResultByAirline.get(airline.id).foreach { linkConsumptions =>
          airlineProfit = linkConsumptions.foldLeft(0L)(_ + _.profit)
          
          
          val totalPassengerKilometers = linkConsumptions.foldLeft(0L) { (foldLong, linkConsumption) =>
            foldLong + linkConsumption.soldSeats.total * linkConsumption.distance
          }
          
          //https://en.wikipedia.org/wiki/World%27s_largest_airlines
          var targetReputation = Math.log(totalPassengerKilometers / 5000) / Math.log(1.2)
          if (targetReputation > Airline.MAX_REPUTATION) {
            targetReputation = Airline.MAX_REPUTATION
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
             val targetServiceQuality = getTargetQuality(airline.getServiceFunding(), totalCapacity) //50x to get 50 target quality, 200x to get max 100 target quality
             val currentServiceQuality = airline.getServiceQuality()
             airline.setServiceQuality(getNewQuality(currentServiceQuality, targetServiceQuality)) 
           } 
        }
        
        airlineProfit -= airline.getServiceFunding()
        
        airlineProfit -= AIRLINE_FIXED_COST
        airline.setBalance(airline.getBalance() + airlineProfit)
        
        println(airline + " profit is: " + airlineProfit + " new balance is " + airline.getBalance() + " reputation " +  airline.getReputation())
    }
    
    AirlineSource.saveAirlineInfo(allAirlines)
  }
  
  val getTargetQuality : (Int, Int) => Double = (funding : Int, capacity :Int) => {
    val computedQuality = Math.sqrt(funding.toDouble / capacity / 50 ) * 50  //50x capacity to get 50 target quality, 200x capacity to get max 100 target quality
    if (computedQuality >= Airline.MAX_SERVICE_QUALITY) {
      Airline.MAX_MAINTENANCE_QUALITY
    } else {
      computedQuality
    }
  }
  val getNewQuality : (Double, Double) => Double = (currentQuality, targetQuality) =>  {
    val delta = targetQuality - currentQuality
    val adjustment = 
      if (delta >= 0) { //going up, slower when current quality is already high
        MAX_SERVICE_QUALITY_INCREMENT * (1 - (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 1x; current quality 100, multiplier 0.1x
      } else { //going down, faster when current quality is already high
        -1 * MAX_SERVICE_QUALITY_INCREMENT * (0.1 + (currentQuality / Airline.MAX_SERVICE_QUALITY * 0.9)) //at current quality 0, multiplier 0.1x; current quality 100, multiplier 1x
      }
    if (adjustment >= 0) {
      if (adjustment + currentQuality >= targetQuality) {
        targetQuality
      } else {
        adjustment + currentQuality
      }
    } else {
      if (currentQuality + adjustment <= targetQuality) {
        targetQuality
      } else {
        currentQuality + adjustment
      }
    } 
  }
}