package com.patson.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import com.patson.Util
/**
 * Flight preference has a computeCost method that convert the existing price of a link to a "perceived price". The "perceived price" will be refer to "cost" here
 * 
 * When a link contains certain properties that the "Flight preference" likes/hates, it might reduce (if like) or increase (if hate) the "perceived price"  
 */
abstract class FlightPreference {
  def computeCost(link : Link) : Double
  def linkClass : LinkClass
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean //whether this flight preference is applicable to this from/to airport
}

/**
 * priceSensitivity : how sensitive to the price, base value is 1 (100%)
 * 
 * 1 : cost is the same as price no adjustment
 * > 1 : more sensitive to price, a price that is deviated from "standard price" will have its effect amplified, for example a 2 (200%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$200                      
 * < 1 : less sensitive to price, a price that is deviated from "standard price" will have its effect weakened, for example a 0.5 (50%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$125
 * 
 * Take note that 0 would means a preference that totally ignore the price difference (could be dangerous as very expensive ticket will get through)
 */
case class SimplePreference(priceSensitivity : Double, linkClass: LinkClass) extends FlightPreference {
  def computeCost(link : Link) = {
    val standardPrice = Pricing.computeStandardPrice(link, linkClass)
    val deltaFromStandardPrice = link.price(linkClass) - standardPrice 
    
    val cost = standardPrice + deltaFromStandardPrice * priceSensitivity
    cost
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
}

case class AppealPreference(appealList : Map[Int, AirlineAppeal], linkClass : LinkClass, loungeLevelRequired : Int, id : Int)  extends FlightPreference{
  val maxLoyalty = AirlineAppeal.MAX_LOYALTY
  //val fixedCostRatio = 0.5 //the composition of constant cost, if at 0, all cost is based on loyalty, at 1, loyalty has no effect at all
  //at max loyalty, passenger can perceive the ticket price down to actual price / maxReduceFactorAtMaxLoyalty.  
  val maxReduceFactorAtMaxLoyalty = 1.7
  //at min loyalty (0), passenger can perceive the ticket price down to actual price / maxReduceFactorAtMinLoyalty.  
  val maxReduceFactorAtMinLoyalty = 1.2
  
  //at max loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty.
  val minReduceFactorAtMaxLoyalty = 1.1
  //at min loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty. (at 0.8 means increasing perceieved price)
  val minReduceFactorAtMinLoyalty = 0.9 
  //val drawPool = new DrawPool(appealList)
  
  def computeCost(link : Link) : Double = {
    val appeal = appealList.getOrElse(link.airline.id, AirlineAppeal(0, 0))
    
    var perceivedPrice = link.price(linkClass);
    val loyalty = appeal.loyalty
    //the maxReduceFactorForThisAirline, if at max loyalty, it is the same as maxReduceFactorAtMaxLoyalty, at 0 loyalty, this is at maxReduceFactorAtMinLoyalty
    val maxReduceFactorForThisAirline = maxReduceFactorAtMinLoyalty + (maxReduceFactorAtMaxLoyalty - 1) * (loyalty.toDouble / maxLoyalty)
    //the minReduceFactorForThisAirline, if at max loyalty, it is the same as minReduceFactorAtMaxLoyalty. at 0 loyalty, this is 1 (no reduction)
    val minReduceFactorForThisAirline = minReduceFactorAtMinLoyalty + (minReduceFactorAtMaxLoyalty - 1) * (loyalty.toDouble / maxLoyalty)
    
    //the actualReduceFactor is random number (linear distribution) from minReduceFactorForThisAirline up to the maxReduceFactorForThisAirline. 
    val actualReduceFactor = minReduceFactorForThisAirline + (maxReduceFactorForThisAirline - minReduceFactorForThisAirline) * Math.random()
    
    perceivedPrice = (perceivedPrice / actualReduceFactor).toInt
    
    //adjust by quality  
    perceivedPrice = (perceivedPrice * link.computeQualityPriceAdjust(linkClass)).toInt
        
    //println(link.airline.name + " loyalty " + loyalty + " from price " + link.price + " reduced to " + perceivedPrice)
    
    //adjust by lounge
    
    if (linkClass.level >= BUSINESS.level) {
      val fromLounge = link.from.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)
      val toLounge = link.to.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)
        
      val fromLoungeLevel = fromLounge.map(_.level).getOrElse(0)
      val toLoungeLevel = toLounge.map(_.level).getOrElse(0)
      
      
      if (loungeLevelRequired > 0) { 
        // Wants a lounge .. SHOULD NOT NEED THEM ON BOTH SIDES as that is impractical
        // But if they are there .. slight extra bonus
        
        if ((fromLoungeLevel >= loungeLevelRequired) || (toLoungeLevel >= loungeLevelRequired)) {
         // Atleast one side has the lounge .. nice 
          perceivedPrice = perceivedPrice - AppealPreference.LOUNGE_PERCEIVED_PRICE_REDUCTION_BASE * (fromLoungeLevel.max(toLoungeLevel) * linkClass.priceMultiplier).toInt
        }
       
        if ((fromLoungeLevel >= loungeLevelRequired) && (toLoungeLevel >= loungeLevelRequired)) {
         // Both sides have the required lounge .. extra nice 
          perceivedPrice = perceivedPrice - AppealPreference.LOUNGE_PERCEIVED_PRICE_REDUCTION_BASE * (fromLoungeLevel.min(toLoungeLevel) * linkClass.priceMultiplier).toInt
        }
       
        if ((fromLoungeLevel < loungeLevelRequired) && (toLoungeLevel < loungeLevelRequired)) {
         // No required lounges .. aww
          perceivedPrice = perceivedPrice + 400 * ((loungeLevelRequired - fromLoungeLevel.max(toLoungeLevel)) * linkClass.priceMultiplier).toInt
        }
      } else {
       //Has little preference for lounges so make perceived price difference smaller. Don't want to suck up too much demand
       //Level should make no difference to these travelers - Lounge level irrelevant
        if (toLoungeLevel > 0) {
          perceivedPrice = perceivedPrice - AppealPreference.LOUNGE_PERCEIVED_PRICE_REDUCTION_BASE * (linkClass.priceMultiplier / 2).toInt 
        }
        if (fromLoungeLevel > 0) {
          perceivedPrice = perceivedPrice - AppealPreference.LOUNGE_PERCEIVED_PRICE_REDUCTION_BASE * (linkClass.priceMultiplier / 2).toInt           
        }
      }
    }
    
//    println(link.price(linkClass) + " vs " + perceivedPrice + " from " + this)
    
    //cost is in terms of flight duration
    val baseCost = perceivedPrice//link.distance * Pricing.standardCostAdjustmentRatioFromPrice(link, linkClass, perceivedPrice)
    
//    println(link.airline.name + " baseCost " + baseCost +  " actual reduce factor " + actualReduceFactor + " max " + maxReduceFactorForThisAirline + " min " + minReduceFactorForThisAirline)
    
 
    val noise = (0.75 + (Util.getBellRandom(0)) * 0.25) // max noise : 0.5 - 1.0

    
    //NOISE?
    val finalCost = baseCost * noise
    
    return finalCost
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = {
    if (loungeLevelRequired > 0) {
      fromAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT && toAirport.size >= Lounge.LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT
    } else {
      true
    }
  }
}

