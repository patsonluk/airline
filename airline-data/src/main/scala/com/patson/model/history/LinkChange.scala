package com.patson.model.history

import com.patson.model.airplane.Model
import com.patson.model.{Airline, Airport, Alliance, Country, LinkClassValues}

case class LinkChange(linkId : Int, price : LinkClassValues, priceDelta : LinkClassValues, capacity : LinkClassValues, capacityDelta : LinkClassValues, fromAirport : Airport, toAirport : Airport, fromCountry : Country, toCountry : Country, fromZone : String, toZone : String, airline : Airline, alliance : Option[Alliance], frequency : Int, flightNumber : Int, airplaneModel : Model, rawQuality : Int, cycle : Int, var id : Int = 0)

 

