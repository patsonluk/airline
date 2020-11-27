package controllers

import com.patson.data.{CountrySource, LinkSource}
import com.patson.model.FlightType.{LONG_HAUL_DOMESTIC, LONG_HAUL_INTERCONTINENTAL, LONG_HAUL_INTERNATIONAL, SHORT_HAUL_DOMESTIC, SHORT_HAUL_INTERCONTINENTAL, SHORT_HAUL_INTERNATIONAL, ULTRA_LONG_HAUL_INTERCONTINENTAL}
import com.patson.model.{Airline, Airport, BUSINESS, Computation, ECONOMY, FIRST, FlightType, Link, LinkClassValues}

import scala.collection.mutable.ListBuffer
import scala.util.Random


object NegotiationUtil {
  val NEW_LINK_BASE_COST = 100


  def negotiate(info : NegotiationInfo, delegateCount : Int) = {
    val odds = info.odds.get(delegateCount) match {
      case Some(value) => value
      case None => 0
    }
    val number = Math.random()
    NegotiationResult(1 - odds, number)
  }



  val NO_NEGOTIATION_REQUIRED = NegotiationInfo(List (), Map(0 -> 1))


  val normalizedCapacity : LinkClassValues => Double = (capacity : LinkClassValues) => {
    capacity(ECONOMY) * ECONOMY.spaceMultiplier + capacity(BUSINESS) * BUSINESS.spaceMultiplier + capacity(FIRST) * FIRST.spaceMultiplier
  }



