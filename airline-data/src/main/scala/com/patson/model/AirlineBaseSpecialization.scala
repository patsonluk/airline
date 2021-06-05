package com.patson.model

import FlightCategory._
import com.patson.data.AirportSource

object AirlineBaseSpecialization extends Enumeration {
  abstract class Specialization() extends super.Val {
    val getType : BaseSpecializationType.Value
    val scaleRequirement : Int
    val label : String
    val descriptions : List[String]
    val free = false
    def apply(airline : Airline, airport : Airport) {}
    def unapply(airline: Airline, airport : Airport) {}
  }
  case class NegotiationExpertSpecialization() extends Specialization {
    override val getType = BaseSpecializationType.NEGOTIATION
    override val label = "Negotiation Expert"
    override val free = true
    override val scaleRequirement : Int = 8
    override val descriptions = List(s"Increased frequency cap gain per scale")
  }

  case class DelegateSpecialization() extends Specialization {
    override val getType = BaseSpecializationType.DELEGATE
    override val label = "Delegates Recruiter"
    val delegateBoost = 3
    override val free = true
    override val scaleRequirement : Int = 12
    override val descriptions = List(s"$delegateBoost extra delegates")
  }

  case class LoyaltySpecialization() extends Specialization {
    override val getType = BaseSpecializationType.LOYALTY
    override val label = "Sports Events Sponsorship"
    val loyaltyBoost = 10
    override val scaleRequirement : Int = 11
    override val descriptions = List(s"Boost loyalty of this airport by $loyaltyBoost")

    override def apply(airline: Airline, airport : Airport) = {
      unapply(airline, airport) //unapply first to avoid duplicates
      AirportSource.saveAirlineAppealBonus(airport.id, airline.id, AirlineBonus(BonusType.BASE_SPECIALIZATION_BONUS, AirlineAppeal(loyalty = loyaltyBoost, awareness = 0), None))
    }

    override def unapply(airline: Airline, airport : Airport) = {
      AirportSource.loadAirlineAppealBonusByAirportAndAirline(airport.id, airline.id).find(_.bonusType == BonusType.BASE_SPECIALIZATION_BONUS).foreach { existingBonus =>
        AirportSource.deleteAirlineAppealBonus(airport.id, airline.id, BonusType.BASE_SPECIALIZATION_BONUS)
      }
    }
  }

  abstract class FlightTypeSpecialization extends Specialization {
    override val getType = BaseSpecializationType.FLIGHT_TYPE

    val staffModifier : FlightCategory.Value => Double
  }
  case class DomesticSpecialization() extends FlightTypeSpecialization {
    override val scaleRequirement : Int = 10
    override val staffModifier : (FlightCategory.Value => Double) = {
      case DOMESTIC => 0.8
      case _ => 1.2
    }
    override val label = "Domestic Hub"
    override val descriptions = List("Reduce staff required for domestic flight by 20%", "Increase staff required for international flight by 20%")
  }

  case class InternationalSpecialization() extends FlightTypeSpecialization {
    override val scaleRequirement : Int = 10
    override val staffModifier : (FlightCategory.Value => Double) = {
      case DOMESTIC => 1.2
      case _ => 0.8
    }

    override val label = "International Hub"
    override val descriptions = List("Reduce staff required for international flight by 20%", "Increase staff required for domestic flight by 20%")
  }

  case class EfficiencySpecialization() extends FlightTypeSpecialization {
    override val scaleRequirement : Int = 11
    override val staffModifier : (FlightCategory.Value => Double) = {
      case _ => 0.95
    }

    override val label = "Efficiency"
    override val descriptions = List("Reduce staff required for all flights by additional 5%")
  }

  implicit def valueToSpecialization(x: Value) = x.asInstanceOf[Specialization]

  val NEGOTIATION_EXPERT = NegotiationExpertSpecialization()
  val DOMESTIC_HUB = DomesticSpecialization()
  val INTERNATIONAL_HUB = InternationalSpecialization()
  val EFFICIENCY = EfficiencySpecialization()
  val SPORTS_SPONSORSHIP = LoyaltySpecialization()
  val DELEGATE_RECRUITER = DelegateSpecialization()

}

object BaseSpecializationType extends Enumeration {
  type SpecializationType = Value
  val FLIGHT_TYPE, DELEGATE, LOYALTY, NEGOTIATION = Value
}