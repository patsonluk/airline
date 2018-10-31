package com.patson.model

case class Lounge(airline : Airline, allianceId : Option[Int], airport : Airport, level : Int, status : LoungeStatus.Value, foundedCycle : Int) {
  def getValue : Long = {
    level * 200000000 
  }
  
  val getUpkeep : Long = {
    (10000 + airport.income) * 5 * level 
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


