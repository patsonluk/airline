package com.patson

import com.patson.model._
import com.patson.model.airplane.Model.Type._
import com.patson.model.airplane.Model.IcaoClass
import com.patson.model.airplane._
import org.scalatest.{Matchers, WordSpecLike}
 
class AirplaneModelSpec extends WordSpecLike with Matchers {
  private val GOOD_PROFIT_MARGIN = Map(LIGHT -> 0.25, REGIONAL -> 0.25, SMALL -> 0.15, MEDIUM -> 0.05, LARGE -> 0.0, X_LARGE -> -0.05, JUMBO -> -0.1)
  private val MAX_PROFIT_MARGIN = Map(LIGHT -> 0.5, REGIONAL -> 0.5, SMALL -> 0.4, MEDIUM -> 0.3, LARGE -> 0.25, X_LARGE -> 0.2, JUMBO -> 0.15)
  
  "all airplane models".must {
    "Generate good profit at MAX LF at suitable range".in {
      Model.models.foreach { airplaneModel =>
        val margin = simulateProfitMargin(airplaneModel, 1)
        println(airplaneModel.name + " => " + margin)
        assert(margin > GOOD_PROFIT_MARGIN(airplaneModel.airplaneType))
        assert(margin < MAX_PROFIT_MARGIN(airplaneModel.airplaneType))
      }
    }
  }
  
  def simulateProfitMargin(airplaneModel : Model, loadFactor : Double) : Double = {
    val consumptionDetails = simulateStandard(airplaneModel, loadFactor)
    println(consumptionDetails)
    consumptionDetails.profit.toDouble / consumptionDetails.revenue
  }
  
  def simulateStandard(airplaneModel : Model, loadFactor : Double) : LinkConsumptionDetails = {
    val distance = if (airplaneModel.range > 10000)  10000 else airplaneModel.range //cap at 10000, otherwise frequency will be very low
    val (flightType, airportSize) = airplaneModel.airplaneType match {
      case CLASS_A => (FlightType.SHORT_HAUL_DOMESTIC, 3)
      case CLASS_B => (FlightType.LONG_HAUL_DOMESTIC, 4)
      case CLASS_C => (FlightType.LONG_HAUL_INTERNATIONAL, 5)
      case CLASS_D => (FlightType.LONG_HAUL_INTERCONTINENTAL, 7)
      case CLASS_E => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
      case X_LARGE => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
      case CLASS_F => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
    }
    val duration = Computation.calculateDuration(airplaneModel, distance)
    val frequency = Computation.calculateMaxFrequency(airplaneModel, distance)
    val capacity = frequency * airplaneModel.capacity
    val fromAirport = Airport.fromId(1).copy(size = airportSize, power = Country.HIGH_INCOME_THRESHOLD, population = 1)
    fromAirport.initAirlineBases(List())
    val toAirport = Airport.fromId(2).copy(size = airportSize)
    toAirport.initAirlineBases(List())
    val price = Pricing.computeStandardPriceForAllClass(distance, flightType)
    val airline = Airline.fromId(1)
    airline.setMaintenanceQuality(Airline.MAX_MAINTENANCE_QUALITY)
    
    val link = Link(fromAirport, toAirport, airline, price = price, distance = distance, LinkClassValues.getInstanceByMap(Map(ECONOMY -> capacity)), rawQuality = fromAirport.expectedQuality(flightType, ECONOMY), duration, frequency, flightType)
    val airplane = Airplane(airplaneModel, airline, constructedCycle = 0 , purchasedCycle = 0, Airplane.MAX_CONDITION, depreciationRate = 0, value = airplaneModel.price)
    
    val updatedAirplane = AirplaneSimulation.decayAirplanesByAirline(Map(airplane -> LinkAssignments(Map(link.id -> LinkAssignment(1, 1)))), airline)(0)
    link.setTestingAssignedAirplanes(Map(updatedAirplane -> frequency))
    link.addSoldSeats(LinkClassValues.getInstanceByMap(Map(ECONOMY -> (capacity * loadFactor).toInt)))
    
    LinkSimulation.computeLinkConsumptionDetail(link, 0)
    
    val consumptionResult = LinkSimulation.computeLinkConsumptionDetail(link , 0)
    consumptionResult
  }
}
