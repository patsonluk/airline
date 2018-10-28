package com.patson.model

case class Lounge(airline : Airline, alliance : Option[Alliance], airport : Airport, level : Int, foundedCycle : Int) {
  def getValue : Long = {
    level * 200000000 
  }
  
  val getUpkeep : Long = {
    (10000 + airport.income) * 5 * level 
  }
}


