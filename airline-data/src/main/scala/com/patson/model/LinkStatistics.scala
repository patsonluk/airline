package com.patson.model

case class LinkStatistics(key : LinkStatisticsKey, passengers : Int, cycle : Int)
case class LinkStatisticsKey(fromAirport : Airport, toAirport : Airport, isDeparture : Boolean, isDestination : Boolean, airline : Airline)