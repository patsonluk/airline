package com.patson.model

case class AirportInfo(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, size : Int, power : Long = 0) {
  val citiesServed = scala.collection.mutable.MutableList[CityInfo]()
  def addCityServed(city : CityInfo) {
    citiesServed += city
  }
}

case class CityInfo(name : String, latitude : Double, longitude : Double, countryCode : String, population: Int, power : Long)