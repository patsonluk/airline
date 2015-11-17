package com.patson.model

case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, size : Int, var power : Long, var population : Long, slots : Int, availableSlots : Int) extends IdObject {
  val citiesServed = scala.collection.mutable.MutableList[(City, Double)]()
  val airlineAppeals = scala.collection.mutable.Map[Airline, AirlineAppeal]()
  def addCityServed(city : City, share : Double) {
    citiesServed += Tuple2(city, share)
  }
  def setAirlineLoyalty(airline : Airline, value : Double) {
    val oldAppeal = airlineAppeals.getOrElse(airline, AirlineAppeal(0, 0))
    airlineAppeals.put(airline, AirlineAppeal(value, oldAppeal.awareness))
  }
  def setAirlineAwareness(airline : Airline, value : Double) {
    val oldAppeal = airlineAppeals.getOrElse(airline, AirlineAppeal(0, 0))
    airlineAppeals.put(airline, AirlineAppeal(oldAppeal.loyalty, value))
  }
}

case class AirlineAppeal(loyalty : Double, awareness : Double)
object AirlineAppeal {
  val MAX_LOYALTY = 100
  val MAX_AWARENESS = 100
}

object Airport {
  def fromId(id : Int) = {
    val airportWithJustId = Airport("", "", "", 0, 0, "", "", 0, 0, 0, 0, 0)
    airportWithJustId.id = id
    airportWithJustId
  }
}

case class Runway(length : Int, runwayType : RunwayType.Value)

object RunwayType extends Enumeration {
    type RunwayType = Value
    val Asphalt, Concrete, Gravel = Value
}


