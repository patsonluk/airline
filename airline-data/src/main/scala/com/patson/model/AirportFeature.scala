package com.patson.model

import com.patson.model.airplane.Model
import com.patson.model.airplane.Model.Type
import FlightType._
import com.patson.model.AirportFeatureType.{AirportFeatureType, DOMESTIC_AIRPORT, FINANCIAL_HUB, ELITE_CHARM, GATEWAY_AIRPORT, INTERNATIONAL_HUB, ISOLATED_TOWN, OLYMPICS_IN_PROGRESS, OLYMPICS_PREPARATIONS, UNKNOWN, VACATION_HUB}
import com.patson.model.IsolatedTownFeature.HUB_RANGE_BRACKETS


abstract class AirportFeature {
  val MAX_STRENGTH = 100
  def strength : Int
  //def airportId : Int
  def featureType : AirportFeatureType.Value
  val strengthFactor : Double = strength.toDouble / MAX_STRENGTH
  
  def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double

  lazy val getDescription = {
    featureType match {
      case INTERNATIONAL_HUB => "International Hub - More international passengers especially premium."
      case ELITE_CHARM => "Elite Destination – Elites travel here. "
      case VACATION_HUB => "Vacation Destination - Tourists travel here."
      case FINANCIAL_HUB => "Business Hub - Center for business passengers."
      case DOMESTIC_AIRPORT => "Domestic Discount Airport – Lower base upkeep. If flight is international, only accepts small aircraft."
      case ISOLATED_TOWN => s"Isolated - Increased demand within ${this.asInstanceOf[IsolatedTownFeature].boostRange}km."
      case GATEWAY_AIRPORT => "Gateway - Has increased demand with other gateway airports."
      case OLYMPICS_PREPARATIONS => "Preparing the Olympic Games."
      case OLYMPICS_IN_PROGRESS => "Year of the Olympic Games."
      case UNKNOWN => "Unknown"
    }
  }
}

object AirportFeature {
  import AirportFeatureType._
  def apply(featureType : AirportFeatureType, strength : Int) : AirportFeature = {
    featureType match {
      case INTERNATIONAL_HUB => InternationalHubFeature(strength)
      case ELITE_CHARM => EliteFeature(strength)
      case VACATION_HUB => VacationHubFeature(strength)
      case FINANCIAL_HUB => FinancialHubFeature(strength)
      case DOMESTIC_AIRPORT => DomesticAirportFeature()
      case GATEWAY_AIRPORT => GatewayAirportFeature()
      case ISOLATED_TOWN => IsolatedTownFeature(strength)
      case OLYMPICS_PREPARATIONS => OlympicsPreparationsFeature(strength)
      case OLYMPICS_IN_PROGRESS => OlympicsInProgressFeature(strength)
    }
  }
}

sealed case class InternationalHubFeature(baseStrength : Int, boosts : List[AirportBoost] = List.empty) extends AirportFeature {
  val featureType = AirportFeatureType.INTERNATIONAL_HUB
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType, affinity : Int, distance : Int) : Double = {
//    if (airportId == toAirport.id) { //only affect if as a destination
//      val multiplier =
//        if (passengerType == PassengerType.BUSINESS) { //more obvious for business travelers
//          2
//        } else {
//          0.5
//        }
//      if (flightType == SHORT_HAUL_INTERNATIONAL || flightType == MEDIUM_HAUL_INTERNATIONAL ) {
//        (rawDemand * (strengthFactor * 0.5) * multiplier).toInt //at MAX_STREGTH, add 1x for business traveler, 0.2x for tourists (short haul)
//      } else if (flightType == LONG_HAUL_INTERNATIONAL || flightType == ULTRA_LONG_HAUL_DOMESTIC || flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL) {
//        (rawDemand * (strengthFactor * 1) * multiplier).toInt //at MAX_STREGTH, add 2x for business traveler, 0.4x for tourists (long haul)
//      } else {
//        0
//      }
//    } else {
      0
//    }
  }

  override lazy val strength = baseStrength + boosts.filter(_.boostType == AirportBoostType.INTERNATIONAL_HUB).map(_.value).sum.toInt
}

sealed case class EliteFeature(baseStrength : Int, boosts : List[AirportBoost] = List.empty) extends AirportFeature {
  val featureType = AirportFeatureType.ELITE_CHARM

  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType, affinity : Int, distance : Int) : Double = {
    0
  }

  override lazy val strength = baseStrength + boosts.filter(_.boostType == AirportBoostType.ELITE_CHARM).map(_.value).sum.toInt
}

