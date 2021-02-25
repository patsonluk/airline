package com.patson.model.negotiation

import com.patson.model.{Airline, Airport, IdObject}

case class LinkNegotiationDiscount(airline : Airline, fromAirport : Airport, toAirport : Airport, discount : BigDecimal, expiry: Int, var id : Int = 0) extends IdObject

object LinkNegotiationDiscount {
  val DURATION = 30
}