object AppealPreference {
  var count: Int = 0
  val LOUNGE_PERCEIVED_PRICE_REDUCTION_BASE = 50
  def getAppealPreferenceWithId(appealList : Map[Int, AirlineAppeal], linkClass : LinkClass, loungeLevelRequired : Int) = {
    count += 1
    AppealPreference(appealList, linkClass, loungeLevelRequired = loungeLevelRequired, count)
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
  val pool : Map[LinkClass, List[FlightPreference]] = preferencesWithWeight.groupBy {
    case (flightPrefernce, weight) => flightPrefernce.linkClass
  }.mapValues { 
    _.flatMap {
      case (flightPreference, weight) => (0 until weight).foldRight(List[FlightPreference]()) { (_, foldList) =>
        flightPreference :: foldList 
      }
    }
  }
  
  
  
//  val pool = preferencesWithWeight.foldRight(List[FlightPreference]()) {
//    (entry, foldList) => 
//      Range(0, entry._2, 1).foldRight(List[FlightPreference]())((_, childFoldList) => entry._1 :: childFoldList) ::: foldList
//  }
  
  def draw(linkClass: LinkClass, fromAirport : Airport, toAirport : Airport) : FlightPreference = {
    //Random.shuffle(pool).apply(0)
    val poolForClass = pool(linkClass).filter(_.isApplicable(fromAirport, toAirport))
    poolForClass(Random.nextInt(poolForClass.length))
  }
}

 


