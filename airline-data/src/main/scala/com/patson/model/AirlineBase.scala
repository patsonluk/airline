package com.patson.model

case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  val getAirportCredits = scale * 50  * (if (headquarter) 2 else 1) 
  def getUpgradeCost(scale : Int) : Long = {
    val baseCost = (200000 + airport.income * 30) * airport.size //for a airport size 7, income 50k city, it will be 12 million base
    
    baseCost * Math.pow(3, (scale - 1)).toLong //to upgrade to scale 9, it would be 12 * 9 =  108 million
  }
  
  val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    (10000 + airport.income) / 10 * airport.size * (Math.pow(3, adjustedScale - 1)).toInt  / (if (headquarter) 1 else 2) 
  }
}


