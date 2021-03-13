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
abstract class FlightPreference(homeAirport : Airport) {
  def computeCost(baseCost : Double, link : Transport, linkClass : LinkClass) : Double
  def preferredLinkClass : LinkClass
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean //whether this flight preference is applicable to this from/to airport
  def getPreferenceType : FlightPreferenceType.Value

  def computeCost(link : Transport, linkClass : LinkClass) : Double = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    var cost = standardPrice * priceAdjustRatio(link, linkClass)

    cost = (cost * qualityAdjustRatio(homeAirport, link, linkClass)).toInt

    cost = (cost * tripDurationAdjustRatio(link)).toInt

    if (loyaltySensitivity > 0) {
      cost = (cost * loyaltyAdjustRatio(link)).toInt
    }

    cost = cost * loungeAdjustRatio(link, loungeLevelRequired, linkClass)

    computeCost(cost, link, linkClass)
  }

  /**
    * For testing and debug purpose only
    * @param link
    * @param linkClass
    * @return
    */
  def computeCostBreakdown(link : Link, linkClass : LinkClass) : CostBreakdown = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    val priceAdjust = priceAdjustRatio(link, linkClass)
    var cost = standardPrice * priceAdjust

    val qualityAdjust = qualityAdjustRatio(homeAirport, link, linkClass)
    cost = (cost * qualityAdjust).toInt

    val tripDurationAdjust = tripDurationAdjustRatio(link)
    cost = (cost * tripDurationAdjust).toInt

    var loyaltyAdjust = 1.0
    if (loyaltySensitivity > 0) {
      loyaltyAdjust = loyaltyAdjustRatio(link)
      cost = (cost * loyaltyAdjust).toInt
    }

    val loungeAdjust = loungeAdjustRatio(link, loungeLevelRequired, linkClass)
    cost = cost * loungeAdjust

    CostBreakdown(computeCost(cost, link, linkClass), priceAdjust, qualityAdjust, tripDurationAdjust, loyaltyAdjust, loungeAdjust)
  }

  /**
    * For debug and testing propose only
    * @param cost
    * @param priceAdjust
    * @param qualityAdjust
    * @param tripDurationAdjust
    * @param loyaltyAdjust
    * @param loungeAdjust
    */
  case class CostBreakdown(cost : Double, priceAdjust : Double, qualityAdjust : Double, tripDurationAdjust : Double, loyaltyAdjust : Double, loungeAdjust : Double)

  val priceSensitivity : Double
  val qualitySensitivity : Double
  val loyaltySensitivity : Double
  val frequencyThreshold : Int
  val frequencySensitivity : Double
  val flightDurationSensitivity : Double
  val loungeSensitivity : Double = 0
  val loungeLevelRequired : Int = 0

  lazy val appealList : Map[Int, AirlineAppeal] = homeAirport.getAirlineAdjustedAppeals
  val maxLoyalty = AirlineAppeal.MAX_LOYALTY
  //val fixedCostRatio = 0.5 //the composition of constant cost, if at 0, all cost is based on loyalty, at 1, loyalty has no effect at all
  //at max loyalty, passenger can perceive the ticket price down to actual price / maxReduceFactorAtMaxLoyalty.
