package com.patson.model

case class LinkConsumptionDetails(linkId : Int, price : Int, capacity: Int, soldSeats: Int, fuelCost : Int, crewCost : Int, fixedCost : Int, revenue : Int, profit : Int, fromAirportId : Int, toAirportId : Int, airlineId : Int, distance : Int, cycle : Int) extends IdObject
