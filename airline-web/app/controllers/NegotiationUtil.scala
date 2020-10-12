package controllers

import scala.collection.mutable.ListBuffer
import scala.util.Random


object NegotiationUtil {
  val NEW_LINK_BASE_COST = 100


  def negotiate(info : NegotiationInfo) = {
    val number = Math.random()
    NegotiationResult(info.odds.value, number)
  }



  val NO_NEGOTIATION_REQUIRED = {
    val odds = new NegotiationOdds()
    odds.addFactor(NegotationFactor.OTHER, 1)
    NegotiationInfo(odds, 0)
  }
}

class NegotiationOdds() {
  private[this] val factors = scala.collection.mutable.LinkedHashMap[NegotationFactor.Value, Double]()
  def addFactor(factor : NegotationFactor.Value, value : Double) = {
    factors.put(factor, value)
  }

  def value = factors.values.sum match {
    case x if x > 1 => 1.0
    case x if x < 0 => 0.0
    case x => x
  }

  def getFactors : Map[NegotationFactor.Value, Double] = factors.toMap
}

object NegotationFactor extends Enumeration {
  type NegotationFactor = Value
  val COUNTRY_RELATIONSHIP, EXISTING_LINKS, INITIAL_LINKS, DECREASE_CAPACITY, INCREASE_CAPACITY, OTHER = Value

  def description(factor : NegotationFactor) =  factor match {
    case COUNTRY_RELATIONSHIP => "Country Relationship"
    case EXISTING_LINKS => "Existing Routes by other Airlines"
    case INITIAL_LINKS => "Bonus for smaller Airlines"
    case INCREASE_CAPACITY => "Decreae Capacity"
    case DECREASE_CAPACITY => "Increase Capacity"
    case OTHER => "Unknown"
  }
}

case class NegotiationInfo(odds : NegotiationOdds, requiredPoints : Int)

case class NegotiationResult(threshold : Double, result : Double) {
  val isSuccessful = result >= threshold
  val SESSION_COUNT = 10
  def getNegotiationSessions() : NegotiationSession = {
    //    val BASE_PASSING_SCORE = 100 //make it a more than 0...just for nicer display
    //
    val passingScore = 75 + threshold * 25

    val score = 75 + result * 25

    val average = score / SESSION_COUNT //average score for each session

    val sessionScores = ListBuffer[Double]()
    for (i <- 0 until SESSION_COUNT) {
      sessionScores.append(average)
    }
    //now generate randomness 100+ and 100- randomly assigned to each number
    for (i <- 0 until 10) {
      val index1 = Random.nextInt(SESSION_COUNT)
      val index2 = Random.nextInt(SESSION_COUNT)
      val variation = Random.nextInt(5)
      sessionScores(index1) = sessionScores(index1) + variation
      sessionScores(index2) = sessionScores(index2) - variation
    }
    NegotiationSession(passingScore, sessionScores.toList)
  }
}

case class NegotiationSession(passingScore : Double, sessionScores : List[Double])