package com.patson.model

case class LinkConsumptionDetails(linkId : Int, price : LinkClassValues, capacity: LinkClassValues, soldSeats: LinkClassValues, fuelCost : Int, crewCost : Int, airportFees: Int, inflightCost : Int, fixedCost : Int, depreciation : Int, revenue : Int, profit : Int, fromAirportId : Int, toAirportId : Int, airlineId : Int, distance : Int, cycle : Int, var id : Int = 0) extends IdObject
