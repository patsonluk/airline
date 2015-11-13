package com.patson.model

import com.patson.model.airplane.Airplane

case class Link(from : Airport, to : Airport, airline: Airline, price : Int, distance : Int, capacity: Int) extends IdObject{
  var availableSeats : Int = capacity
  var assignedAirplanes : List[Airplane] = _
}