//  val maxReduceFactorAtMaxLoyalty = 1.7
//  //at min loyalty (0), passenger can perceive the ticket price down to actual price / maxReduceFactorAtMinLoyalty.
//  val maxReduceFactorAtMinLoyalty = 1.0
//
//  //at max loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty.
//  val minReduceFactorAtMaxLoyalty = 1.1
//  //at min loyalty, passenger at least perceive the ticket price down to actual price / minReduceFactorAtMaxLoyalty. (at 0.8 means increasing perceieved price)
//  val minReduceFactorAtMinLoyalty = 0.8

  def priceAdjustRatio(link : Transport, linkClass : LinkClass) = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    val deltaFromStandardPrice = priceAdjustedByLinkClassDiff(link, linkClass) - standardPrice

    1 + deltaFromStandardPrice * priceSensitivity / standardPrice
  }

  def loyaltyAdjustRatio(link : Transport) = {
    val appeal = appealList.getOrElse(link.airline.id, AirlineAppeal(0, 0))
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
    val base =  1 + (-0.1 + loyalty.toDouble / maxLoyalty / 2.25)  * loyaltySensitivity

    //println("factor " + loyaltyRatio + " at loyalty " + loyalty + " : " + adjustment)
    1 / base

  }


  def qualityAdjustRatio(homeAirport : Airport, link : Transport, linkClass : LinkClass) : Double = {
    val qualityExpectation = homeAirport.expectedQuality(link.flightType, linkClass)
    val qualityDelta = link.computedQuality - qualityExpectation
    val GOOD_QUALITY_DELTA = 20
    val priceAdjust =
      if (qualityDelta < 0) {
        1 - qualityDelta.toDouble / Link.MAX_QUALITY * 1
      } else if (qualityDelta < GOOD_QUALITY_DELTA) {
        1 - qualityDelta.toDouble / Link.MAX_QUALITY * 0.5
      } else { //reduced benefit on extremely high quality
        val extraDelta = qualityDelta - GOOD_QUALITY_DELTA
        1 - GOOD_QUALITY_DELTA.toDouble / Link.MAX_QUALITY * 0.5 - extraDelta.toDouble / Link.MAX_QUALITY * 0.3
      }

    //TODO this makes higher income country
    //println(qualityExpectation + " vs " + link.computedQuality + " : " + priceAdjust)
    1 + (priceAdjust - 1) * qualitySensitivity
  }


  /**
   *	flattop bell random centered at 0
   */
  def getFlatTopBellRandom(topWidth : Double, bellExtenstion : Double) = {
    topWidth / 2 - Math.random * topWidth + Util.getBellRandom(0) * bellExtenstion
  }

  val priceAdjustedByLinkClassDiff = (link : Transport, linkClass : LinkClass) => {
    val cost = link.cost(linkClass) //use cost here
    if (linkClass.level != preferredLinkClass.level) {
      val classDiffMultiplier: Double = 1 + (preferredLinkClass.level - linkClass.level) * 0.4
      (cost / linkClass.priceMultiplier * preferredLinkClass.priceMultiplier * classDiffMultiplier).toInt //have to normalize the price to match the preferred link class, * 2.5 for unwillingness to downgrade
    } else {
      cost
    }
  }

  val connectionCostRatio = 1.0

  //waitThreshold => if lower than threshold, adjust cost down (< 1); otherwise adjust up
  //waitMultiplier, flightDurationMultiplier => how does wait time and speed affect ratio, 0 = no effect, 0.1 = 10%
  val tripDurationAdjustRatio = (link : Transport) => {
    //by default waitThreshold extra minute increases ratio by 0.1 (max). and no wait (infinity frequency) decreases ratio by 0.1 (min)
    //full penalty on 0 freq, full bonus if 2 * theshold. at threshold it's neutral
    val frequencyRatioDelta = Math.max(-1, (frequencyThreshold - link.frequency).toDouble / frequencyThreshold) * frequencySensitivity

    val flightDurationRatioDelta =
      if (flightDurationSensitivity == 0) {
        0
      } else {
        val flightDurationThreshold = Computation.computeStandardFlightDuration(link.distance)
        Math.min(flightDurationSensitivity, (link.duration - flightDurationThreshold).toFloat / flightDurationThreshold * flightDurationSensitivity)
      }

    1 + frequencyRatioDelta + flightDurationRatioDelta
  }

  def loungeAdjustRatio(link : Transport, loungeLevelRequired : Int, linkClass: LinkClass) = {
    if (loungeSensitivity == 0 || linkClass.level < BUSINESS.level) {
      1.0
    } else {
      val fromLounge = link.from.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)
      val toLounge = link.to.getLounge(link.airline.id, link.airline.getAllianceId, activeOnly = true)

      val fromLoungeLevel = fromLounge.map(_.level).getOrElse(0)
      val toLoungeLevel = toLounge.map(_.level).getOrElse(0)


      val fromLoungeRatioDelta : Double =
        if (fromLoungeLevel < loungeLevelRequired) { //penalty for not having lounge required
          if (link.distance <= 2000) { //shorter flight has much less impact
            (loungeLevelRequired - fromLoungeLevel) * 0.05
          } else if (link.distance <= 5000) {
            (loungeLevelRequired - fromLoungeLevel) * 0.1
          } else {
            (loungeLevelRequired - fromLoungeLevel) * 0.15
          }
        } else {
          fromLounge.map(_.getPriceReduceFactor(link.distance)).getOrElse(0)
        }

      val toLoungeRatioDelta : Double =
        if (toLoungeLevel < loungeLevelRequired) { //penalty for not having lounge required
          if (link.distance <= 2000) { //shorter flight has less impact
            (loungeLevelRequired - toLoungeLevel) * 0.05
          } else if (link.distance <= 5000) {
            (loungeLevelRequired - toLoungeLevel) * 0.1
          } else {
            (loungeLevelRequired - toLoungeLevel) * 0.15
          }
        } else {
          toLounge.map(_.getPriceReduceFactor(link.distance)).getOrElse(0)
        }
      1 + fromLoungeRatioDelta + toLoungeRatioDelta
    }
  }
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
  val LOYAL   = Val("Brand Conscious", "")
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
case class SimplePreference(homeAirport : Airport, priceSensitivity : Double, preferredLinkClass: LinkClass) extends FlightPreference(homeAirport : Airport) {
  def computeCost(baseCost : Double, link : Transport, linkClass : LinkClass) = {
    val noise = 0.8 + getFlatTopBellRandom(0.2, 0.1)
    
    val finalCost = baseCost * noise
    
    if (finalCost >= 0) {
      finalCost  
    } else { //just to play safe - do NOT allow negative cost link
      0
    }
  }

  override val qualitySensitivity = 1.0 / 2
  override val loyaltySensitivity = 0
  override val frequencyThreshold = 3
  override val frequencySensitivity = 0.02
  override val flightDurationSensitivity = 0



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

case class SpeedPreference(homeAirport : Airport, preferredLinkClass: LinkClass) extends FlightPreference(homeAirport = homeAirport) {
  override val priceSensitivity = 0.9
  override val qualitySensitivity = 0.5
  override val loyaltySensitivity = 0
  override val frequencyThreshold = 14
  override val frequencySensitivity = 0.15
  override val flightDurationSensitivity = 0.4

  def computeCost(baseCost : Double, link : Transport, linkClass : LinkClass) = {
    val noise = 0.9 + getFlatTopBellRandom(0.2, 0.1)

    //NOISE?
    val finalCost = baseCost * noise
    
    finalCost 
  }
  
  val getPreferenceType = {
    SPEED
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
  
  override val connectionCostRatio = 2.0

}

case class AppealPreference(homeAirport : Airport, preferredLinkClass : LinkClass, override val loungeLevelRequired : Int, loyaltyRatio : Double, id : Int)  extends FlightPreference(homeAirport) {
  override val priceSensitivity = preferredLinkClass.priceSensitivity
  override val qualitySensitivity = 1
  override val loyaltySensitivity = loyaltyRatio
  override val frequencyThreshold = 14
  override val frequencySensitivity = 0.05
  override val flightDurationSensitivity = 0.15
  override val loungeSensitivity : Double = 1

  def computeCost(baseCost: Double, link : Transport, linkClass : LinkClass) : Double = {
    //println(link.airline.name + " loyalty " + loyalty + " from price " + link.price + " reduced to " + perceivedPrice)
    var perceivedPrice = baseCost

//    println(link.airline.name + " baseCost " + baseCost +  " actual reduce factor " + actualReduceFactor + " max " + maxReduceFactorForThisAirline + " min " + minReduceFactorForThisAirline)
    val noise = 0.9 + getFlatTopBellRandom(0.3, 0.25)

    //NOISE?
    val finalCost = perceivedPrice * noise
    
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
  }.view.mapValues {
    _.flatMap {
      case (flightPreference, weight) => (0 until weight).foldRight(List[FlightPreference]()) { (_, foldList) =>
        flightPreference :: foldList 
      }
    }
  }.toMap
  
  
  
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




