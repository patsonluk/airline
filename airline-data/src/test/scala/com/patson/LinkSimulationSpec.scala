package com.patson

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import com.patson.model._
import com.patson.model.FlightType._
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
 
class LinkSimulationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1")
  val testAirline2 = Airline("airline 2")
  val fromAirport = Airport.fromId(1).copy(size = 3)
  fromAirport.initAirlineBases(List.empty)
  val toAirport = Airport.fromId(2).copy(size = 3)
  toAirport.initAirlineBases(List.empty)
  
  val lightModel = Model.modelByName("Cessna Caravan")
  val regionalModel = Model.modelByName("Embraer ERJ 140")
  val smallModel = Model.modelByName("Bombardier CS100")
  val mediumModel = Model.modelByName("Boeing 787-8 Dreamliner")
  val largeAirplaneModel = Model.modelByName("Boeing 777-300")
                      
  val decayRate = (AirplaneSimulation.MAX_DECAY + AirplaneSimulation.MIN_DECAY) / 2
  val lightAirplane = Airplane(lightModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(lightModel, decayRate), lightModel.price)      
  val regionalAirplane = Airplane(regionalModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(regionalModel, decayRate), regionalModel.price)
  val smallAirplane = Airplane(smallModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(smallModel, decayRate), smallModel.price)
  val mediumAirplane = Airplane(mediumModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(mediumModel, decayRate), mediumModel.price)
  val largeAirplane = Airplane(largeAirplaneModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(largeAirplaneModel, decayRate), largeAirplaneModel.price)
  
  import Model.Type._
  //LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, JUMBO
  private val GOOD_PROFIT_MARGIN = Map(LIGHT -> 0.3, REGIONAL -> 0.3, SMALL -> 0.3, MEDIUM -> 0.3, LARGE -> 0.4, JUMBO -> 0.4)
  private val MAX_PROFIT_MARGIN = Map(LIGHT -> 0.6, REGIONAL -> 0.7, SMALL -> 0.7, MEDIUM -> 0.8, LARGE -> 0.8, JUMBO -> 0.8)
  
  "Compute profit".must {
    "More profitable with more frequency flight (max LF)".in {
      val distance = 200
      val airplane = lightAirplane
      val duration = Computation.calculateDuration(airplane.model, distance)
      val price = Pricing.computeStandardPrice(distance, SHORT_HAUL_DOMESTIC, ECONOMY)
      
      var frequency = Computation.calculateMaxFrequency(airplane.model, distance)
      var capacity = frequency * airplane.model.capacity
      
      var link = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(ECONOMY -> price)), distance, LinkClassValues(Map(ECONOMY -> capacity)), rawQuality = 0, duration, frequency)
      link.availableSeats = LinkClassValues(Map(ECONOMY -> 0))
      link.setAssignedAirplanes(List(airplane))
      
      val consumptionResultHighFequency = LinkSimulation.computeLinkConsumptionDetail(link , 0)
      
      frequency = 1
      capacity = frequency * airplane.model.capacity
      link = link.copy(capacity = LinkClassValues(Map(ECONOMY -> capacity)), frequency = frequency)
      link.availableSeats = LinkClassValues(Map(ECONOMY -> 0))
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
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERCONTINENTAL, 0.1, 6)
      consumptionResult.profit.should(be < 0)
    }
    
    "Some profit (but not good) at 0.6 LF at suitable range".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 0.6, airportSize = 2)
      consumptionResult.profit.should(be > 0)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(1000, airplane, SHORT_HAUL_DOMESTIC, 0.6, 3)
      consumptionResult.profit.should(be > 0)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(4000, airplane, LONG_HAUL_DOMESTIC, 0.6, 4)
      consumptionResult.profit.should(be > 0)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(8000, airplane, LONG_HAUL_INTERNATIONAL, 0.6, 5)
      consumptionResult.profit.should(be > 0)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERCONTINENTAL, 0.6, 6)
      consumptionResult.profit.should(be > 0)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
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
      consumptionResult = simulateStandard(10000, airplane, ULTRA_LONG_HAUL_INTERCONTINENTAL, 1, 6)
      consumptionResult.profit.should(be > 0)
