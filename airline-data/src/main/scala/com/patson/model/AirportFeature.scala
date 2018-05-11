package com.patson.model

import com.patson.model.airplane.Model
import com.patson.model.airplane.Model.Type
import FlightType._


abstract class AirportFeature {
  val MAX_STREGTH = 10
  def strength : Int
  //def airportId : Int
  def featureType : AirportFeatureType.Value
  val strengthFactor : Double = strength.toDouble / MAX_STREGTH
  
  def demandAdjustment(rawDemand : Int, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport) : Int
}

object AirportFeature {
  import AirportFeatureType._
  def apply(featureType : AirportFeatureType, strength : Int) : AirportFeature = {
    featureType match {
      case INTERNATIONAL_HUB => InternationalHubFeature(strength)  
      case VACATION_HUB => VacationHubFeature(strength)
      case FINANCIAL_HUB => FinancialHubFeature(strength)
      case DOMESTIC_AIRPORT => DomesticAirportFeature(strength)
    }
  }
}

sealed case class InternationalHubFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.INTERNATIONAL_HUB
  override def demandAdjustment(rawDemand : Int, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport) : Int = {
    if (airportId == toAirport.id) { //only affect if as a destination
      val multiplier =
        if (passengerType == PassengerType.BUSINESS) { //more obvious for business travelers
          4  
        } else {
          1
        }
      val flightType = Computation.getFlightType(fromAirport, toAirport)
      if (flightType == SHORT_HAUL_INTERNATIONAL || flightType == SHORT_HAUL_INTERCONTINENTAL) {
        (rawDemand * (strengthFactor * 0.5) * multiplier).toInt //at MAX_STREGTH, add 2x for business traveler, 0.5x for tourists (short haul)   
      } else if (flightType == LONG_HAUL_INTERNATIONAL || flightType == LONG_HAUL_INTERCONTINENTAL || flightType == ULTRA_LONG_HAUL_INTERCONTINENTAL) {
        (rawDemand * (strengthFactor * 1) * multiplier).toInt //at MAX_STREGTH, add 4x for business traveler, 1x for tourists (long haul)
      } else {
        0
      }
    } else {
      0
    }
  }
}

sealed case class VacationHubFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.VACATION_HUB
  override def demandAdjustment(rawDemand : Int, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport) : Int = {
    val flightType = Computation.getFlightType(fromAirport, toAirport)
    if (toAirport.id == airportId && passengerType == PassengerType.TOURIST) { //only affect if as a destination and tourists
      val goFactor = { //out of how many people, will there be 1 going to this spot per year
        if (flightType == SHORT_HAUL_DOMESTIC || flightType == LONG_HAUL_DOMESTIC) {
          5
        } else if (flightType == SHORT_HAUL_INTERNATIONAL) {
          15
        } else {
          30
        }
      }
      (fromAirport.population / goFactor / 52 * fromAirport.income / 50000  * strengthFactor).toInt //assume in a city of 50k income out of goFactor people, 1 will visit this spot at full strength (10)
    } else {
      0
    }
  }
}

sealed case class FinancialHubFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.FINANCIAL_HUB
  override def demandAdjustment(rawDemand : Int, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport) : Int = {
    val flightType = Computation.getFlightType(fromAirport, toAirport)
    if (toAirport.id == airportId && passengerType == PassengerType.BUSINESS) { //only affect if as a destination and tourists
      val goFactor = 100 //out of how many people, will there be 1 going to this spot per year

      (fromAirport.population / goFactor / 52 * fromAirport.income / 50000  * strengthFactor).toInt //assume in a city of 50k income out of goFactor people, 1 will visit this spot
    } else {
      0
    }
  }
}

sealed case class DomesticAirportFeature(strength : Int) extends AirportFeature {
  val featureType = AirportFeatureType.DOMESTIC_AIRPORT
  override def demandAdjustment(rawDemand : Int, passengerType : PassengerType.Value, airportId : Int, fromAirport : Airport, toAirport : Airport) : Int = {
    val flightType = Computation.getFlightType(fromAirport, toAirport)
    if (flightType == SHORT_HAUL_DOMESTIC || flightType == LONG_HAUL_DOMESTIC) {
       (rawDemand * (strengthFactor)).toInt // * 2 demand 
    } else {
       (-1 * rawDemand * (strengthFactor)).toInt //remove all demand if it's a purely domestic one (stregth 10)
    }
  }
}

object AirportFeatureType extends Enumeration {
    type AirportFeatureType = Value
    val INTERNATIONAL_HUB, VACATION_HUB, FINANCIAL_HUB, DOMESTIC_AIRPORT, UNKNOWN = Value
    def getDescription(featureType : AirportFeatureType) = {
      featureType match {
        case INTERNATIONAL_HUB => "Internation Hub"
        case VACATION_HUB => "Vacation Hub"
        case FINANCIAL_HUB => "Financial Hub"
        case DOMESTIC_AIRPORT => "Domestic Airport"
        case UNKNOWN => "Unknown"
      }
    }
}