package com.patson.model

case class Link(from : Airport, to : Airport, airline: Airline, price : Double, distance : Double, capacity: Int) extends IdObject{
  var availableSeats : Int = capacity
}
