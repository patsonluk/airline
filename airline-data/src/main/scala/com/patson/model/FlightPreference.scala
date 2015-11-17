package com.patson.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import com.patson.Util

abstract class FlightPreference {
  def computeCost(link : Link) : Double  
}

/**
 * priceWeight 		to what extent would deviation from standard pricing affects the cost. 
 * maxPriceWeight what is the maxPriceWeight possible
 */
case class SimplePreference(priceWeight : Int, maxPriceWeight : Int) extends FlightPreference{
  def computeCost(link : Link) = {
    val minFactorForPriceWeight = 0.2
    val maxFactorForPriceWeight = 1
    
    //from minFactorForPriceWeight up to maxFactorForPriceWeight. Proportional to priceWeight/maxPriceWeight
    val actualFactorForPriceWeight = minFactorForPriceWeight + priceWeight.toDouble / maxPriceWeight * (maxFactorForPriceWeight - minFactorForPriceWeight)
    
    val cost = link.distance + Pricing.standardCostAdjustmentFromPrice(link.distance, link.price) * actualFactorForPriceWeight
    if (cost <= 0) 1 else cost //just in case
  }
}



case class AppealPreference(appealList : Map[Airline, AirlineAppeal], id : Int)  extends FlightPreference{
  val maxLoyalty = AirlineAppeal.MAX_LOYALTY
  val fixedCostRatio = 0.5 //the composition of constant cost, if at 0, all cost is based on loyalty, at 1, loyalty has no effect at all
  //val drawPool = new DrawPool(appealList)
  
  def computeCost(link : Link) = {
    //at max loyalty, passenger can perceive the ticket price down to actual price / maxReduceFactorAtMaxLoyalty.  
    val maxReduceFactorAtMaxLoyalty = 3
    //at min loyalty (0), passenger can perceive the ticket price down to actual price / maxReduceFactorAtMinLoyalty.  
    val maxReduceFactorAtMinLoyalty = 1.5
    
    //at max loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty.
    val minReduceFactorAtMaxLoyalty = 1.5
    //at min loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty. (at 1, means no reduction)
    val minReduceFactorAtMinLoyalty = 1
    
    val appeal = appealList.getOrElse(link.airline, AirlineAppeal(0, 0))
    
    var perceivedPrice = link.price;
    val loyalty = appeal.loyalty
    if (loyalty != 0) {
      //the maxReduceFactorForThisAirline, if at max loyalty, it is the same as maxReduceFactorAtMaxLoyalty, at 0 loyalty, this is at maxReduceFactorAtMinLoyalty
      val maxReduceFactorForThisAirline = maxReduceFactorAtMinLoyalty + (maxReduceFactorAtMaxLoyalty - 1) * (loyalty.toDouble / maxLoyalty)
      //the minReduceFactorForThisAirline, if at max loyalty, it is the same as minReduceFactorAtMaxLoyalty. at 0 loyalty, this is 1 (no reduction)
      val minReduceFactorForThisAirline = minReduceFactorAtMinLoyalty + (minReduceFactorAtMaxLoyalty - 1) * (loyalty.toDouble / maxLoyalty)
      
      //the actualReduceFactor is random number (linear distribution) from minReduceFactorForThisAirline up to the maxReduceFactorForThisAirline. 
      val actualReduceFactor = minReduceFactorForThisAirline + (maxReduceFactorForThisAirline - minReduceFactorForThisAirline) * Math.random()
      
      perceivedPrice = (link.price / actualReduceFactor).toInt
    }
    
    //TODO quality matters too!
        
    //println(link.airline.name + " loyalty " + loyalty + " from price " + link.price + " reduced to " + perceivedPrice)
    
    val baseCost = link.distance + Pricing.standardCostAdjustmentFromPrice(link.distance, perceivedPrice)
    
//    println(link.airline.name + " baseCost " + baseCost +  " actual reduce factor " + actualReduceFactor + " max " + maxReduceFactorForThisAirline + " min " + minReduceFactorForThisAirline)
    
 
    //val noise = (1 + (Util.getBellRandom(0)) * 0.8) // max 10% noise : 0.6 - 1.4
    val noise = (1 + (0.5 - Math.random()) * 0.8) // max 10% noise : 0.6 - 1.4
    
    
    //NOISE?
    baseCost * noise
  }
}

object AppealPreference {
  var count: Int = 0
  def getAppealPreferenceWithId(appealList : Map[Airline, AirlineAppeal]) = {
    count += 1
    AppealPreference(appealList, count)
  }
}

//class DrawPool(appealList : Map[Airline, AirlineAppeal]) {
//  val asList = appealList.toList.map(_._1)
//  def draw() : Airline = {
//    val pickedNumber = Random.nextInt(weightSum)
//    var walkerSum = pickedNumber
//    for (Tuple2(airline, weight) <- loyaltyList) {
//      walkerSum -= weight
//      if (walkerSum < 0) {
//        return Some(airline)
//      }
//    }
//    None
//    asList(Random.nextInt(asList.length))
//  }
//}


class FlightPreferencePool(preferencesWithWeight : List[(FlightPreference, Int)]) {
  val pool = ListBuffer[FlightPreference]()
  preferencesWithWeight.foreach { 
    case (flightPreference, weight) =>
      for (i <- 0 until weight) {
        pool.append(flightPreference)
      }
  }
//  val pool = preferencesWithWeight.foldRight(List[FlightPreference]()) {
//    (entry, foldList) => 
//      Range(0, entry._2, 1).foldRight(List[FlightPreference]())((_, childFoldList) => entry._1 :: childFoldList) ::: foldList
//  }
  
  def draw : FlightPreference = {
    //Random.shuffle(pool).apply(0)
    pool(Random.nextInt(pool.length))
  }
}

 


