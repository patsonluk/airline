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
  def computeCost(link : Link, linkClass : LinkClass) : Double
  def preferredLinkClass : LinkClass
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean //whether this flight preference is applicable to this from/to airport
  def getPreferenceType : FlightPreferenceType.Value
  
  def getQualityAdjustRatio(homeAirport : Airport, link : Link, linkClass : LinkClass) : Double = {
    val qualityExpectation = homeAirport.expectedQuality(link.flightType, linkClass)
    val qualityDelta = link.computedQuality - qualityExpectation
    val GOOD_QUALITY_DELTA = 20 
    val priceAdjust =
      if (qualityDelta < 0) { 
        1 - qualityDelta.toDouble / Link.MAX_QUALITY * 2 
      } else if (qualityDelta < GOOD_QUALITY_DELTA) { 
        1 - qualityDelta.toDouble / Link.MAX_QUALITY * 0.5 
      } else { //reduced benefit on extremely high quality
        val extraDelta = qualityDelta - GOOD_QUALITY_DELTA
        1 - GOOD_QUALITY_DELTA.toDouble / Link.MAX_QUALITY * 0.5 - extraDelta.toDouble / Link.MAX_QUALITY * 0.2
      }
    
    //TODO this makes higher income country
    //println(qualityExpectation + " vs " + link.computedQuality + " : " + priceAdjust)
    priceAdjust
  }
  
  
  /**
   *	flattop bell random centered at 0 
   */
  def getFlatTopBellRandom(topWidth : Double, bellExtenstion : Double) = {
    topWidth / 2 - Math.random * topWidth + Util.getBellRandom(0) * bellExtenstion
  }
  
  val connectionCostRatio = 1.0
}

object FlightPreferenceType extends Enumeration {
  type FlightType = Value
  
  
  protected case class Val(title : String, description : String) extends super.Val { 
    
  } 
  implicit def valueToFlightPreferenceTypeVal(x: Value) = x.asInstanceOf[Val] 

  val BUDGET = Val("Budget", "") 
  val SIMPLE  = Val("Carefree", "")
  val SPEED = Val("Swift", "")
  val APPEAL   = Val("Comprehensive", "") 
  val LOYAL   = Val("Loyalist", "")
  val ELITE    = Val("Elite", "") 
}

import FlightPreferenceType._
/**
 * priceSensitivity : how sensitive to the price, base value is 1 (100%)
 * 
 * 1 : cost is the same as price no adjustment
 * > 1 : more sensitive to price, a price that is deviated from "standard price" will have its effect amplified, for example a 2 (200%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$200                      
 * < 1 : less sensitive to price, a price that is deviated from "standard price" will have its effect weakened, for example a 0.5 (50%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$125
 * 
 * Take note that 0 would means a preference that totally ignore the price difference (could be dangerous as very expensive ticket will get through)
 */
case class SimplePreference(homeAirport : Airport, priceSensitivity : Double, preferredLinkClass: LinkClass) extends FlightPreference {
  def computeCost(link : Link, linkClass : LinkClass) = {
    val standardPrice = Pricing.computeStandardPrice(link, linkClass)
    val deltaFromStandardPrice = link.price(linkClass) - standardPrice 
    
    var cost = standardPrice + deltaFromStandardPrice * priceSensitivity
    
    val qualityAdjustedRatio = (getQualityAdjustRatio(homeAirport, link, linkClass) + 2) / 3  //dampen the effect
    cost = (cost * qualityAdjustedRatio).toInt
    
    val noise = 0.9 + getFlatTopBellRandom(0.2, 0.1)
    
    //NOISE?
    val finalCost = cost * noise
    
    if (finalCost >= 0) {
      finalCost  
    } else { //just to play safe - do NOT allow negative cost link
      0
    }
  }
  
  val getPreferenceType = {
    if (priceSensitivity >= 1) {
      BUDGET
    } else {
      SIMPLE
    }
  }
  
  override val connectionCostRatio = 0.5 //more okay with taking connection
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
}

