package com.patson.model

import com.patson.data.{AirlineSource, AirportSource, CountrySource}
import com.patson.model.AirlineBaseSpecialization.FlightTypeSpecialization
import com.patson.util.AirportCache


case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  lazy val getValue : Long = {
    if (scale == 0) {
      0
    } else if (headquarter && scale == 1) { //free to start HQ
      0
    } else {
      var baseCost = (1000000 + airport.rating.overallRating * 120000).toLong
      (baseCost * airportTypeMultiplier * airportSizeRatio * Math.pow (COST_EXPONENTIAL_BASE, (scale - 1) )).toLong
    }
  }

  val COST_EXPONENTIAL_BASE = 1.7

  lazy val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    var baseUpkeep = (5000 + airport.rating.overallRating * 150).toLong

    (baseUpkeep * airportTypeMultiplier * airportSizeRatio * Math.pow(COST_EXPONENTIAL_BASE, adjustedScale - 1)).toInt
  }

  lazy val airportTypeMultiplier =
    if (airport.isDomesticAirport()) {
      0.7
    } else if (airport.isGateway()) {
      1.1
    } else {
      1.0
    }

  lazy val airportSizeRatio =
    if (airport.size > 7) {
      1.0
    } else { //discount for size < 7
      0.3 + airport.size * 0.1
    }

  val getOfficeStaffCapacity = AirlineBase.getOfficeStaffCapacity(scale, headquarter)

  val delegatesRequired = {
    if (headquarter) {
      Math.max(0, Math.ceil(scale.toDouble / 2) - 1)
    } else {
      Math.ceil(scale.toDouble / 2)
    }
  }.toInt

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
    if (title.title.id <= requiredTitle.id) { //lower id means higher title
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

  def delete(): Unit = {
    AirlineSource.loadLoungeByAirlineAndAirport(airline.id, airport.id).foreach { lounge =>
      AirlineSource.deleteLounge(lounge)
    }

    //remove all base spec and bonus since it has no foreign key on base
    specializations.foreach { spec =>
      spec.unapply(airline, airport)
    }
    AirportSource.updateAirportBaseSpecializations(airport.id, airline.id, List.empty)
    //then delete the base itself
    AirlineSource.deleteAirlineBase(this)
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




