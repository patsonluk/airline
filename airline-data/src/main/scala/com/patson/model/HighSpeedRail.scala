package com.patson.model

case class HighSpeedRail(from : Airport, to : Airport, airline : Airline, distance : Int, var capacity: LinkClassValues, var frequency : Int, var id : Int = 0) extends Transport {
  override val transportType : TransportType.Value = TransportType.HIGH_SPEED_RAIL
  override val duration = (distance.toDouble / HighSpeedRail.SPEED * 60).toInt
  override def computedQuality() : Int = HighSpeedRail.QUALITY //constant quality for now
  override val flightType : FlightType.Value = FlightType.SHORT_HAUL_DOMESTIC //has to be defined before cost, as standardPrice uses it

  override val price : LinkClassValues = LinkClassValues.getInstance() //TODO price modelling in later PR
  //hidden cost similar to generic transit. TODO actual pricing/perceived cost in later PR
  override val cost : LinkClassValues = LinkClassValues.getInstance(
    economy = (standardPrice(ECONOMY) * HighSpeedRail.COST_RATIO).toInt,
    business = (standardPrice(BUSINESS) * HighSpeedRail.COST_RATIO).toInt,
    first = (standardPrice(FIRST) * HighSpeedRail.COST_RATIO).toInt)

  override var minorDelayCount : Int = 0
  override var majorDelayCount : Int = 0
  override var cancellationCount : Int = 0

  override def toString() = {
    s"High speed rail $id; ${airline.name}; ${from.city}(${from.iata}) => ${to.city}(${to.iata}); distance $distance"
  }

  override val frequencyByClass  = (_ : LinkClass) =>  frequency
}

object HighSpeedRail {
  val QUALITY = 70
  val SPEED = 150 //km/h
  val COST_RATIO = 0.5 //placeholder, ratio of standard price as the hidden cost
  val CONNECTION_COST = 20 //slightly lower than generic transit (25)
  val ALLIANCE_CONNECTION_DISCOUNT = 0.5 //discount on connection cost if the HSR is operated by the same airline/alliance as the connecting link
}
