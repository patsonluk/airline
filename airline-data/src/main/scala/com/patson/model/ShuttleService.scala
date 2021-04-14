package com.patson.model

case class ShuttleService(airline : Airline, allianceId : Option[Int], airport : Airport, name : String = "", level : Int, foundedCycle : Int) {
  val getValue = level * 25000000
  val getCapacity = level * 2000

  val basicUpkeep : Long = {
    (10000 + airport.income) * 0.3 * level
  }.toLong
}

object ShuttleService {
  def getBaseScaleRequirement(level : Int) = {
    if (level == 3) {
      11
    } else if (level == 2) {
      8
    } else {
      5
    }
  }

  val COVERAGE_RANGE = 50
}



