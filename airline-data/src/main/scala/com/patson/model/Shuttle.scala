package com.patson.model

case class Shuttle(from : Airport, to : Airport, airline: Airline, distance : Int, var capacity: LinkClassValues, duration : Int, var frequency : Int, var id : Int = 0) extends Transport {
  override val transportType : TransportType.Value = TransportType.SHUTTLE

  override def computedQuality() : Int = 40 //constant quality
  override val price : LinkClassValues = LinkClassValues.getInstance()
  //override val price : LinkClassValues = LinkClassValues.getInstance() //has to have some hidden price? otherwise it will be too strong?

  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC

  override val cost = LinkClassValues.getInstance(economy = Pricing.computeStandardPrice(distance, FlightType.SHORT_HAUL_DOMESTIC, ECONOMY)) //hidden cost of taking shuttle
}
