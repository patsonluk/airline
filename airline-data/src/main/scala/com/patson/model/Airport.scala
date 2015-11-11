package com.patson.model

case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, size : Int, var power : Long = 0) extends IdObject {
  val citiesServed = scala.collection.mutable.MutableList[City]()
  val airlineLoyalties = scala.collection.mutable.Map[Airline, Int]()
  def addCityServed(city : City) {
    citiesServed += city
  }
  def setAirlineLoyalty(airline : Airline, value : Int) {
    airlineLoyalties.put(airline, value)
  }
}

object Airport {
  def fromId(id : Int) = {
    val airportWithJustId = Airport("", "", "", 0, 0, "", "", 0, 0)
    airportWithJustId.id = id
    airportWithJustId
  }
}

