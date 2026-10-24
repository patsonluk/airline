package com.patson.model

case class GenericTransit(from : Airport, to : Airport, distance : Int, var capacity: LinkClassValues, var id : Int = 0) extends Transport {
  override val transportType : TransportType.Value = TransportType.GENERIC_TRANSIT
  override val duration = GenericTransit.computeDuration(distance)
  override var frequency : Int = 24 * 7
  override def computedQuality() : Int = GenericTransit.QUALITY //constant quality
  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC

  override val price : LinkClassValues = LinkClassValues.getInstance(economy = Pricing.computeStandardPrice(distance, FlightType.SHORT_HAUL_DOMESTIC, ECONOMY)) * 0.25 //ticket price pax pay for local transit

  val upkeep = 0
  override var minorDelayCount : Int = 0
  override var majorDelayCount : Int = 0
  override var cancellationCount : Int = 0

  override def toString() = {
    s"Generic transit $id; ${from.city}(${from.iata}) => ${to.city}(${to.iata}); distance $distance"
  }

  override val frequencyByClass  = (_ : LinkClass) =>  frequency
  override val airline : Airline = GenericTransit.TRANSIT_PROVIDER
}

object GenericTransit {
  val QUALITY = 35
  val TRANSIT_PROVIDER = Airline.fromId(0).copy(name = "Local Transit")
  val SPEED = 30 //km/h

  def computeDuration(distance : Int) : Int = (distance.toDouble / SPEED * 60).toInt
}

