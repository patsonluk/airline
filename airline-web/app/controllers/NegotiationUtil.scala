package controllers

import com.patson.data.LinkSource
import com.patson.model.FlightType._
import com.patson.model._
import com.patson.util.CountryCache

import scala.collection.mutable.ListBuffer
import scala.util.Random


object NegotiationUtil {
  val NEW_LINK_BASE_COST = 100
  val MAX_ASSIGNED_DELEGATE = 10
  val FREE_LINK_THRESHOLD = 5 //for newbie
  val FREE_LINK_FREQUENCY_THRESHOLD = 5


  def negotiate(info : NegotiationInfo, delegateCount : Int) = {
    val odds = info.odds.get(delegateCount) match {
      case Some(value) => value
      case None => 0
    }
    val number = Math.random()
    NegotiationResult(1 - odds, number)
  }



  val NO_NEGOTIATION_REQUIRED = NegotiationInfo(List (), List (), List (), List (), 0, Map(0 -> 1))


  val normalizedCapacity : LinkClassValues => Double = (capacity : LinkClassValues) => {
    capacity(ECONOMY) * ECONOMY.spaceMultiplier + capacity(BUSINESS) * BUSINESS.spaceMultiplier + capacity(FIRST) * FIRST.spaceMultiplier
  }



//  val getLinkLimit = (base : Option[AirlineBase]) => base match {
//    case Some(base) =>
//      val titlesByCountryCode: Map[String, Title.Value] = CountrySource.loadCountryAirlineTitlesByCriteria(List(("airline", base.airline.id))).map(entry => (entry.country.countryCode, entry.title)).toMap
//      base.getLinkLimit(titlesByCountryCode.get(base.countryCode))
//    case None => 0 //should not happen
//  }

  def getFromAirportRequirements(airline : Airline, newLink : Link, existingLinkOption : Option[Link], airlineLinks : List[Link]) = {
    import NegotiationRequirementType._
    val requirements = ListBuffer[NegotiationRequirement]()
    val isNewLink = existingLinkOption.isEmpty
    val airport = newLink.from
    if (isNewLink) {
      val officeStaffCount : Int = airline.getBases().find(_.airport.id == airport.id).map(_.getOfficeStaffCapacity).getOrElse(0)
      val airlineLinksFromThisAirport = airlineLinks.filter(_.from.id == airport.id)
      val currentOfficeStaffUsed = airlineLinksFromThisAirport.map(_.getOfficeStaffRequired).sum
      val newOfficeStaffRequired = newLink.getOfficeStaffRequired
      val newTotal = currentOfficeStaffUsed + newOfficeStaffRequired

      if (newTotal < officeStaffCount) {
        requirements.append(NegotiationRequirement(LINK_CAP, 0, s"Requires ${newOfficeStaffRequired} office staff, within your base capacity : ${newTotal} / ${officeStaffCount}"))
      } else {
        val requirement = (newTotal - officeStaffCount).toDouble / 20
        requirements.append(NegotiationRequirement(LINK_CAP, requirement, s"Requires ${newOfficeStaffRequired} office staff, over your base capacity : ${newTotal} / ${officeStaffCount}"))
      }
    }
    val newFrequency = newLink.futureFrequency()
    val frequencyDelta = newFrequency - existingLinkOption.map(_.futureFrequency()).getOrElse(0)
    if (frequencyDelta > 0) {
      val maxFrequency = Computation.getMaxFrequencyThreshold(airline)
      if (newFrequency > maxFrequency) {
        requirements.append(NegotiationRequirement(EXCESSIVE_FREQUENCY, newFrequency - maxFrequency, s"Excessive frequency $newFrequency over allowed $maxFrequency"))
      }
    }

    requirements.toList
  }

