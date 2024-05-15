package com.patson.model
import FlightType._

/**
 * Cost base model
 */
object Pricing {
  //base 100
  //200 km = 100 + 50
  //1000 km = 150 + 100 = 250  (800 * 0.125) 
  //2000 km = 250 + 100 = 350  (1000 * 0.1)
  //10000 km = 350 + 400 = 750 (8000 * 0.05)
  val modifierBrackets = List((200, 0.25),(800, 0.125),(1000, 0.1),(Int.MaxValue, 0.05))
  val INTERNATIONAL_PRICE_MULTIPLIER = 1.05

  def computeStandardPrice(link : Link, linkClass : LinkClass) : Int = {
    computeStandardPrice(link.distance, link.flightType, linkClass)
  }
  def computeStandardPrice(distance : Int, flightType : FlightType, linkClass : LinkClass) : Int = {
    var remainDistance = distance
    var price = 100.0
    for (priceBracket <- modifierBrackets if(remainDistance > 0)) {
      if (priceBracket._1 >= remainDistance) {
        price += remainDistance * priceBracket._2
      } else {
        price += priceBracket._1 * priceBracket._2
      }
      remainDistance -= priceBracket._1
    }
    price = ((flightType match {
      case SHORT_HAUL_INTERNATIONAL | MEDIUM_HAUL_INTERNATIONAL | LONG_HAUL_INTERNATIONAL | ULTRA_LONG_HAUL_INTERCONTINENTAL => (price * INTERNATIONAL_PRICE_MULTIPLIER)
      case _ => price
    }) * linkClass.priceMultiplier).toInt
    
    (price * 1.5).toInt //increase the standard price by 50%
  }
  
  def computeStandardPriceForAllClass(distance : Int, fromAirport : Airport, toAirport : Airport) : LinkClassValues = {
    val priceByLinkClass : List[(LinkClass, Int)] = LinkClass.values.map { linkClass =>
      (linkClass, computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), linkClass))
    }
    LinkClassValues.getInstanceByMap(priceByLinkClass.toMap)
  }
  
  def computeStandardPriceForAllClass(distance : Int, flightType : FlightType.Value) : LinkClassValues = {
    val priceByLinkClass : List[(LinkClass, Int)] = LinkClass.values.map { linkClass =>
      (linkClass, computeStandardPrice(distance, flightType, linkClass))
    }
    LinkClassValues.getInstanceByMap(priceByLinkClass.toMap)
  }

}