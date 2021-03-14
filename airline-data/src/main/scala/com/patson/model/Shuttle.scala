package com.patson.model

case class Shuttle(from : Airport, to : Airport, airline: Airline, distance : Int, var capacity: LinkClassValues, duration : Int, var frequency : Int, var id : Int = 0) extends Transport {
  override val transportType : TransportType.Value = TransportType.SHUTTLE

  override def computedQuality() : Int = Shuttle.QUALITY //constant quality
  override val price : LinkClassValues = LinkClassValues.getInstance()
  //override val price : LinkClassValues = LinkClassValues.getInstance() //has to have some hidden price? otherwise it will be too strong?

  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC

  override val cost = LinkClassValues.getInstance(economy = Pricing.computeStandardPrice(distance, FlightType.SHORT_HAUL_DOMESTIC, ECONOMY)) //hidden cost of taking shuttle

  val upkeep = capacity.total * Shuttle.UPKEEP_PER_CAPACITY
  override var minorDelayCount : Int = 0
  override var majorDelayCount : Int = 0
  override var cancellationCount : Int = 0
}

object Shuttle {
  val QUALITY = 40
  val UPKEEP_PER_CAPACITY = 10
}
