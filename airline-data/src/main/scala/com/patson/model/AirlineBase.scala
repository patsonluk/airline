package com.patson.model

case class AirlineBase(airline : Airline, airport : Airport, countryCode : String, scale : Int, foundedCycle : Int, headquarter : Boolean = false) {
  val getAirportCredits = scale * 50
  def getUpgradeCost(scale : Int) : Long = {
    val baseCost = (100000 + airport.income * 10) * airport.size //for a airport size 7, income 50k city, it will be 4.2 million base
    
    baseCost * Math.pow(3, (scale - 1)).toLong //to upgrade to scale 3, it would be 4.2 * 9 = 37.8 million
  }
  
  val getUpkeep : Long = {
    val adjustedScale = if (scale == 0) 1 else scale //for non-existing base, calculate as if the base is 1
    (10 * airport.income * (Math.pow(3, adjustedScale - 1)).toInt / 52)  / (if (headquarter) 1 else 2)//assume scale 1 HQ is 10 people's annual salary, other base half 
  }
}


