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
      
    return baseCost * Math.pow(2, (scale - 1)).toLong //to upgrade to scale 3, it would be 12 * 4 =  48 million, to upgrade to scale 9,it would be 12 * 256 = 3072 million 
  }
  
  val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    (10000 + airport.income) / 10 * airport.size * (Math.pow(2, adjustedScale - 1)).toInt  / (if (headquarter) 1 else 2)
  }

  def getLinkLimit(titleOption : Option[Title.Value]) : Int = {
    val bonus = titleOption match {
      case Some(title) => CountryAirlineTitle.getLinkLimitBonus(title)
      case None => 0
    }

    val baseLimit = 10 + 5 * scale
    baseLimit + bonus
  }

  def getOvertimeCompensation(linkLimit : Int, linkCount : Int) = {
    if (linkLimit >= linkCount) {
      0
    } else {
      val delta = linkCount - linkLimit
      var compensation = 0
      val income = CountrySource.loadCountryByCode(countryCode).map(_.income).getOrElse(0)
      for (i <- 1 to delta) {
        val multiplier = if (i < 10) i else 10 //first 10 increasingly more expensive, then max out at 10
        compensation += multiplier * (20000 + income)
      }
      compensation
    }
  }
}


