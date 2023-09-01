package com.patson.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import com.patson.Util

import java.util.concurrent.ThreadLocalRandom
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

  def computeCost(link : Transport, linkClass : LinkClass, externalCostModifier : Double = 1.0) : Double = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    var cost = standardPrice * priceAdjustRatio(link, linkClass)

    cost = (cost * qualityAdjustRatio(homeAirport, link, linkClass)).toInt

    cost = (cost * tripDurationAdjustRatio(link, linkClass)).toInt

    if (loyaltySensitivity > 0) {
      cost = (cost * loyaltyAdjustRatio(link)).toInt
    }

    cost = cost * loungeAdjustRatio(link, loungeLevelRequired, linkClass)

    cost *= externalCostModifier

    computeCost(cost, link, linkClass)
  }

  /**
    * For testing and debug purpose only
    * @param link
    * @param linkClass
    * @return
    */
  def computeCostBreakdown(link : Transport, linkClass : LinkClass) : CostBreakdown = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    val priceAdjust = priceAdjustRatio(link, linkClass)
    var cost = standardPrice * priceAdjust

    val qualityAdjust = qualityAdjustRatio(homeAirport, link, linkClass)
    cost = (cost * qualityAdjust).toInt

    val tripDurationAdjust = tripDurationAdjustRatio(link, linkClass)
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
  
  /**
   * priceSensitivity : how sensitive to the price, base value is 1 (100%)
   * 
   * 1 : cost is the same as price no adjustment
   * > 1 : more sensitive to price, a price that is deviated from "standard price" will have its effect amplified, for example a 2 (200%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$200                      
   * < 1 : less sensitive to price, a price that is deviated from "standard price" will have its effect weakened, for example a 0.5 (50%) would mean a $$150 ticket with suggested price of $$100, will be perceived as $$125
   * 
   * Take note that 0 would means a preference that totally ignore the price difference (could be dangerous as very expensive ticket will get through)
   */
  def priceAdjustRatio(link : Transport, linkClass : LinkClass) = {
    val standardPrice = link.standardPrice(preferredLinkClass)
    val deltaFromStandardPrice = priceAdjustedByLinkClassDiff(link, linkClass) - standardPrice

    1 + deltaFromStandardPrice * priceSensitivity / standardPrice
  }

  def loyaltyAdjustRatio(link : Transport) = {
    val appeal = appealList.getOrElse(link.airline.id, AirlineAppeal(0))
    val loyalty = appeal.loyalty
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

    1 + (priceAdjust - 1) * qualitySensitivity
  }


  /**
   *	flattop bell random centered at 0
   */
  def getFlatTopBellRandom(topWidth : Double, bellExtension : Double) = {
    topWidth / 2 - ThreadLocalRandom.current().nextDouble() * topWidth + Util.getBellRandom(0) * bellExtension
  }

  val priceAdjustedByLinkClassDiff = (link : Transport, linkClass : LinkClass) => {
    val cost = link.cost(linkClass) //use cost here
    if (linkClass.level != preferredLinkClass.level) {
      val classDiffMultiplier: Double = 1 + (preferredLinkClass.level - linkClass.level) * 0.2
      (cost / linkClass.priceMultiplier * preferredLinkClass.priceMultiplier * classDiffMultiplier).toInt //have to normalize the price to match the preferred link class, * classDiffMultiplier for unwillingness to downgrade
    } else {
      cost
    }
  }

  val connectionCostRatio = 1.0

  val tripDurationAdjustRatio = (link : Transport, linkClass : LinkClass) => {
    //shorter duration flights care much more about flight frequency
    val frequencyImportance = {
      if (frequencySensitivity == 0 || link.transportType != TransportType.FLIGHT) {
        0
      } else {
        Math.max(1, (360.0/link.duration).toDouble) - 0.9
      }
    }
    //at threshold it's neutral, full bonus if 2 * threshold. 
    val frequencyRatioDelta = Math.max(-1, (frequencyThreshold - link.frequencyByClass(linkClass)).toDouble / frequencyThreshold) * frequencySensitivity * frequencyImportance

    val flightDurationRatioDelta = {
      if (flightDurationSensitivity == 0 || link.transportType != TransportType.FLIGHT) {
        0
      } else {
        val flightDurationThreshold = Computation.computeStandardFlightDuration(link.distance)
        Math.min(flightDurationSensitivity, (link.duration - flightDurationThreshold).toFloat / flightDurationThreshold * flightDurationSensitivity)
      }
    }
    val maxDiscount = frequencySensitivity * -1
    val finalDelta = Math.max(maxDiscount, frequencyRatioDelta + flightDurationRatioDelta)

    Math.min(2.0, 1 + finalDelta) //max 2x penalty
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
            (loungeLevelRequired - fromLoungeLevel) * 0.03
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
  val SIMPLE  = Val("Simple", "")
  val SPEED = Val("Swift", "")
  val APPEAL   = Val("Comprehensive", "") 
  val LOYAL   = Val("Brand Conscious", "")
  val ELITE    = Val("Elite", "") 
}

import FlightPreferenceType._

