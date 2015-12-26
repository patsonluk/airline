package com.patson.model

object PassengerType extends Enumeration {
    val BUSINESS, TOURIST = Value
}

case class PassengerGroup(fromAirport : Airport, preference : FlightPreference, passengerType : PassengerType.Value)

