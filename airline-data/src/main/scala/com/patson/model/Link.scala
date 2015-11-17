package com.patson.model

import com.patson.model.airplane.Airplane

case class Link(from : Airport, to : Airport, airline: Airline, price : Int, distance : Int, capacity: Int, quality : Int, duration : Int, frequency : Int) extends IdObject{
  var availableSeats : Int = capacity
  var assignedAirplanes : List[Airplane] = List.empty
}

case class LinkWithCost(link : Link, cost : Double, inverted : Boolean) {
    def from : Airport = if (inverted) link.to else link.from
    def to : Airport = if (inverted) link.from else link.to
    
    override def toString() : String = {
      this.getClass.getSimpleName + "(" + from.name + " => " + to.name + " (inverted?) " + inverted + ")"
    }
}