sealed case class VacationHubFeature(baseStrength : Int, boosts : List[AirportBoost] = List.empty) extends AirportFeature {
  val featureType = AirportFeatureType.VACATION_HUB

  /**
   *
   */
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    if (toAirport.id == airportId && passengerType == PassengerType.TOURIST) { //only affect if as a destination and tourists
      /**
       * based off Disney World, which has 13.6m domestic visitors or 4% of USA population, so at 100 strength charm 4% want to visit or a 1 charm 0.04%
       * (strengthFactor is already a percent)
       */
      val charmStrength = 0.0004 * strengthFactor

      val affinityDistanceModifier = {
        val affinityMod = if (affinity <= 0) 0.0 else affinity.toDouble / 5.0
        val distanceMod = if (distance < 5000) 1.0 else Math.max(0.4, 5000 / distance)
        affinityMod * distanceMod
      }

      (fromAirport.popMiddleIncome * charmStrength * affinityDistanceModifier * fromAirport.income / 50000).toInt
    } else {
      0
    }
  }

  override lazy val strength = baseStrength + boosts.filter(_.boostType == AirportBoostType.VACATION_HUB).map(_.value).sum.toInt
}

sealed case class FinancialHubFeature(baseStrength : Int, boosts : List[AirportBoost] = List.empty) extends AirportFeature {
  val featureType = AirportFeatureType.FINANCIAL_HUB
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    if (toAirport.id == airportId && passengerType == PassengerType.BUSINESS) {
      val charmStrength = 0.0008 * strengthFactor

      val affinityDistanceModifier = {
        val affinityMod = if (affinity <= 0) 0.0 else affinity.toDouble / 5.0
        val distanceMod = if (distance < 5000) 1.0 else 5000 / distance
        affinityMod * distanceMod
      }

      (fromAirport.popMiddleIncome * fromAirport.income / 50000 * charmStrength * affinityDistanceModifier).toInt
    } else {
      0
    }
  }
  override lazy val strength = baseStrength + boosts.filter(_.boostType == AirportBoostType.FINANCIAL_HUB).map(_.value).sum.toInt
}

sealed case class DomesticAirportFeature() extends AirportFeature {
  val featureType = AirportFeatureType.DOMESTIC_AIRPORT
  def strength = 0
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    if (affinity == 5) {//domestic
      (rawDemand / 3).toInt
    } else {
       (-1 * rawDemand / 2).toInt
    }
  }
}

sealed case class GatewayAirportFeature() extends AirportFeature {
  val featureType = AirportFeatureType.GATEWAY_AIRPORT
  def strength = 0
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    if (airportId != fromAirport.id) {
      0
    } else if (affinity < 1) {
      0
    } else {
      if (
        fromAirport.getFeatures().map(_.featureType).contains(AirportFeatureType.GATEWAY_AIRPORT) &&
          toAirport.getFeatures().map(_.featureType).contains(AirportFeatureType.GATEWAY_AIRPORT)
      ) { //extra demand if both airports are gateway, mostly for tiny island nations
        val base = (fromAirport.power + toAirport.power) / 25000
        if (base >= 1) {
          val distanceMultiplier = {
            if (distance <= 2000) {
              4
            } else if (distance <= 5000) {
              2.5
            } else {
              0.5
            }
          }
          Math.log(base) * distanceMultiplier
        } else {
          0
        }
      } else {
        0
      }
    }
  }
}

object IsolatedTownFeature {
  val HUB_RANGE_BRACKETS = Array(300, 600, 1200, 2400) //if pop not within X km
}

sealed case class IsolatedTownFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.ISOLATED_TOWN
  val boostRange =
    if (strength < HUB_RANGE_BRACKETS.size) { //up to 4
      HUB_RANGE_BRACKETS(strength)
    } else {
      HUB_RANGE_BRACKETS.last + 1000
    }

  import IsolatedTownFeature._
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    if (distance <= boostRange && affinity >= 3) {
      if (rawDemand < 0.01) { //up to 30
        5 + rawDemand / 0.01 * 25
      } else if (rawDemand <= 0.1) { //up to 60
        30 + rawDemand / 0.1 * 30
      } else { //up to 100
        60 + rawDemand * 0.1
      }
    } else {
      0
    }
  }
}

sealed case class OlympicsPreparationsFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.OLYMPICS_PREPARATIONS
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    0
  }
}

sealed case class OlympicsInProgressFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.OLYMPICS_IN_PROGRESS
  override def demandAdjustment(rawDemand : Double, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport, flightType : FlightType.Value, affinity : Int, distance : Int) : Double = {
    0
  }
}


object AirportFeatureType extends Enumeration {
    type AirportFeatureType = Value
    val INTERNATIONAL_HUB, VACATION_HUB, FINANCIAL_HUB, ELITE_CHARM, DOMESTIC_AIRPORT, ISOLATED_TOWN, GATEWAY_AIRPORT, OLYMPICS_PREPARATIONS, OLYMPICS_IN_PROGRESS, UNKNOWN = Value
}