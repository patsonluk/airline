package com.patson.model

import com.patson.data.{AirportSource, CountrySource}
import com.patson.model.AirlineBaseSpecialization.FlightTypeSpecialization
import com.patson.util.AirportCache


case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  lazy val getValue : Long = {
    if (scale == 0) {
      0
    } else if (headquarter && scale == 1) { //free to start HQ
      0
    } else {
      val baseCost = (1000000 + AirportRating.rateAirport(airport).overallRating * 120000).toLong

      baseCost * Math.pow (COST_EXPONENTIAL_BASE, (scale - 1) ).toLong
    }
  }

  val COST_EXPONENTIAL_BASE = 1.7
  
  lazy val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    val baseUpkeep = (5000 + AirportRating.rateAirport(airport).overallRating * 150).toLong
    baseUpkeep * (Math.pow(COST_EXPONENTIAL_BASE, adjustedScale - 1)).toInt
  }

//  def getLinkLimit(titleOption : Option[Title.Value]) : Int = {
//    val base = 5
//    val titleBonus = titleOption match {
//      case Some(title) => CountryAirlineTitle.getLinkLimitBonus(title)
//      case None => 0
//    }
//
//    val scaleBonus =
//      if (headquarter) {
//        4 * scale
//      } else {
//        2 * scale
//      }
//
//    base + titleBonus + scaleBonus
//  }

  val getOfficeStaffCapacity = AirlineBase.getOfficeStaffCapacity(scale, headquarter)

//  val HQ_BASIC_DELEGATE = 7
//  val NON_HQ_BASIC_DELEGATE = 3
//  val delegateCapacity : Int =
//    (if (headquarter) HQ_BASIC_DELEGATE else NON_HQ_BASIC_DELEGATE) + scale / (if (headquarter) 1 else 2)

  val delegatesRequired : Int = {
    if (headquarter) {
      scale / 2
    } else {
      1 + scale / 2
    }
  }


  def getOvertimeCompensation(staffRequired : Int) = {
    if (getOfficeStaffCapacity >= staffRequired) {
      0
    } else {
      val delta = staffRequired - getOfficeStaffCapacity
      var compensation = 0
      val income = CountrySource.loadCountryByCode(countryCode).map(_.income).getOrElse(0)
      compensation += delta * (50000 + income) / 52 * 10 //weekly compensation, *10, as otherwise it's too low

      compensation
    }
  }

  /**
    * if not allowed, return LEFT[the title required]
    */
  lazy val allowAirline : Airline => Either[Title.Value, Title.Value]= (airline : Airline) => {

    val requiredTitle =
      if (airport.isGateway()) {
        Title.ESTABLISHED_AIRLINE
      } else {
        Title.PRIVILEGED_AIRLINE
      }
    val title = CountryAirlineTitle.getTitle(airport.countryCode, airline)
    if (title.title.id <= Title.ESTABLISHED_AIRLINE.id) { //lower id means higher title
      Right(requiredTitle)
    } else {
      Left(requiredTitle)
    }
  }

  lazy val getStaffModifier : (FlightCategory.Value => Double) = flightCategory => {
    val flightTypeSpecializations = specializations.filter(_.getType == BaseSpecializationType.FLIGHT_TYPE).map(_.asInstanceOf[FlightTypeSpecialization])
    if (flightTypeSpecializations.isEmpty) {
      1
    } else {
      flightTypeSpecializations.map(_.staffModifier(flightCategory)).sum - (flightTypeSpecializations.size - 1)
    }
  }

  lazy val specializations : List[AirlineBaseSpecialization.Value] = {
    (AirlineBaseSpecialization.values.filter(_.free).toList ++
    AirportSource.loadAirportBaseSpecializations(airport.id, airline.id)).filter(_.scaleRequirement <= scale)
  }
}

object AirlineBase {
  def getOfficeStaffCapacity(scale : Int, isHeadquarters : Boolean) = {
    val base =
      if (isHeadquarters) {
        60
      } else {
        0
      }
    val scaleBonus =
      if (isHeadquarters) {
        80 * scale
      } else {
        60 * scale
      }

    base + scaleBonus
  }
}




