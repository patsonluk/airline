package com.patson.model

object FlightCategory extends Enumeration {
    type FlightCategory = Value
    val DOMESTIC, REGIONAL, INTERCONTINENTAL = Value
}

case class FlightCateogryLimits(domestic : Int, regional : Int) {
  def +(otherValue : FlightCateogryLimits) : FlightCateogryLimits = {
//    map.foreach {
//      case (key, value) => map.update(key, map(key) + otherValue(key)) 
//    }
//    this
    FlightCateogryLimits(domestic + otherValue.domestic, regional + otherValue.regional)
  }
}