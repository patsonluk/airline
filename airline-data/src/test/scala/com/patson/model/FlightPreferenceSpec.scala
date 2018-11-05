package com.patson.model

import scala.collection.mutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
import com.patson.DemandGenerator
import com.patson.Util
 
class FlightPreferenceSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val defaultCapacity = LinkClassValues.getInstance(10000, 10000, 10000)
  
  val testAirline1 = Airline("airline 1", id = 1)
  val testAirline2 = Airline("airline 2", id = 2)
  val topAirline = Airline("top airline", id = 3)
  val fromAirport = Airport("", "", "From Airport", 0, 0, "", "", "", 1, 0, 0, 0, 0)
  val toAirport = Airport("", "", "To Airport", 0, 180, "", "", "", 1, 0, 0, 0, 0)
  
  
  val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  val defaultPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
  
  fromAirport.initAirlineAppeals(scala.collection.immutable.Map.empty)
  toAirport.initAirlineAppeals(scala.collection.immutable.Map.empty)
  fromAirport.initLounges(scala.collection.immutable.List.empty)
  toAirport.initLounges(scala.collection.immutable.List.empty)
  val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
  val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, distance = distance, defaultCapacity, rawQuality = Link.neutralQualityOfClass(FIRST, fromAirport, toAirport, flightType), 600, 1, flightType)  //make the quality a bit higher
  val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, distance = distance, defaultCapacity, rawQuality = Link.neutralQualityOfClass(FIRST, fromAirport, toAirport, flightType), 600, 1, flightType)  //make the quality a bit higher 
  val topAirlineLink = Link(fromAirport, toAirport, testAirline2, defaultPrice, distance = distance, defaultCapacity, rawQuality = 100, 600, 1, flightType) 
  airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), testAirline1, 0, 100, 0, 0)))
  airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), testAirline2, 0, 100, 0, 0)))
  topAirlineLink.setAssignedAirplanes(List(Airplane(Model.fromId(0), topAirline, 0, 100, 0, 0)))
  
  "An AppealPreference".must {
    "generate similar cost if price and distance is the same, and small differece in loyalty".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 30)
      fromAirport.setAirlineLoyalty(testAirline2.id, 32)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        //should be around 50 50
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
        
      }
      val ratio = airline1Picked.toDouble / airline2Picked
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
      
    }
    "generate similar cost if distance and loyalty is the same, and small differece in price".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1005), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        //should be around 50 50
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
        
      }
      val ratio = airline1Picked.toDouble / airline2Picked
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if price and loyalty is the same, and small differece in distance".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if everything is the same but small differece in raw link quality".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 51, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 1000, defaultCapacity, 50, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if everything is the same, and small differece in raw link quality".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 51, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10100, defaultCapacity, 50, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    
    
    "generate differentiating but overlapping cost if everything is the same, but loyalty at big difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 10)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.5)) //significantly more people should pick airline 2
      ratio.shouldBe( > (0.1)) //yet some will still pick airline 1
    }
    "generate almost no overlapping cost if everything is the same, but loyalty at min vs max".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 0)
      fromAirport.setAirlineLoyalty(testAirline2.id, 100)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.1)) //almost everyone should go for airline 2
      ratio.shouldBe( > (0.0)) //yet some will still pick airline 1 due to randomness 
    }
    "generate differentiating but overlapping cost if everything is the same, but price at big difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1300), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (2.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (20.0)) //yet some will still pick airline 2
    }
    "generate no overlapping cost if everything is the same, but price is at huge difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 50)
      fromAirport.setAirlineLoyalty(testAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(5000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      airline1Picked.shouldBe(0) //noone should pick airline 1
    }
    "generate differentiating but overlapping cost if everything is the same, but quality at big difference".in {
      val adjustedAirline1 = testAirline1.copy()
      adjustedAirline1.setServiceQuality(70)
      val adjustedAirline2 = testAirline2.copy()
      adjustedAirline2.setServiceQuality(20)
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1, flightType)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (2.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (30.0)) //yet some will still pick airline 2
    }
    "generate almost no overlapping cost if everything is the same, but quality at huge difference (0 vs 100)".in {
      val adjustedAirline1 = testAirline1.copy()
      adjustedAirline1.setServiceQuality(100)
      val adjustedAirline2 = testAirline2.copy()
      adjustedAirline2.setServiceQuality(0)
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 100, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 0, 0, 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (4.0)) //significantly more people should pick airline 1
      //ratio.shouldBe( < (.0)) //yet some will still pick airline 2
    }
    
    "generate similar cost if everything is the same but one has lounge but all passengers are econ".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 0.9)
      assert(ratio < 1.1)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has level 1 lounge and all passengers are business class with some lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, loungeLevelRequired = 0 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 2)
      assert(ratio < 5)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has level 3 lounge and all passengers are business class with no lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, loungeLevelRequired = 0 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 10)
      assert(ratio < 20)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has higher level lounge and all passengers are business class with some lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 2, status = LoungeStatus.ACTIVE, foundedCycle = 0),
                                   Lounge(adjustedAirline2, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 2, status = LoungeStatus.ACTIVE, foundedCycle = 0),
                                 Lounge(adjustedAirline2, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, loungeLevelRequired = 1 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 2)
      assert(ratio < 4)
    }
    "generate no overlaps if everything is the same but one has lounge but all passengers are business class with max lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100, 0, 0)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100, 0, 0)))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, loungeLevelRequired = 3 , 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      assert(airline2Picked == 0)
    }
    "generate higher cost for higher link class if the quality is the same". in {
      var economyTotalCost : Long = 0
      var businessTotalCost : Long = 0
      var firstTotalCost : Long = 0
      for (i <- 0 until 100000) {
        val economyCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, loungeLevelRequired = 0, 0).computeCost(airline1Link)
        val businessCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, loungeLevelRequired = 0, 0).computeCost(airline1Link)
        val firstCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, FIRST, loungeLevelRequired = 0, 0).computeCost(airline1Link)
        economyTotalCost += economyCost.toLong
        businessTotalCost += businessCost.toLong
        firstTotalCost += firstCost.toLong
      }
      
      economyTotalCost.should(be < businessTotalCost)
      businessTotalCost.should(be < firstTotalCost)
    }
  }
   "An SimplePreference".must {
      "adjust price accordingly due to price weight ". in {
      val expensiveLink = airline1Link.copy(price = LinkClassValues.getInstance(10000, 10000, 10000))
      val cost1 = SimplePreference(0.8, ECONOMY).computeCost(expensiveLink)
      val cost2 = SimplePreference(1.2, ECONOMY).computeCost(expensiveLink)
      val standardPrice = Pricing.computeStandardPrice(expensiveLink, ECONOMY)
      val delta1 = Math.abs(cost1 - standardPrice) //should be small delta as this group of customer care less about price
      val delta2 = Math.abs(cost2 - standardPrice)
      delta1.should(be < delta2)
    }
//     "should not completely ignore price delta even at lowest price sensitivity". in {
//      val cost1 = SimplePreference(0, 10, ECONOMY).computeCost(airline1Link)
//      val standardPrice = Pricing.computeStandardPrice(airline1Link, ECONOMY)
//      val delta1 = Math.abs(cost1 - standardPrice) 
//      delta1.should(be > 0.0)
//    }
      
   }
   
  
  //this is from DemandGenerator... 
  "Get FlightPreference pool".must {
    "generate some preference to compute to a cost lower than suggested price if the link is priced at standard price".in {
       var lowerCount = 0
       for (i <- 0 until 100) {
         DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.foreach {
           case (linkClass, flightPreferences) => flightPreferences.foreach { flightPreference =>
             val cost = flightPreference.computeCost(airline1Link)
             if (cost <= airline1Link.price(linkClass)) {
               lowerCount += 1
             }
           }
         }
       }
       assert(lowerCount > 0)
    }
    "not generate preference to compute to a cost lower than standard price with extremely overpriced ticket even with perfect link/airline".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, AirlineAppeal.MAX_LOYALTY) //
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
      val extremePrice = suggestedPrice * 5
      val extremeLink = airline1Link.copy(price = extremePrice, rawQuality = Link.MAX_QUALITY)
      for (i <- 0 until 100) {
        DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.foreach {
          case (linkClass, flightPreferences) => flightPreferences.foreach { flightPreference =>
            val cost = flightPreference.computeCost(extremeLink)
            assert(cost >= suggestedPrice(linkClass)) //the cost should not be reduced to lower than the standard price no matter how good it is
          }
         
        }
      }
    }
    "generate some preference to compute to a cost lower than standard price with overpriced ticket with perfect link/airline".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, AirlineAppeal.MAX_LOYALTY) //
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
      val highPrice = suggestedPrice * 1.2
      val highPriceLink = airline1Link.copy(price = highPrice, rawQuality = Link.MAX_QUALITY)
      
      
      for (i <- 0 until 100) {
        val lowerCostOption = DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.find {
           case (linkClass, flightPreferences) => flightPreferences.find{ flightPreference =>
             val cost = flightPreference.computeCost(highPriceLink)
             cost < suggestedPrice(linkClass)
           }.isDefined
        }
        
        assert(lowerCostOption.isDefined)
      }
    }
    
    
  }
}