  def getToAirportRequirements(airline : Airline, newLink : Link, existingLinkOption : Option[Link], airlineLinks : List[Link]) = {
    val newCapacity : LinkClassValues = newLink.futureCapacity()
    val newFrequency = newLink.futureFrequency()

    val existingCapacity = existingLinkOption.map(_.futureCapacity()).getOrElse(LinkClassValues.getInstance())
    val existingFrequency = existingLinkOption.map(_.futureFrequency()).getOrElse(0)

    val capacityDelta = normalizedCapacity(newCapacity - existingCapacity)
    val frequencyDelta = newFrequency - existingFrequency
    val requirements = ListBuffer[NegotiationRequirement]()

    val flightTypeMultiplier = Computation.getFlightType(newLink.from, newLink.to) match {
      case SHORT_HAUL_DOMESTIC => 1
      case LONG_HAUL_DOMESTIC => 1.5
      case SHORT_HAUL_INTERNATIONAL => 2
      case LONG_HAUL_INTERNATIONAL => 2.5
      case SHORT_HAUL_INTERCONTINENTAL => 2.5
      case MEDIUM_HAUL_INTERCONTINENTAL => 3
      case LONG_HAUL_INTERCONTINENTAL => 3.5
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 3.5
    }
    val NEW_LINK_BASE_REQUIREMENT = 1

    import NegotiationRequirementType._

    if (existingLinkOption.isEmpty) {
      requirements.append(NegotiationRequirement(NEW_LINK, NEW_LINK_BASE_REQUIREMENT * flightTypeMultiplier, "New Flights"))
    }

    if (capacityDelta > 0) {
      val capacityChangeCost = capacityDelta.toDouble / 2000
      requirements.append(NegotiationRequirement(INCREASE_CAPACITY, capacityChangeCost * flightTypeMultiplier, s"Capacity increment : $capacityDelta"))
    }

    if (frequencyDelta > 0) {
      val frequencyChangeCost = frequencyDelta.toDouble / 3
      requirements.append(NegotiationRequirement(INCREASE_FREQUENCY, frequencyChangeCost, s"Frequency increment : $frequencyDelta"))
    }

    //val odds = new NegotiationOdds()
    existingLinkOption match {
      case Some(link) => //then consider existing load factor of this link
        val loadFactor : Double =(link.getTotalCapacity - link.getTotalSoldSeats).toDouble / link.getTotalCapacity
        if (loadFactor < 0.8) {
          val cost = Math.ceil(1 + (0.8 - loadFactor * 4))
          requirements.append(NegotiationRequirement(LOW_LOAD_FACTOR, cost, s"Low load factor ${BigDecimal(loadFactor * 100).setScale(2)}%"))
        }

      case None => //let's not make it too hard for newcomer for now
      //      case None => //existing competition
      //        //consider how many existing routes - if more than 2 reduce the odds
      //        val competingLinks = LinkSource.loadLinksByAirports(newLink.from.id, newLink.to.id, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByAirports(newLink.to.id, newLink.from.id, LinkSource.ID_LOAD)
      //        val competingLinksCount = competingLinks.filter(_.capacity.total > 0).size
      //        if (competingLinksCount >= 2) {
      //          requirements.append(NegotiationRequirement(EXISTING_COMPETITION, competingLinksCount - 2))
      //        }
        val airport = newLink.to
        CountryCache.getCountry(airport.countryCode).foreach { country =>
          airline.getCountryCode().foreach { homeCountryCode =>
            if (homeCountryCode != airport.countryCode) { //closed country are anti foreign airlines
              requirements.append(NegotiationRequirement(FOREIGN_AIRLINE, (12 - country.openness) * 0.5, "Foreign Airline"))
            }
          }
        }
    }
    requirements.toList
  }
  val getStaffRequired = (link : Link) => {
    Computation.getFlightType(link.from, link.to) match {
      case SHORT_HAUL_DOMESTIC => 5
      case LONG_HAUL_DOMESTIC => 8
      case SHORT_HAUL_INTERNATIONAL => 10
      case LONG_HAUL_INTERNATIONAL => 12
      case SHORT_HAUL_INTERCONTINENTAL => 12
      case MEDIUM_HAUL_INTERCONTINENTAL => 20
      case LONG_HAUL_INTERCONTINENTAL => 30
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 30
    }
  }