//      verfiyReturnRate(consumptionResult, airplane.model, false)
    }
    
    
    "Not profitable at all on very short route < 200 km at large airport (max LF)".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 8)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 8)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = smallAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 8)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = mediumAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 8)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
      
      airplane = largeAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, 8)
      verfiyProfitMargin(consumptionResult, airplane.model, false)
    }
    
    "Good profit with smaller jets on very short route < 200 km at small airport (max LF)".in {
      var airplane = lightAirplane
      var consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, airportSize = 2)
      verfiyProfitMargin(consumptionResult, airplane.model, true)
      
      airplane = regionalAirplane
      consumptionResult = simulateStandard(200, airplane, SHORT_HAUL_DOMESTIC, 1, airportSize = 2)
      verfiyProfitMargin(consumptionResult, airplane.model, true)
    }
    "More profit with more jets on route if LF is full".in {
       val onePlaneResult = simulateStandard(200, lightModel, SHORT_HAUL_DOMESTIC, 1, airplaneCount = 1)
       val fivePlaneResult = simulateStandard(200, lightModel, SHORT_HAUL_DOMESTIC, 1, airplaneCount = 5)
       
       fivePlaneResult.profit.should(be >= (onePlaneResult.profit * 4.9).toInt) //4.9 as some truncation might make the number off a tiny bit 
    }
    "More profit at higher link class at max LF (regional plane)".in  {
      val airplane = regionalAirplane
      val airplaneModel = airplane.model
      val distance = 2000
      val duration = Computation.calculateDuration(airplaneModel, distance)
      val frequency = Computation.calculateMaxFrequency(airplaneModel, distance)
      val maxEconomyCapacity = (airplaneModel.capacity / ECONOMY.spaceMultiplier).toInt * frequency
      val maxBusinessCapacity = (airplaneModel.capacity / BUSINESS.spaceMultiplier).toInt * frequency
      val maxFirstCapacity = (airplaneModel.capacity / FIRST.spaceMultiplier).toInt * frequency
      val allEconomyCapacity : LinkClassValues = LinkClassValues.getInstance(maxEconomyCapacity, 0, 0)
      val allBusinessCapacity : LinkClassValues = LinkClassValues.getInstance(0, maxBusinessCapacity, 0)
      val allFirstCapacity : LinkClassValues = LinkClassValues.getInstance(0, 0, maxFirstCapacity)
      
      val economyPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_DOMESTIC, ECONOMY)
      val businessPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_DOMESTIC, BUSINESS)
      val firstPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_DOMESTIC, FIRST)
    
      val economylink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(ECONOMY -> economyPrice)), distance = distance, allEconomyCapacity, rawQuality = 60, duration, frequency)
      val businessLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(BUSINESS -> businessPrice)), distance = distance, allBusinessCapacity, rawQuality = 60, duration, frequency)
      val firstLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(FIRST -> firstPrice)), distance = distance, allFirstCapacity, rawQuality = 60, duration, frequency)
    
      economylink.availableSeats = LinkClassValues.getInstance() //all consumed
      businessLink.availableSeats = LinkClassValues.getInstance() //all consumed
      firstLink.availableSeats = LinkClassValues.getInstance() //all consumed
    
      economylink.setAssignedAirplanes(List(airplane))
      businessLink.setAssignedAirplanes(List(airplane))
      firstLink.setAssignedAirplanes(List(airplane))
      
    
      val economyResult = LinkSimulation.computeLinkConsumptionDetail(economylink , 0)
      val businessResult = LinkSimulation.computeLinkConsumptionDetail(businessLink , 0)
      val firstResult = LinkSimulation.computeLinkConsumptionDetail(firstLink , 0)
      
      economyResult.profit.should(be < businessResult.profit)
      businessResult.profit.should(be < firstResult.profit)
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType)) 
    }
    "More profit at higher link class at max LF (small plane)".in  {
      val airplane = smallAirplane
      val airplaneModel = airplane.model
      val distance = 5000
      val duration = Computation.calculateDuration(airplaneModel, distance)
      val frequency = Computation.calculateMaxFrequency(airplaneModel, distance)
      val maxEconomyCapacity = (airplaneModel.capacity / ECONOMY.spaceMultiplier).toInt * frequency
      val maxBusinessCapacity = (airplaneModel.capacity / BUSINESS.spaceMultiplier).toInt * frequency
      val maxFirstCapacity = (airplaneModel.capacity / FIRST.spaceMultiplier).toInt * frequency
      val allEconomyCapacity : LinkClassValues = LinkClassValues.getInstance(maxEconomyCapacity, 0, 0) 
      val allBusinessCapacity : LinkClassValues = LinkClassValues.getInstance(0, maxBusinessCapacity, 0)
      val allFirstCapacity : LinkClassValues = LinkClassValues.getInstance(0, 0, maxFirstCapacity)
      
      val economyPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_INTERNATIONAL, ECONOMY)
      val businessPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_INTERNATIONAL, BUSINESS)
      val firstPrice = Pricing.computeStandardPrice(distance, SHORT_HAUL_INTERNATIONAL, FIRST)
    
      val economylink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(ECONOMY -> economyPrice)), distance = distance, allEconomyCapacity, rawQuality = 60, duration, frequency)
      val businessLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(BUSINESS -> businessPrice)), distance = distance, allBusinessCapacity, rawQuality = 60, duration, frequency)
      val firstLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(FIRST -> firstPrice)), distance = distance, allFirstCapacity, rawQuality = 60, duration, frequency)
    
      economylink.availableSeats = LinkClassValues.getInstance() //all consumed
      businessLink.availableSeats = LinkClassValues.getInstance() //all consumed
      firstLink.availableSeats = LinkClassValues.getInstance() //all consumed
    
      economylink.setAssignedAirplanes(List(airplane))
      businessLink.setAssignedAirplanes(List(airplane))
      firstLink.setAssignedAirplanes(List(airplane))
      
    
      val economyResult = LinkSimulation.computeLinkConsumptionDetail(economylink , 0)
      val businessResult = LinkSimulation.computeLinkConsumptionDetail(businessLink , 0)
      val firstResult = LinkSimulation.computeLinkConsumptionDetail(firstLink , 0)
      
      economyResult.profit.should(be < businessResult.profit)
      businessResult.profit.should(be < firstResult.profit)
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType)) 
    }
    "More profit at higher link class at max LF (large plane)".in  {
      val airplane = largeAirplane
      val airplaneModel = airplane.model
      val distance = 10000
      val duration = Computation.calculateDuration(airplaneModel, distance)
      val frequency = Computation.calculateMaxFrequency(airplaneModel, distance)
      val maxEconomyCapacity = (airplaneModel.capacity / ECONOMY.spaceMultiplier).toInt * frequency
      val maxBusinessCapacity = (airplaneModel.capacity / BUSINESS.spaceMultiplier).toInt * frequency
      val maxFirstCapacity = (airplaneModel.capacity / FIRST.spaceMultiplier).toInt * frequency
      val allEconomyCapacity : LinkClassValues = LinkClassValues.getInstance(maxEconomyCapacity, 0, 0)
      val allBusinessCapacity : LinkClassValues = LinkClassValues.getInstance(0, maxBusinessCapacity, 0)
      val allFirstCapacity : LinkClassValues = LinkClassValues.getInstance(0, 0, maxFirstCapacity)
      
      val economyPrice = Pricing.computeStandardPrice(distance, ULTRA_LONG_HAUL_INTERCONTINENTAL, ECONOMY)
      val businessPrice = Pricing.computeStandardPrice(distance, ULTRA_LONG_HAUL_INTERCONTINENTAL, BUSINESS)
      val firstPrice = Pricing.computeStandardPrice(distance, ULTRA_LONG_HAUL_INTERCONTINENTAL, FIRST)
    
      val economylink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(ECONOMY -> economyPrice)), distance = distance, allEconomyCapacity, rawQuality = 60, duration, frequency)
      val businessLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(BUSINESS -> businessPrice)), distance = distance, allBusinessCapacity, rawQuality = 60, duration, frequency)
      val firstLink = Link(fromAirport, toAirport, testAirline1, LinkClassValues(Map(FIRST -> firstPrice)), distance = distance, allFirstCapacity, rawQuality = 60, duration, frequency)
    
      economylink.availableSeats = LinkClassValues.getInstance() //all consumed
      businessLink.availableSeats = LinkClassValues.getInstance() //all consumed
      firstLink.availableSeats = LinkClassValues.getInstance() //all consumed
    
      economylink.setAssignedAirplanes(List(airplane))
      businessLink.setAssignedAirplanes(List(airplane))
      firstLink.setAssignedAirplanes(List(airplane))
      
    
      val economyResult = LinkSimulation.computeLinkConsumptionDetail(economylink , 0)
      val businessResult = LinkSimulation.computeLinkConsumptionDetail(businessLink , 0)
      val firstResult = LinkSimulation.computeLinkConsumptionDetail(firstLink , 0)
      
      economyResult.profit.should(be < businessResult.profit)
      businessResult.profit.should(be < firstResult.profit)
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be > GOOD_PROFIT_MARGIN(airplane.model.airplaneType))
      
      (economyResult.profit.toDouble / economyResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))      
      (businessResult.profit.toDouble / businessResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType))
      (firstResult.profit.toDouble / firstResult.revenue.toDouble).should(be < MAX_PROFIT_MARGIN(airplane.model.airplaneType)) 
    }
  }
  
  def verfiyProfitMargin(consumptionResult : LinkConsumptionDetails, model : Model, expectGoodReturn : Boolean) = {
    val profitMargin = consumptionResult.profit.toDouble / consumptionResult.revenue.toDouble
    println(consumptionResult.soldSeats(ECONOMY) * 100 / consumptionResult.capacity(ECONOMY) + "%" + " PM:" +  profitMargin + " " +  model.name + " " + consumptionResult)
    if (expectGoodReturn) {
      profitMargin.should(be >= GOOD_PROFIT_MARGIN(model.airplaneType) and be <= MAX_PROFIT_MARGIN(model.airplaneType))
    } else {
      profitMargin.should(be < GOOD_PROFIT_MARGIN(model.airplaneType))
    }
  }
  def simulateStandard(distance : Int, airplane : Airplane, flightType : FlightType, loadFactor : Double) : LinkConsumptionDetails = {
    simulateStandard(distance, airplane.model, flightType, loadFactor, 3, 1)
  }
  def simulateStandard(distance : Int, airplane : Airplane, flightType : FlightType, loadFactor : Double, airportSize : Int) : LinkConsumptionDetails = {
    simulateStandard(distance, airplane.model, flightType, loadFactor, airportSize, 1)
  }
  
  def simulateStandard(distance : Int, airplaneModel : Model, flightType : FlightType, loadFactor : Double, airportSize : Int = 3, airplaneCount : Int = 1) : LinkConsumptionDetails = {
    val duration = Computation.calculateDuration(airplaneModel, distance)
    val frequency = Computation.calculateMaxFrequency(airplaneModel, distance) * airplaneCount
    val capacity = frequency * airplaneModel.capacity
    val price = Pricing.computeStandardPrice(distance, flightType, ECONOMY)
    
    val neutralQuality = 
      flightType match {
      case SHORT_HAUL_DOMESTIC => 30
      case SHORT_HAUL_INTERNATIONAL => 40
      case SHORT_HAUL_INTERCONTINENTAL => 45
      case LONG_HAUL_DOMESTIC => 50
      case LONG_HAUL_INTERNATIONAL => 60
      case LONG_HAUL_INTERCONTINENTAL => 65
      case ULTRA_LONG_HAUL_INTERCONTINENTAL => 75
    }
    
    val fromAirportClone = fromAirport.copy(size = airportSize)
    fromAirportClone.initAirlineBases(fromAirport.getAirlineBases().map(_._2).toList)
    val toAirportClone = toAirport.copy(size = airportSize)
    toAirportClone.initAirlineBases(toAirport.getAirlineBases().map(_._2).toList)
    
    val link = Link(fromAirportClone, toAirportClone, testAirline1, LinkClassValues(Map(ECONOMY -> price)), distance = distance, LinkClassValues(Map(ECONOMY -> capacity)), rawQuality = neutralQuality, duration, frequency)
    link.availableSeats = LinkClassValues(Map(ECONOMY -> (capacity * (1 - loadFactor)).toInt))
    
    
    link.setAssignedAirplanes((0 until airplaneCount).foldRight(List[Airplane]()) { 
      case (_, foldList) => Airplane(airplaneModel, testAirline1, 0, 100, AirplaneSimulation.computeDepreciationRate(airplaneModel, decayRate), airplaneModel.price) :: foldList  
    })
    
    val consumptionResult = LinkSimulation.computeLinkConsumptionDetail(link , 0)
    println(consumptionResult)
    consumptionResult
  }
}


