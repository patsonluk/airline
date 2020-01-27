package com.patson

import com.patson.model._
import com.patson.model.airplane._
import org.scalatest.{Matchers, WordSpecLike}
 
class AirplaneSimulationSpec extends WordSpecLike with Matchers {
  private[this] val model = Model.modelByName("Cessna 421")
  val airline1 = Airline("test-1")
  airline1.setMaintenanceQuality(80)
  val airline2 = Airline("test-2")
  airline2.setMaintenanceQuality(70)
   
  val airplane1 = Airplane(model, airline1, 0, 0, 100, 0, model.price)
  val airplane2 = Airplane(model, airline2, 0, 0, 100, 0, model.price)
  val airport1 = Airport.fromId(1)
  val airport2 = Airport.fromId(2)
  val link1 = Link(airport1, airport2, airline1, LinkClassValues.getInstance(), 0, LinkClassValues.getInstance(), 0, 0, 1, FlightType.SHORT_HAUL_DOMESTIC)
  val link2 = Link(airport1, airport2, airline2, LinkClassValues.getInstance(), 0, LinkClassValues.getInstance(), 0, 0, 1, FlightType.SHORT_HAUL_DOMESTIC)  
  
  "decayAirplanesByAirline".must {
    "decay airplane according to airline maintenance quality".in {
       val result1 = AirplaneSimulation.decayAirplanesByAirline(Map(airplane1 -> LinkAssignments(Map(link1.id -> LinkAssignment(1, 1)))), airline1)
       val result2 = AirplaneSimulation.decayAirplanesByAirline(Map(airplane2 -> LinkAssignments(Map(link2.id -> LinkAssignment(1, 1)))), airline2)
       
       result1(0).condition.should(be > result2(0).condition)
    }
    "decay slower if no assigned link".in {
       val result1 = AirplaneSimulation.decayAirplanesByAirline(Map(airplane1 -> LinkAssignments(Map(link1.id -> LinkAssignment(1, 1)))), airline1)
       val result2 = AirplaneSimulation.decayAirplanesByAirline(Map(airplane1.copy() -> LinkAssignments(Map.empty)), airline2)
       result1(0).condition.should(be < result2(0).condition)
    }
    "not decay beyond 0% in lifespan".in {
       val badAirline = Airline("bad-airline")
       badAirline.setMaintenanceQuality(0)
       
       var airplane = airplane1.copy(owner = badAirline)
       val link = link1.copy(airline = badAirline)
       
       for (i <- 0 until airplane.model.lifespan) {
         airplane = AirplaneSimulation.decayAirplanesByAirline(Map(airplane -> LinkAssignments(Map(link.id -> LinkAssignment(1, 1)))), badAirline)(0)
       }
       
       airplane.condition.should(be >= 0.0)
       airplane.condition.should(be < 1.0)
    }
  }
  "computeDepreciationRate".must {
    "compute the rate based on decay".in {
      val depreciationRate = AirplaneSimulation.computeDepreciationRate(model, 0.1) 
      depreciationRate.shouldBe(model.price * (0.1/100)) //condition on scale of 100
    }
  }
}