case class SimplePreference(homeAirport : Airport, priceSensitivity : Double, preferredLinkClass: LinkClass) extends FlightPreference(homeAirport : Airport) {
  override val qualitySensitivity = {
    if (priceSensitivity >= 1) {
      0.25
    } else {
      0.5
    }
  }
  override val loyaltySensitivity = 0
  override val frequencyThreshold = 3
  override val frequencySensitivity = 0.02
  override val flightDurationSensitivity = 0

  def computeCost(baseCost : Double, link : Transport, linkClass : LinkClass) = {
    baseCost * 0.83
  }

  val getPreferenceType = {
    if (priceSensitivity >= 1) {
      BUDGET
    } else {
      SIMPLE
    }
  }
  
  override val connectionCostRatio = 0.25 //more okay with taking connection
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
}

case class SpeedPreference(homeAirport : Airport, preferredLinkClass: LinkClass) extends FlightPreference(homeAirport = homeAirport) {
  override val priceSensitivity = 0.8 * preferredLinkClass.priceSensitivity
  override val qualitySensitivity = 1.0
  override val loyaltySensitivity = 0
  override val frequencyThreshold = 21
  override val frequencySensitivity = 0.75
  override val flightDurationSensitivity = 1.0

  def computeCost(baseCost : Double, link : Transport, linkClass : LinkClass) = {
    val noise = 0.9 + getFlatTopBellRandom(0.3, 0.25)
    val finalCost = baseCost * noise
    finalCost 
  }
  
  val getPreferenceType = {
    SPEED
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
  
  override val connectionCostRatio = 2.0

}

case class ElitePreference(homeAirport : Airport, preferredLinkClass : LinkClass, override val loungeLevelRequired : Int)  extends FlightPreference(homeAirport) {
  override val priceSensitivity = 0.8 * preferredLinkClass.priceSensitivity
  override val qualitySensitivity = 2.0
  override val loyaltySensitivity = 0
  override val frequencyThreshold = 7
  override val frequencySensitivity = 0.1
  override val flightDurationSensitivity = preferredLinkClass match {
    case FIRST => 0.75
    case BUSINESS => 0.5
    case ECONOMY => 0.25
  }
  override val loungeSensitivity : Double = 1
  override val connectionCostRatio = 2.0

  def computeCost(baseCost: Double, link : Transport, linkClass : LinkClass) : Double = {
    var perceivedPrice = baseCost + getFlatTopBellRandom(0.3, 0.25)

    //find luxurious flight attractive
    if ( link.computedQuality() > 75) {
      val discount = (link.computedQuality() - 75) / 25.0 * 0.5
      perceivedPrice = perceivedPrice * (1 - discount)
    }
    
    if (perceivedPrice >= 0) {
      return perceivedPrice  
    } else { //just to play safe - do NOT allow negative cost link
      return 0
    }
  }

  val getPreferenceType = ELITE
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = {
    if (fromAirport.size >= 4) {
      true
    } else {
      false
    }
  }
}

case class AppealPreference(homeAirport : Airport, preferredLinkClass : LinkClass, override val loungeLevelRequired : Int, loyaltyRatio : Double, id : Int)  extends FlightPreference(homeAirport) {
  override val priceSensitivity = preferredLinkClass.priceSensitivity
  override val qualitySensitivity = 1
  override val loyaltySensitivity = loyaltyRatio
  override val flightDurationSensitivity = preferredLinkClass match {
    case FIRST => 0.55
    case BUSINESS => 0.4
    case ECONOMY => 0.25
  }
  override val loungeSensitivity : Double = 1
  override val frequencySensitivity = 0.2
  override val frequencyThreshold = {
    if (loyaltyRatio > 1) {
      5
    } else {
      14
    }
  }
  override val connectionCostRatio = {
    if (loyaltyRatio > 1) {
      0.5
    } else {
      1.0
    }
  }
  val getPreferenceType = {
   if (loyaltyRatio > 1) {
      //BRAND
      LOYAL
    } else {
      //COMPREHENSIVE
      APPEAL
    }
  }

  def computeCost(baseCost: Double, link : Transport, linkClass : LinkClass) : Double = {
    var perceivedPrice = baseCost

    val noise = 0.9 + getFlatTopBellRandom(0.3, 0.25)
    val finalCost = perceivedPrice * noise
    
    if (finalCost >= 0) {
      return finalCost  
    } else { //just to play safe - do NOT allow negative cost link
      return 0
    }
  }
  
  def isApplicable(fromAirport : Airport, toAirport : Airport) : Boolean = true
}

object AppealPreference {
  var count: Int = 0
  def getAppealPreferenceWithId(homeAirport : Airport, linkClass : LinkClass, loungeLevelRequired : Int, loyaltyRatio : Double = 1.0) = {
    count += 1
    AppealPreference(homeAirport, linkClass, loungeLevelRequired = loungeLevelRequired, loyaltyRatio = loyaltyRatio, count)
  }
  
}


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
    poolForClass(ThreadLocalRandom.current().nextInt(poolForClass.length))
  }
}