case class SpeedPreference(homeAirport : Airport, preferredLinkClass: LinkClass) extends FlightPreference {
  def computeCost(link : Link, linkClass : LinkClass) = {
    val standardPrice = Pricing.computeStandardPrice(link, linkClass)
    val deltaFromStandardPrice = link.price(linkClass) - standardPrice 
    
    var cost = standardPrice + deltaFromStandardPrice * 0.5 //care much less about price
    val qualityAdjustedRatio = (getQualityAdjustRatio(homeAirport, link, linkClass) + 1) / 2  //dampen the effect
    cost = (cost * qualityAdjustedRatio).toInt
    
    if (link.frequency < Link.HIGH_FREQUENCY_THRESHOLD) { //less than twice a day. I NEED THE FLIGHT NOW! extra penalty up to double if frequency is min 
      cost = cost * (1 + (Link.HIGH_FREQUENCY_THRESHOLD - link.frequency).toDouble / Link.HIGH_FREQUENCY_THRESHOLD / 5)  
    }
    
    val noise = 0.8 + getFlatTopBellRandom(0.2, 0.1)

    //NOISE?
    val finalCost = cost * noise
    
    finalCost 
  }
  
  val getPreferenceType = {
    SPEED
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
  
  override val connectionCostRatio = 2.0
}

case class AppealPreference(homeAirport : Airport, preferredLinkClass : LinkClass, loungeLevelRequired : Int, loyaltyRatio : Double, id : Int)  extends FlightPreference{
  val appealList : Map[Int, AirlineAppeal] = homeAirport.getAirlineAppeals
  val maxLoyalty = AirlineAppeal.MAX_LOYALTY
  //val fixedCostRatio = 0.5 //the composition of constant cost, if at 0, all cost is based on loyalty, at 1, loyalty has no effect at all
  //at max loyalty, passenger can perceive the ticket price down to actual price / maxReduceFactorAtMaxLoyalty.  
  val maxReduceFactorAtMaxLoyalty = 1.7
  //at min loyalty (0), passenger can perceive the ticket price down to actual price / maxReduceFactorAtMinLoyalty.  
  val maxReduceFactorAtMinLoyalty = 1.0
  
  //at max loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty.
  val minReduceFactorAtMaxLoyalty = 1.1
  //at min loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty. (at 0.8 means increasing perceieved price)
  val minReduceFactorAtMinLoyalty = 0.8 
  //val drawPool = new DrawPool(appealList)
  
  def computeCost(link : Link, linkClass : LinkClass) : Double = {
    val appeal = appealList.getOrElse(link.airline.id, AirlineAppeal(0, 0))
    
    var perceivedPrice = link.price(linkClass);
    if (linkClass.level != preferredLinkClass.level) {
      perceivedPrice = (perceivedPrice / linkClass.priceMultiplier * preferredLinkClass.priceMultiplier * 2).toInt //have to normalize the price to match the preferred link class, * 2 for unwillingness to downgrade
    }
    
    val loyalty = appeal.loyalty
    //the maxReduceFactorForThisAirline, if at max loyalty, it is the same as maxReduceFactorAtMaxLoyalty, at 0 loyalty, this is at maxReduceFactorAtMinLoyalty
//    val maxReduceFactorForThisAirline = maxReduceFactorAtMinLoyalty + (maxReduceFactorAtMaxLoyalty - maxReduceFactorAtMinLoyalty) * (loyalty.toDouble / maxLoyalty)
//    //the minReduceFactorForThisAirline, if at max loyalty, it is the same as minReduceFactorAtMaxLoyalty. at 0 loyalty, this is 1 (no reduction)
//    val minReduceFactorForThisAirline = minReduceFactorAtMinLoyalty + (minReduceFactorAtMaxLoyalty - minReduceFactorAtMinLoyalty) * (loyalty.toDouble / maxLoyalty)
//    
//    println("factor " + loyaltyRatio + " at loyalty " + loyalty + " : " + minReduceFactorForThisAirline  + " -> " + maxReduceFactorForThisAirline)
//    
//    //the actualReduceFactor is random number (linear distribution) from minReduceFactorForThisAirline up to the maxReduceFactorForThisAirline. 
//    val actualReduceFactor = (minReduceFactorForThisAirline + maxReduceFactorForThisAirline) / 2 + (maxReduceFactorForThisAirline - minReduceFactorForThisAirline) * Math.random() / 2 * loyaltyRatio
    val adjustment = 1 + (-0.15 + loyalty.toDouble / maxLoyalty / 2)  * loyaltyRatio
    
 	//println("factor " + loyaltyRatio + " at loyalty " + loyalty + " : " + adjustment)    
    
    perceivedPrice = (perceivedPrice / adjustment).toInt
    //perceivedPrice = (perceivedPrice / actualReduceFactor).toInt
    
    //adjust by quality
    
    //println("neutral quality : " + neutralQuality + " distance : " + distance)
  
    
    perceivedPrice = (perceivedPrice * getQualityAdjustRatio(homeAirport, link, linkClass)).toInt
    
    if (link.frequency < Link.HIGH_FREQUENCY_THRESHOLD) {  
      perceivedPrice = (perceivedPrice * (1 + (Link.HIGH_FREQUENCY_THRESHOLD - link.frequency).toDouble / Link.HIGH_FREQUENCY_THRESHOLD / 15)).toInt  
    }
        
    //println(link.airline.name + " loyalty " + loyalty + " from price " + link.price + " reduced to " + perceivedPrice)
    
    //adjust by lounge
    if (linkClass.level >= BUSINESS.level) {
      val fromLounge = link.from.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)
      val toLounge = link.to.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)
        
      val fromLoungeLevel = fromLounge.map(_.level).getOrElse(0)
      val toLoungeLevel = toLounge.map(_.level).getOrElse(0)
      
       
      if (fromLoungeLevel < loungeLevelRequired) { //penalty for not having lounge required
        perceivedPrice = perceivedPrice + 500 * ((loungeLevelRequired - fromLoungeLevel) * linkClass.priceMultiplier).toInt
      } else {
        if (fromLounge.isDefined) {
          perceivedPrice = (perceivedPrice * fromLounge.get.getPriceReduceFactor(link.distance)).toInt
        }
      }
      
      if (toLoungeLevel < loungeLevelRequired) { //penalty for not having lounge required
        perceivedPrice = perceivedPrice + 500 * ((loungeLevelRequired - toLoungeLevel) * linkClass.priceMultiplier).toInt
      } else {
        if (toLounge.isDefined) {
          perceivedPrice = (perceivedPrice * toLounge.get.getPriceReduceFactor(link.distance)).toInt
        }
      }
    }
    
//    println(link.price(linkClass) + " vs " + perceivedPrice + " from " + this)
    
    //cost is in terms of flight duration
    val baseCost = perceivedPrice//link.distance * Pricing.standardCostAdjustmentRatioFromPrice(link, linkClass, perceivedPrice)
    
//    println(link.airline.name + " baseCost " + baseCost +  " actual reduce factor " + actualReduceFactor + " max " + maxReduceFactorForThisAirline + " min " + minReduceFactorForThisAirline)
    
 
    val noise = 0.7 + getFlatTopBellRandom(0.3, 0.25)

    
    //NOISE?
    val finalCost = baseCost * noise
    
    if (finalCost >= 0) {
      return finalCost  
    } else { //just to play safe - do NOT allow negative cost link
      return 0
    }
  }
  
  val getPreferenceType = {
    if (loungeLevelRequired > 0) {
      ELITE
    } else {
      if (loyaltyRatio > 1) {
        LOYAL
      } else {
        APPEAL
      }
    }
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
  def getAppealPreferenceWithId(homeAirport : Airport, linkClass : LinkClass, loungeLevelRequired : Int, loyaltyRatio : Double = 1.0) = {
    count += 1
    AppealPreference(homeAirport, linkClass, loungeLevelRequired = loungeLevelRequired, loyaltyRatio = loyaltyRatio, count)
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
    case (flightPrefernce, weight) => flightPrefernce.preferredLinkClass
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

 


