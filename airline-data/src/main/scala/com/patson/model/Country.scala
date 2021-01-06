package com.patson.model

import com.patson.data.AirportSource

case class Country(countryCode : String, name : String, airportPopulation : Int, income : Int, openness : Int) {
//  lazy val championBonusRankingCount = { //this one HAS to be lazy, otherwise it runs into a loop -> Computation.computeReputationBoost will try to get the model power which will loop back to here
//    val firstBonus = Computation.computeReputationBoost(this, 1)
//
//    if (firstBonus > 10) {
//      10
//    } else {
//      5
//    }
//  }
}

object Country {
  val MAX_OPENNESS : Int = 10
  val HOSTILE_RELATIONSHIP_THRESHOLD = -2
  val INTERNATIONAL_INBOUND_MIN_OPENNESS = 2;
  val OPEN_DOMESTIC_MARKET_MIN_OPENNESS = 4;
  val SIXTH_FREEDOM_MIN_OPENNESS = 7
  val LOW_INCOME_THRESHOLD = 20000
  val HIGH_INCOME_THRESHOLD = 50000
  val DEFAULT_UNKNOWN_INCOME = 1000

  def fromCode(countryCode : String) = Country(countryCode, countryCode, 0, 0, 0)
}