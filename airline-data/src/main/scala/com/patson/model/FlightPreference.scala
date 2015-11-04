package com.patson.model

abstract class FlightPreference {
  def computeCost(link : Link) : Double  
}


case class SimplePreference(distanceWeight : Double, priceWeight : Double) extends FlightPreference{
  def computeCost(link : Link) = {
    link.distance * distanceWeight + link.price * priceWeight
  }
}