  val getBaseStaffCount = (base : AirlineBase) =>  {

  }

  def getNegotiationRequirements(newLink : Link, existingLinkOption : Option[Link], airline : Airline, airlineLinks : List[Link]) = {
    val fromAirportRequirements : List[NegotiationRequirement] = getFromAirportRequirements(airline, newLink, existingLinkOption, airlineLinks)
    val toAirportRequirements : List[NegotiationRequirement] = getToAirportRequirements(airline, newLink, existingLinkOption, airlineLinks)

    (fromAirportRequirements, toAirportRequirements)
  }

  def getNegotiationDiscounts(airport : Airport, airline : Airline) = {
    val discounts = ListBuffer[NegotiationDiscount]()
    //how busy is this airport
    val airportLinks =  LinkSource.loadLinksByFromAirport(airport.id) ++ LinkSource.loadLinksByToAirport(airport.id)
    val totalFrequency = airportLinks.map(_.frequency).sum
    import NegotiationDiscountType._
    if (totalFrequency <= 50 * airport.size) { //under serve
      discounts.append(NegotiationDiscount(BELOW_CAPACITY, 0.3)) //30%
    } else if (totalFrequency >= 300 * airport.size) {
      //penalty multiplier start from 1 up to 10
      val multiplier = Math.min(10, 1 + (totalFrequency - 300 * airport.size).toDouble / (100 * airport.size))
      discounts.append(NegotiationDiscount(OVER_CAPACITY, -0.2 * multiplier)) //crowded, start from 20% to up to 200% penalty
    }

    //if 100 relationship, give 50% discount (0.5)
    val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(airport.countryCode, airline).relationship
    if (relationship >= 0) {
      var discount = relationship * 0.005
      discount = Math.min(discount, 0.5)
      discounts.append(NegotiationDiscount(COUNTRY_RELATIONSHIP, discount))
    } else if (relationship < 0) { //very penalizing
      val discount = relationship * 0.1
      discounts.append(NegotiationDiscount(COUNTRY_RELATIONSHIP, discount))
    }

    discounts.toList
  }

  def getLinkNegotiationInfo(airline : Airline, newLink : Link, existingLinkOption : Option[Link]) : NegotiationInfo = {
    val fromAirport : Airport = newLink.from
    val toAirport : Airport = newLink.to
    val newCapacity : LinkClassValues = newLink.futureCapacity()
    val newFrequency = newLink.futureFrequency()

    val existingCapacity = existingLinkOption.map(_.futureCapacity()).getOrElse(LinkClassValues.getInstance())
    val existingFrequency = existingLinkOption.map(_.futureFrequency()).getOrElse(0)

    val capacityDelta = normalizedCapacity(newCapacity - existingCapacity)
    val frequencyDelta = newFrequency - existingFrequency

    val airlineLinks = LinkSource.loadLinksByAirlineId(airline.id)

    //reduction of service is always okay for now
    if (capacityDelta <= 0 && frequencyDelta <= 0) {
      return NegotiationUtil.NO_NEGOTIATION_REQUIRED
    }

    val (fromAirportRequirements, toAirportRequirements) = getNegotiationRequirements(newLink, existingLinkOption, airline, airlineLinks)
    val fromAirportDiscounts = getNegotiationDiscounts(fromAirport, airline)
    val toAirportDiscounts = getNegotiationDiscounts(toAirport, airline)

    val fromRequirementBase = fromAirportRequirements.map(_.value).sum
    val toRequirementBase = toAirportRequirements.map(_.value).sum
    val totalFromDiscount = Math.min(1, fromAirportDiscounts.map(_.value).sum)
    val totalToDiscount = Math.min(1, toAirportDiscounts.map(_.value).sum)
    val fromAirportRequirementValue = fromRequirementBase * (1 - totalFromDiscount)
    val toAirportRequirementValue = toRequirementBase * (1 - totalToDiscount)
    val finalRequirementValue = fromAirportRequirementValue + toAirportRequirementValue

    //check for freebie bonus
    if (airlineLinks.length < FREE_LINK_THRESHOLD &&
      newFrequency < FREE_LINK_FREQUENCY_THRESHOLD &&  //to prevent many small increase
      finalRequirementValue < 4
    ) {
      return NegotiationUtil.NO_NEGOTIATION_REQUIRED
    }

    val info = NegotiationInfo(fromAirportRequirements, toAirportRequirements, fromAirportDiscounts, toAirportDiscounts, finalRequirementValue, computeOdds(finalRequirementValue, Math.min(MAX_ASSIGNED_DELEGATE, airline.getDelegateInfo.availableCount)))
    return info
  }