  def getLinkNegotiationInfo(airline : Airline, newLink : Link, existingLinkOption : Option[Link]) : NegotiationInfo = {
    import FlightType._
    import NegotiationRequirementType._

    val fromAirport : Airport = newLink.from
    val toAirport : Airport = newLink.to
    val newCapacity : LinkClassValues = newLink.futureCapacity()
    val newFrequency = newLink.futureFrequency()

    val existingCapacity = existingLinkOption.map(_.futureCapacity()).getOrElse(LinkClassValues.getInstance())
    val existingFrequency = existingLinkOption.map(_.futureFrequency()).getOrElse(0)

    val capacityDelta = normalizedCapacity(newCapacity - existingCapacity)
    val frequencyDelta = newFrequency - existingFrequency

    //reduction of service is always okay for now
    if (capacityDelta <= 0 && frequencyDelta <= 0) {
      return NegotiationUtil.NO_NEGOTIATION_REQUIRED
    }

    //at this point negotiation is required

    val flightTypeMultiplier = Computation.getFlightType(fromAirport, toAirport) match {
      case SHORT_HAUL_DOMESTIC => 1
      case LONG_HAUL_DOMESTIC => 1.5
      case SHORT_HAUL_INTERNATIONAL => 2
      case LONG_HAUL_INTERNATIONAL => 2.5
      case SHORT_HAUL_INTERCONTINENTAL => 4
      case LONG_HAUL_INTERCONTINENTAL => 5
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 5
    }
    val NEW_LINK_BASE_REQUIREMENT = 1

    val requirements = ListBuffer[NegotiationRequirement]()
    if (existingLinkOption.isEmpty) {
      requirements.append(NegotiationRequirement(NEW_LINK, NEW_LINK_BASE_REQUIREMENT * flightTypeMultiplier))
    }


    if (capacityDelta > 0) {
      val capacityChangeCost = Math.ceil(capacityDelta.toDouble / 1000)
      requirements.append(NegotiationRequirement(INCREASE_CAPACITY, capacityChangeCost * flightTypeMultiplier))
    }

    if (frequencyDelta > 0) {
      val frequencyChangeCost = Math.ceil(frequencyDelta.toDouble / 3)
      requirements.append(NegotiationRequirement(INCREASE_FREQUENCY, frequencyChangeCost * flightTypeMultiplier))
    }



    //val cost = ((baseNegotiationCost + capacityChangeCost) * flightTypeMultiplier).toInt

    val countryRelationships = CountrySource.getCountryRelationshipsByAirline(airline)
    val fromCountryRelationship = countryRelationships.getOrElse(fromAirport.countryCode, 0)
    val toCountryRelationship = countryRelationships.getOrElse(toAirport.countryCode, 0)

    requirements.append(NegotiationRequirement(FROM_COUNTRY_RELATIONSHIP, -1 * fromCountryRelationship))
    requirements.append(NegotiationRequirement(TO_COUNTRY_RELATIONSHIP, -1 * toCountryRelationship))

    //val odds = new NegotiationOdds()

    existingLinkOption match {
      case Some(link) => //then consider existing load factor of this link
        val loadFactor : Double =(link.getTotalCapacity - link.getTotalSoldSeats).toDouble / link.getTotalCapacity
        if (loadFactor < 0.8) {
          val cost = Math.ceil(1 + (0.8 - loadFactor * 4))
          requirements.append(NegotiationRequirement(LOW_LOAD_FACTOR, cost))
        }

      case None => //existing competition
        //consider how many existing routes - if more than 2 reduce the odds
        val competingLinks = LinkSource.loadLinksByAirports(fromAirport.id, toAirport.id, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByAirports(toAirport.id, fromAirport.id, LinkSource.ID_LOAD)
        val competingLinksCount = competingLinks.filter(_.capacity.total > 0).size
        if (competingLinksCount >= 2) {
          requirements.append(NegotiationRequirement(EXISTING_COMPETITION, competingLinksCount - 2))
        }
    }

    val info = NegotiationInfo(requirements.toList, computeOdds(requirements.toList, airline.getDelegateInfo.availableCount))
    return info
  }

  def computeOdds(requirements : List[NegotiationRequirement], maxDelegateCount : Int) : Map[Int, Double] = {
    val totalRequirements = requirements.map(_.value).sum
    (0 to maxDelegateCount).map { delegateCount =>
      val oddsForThisDelegatecount =
        if (requirements.size == 0) {
          1
        } else {
          if (delegateCount == 0) {
            0
          } else {
            Math.min(1, delegateCount.toDouble / totalRequirements)
          }
        }
      (delegateCount, oddsForThisDelegatecount)
    }.toMap
  }

}

//class NegotiationOdds() {
//  private[this] val factors = scala.collection.mutable.LinkedHashMap[NegotationFactor.Value, Double]()
//  def addFactor(factor : NegotationFactor.Value, value : Double) = {
//    factors.put(factor, value)
//  }
//
//  def value = factors.values.sum match {
//    case x if x > 1 => 1.0
//    case x if x < 0 => 0.0
//    case x => x
//  }
//
//  def getFactors : Map[NegotationFactor.Value, Double] = factors.toMap
//}

//object NegotationFactor extends Enumeration {
//  type NegotationFactor = Value
//  val COUNTRY_RELATIONSHIP, EXISTING_LINKS, INITIAL_LINKS, DECREASE_CAPACITY, INCREASE_CAPACITY, OTHER = Value
//
//  def description(factor : NegotationFactor) =  factor match {
//    case COUNTRY_RELATIONSHIP => "Country Relationship"
//    case EXISTING_LINKS => "Existing Routes by other Airlines"
//    case INITIAL_LINKS => "Bonus for smaller Airlines"
//    case INCREASE_CAPACITY => "Increase Capacity"
//    case DECREASE_CAPACITY => "Decrease Capacity"
//    case OTHER => "Unknown"
//  }
//}

/**
  *
  * @param requirements
  * @param odds map of key : assigned delegate count, value : odds for that count
  */
case class NegotiationInfo(requirements : List[NegotiationRequirement], odds : Map[Int, Double])

object NegotiationRequirementType extends Enumeration {
  type NegotiationRequirementType = Value
  val FROM_COUNTRY_RELATIONSHIP, TO_COUNTRY_RELATIONSHIP, EXISTING_COMPETITION, NEW_LINK_FREE, NEW_LINK, INCREASE_CAPACITY, INCREASE_FREQUENCY, LOW_LOAD_FACTOR, OTHER = Value

  def description(requirementType : NegotiationRequirementType.Value, link : Link) =  requirementType match {
    case FROM_COUNTRY_RELATIONSHIP => "Country Relationship with departure country " + link.from.countryCode
    case TO_COUNTRY_RELATIONSHIP => "Country Relationship with destination country " + link.to.countryCode
    case EXISTING_COMPETITION => "Existing Routes by other Airlines"
    case NEW_LINK_FREE => "New Flights (free)"
    case NEW_LINK => "New Flights"
    case INCREASE_CAPACITY => "Increase Capacity"
    case LOW_LOAD_FACTOR => "Low Load Factor"
    //case DECREASE_CAPACITY => "Decrease Capacity"
    case INCREASE_FREQUENCY => "Increase Frequency"
    case OTHER => "Unknown"
  }
}

case class NegotiationRequirement(requirementType : NegotiationRequirementType.Value, value : Double)

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