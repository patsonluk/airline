package com.patson.model

import com.patson.data.CountrySource

case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  def getValue : Long = {
    if (scale == 0) {
      return 0
    }
    if (headquarter && scale == 1) { //free to start HQ
      return 0
    } 
    
    val baseCost = (200000 + airport.income * 30) * airport.size //for a airport size 7, income 50k city, it will be 12 million base
      
    return baseCost * Math.pow(COST_EXPONENTIAL_BASE, (scale - 1)).toLong
  }

  val COST_EXPONENTIAL_BASE = 1.7
  
  val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    (10000 + airport.income) / 10 * airport.size * (Math.pow(COST_EXPONENTIAL_BASE, adjustedScale - 1)).toInt  / (if (headquarter) 1 else 2)
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

  val getOfficeStaffCapacity = {
    val base =
      if (headquarter) {
        60
      } else {
        0
      }
    val scaleBonus =
      if (headquarter) {
        80 * scale
      } else {
        60 * scale
      }

    base + scaleBonus
  }

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
  val allowAirline : Airline => Either[Title.Value, Title.Value]= (airline : Airline) => {

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
}


