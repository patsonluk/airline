package com.patson.model

case class Country(countryCode : String, name : String, airportPopulation : Int, income : Int, openness : Int)

object Country {
  val MAX_OPENNESS : Int = 10
  val HOSTILE_RELATIONSHIP_THRESHOLD = -2
  val INTERNATIONAL_INBOUND_MIN_OPENNESS = 4;
  val OPEN_DOMESTIC_MARKET_MIN_OPENNESS = 6;
  val SIXTH_FREEDOM_MIN_OPENNESS = 8
}