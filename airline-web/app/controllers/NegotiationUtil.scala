package controllers

import com.patson.model.Link
import com.patson.model.Airport
import com.patson.model.Computation
import com.patson.model.FlightType
import com.patson.model.LinkClassValues
import com.patson.model.ECONOMY
import com.patson.model.BUSINESS
import com.patson.model.FIRST

object NegotiationUtil {
  val NEW_LINK_BASE_COST = 100
   def getLinkNegotationInfo(existingLinkOption : Option[Link], fromAirport : Airport, toAirport : Airport, newCapacity : LinkClassValues, newFrequency : Int) : NegotationInfo = {
     import FlightType._
     
     existingLinkOption.foreach{ link =>
       if (link.capacity == newCapacity) {
           return NegotationInfo(1, 0)
       }
     }
     
     val flightTypeMultiplier = Computation.getFlightType(fromAirport, toAirport) match {
       case SHORT_HAUL_DOMESTIC => 1
       case LONG_HAUL_DOMESTIC => 1.5
       case SHORT_HAUL_INTERNATIONAL => 2
       case LONG_HAUL_INTERNATIONAL => 2.5
       case SHORT_HAUL_INTERCONTINENTAL => 4
       case LONG_HAUL_INTERCONTINENTAL => 5
       case ULTRA_LONG_HAUL_INTERCONTINENTAL => 5
     }
     
     val baseNegotationCost = if (existingLinkOption.isDefined) 0 else NEW_LINK_BASE_COST
     val existingCapacity = existingLinkOption.map(_.capacity).getOrElse(LinkClassValues.getInstance())
     val capacityChange : Double = normalizedCapacity(newCapacity) - normalizedCapacity(existingCapacity)
     val capacityChangeCost = 
       if (capacityChange < 0) { //reducing capacity
         Math.ceil(capacityChange * -1 / 100)
       } else {
          Math.ceil(capacityChange / 100) 
       }
      
     val cost = ((baseNegotationCost + capacityChangeCost) * flightTypeMultiplier).toInt
     
     return NegotationInfo(0.5, cost)
   }
  
  val normalizedCapacity : LinkClassValues => Double = (capacity : LinkClassValues) => {
    capacity(ECONOMY) * ECONOMY.spaceMultiplier + capacity(BUSINESS) * BUSINESS.spaceMultiplier + capacity(FIRST) * FIRST.spaceMultiplier  
  }
}

case class NegotationInfo(odds : Double, requiredPoints : Int) {
}