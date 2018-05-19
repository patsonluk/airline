package com.patson.model

case class Country(countryCode : String, name : String, airportPopulation : Int, income : Int, openness : Int)

object Country {
  val MAX_OPENNESS : Int = 10
}