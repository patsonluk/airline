package com.patson.model

case class Lounge(airline : Airline, allianceId : Option[Int], airport : Airport, name : String = "", level : Int, status : LoungeStatus.Value, foundedCycle : Int) {
  def getValue : Long = {
    level * 50000000 
  }
  
  val getUpkeep : Long = {
    if (status == LoungeStatus.ACTIVE) (10000 + airport.baseIncome) * 5 * level else 0 //use base income for calculation here
  }

  val rankingThreshold = Map(
    5 -> 2,
    6 -> 3,
    7 -> 4,
    8 -> 4,
    9 -> 5,
    10 -> 5
  )
  //to be considered active, it should have passenger ranking smaller (ie higher) or equals to this value)
  val getActiveRankingThreshold : Int = {
    rankingThreshold.getOrElse(airport.size, 1)
  }
  
  val baseReduceRate = 0.005 + level * 0.01
  val getPriceReduceFactor : (Int => Double) = flightDistance => -1 * (baseReduceRate * Math.max(0.5, Math.min(1.0, flightDistance / 10000.0)))
  
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


