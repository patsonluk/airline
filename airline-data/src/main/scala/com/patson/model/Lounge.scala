package com.patson.model

case class Lounge(airline : Airline, allianceId : Option[Int], airport : Airport, level : Int, status : LoungeStatus.Value, foundedCycle : Int) {
  def getValue : Long = {
    level * 200000000 
  }
  
  val getUpkeep : Long = {
    (10000 + airport.income) * 5 * level 
  }
}

object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE = Value
}


