package com.patson.model

import FlightCategory._

object AirlineBaseSpecialization extends Enumeration {
  protected abstract class Specialization() extends super.Val {
    val getType : BaseSpecializationType.Value
    val scaleRequirement : Int
    val label : String
    val descriptions : List[String]
  }
  abstract class FlightTypeSpecialization extends Specialization {
    override val getType = BaseSpecializationType.FLIGHT_TYPE
    override val scaleRequirement : Int = 10
    val staffModifier : FlightCategory.Value => Double
  }
  case class DomesticSpecialization() extends FlightTypeSpecialization {
    override val staffModifier : (FlightCategory.Value => Double) = {
      case DOMESTIC => 0.8
      case _ => 1.2
    }
    override val label = "Domestic Hub"
    override val descriptions = List("Reduce staff required for domestic flight by 20%", "Increase staff required for international flight by 20%")
  }

  case class InternationalSpecialization() extends FlightTypeSpecialization {
    override val staffModifier : (FlightCategory.Value => Double) = {
      case DOMESTIC => 1.2
      case _ => 0.8
    }

    override val label = "International Hub"
    override val descriptions = List("Reduce staff required for international flight by 20%", "Increase staff required for domestic flight by 20%")
  }

  implicit def valueToSpecialization(x: Value) = x.asInstanceOf[Specialization]

  val DOMESTIC_HUB = DomesticSpecialization()
  val INTERNATIONAL_HUB = InternationalSpecialization()

}

object BaseSpecializationType extends Enumeration {
  type SpecializationType = Value
  val FLIGHT_TYPE = Value
}