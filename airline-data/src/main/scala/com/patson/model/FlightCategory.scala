package com.patson.model

object FlightCategory extends Enumeration {
    type FlightCategory = Value
    val DOMESTIC, REGIONAL, INTERCONTINENTAL = Value
}

case class FlightCateogryLimits(domestic : Int, regional : Int) {
  def +(otherValue : FlightCateogryLimits) : FlightCateogryLimits = {
    FlightCateogryLimits(domestic + otherValue.domestic, regional + otherValue.regional)
  }
  
  def *(multiplier : Int) : FlightCateogryLimits = {
    FlightCateogryLimits(domestic * multiplier, regional * multiplier)
  }
}