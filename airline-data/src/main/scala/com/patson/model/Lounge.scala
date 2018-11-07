package com.patson.model

case class Lounge(airline : Airline, allianceId : Option[Int], airport : Airport, name : String = "", level : Int, status : LoungeStatus.Value, foundedCycle : Int) {
  def getValue : Long = {
    level * 50000000 
  }
  
  val getUpkeep : Long = {
    (10000 + airport.income) * 5 * level 
  }
  
  //to be considered active, it should have passenger ranking smaller (ie higher) or equals to this value)
  val getActiveRankingThreshold : Int = {
    if (airport.size <= 4) {
      1
    } else {
      (airport.size - 1) / 2
    }
  }
  
  
  val getPriceReduceFactor : Double = 1 - (0.025 + level * 0.005) 
  
}

object Lounge {
  val PER_VISITOR_COST = 50 //how much extra cost to serve 1 visitor
  val PER_VISITOR_CHARGE = 100 //how much to charge an airline (self and alliance member) per 1 visitor. This has to be higher to make popular lounge profitable
  val MAX_LEVEL = 3
  val LOUNGE_PASSENGER_AIRPORT_SIZE_REQUIREMENT = 4 //lounge passenger only spawn if from and to airport fulfills this
  
  def getBaseScaleRequirement(loungeLevel : Int) = {
    if (loungeLevel == 3) {
      9
    } else if (loungeLevel == 2) {
      6
    } else {
      3
    }
  }
}

object LoungeStatus extends Enumeration {
  type LoungeStatus = Value
  val ACTIVE, INACTIVE = Value
}


