package com.patson.model

import FlightCategory._
import com.patson.data.AirportSource
import com.patson.util.AirportCache

object AirlineBaseSpecialization extends Enumeration {
  abstract class Specialization() extends super.Val {
    val getType : BaseSpecializationType.Value
    val scaleRequirement : Int
    val label : String
    def descriptions(airport : Airport) : List[String]
    val free = false
    def apply(airline : Airline, airport : Airport) {}
    def unapply(airline: Airline, airport : Airport) {}
  }
  case class NegotiationExpertSpecialization() extends Specialization {
    override val getType = BaseSpecializationType.NEGOTIATION
    override val label = "Negotiation Expert I"
    override val free = true
    override val scaleRequirement : Int = 8
    override def descriptions(airport : Airport) = List(s"Increased frequency cap gain per scale")
  }

  case class NegotiationExpert2Specialization() extends Specialization {
    override val getType = BaseSpecializationType.NEGOTIATION
    override val label = "Negotiation Expert II"
    override val free = true
    override val scaleRequirement : Int = 13
    override def descriptions(airport : Airport) = List(s"Further increased frequency cap gain per scale")
  }

  case class DelegateSpecialization() extends Specialization {
    override val getType = BaseSpecializationType.DELEGATE
    override val label = "Delegates Recruiter"
    val delegateBoost = 3
    override val free = true
    override val scaleRequirement : Int = 12
    override def descriptions(airport : Airport) = List(s"$delegateBoost extra delegates")
  }

  case class PowerhouseSpecialization() extends Specialization {
    override val getType = BaseSpecializationType.AIRPORT_POWER
    override val label = "Powerhouse"
    override val free = true
    override val scaleRequirement : Int = 14
    val populationBoost = 30000
    private[this] val minIncomeBoost = 3000 //should at least boost income by $3000
    private[this] val percentageBoost = 20 //20% if lower than minIncomeBoost then minIncomeBoost

    def incomeBoost(airport : Airport) = {
      val incomeIncrement = airport.baseIncome * percentageBoost / 100
      Math.max(minIncomeBoost, incomeIncrement)
    }
    override def descriptions(airport : Airport) =  {
      List(s"Increase population by $populationBoost", s"Increase income by $$${incomeBoost(airport)}")
    }
    override def apply(airline: Airline, airport : Airport) = {
      AirportCache.invalidateAirport(airport.id)
    }
    override def unapply(airline: Airline, airport : Airport) = {
      AirportCache.invalidateAirport(airport.id)
    }
  }

  case class LoyaltySpecialization() extends Specialization {
    override val getType = BaseSpecializationType.LOYALTY
    override val label = "Sports Events Sponsorship"
    val loyaltyBoost = 10
    override val scaleRequirement : Int = 11
    override def descriptions(airport : Airport) = List(s"Boost loyalty of this airport by $loyaltyBoost")

    override def apply(airline: Airline, airport : Airport) = {
      unapply(airline, airport) //unapply first to avoid duplicates
      AirportSource.saveAirlineAppealBonus(airport.id, airline.id, AirlineBonus(BonusType.BASE_SPECIALIZATION_BONUS, AirlineAppeal(loyalty = loyaltyBoost), None))
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
    override def descriptions(airport : Airport) = List("Reduce staff required for domestic flight by 20%", "Increase staff required for international flight by 20%")
  }

  case class InternationalSpecialization() extends FlightTypeSpecialization {
    override val scaleRequirement : Int = 10
    override val staffModifier : (FlightCategory.Value => Double) = {
      case DOMESTIC => 1.2
      case _ => 0.8
    }

    override val label = "International Hub"
    override def descriptions(airport : Airport) = List("Reduce staff required for international flight by 20%", "Increase staff required for domestic flight by 20%")
  }

  case class EfficiencySpecialization() extends FlightTypeSpecialization {
    override val scaleRequirement : Int = 11
    override val staffModifier : (FlightCategory.Value => Double) = {
      case _ => 0.95
    }

    override val label = "Efficiency"
    override def descriptions(airport : Airport) = List("Reduce staff required for all flights by additional 5%")
  }

  abstract class BrandSpecialization extends Specialization {
    override val getType = BaseSpecializationType.BRANDING

    val linkCostDeltaByClass : Map[LinkClass, Double]
  }


  case class BudgetAirlineSpecialization() extends BrandSpecialization {
    override val scaleRequirement : Int = 9
    override val label = "Branding: Budget"
    override def descriptions(airport : Airport) = List("Your flights from/to this airport are slightly more appealing to Economy class PAX", "Your flights from/to this airport are slightly less appealing to Business and First class PAX")
    override val linkCostDeltaByClass : Map[LinkClass, Double] = Map(
      ECONOMY -> -0.05,
      BUSINESS -> 0.05,
      FIRST -> 0.05
    )
  }

  case class PremiumAirlineSpecialization() extends BrandSpecialization {
    override val scaleRequirement : Int = 9
    override val label = "Branding: Premium"
    override def descriptions(airport : Airport) = List("Your flights from/to this airport are slightly less appealing to Economy class PAX", "Your flights from/to this airport are slightly more appealing to Business and First class PAX")
    override val linkCostDeltaByClass : Map[LinkClass, Double] = Map(
      ECONOMY -> 0.05,
      BUSINESS -> -0.05,
      FIRST -> -0.05
    )
  }

  implicit def valueToSpecialization(x: Value) = x.asInstanceOf[Specialization]

  val NEGOTIATION_EXPERT = NegotiationExpertSpecialization()
  val NEGOTIATION_EXPERT2 = NegotiationExpert2Specialization()
  val DOMESTIC_HUB = DomesticSpecialization()
  val INTERNATIONAL_HUB = InternationalSpecialization()
  val EFFICIENCY = EfficiencySpecialization()
  val SPORTS_SPONSORSHIP = LoyaltySpecialization()
  val DELEGATE_RECRUITER = DelegateSpecialization()
  val BRANDING_BUDGET = BudgetAirlineSpecialization()
  val BRANDING_PREMIUM = PremiumAirlineSpecialization()
  val POWERHOUSE = PowerhouseSpecialization()
}

object BaseSpecializationType extends Enumeration {
  type SpecializationType = Value
  val FLIGHT_TYPE, DELEGATE, BRANDING, LOYALTY, NEGOTIATION, AIRPORT_POWER = Value
  val COOLDOWN = 100 //change every 100 cycles
}