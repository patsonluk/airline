package com.patson.model

case class HighSpeedRail(from : Airport, to : Airport, airline : Airline, distance : Int, var capacity: LinkClassValues, var frequency : Int, var id : Int = 0) extends Transport {
  override val transportType : TransportType.Value = TransportType.HIGH_SPEED_RAIL
  override val duration = (distance.toDouble / HighSpeedRail.SPEED * 60).toInt
  override def computedQuality() : Int = HighSpeedRail.QUALITY //constant quality for now
  override val price : LinkClassValues = LinkClassValues.getInstance() //TODO price modelling in next part
  override val cost : LinkClassValues = LinkClassValues.getInstance() //TODO cost modelling in next part

  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC

  override var minorDelayCount : Int = 0
  override var majorDelayCount : Int = 0
  override var cancellationCount : Int = 0

  override def toString() = {
    s"High speed rail $id; ${airline.name}; ${from.city}(${from.iata}) => ${to.city}(${to.iata}); distance $distance"
  }

  override val frequencyByClass  = (_ : LinkClass) =>  frequency
}

object HighSpeedRail {
  val QUALITY = 60
  val SPEED = 250 //km/h
}
