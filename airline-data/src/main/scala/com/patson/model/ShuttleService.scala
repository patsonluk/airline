package com.patson.model

case class ShuttleService(airline : Airline, allianceId : Option[Int], airport : Airport, name : String = "", level : Int, foundedCycle : Int) {
  val getValue = level * 100000000
  val getCapacity = level * 2000
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
}



