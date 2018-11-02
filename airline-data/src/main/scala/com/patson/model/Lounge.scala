package com.patson.model

case class Lounge(airline : Airline, allianceId : Option[Int], airport : Airport, level : Int, status : LoungeStatus.Value, foundedCycle : Int) {
  def getValue : Long = {
    level * 200000000 
  }
  
  val getUpkeep : Long = {
    (10000 + airport.income) * 5 * level 
  }
  
  //to be considered active, it should have passenger ranking smaller (ie higher) or equals to this value)
  val getActiveRankingThreshold : Int = {
    if (level <= 4) {
      1
    } else {
      (airport.size - 1) / 2
    }
  }
}

object Lounge {
  val PER_VISITOR_COST = 30 //how much extra cost to serve 1 visitor
  val PER_VISITOR_CHARGE = 50 //how much to charge an airline (self and alliance member) per 1 visitor. This has to be higher to make popular lounge profitable
}

object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE = Value
}


