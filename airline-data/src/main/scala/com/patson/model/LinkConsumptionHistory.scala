package com.patson.model

case class LinkConsumptionHistory(link : Link, passengerCount : Int, homeCountryCode : String, passengerType : PassengerType.Value, preferenceType : FlightPreferenceType.Value, preferredLinkClass : LinkClass, linkClass : LinkClass, satisfaction : Float)