  /**
    *
    * @param finalRequirementValue
    * @param maxDelegateCount
    * @return a map of delegate count vs odds, which 0 <= odds <= 1
    */
  def computeOdds(finalRequirementValue : Double, maxDelegateCount : Int) : Map[Int, Double] = {
    val requiredDelegates = finalRequirementValue
    var accumulativeOdds = 0.0
    (0 to maxDelegateCount).map { delegateCount =>
      val oddsForThisDelegateCount : Double =
        if (finalRequirementValue == 0) {
          1
        } else {
          if (delegateCount < requiredDelegates) {
            0
          } else {
            val base = (15 - requiredDelegates) * 0.04
            if (delegateCount < requiredDelegates + 1) {
              accumulativeOdds = base
            } else {
              accumulativeOdds = Math.min(1, accumulativeOdds + 0.3 * Math.pow(0.7, delegateCount - requiredDelegates))
            }
            accumulativeOdds
          }
        }
      (delegateCount, oddsForThisDelegateCount)
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

case class NegotiationInfo(fromAirportRequirements : List[NegotiationRequirement], toAirportRequirements : List[NegotiationRequirement], fromAirportDiscounts : List[NegotiationDiscount], toAirportDiscounts : List[NegotiationDiscount], finalRequirementValue : Double, odds : Map[Int, Double])

object NegotiationRequirementType extends Enumeration {
  type NegotiationRequirementType = Value
  val FROM_COUNTRY_RELATIONSHIP, TO_COUNTRY_RELATIONSHIP, EXISTING_COMPETITION, NEW_LINK, INCREASE_CAPACITY, INCREASE_FREQUENCY, EXCESSIVE_FREQUENCY, LOW_LOAD_FACTOR, FOREIGN_AIRLINE, LINK_CAP, OTHER = Value

//  def description(requirementType : NegotiationRequirementType.Value, link : Link) =  requirementType match {
//    case EXISTING_COMPETITION => "Existing Routes by other Airlines"
//    case NEW_LINK => "New Flights"
//    case INCREASE_CAPACITY => "Increase Capacity"
//    case LOW_LOAD_FACTOR => "Low Load Factor"
//    case INCREASE_FREQUENCY => "Increase Frequency"
//    case FOREIGN_AIRLINE => "Foreign Airline"
//    case OTHER => "Unknown"
//  }
}

object NegotiationDiscountType extends Enumeration {
  type NegotiationDiscountType = Value
  val COUNTRY_RELATIONSHIP, BELOW_CAPACITY, OVER_CAPACITY, NEW_AIRLINE = Value

  def description(adjustmentType : NegotiationDiscountType.Value, airport : Airport) =  adjustmentType match {
    case COUNTRY_RELATIONSHIP => s"Country Relationship with ${airport.countryCode}"
    case BELOW_CAPACITY => s"${airport.displayText} is under capacity"
    case OVER_CAPACITY => s"${airport.displayText} is over capacity"
    case NEW_AIRLINE => s"New airline bonus"
  }
}

case class NegotiationRequirement(requirementType : NegotiationRequirementType.Value, value : Double, description : String) {

}
case class NegotiationDiscount(adjustmentType : NegotiationDiscountType.Value, value : Double)

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