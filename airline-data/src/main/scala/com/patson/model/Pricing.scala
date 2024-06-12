package com.patson.model
import FlightType._

/**
 * Cost base model
 */
object Pricing {
  //base 10
  //200 km = 10 + 40
  //1000 km = 50 + 100 = 150  (800 * 0.125) // 250
  //2000 km = 150 + 100 = 250  (1000 * 0.1) // 350
  //10000 km = 150 + 900 = 1050  (9000 * 0.1) // 750
  val modifierBrackets = List((200, 0.2),(800, 0.125),(Int.MaxValue, 0.1))
  val INTERNATIONAL_PRICE_MULTIPLIER = 1.05

  def computeStandardPrice(link : Link, linkClass : LinkClass) : Int = {
    computeStandardPrice(link.distance, link.flightType, linkClass)
  }
  def computeStandardPrice(distance : Int, flightType : FlightType, linkClass : LinkClass) : Int = {
    var remainDistance = distance
    var price = 10.0
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
    
    (price * 1.20).toInt //increase the standard price by 20%
    // 150 * 1.2 = 180    250 * 1.5 = 375 | 57%
    // 250 * 1.2 = 300    350 * 1.5 = 525
    // 1050 * 1.2 = 1260  750 * 1.5 = 1125
  }
  
//  def computeStandardPriceForAllClass(distance : Int, fromAirport : Airport, toAirport : Airport) : LinkClassValues = {
//    val priceByLinkClass : List[(LinkClass, Int)] = LinkClass.values.map { linkClass =>
//      (linkClass, computeStandardPrice(distance, Computation.getFlightType(fromAirport, toAirport, distance), linkClass))
//    }
//    LinkClassValues.getInstanceByMap(priceByLinkClass.toMap)
//  }
  
  def computeStandardPriceForAllClass(distance : Int, flightType : FlightType.Value) : LinkClassValues = {
    val priceByLinkClass : List[(LinkClass, Int)] = LinkClass.values.map { linkClass =>
      (linkClass, computeStandardPrice(distance, flightType, linkClass))
    }
    LinkClassValues.getInstanceByMap(priceByLinkClass.toMap)
  }

}