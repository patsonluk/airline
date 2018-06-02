package com.patson.model

case class LinkConsumptionDetails(linkId : Int, price : LinkClassValues, capacity: LinkClassValues, soldSeats: LinkClassValues, quality : Int, fuelCost : Int, crewCost : Int, airportFees: Int, inflightCost : Int, maintenanceCost : Int, revenue : Int, profit : Int, fromAirportId : Int, toAirportId : Int, airlineId : Int, distance : Int, cycle : Int, var id : Int = 0) extends IdObject
