package com.patson

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model._
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane._
import Model.Type._
 
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
      case LIGHT => (FlightType.SHORT_HAUL_DOMESTIC, 3)
      case REGIONAL => (FlightType.LONG_HAUL_DOMESTIC, 4)
      case SMALL => (FlightType.LONG_HAUL_INTERNATIONAL, 5)
      case MEDIUM => (FlightType.LONG_HAUL_INTERCONTINENTAL, 7)
      case LARGE => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
      case X_LARGE => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
      case JUMBO => (FlightType.ULTRA_LONG_HAUL_INTERCONTINENTAL, 8)
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
    airline.setMaintainenceQuality(Airline.MAX_MAINTENANCE_QUALITY)
    
    val link = Link(fromAirport, toAirport, airline, price = price, distance = distance, LinkClassValues.getInstanceByMap(Map(ECONOMY -> capacity)), rawQuality = fromAirport.expectedQuality(flightType, ECONOMY), duration, frequency, flightType)
    val airplane = Airplane(airplaneModel, airline, constructedCycle = 0 , Airplane.MAX_CONDITION, depreciationRate = 0, value = airplaneModel.price)
    
    val updatedAirplane = AirplaneSimulation.decayAirplanesByAirline(List((airplane, Some(link))), airline)(0)
    link.setAssignedAirplanes(List(updatedAirplane))
    link.addSoldSeats(LinkClassValues.getInstanceByMap(Map(ECONOMY -> (capacity * loadFactor).toInt)))
    
    LinkSimulation.computeLinkConsumptionDetail(link, 0)
    
    val consumptionResult = LinkSimulation.computeLinkConsumptionDetail(link , 0)
    consumptionResult
  }
}
