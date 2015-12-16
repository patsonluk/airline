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
import com.patson.model.airplane.Model
import com.patson.model.airplane.Airplane
import FlightType._
 
class LinkSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1")
  val testAirline2 = Airline("airline 2")
  val fromAirport = Airport.fromId(1).copy(size = 3)
  val toAirport = Airport.fromId(2).copy(size = 3)
  
  val lightModel = Model("Cessna Caravan", capacity = 14, fuelBurn = 15, speed = 344, range = 2400, price = 1600000)
  val regionalModel = Model("Embraer ERJ 140", capacity = 44, fuelBurn = 81, speed = 828, range = 2315, price = 17000000)
  val smallModel = Model("Bombardier CS100", capacity = 133, fuelBurn = 267, speed = 828, range = 5741, price = 71800000)
  val mediumModel = Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = 274, speed = 907, range = 13621, price = 225000000)
  val largeAirplaneModel = Model("Boeing 777-300", capacity = 550, fuelBurn = 451, speed = 945, range = 11121, price = 250000000)
                      
  val lightAirplane = Airplane(lightModel, testAirline1, 0, 100)      
  val regionalAirplane = Airplane(regionalModel, testAirline1, 0, 100)
  val smallAirplane = Airplane(smallModel, testAirline1, 0, 100)
  val mediumAirplane = Airplane(mediumModel, testAirline1, 0, 100)
  val largeAirplane = Airplane(largeAirplaneModel, testAirline1, 0, 100)
  
  import Model.Type._
  //LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, JUMBO
  private val GOOD_RETURN_RATE = Map(LIGHT -> 0.02, REGIONAL -> 0.015, SMALL -> 0.007, MEDIUM -> 0.005, LARGE -> 0.005, JUMBO -> 0.005)
  private val MAX_RETURN_RATE = 0.06 //nothing should exceed 0.06
  
  "Compute profit".must {
    "More profitable with more frequency flight (max LF)".in {
      val distance = 200
      val airplane = lightAirplane
      val duration = Computation.calculateDuration(airplane.model, distance)
      val price = Pricing.computeStandardPrice(distance, SHORT_HAUL_DOMESTIC)
      
      var frequency = Computation.calculateMaxFrequency(airplane.model, distance)
      var capacity = frequency * airplane.model.capacity
      
      var link = Link(fromAirport, toAirport, testAirline1, price, distance, capacity, rawQuality = 0, duration, frequency)
      link.availableSeats = 0
      link.setAssignedAirplanes(List(airplane))
      
      val consumptionResultHighFequency = LinkSimulation.computeLinkConsumptionDetail(link , 0)
      
      frequency = 1
      capacity = frequency * airplane.model.capacity
      link = link.copy(capacity = capacity, frequency = frequency)
      link.availableSeats = 0
      link.setAssignedAirplanes(List(airplane))
      val consumptionResultLowFequency = LinkSimulation.computeLinkConsumptionDetail(link , 0)
      
      consumptionResultHighFequency.profit.should(be > consumptionResultLowFequency.profit)
      
    }
    "Not at all profitable at very low LF (0.1 LF) even at suitable range".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.1, 3)
      consumptionResult.profit.should(be < 0)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(1000, airplane, SHORT_HAUL_DOMESTIC, 0.1, 3)
      consumptionResult.profit.should(be < 0)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(4000, airplane, LONG_HAUL_DOMESTIC, 0.1, 4)
      consumptionResult.profit.should(be < 0)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(8000, airplane, LONG_HAUL_INTERNATIONAL, 0.1, 5)
      consumptionResult.profit.should(be < 0)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERNATIONAL, 0.1, 6)
      consumptionResult.profit.should(be < 0)
    }
    
    "Some profit (but not good) at 0.5 LF at suitable range".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5, 3)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(1000, airplane, SHORT_HAUL_DOMESTIC, 0.5, 3)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(4000, airplane, LONG_HAUL_DOMESTIC, 0.5, 4)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(8000, airplane, LONG_HAUL_INTERNATIONAL, 0.5, 5)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERNATIONAL, 0.5, 6)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    
    "Good profit at MAX LF at suitable range".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 3)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, true)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(1000, airplane, SHORT_HAUL_DOMESTIC, 1, 3)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, true)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(4000, airplane, LONG_HAUL_DOMESTIC, 1, 4)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, true)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(8000, airplane, LONG_HAUL_INTERNATIONAL, 1, 5)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, true)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERNATIONAL, 1, 6)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    
    
    "Not profitable at all on very short route < 200 km at large airport (max LF)".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 6)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 6)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 6)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 6)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 6)
      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    
    "Only good profit with smaller jets on very short route < 200 km at small airport (max LF)".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1)
      verfiyReturnRate(consumptionResult, airplane.model, true)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1)
      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    "Only profitable with smaller jets on very short route < 200 km at small airport (0.5 LF)".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5)
      consumptionResult.profit.should(be > 0)
            
      airplane = regionalAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5)
      consumptionResult.profit.should(be > 0)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5)
      verfiyReturnRate(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.5)
      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    
    
    
  }
  
  def verfiyReturnRate(consumptionResult : LinkConsumptionDetails, model : Model, expectGoodReturn : Boolean) = {
    val returnRate = consumptionResult.profit.toDouble / model.price
    println("return rate on  " + model + " : " +  returnRate)
    if (expectGoodReturn) {
      returnRate.should(be >= GOOD_RETURN_RATE(model.airplaneType) and be <= MAX_RETURN_RATE)
    } else {
      returnRate.should(be < GOOD_RETURN_RATE(model.airplaneType))
    }
  }
  
  
  def simulateStandard(distance : Int, airplane : Airplane, flightType : FlightType, loadFactor : Double, airportSize : Int = 3) : LinkConsumptionDetails = {
    val duration = Computation.calculateDuration(airplane.model, distance)
    val frequency = Computation.calculateMaxFrequency(airplane.model, distance)
    val capacity = frequency * airplane.model.capacity
    val price = Pricing.computeStandardPrice(distance, flightType)
    
    val neutralQuality = 
      flightType match {
      case SHORT_HAUL_DOMESTIC => 30
      case SHORT_HAUL_INTERNATIONAL => 40
      case LONG_HAUL_DOMESTIC => 50
      case LONG_HAUL_INTERNATIONAL => 60
      case ULTRA_LONG_HAUL_INTERNATIONAL => 70
    }
    
    val link = Link(fromAirport.copy(size = airportSize), toAirport.copy(size = airportSize), testAirline1, price, distance = distance, capacity, rawQuality = neutralQuality, duration, frequency)
    link.availableSeats = (capacity * (1 - loadFactor)).toInt
    link.setAssignedAirplanes(List(airplane))
    
    val consumptionResult = LinkSimulation.computeLinkConsumptionDetail(link , 0)
    println(consumptionResult)
    consumptionResult
  }
}


