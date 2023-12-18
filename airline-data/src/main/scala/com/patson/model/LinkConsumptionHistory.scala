package com.patson.model

case class LinkConsumptionHistory(link : Link, passengerCount : Int, homeAirport : Airport, destinationAirport : Airport, passengerType : PassengerType.Value, preferenceType : FlightPreferenceType.Value, preferredLinkClass : LinkClass, linkClass : LinkClass, satisfaction : Double)
