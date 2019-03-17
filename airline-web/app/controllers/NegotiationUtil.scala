package controllers

import com.patson.model.Link
import com.patson.model.Airport
import com.patson.model.Computation
import com.patson.model.FlightType
import com.patson.model.LinkClassValues
import com.patson.model.ECONOMY
import com.patson.model.BUSINESS
import com.patson.model.FIRST
import scala.collection.mutable.ListBuffer
import scala.util.Random

object NegotiationUtil {
  val NEW_LINK_BASE_COST = 100
   def getLinkNegotationInfo(existingLinkOption : Option[Link], fromAirport : Airport, toAirport : Airport, newCapacity : LinkClassValues, newFrequency : Int) : NegotiationInfo = {
     import FlightType._
     
     existingLinkOption.foreach{ link =>
       if (link.capacity == newCapacity) {
           return NegotiationInfo(1, 0)
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
     
     val info = NegotiationInfo(0.5, cost)
     negotiate(info) //TODO test
     return info
   }
  
  def negotiate(info : NegotiationInfo) = {
    val number = Math.random()
    NegotiationResult(info.odds, number)
  }
  
  val normalizedCapacity : LinkClassValues => Double = (capacity : LinkClassValues) => {
    capacity(ECONOMY) * ECONOMY.spaceMultiplier + capacity(BUSINESS) * BUSINESS.spaceMultiplier + capacity(FIRST) * FIRST.spaceMultiplier  
  }
}

case class NegotiationInfo(odds : Double, requiredPoints : Int)

case class NegotiationResult(threshold : Double, result : Double) {
  val isSuccessful = result >= threshold
  val SESSION_COUNT = 10
  def getNegotiationSessions() : NegotiationSession = {
    val BASE_PASSING_SCORE = 100 //make it a more than 0...just for nicer display
    
    val passingScore = BASE_PASSING_SCORE + threshold * 100
    
    val average = (BASE_PASSING_SCORE + result * 100) / SESSION_COUNT //average score for each session
    
    val sessionScores = ListBuffer[Double]()
    for (i <- 0 until SESSION_COUNT) {
      sessionScores.append(average)
    }
    //now generate randomness 100+ and 100- randomly assigned to each number
    for (i <- 0 until 10) {
      val index1 = Random.nextInt(SESSION_COUNT)
      val index2 = Random.nextInt(SESSION_COUNT)
      val variation = Random.nextInt(10)
      sessionScores(index1) = sessionScores(index1) + variation   
      sessionScores(index2) = sessionScores(index2) - variation
    }
    NegotiationSession(passingScore, sessionScores.toList)
  }
}

case class NegotiationSession(passingScore : Double, sessionScores : List[Double])
