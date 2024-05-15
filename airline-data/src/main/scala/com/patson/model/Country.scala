package com.patson.model

import com.patson.data.AirportSource

case class Country(countryCode : String, name : String, airportPopulation : Int, income : Int, openness : Int, gini: Double)

object Country {
  type CountryCode = String

  val MAX_OPENNESS : Int = 10
  val SIXTH_FREEDOM_MIN_OPENNESS = 7
  val LOW_INCOME_THRESHOLD = 20000
  val HIGH_INCOME_THRESHOLD = 50000
  val DEFAULT_UNKNOWN_INCOME = 1000

  def fromCode(countryCode : String) = Country(countryCode, countryCode, 0, 0, 0, 0)
}