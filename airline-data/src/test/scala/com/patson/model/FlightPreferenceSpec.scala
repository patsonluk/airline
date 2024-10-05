package com.patson.model

import scala.collection.mutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.ImplicitSender
import org.apache.pekko.testkit.TestKit
import com.patson.model.airplane.{Airplane, AirplaneConfiguration, LinkAssignment, Model}
import com.patson.DemandGenerator
import com.patson.Util

import scala.collection.mutable
 
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
  val fromAirport = Airport("", "", "From Airport", 0, 0, "", "", "", 1, baseIncome = 40000, basePopulation = 1, 0, 0)
  val toAirport = Airport("", "", "To Airport", 0, 180, "", "", "", 1, baseIncome = 40000, basePopulation = 1, 0, 0)
  
  
  val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  val defaultPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
  
  fromAirport.initAirlineAppeals(scala.collection.immutable.Map.empty)
  toAirport.initAirlineAppeals(scala.collection.immutable.Map.empty)
  fromAirport.initLounges(scala.collection.immutable.List.empty)
  toAirport.initLounges(scala.collection.immutable.List.empty)
  val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
  val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, distance = distance, defaultCapacity, rawQuality = 0, 600, 1, flightType)  
  val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, distance = distance, defaultCapacity, rawQuality = 0, 600, 1, flightType)  
  airline1Link.setQuality(fromAirport.expectedQuality(flightType, FIRST))
  airline2Link.setQuality(fromAirport.expectedQuality(flightType, FIRST))
  val topAirlineLink = Link(fromAirport, toAirport, testAirline2, defaultPrice, distance = distance, defaultCapacity, rawQuality = 100, 600, 1, flightType)
  val model = Model.modelByName("Boeing 737 MAX 9")
  airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
  airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, testAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
  topAirlineLink.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, topAirline, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
  
  "An AppealPreference".must {
    "generate similar cost if price and distance is the same, and small differece in loyalty".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(30), testAirline2.id -> AirlineAppeal(32)))

      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        //should be around 50 50
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
        
      }
      val ratio = airline1Picked.toDouble / airline2Picked
      ratio.shouldBe( >= (0.8))         //should be around 50 50
      ratio.shouldBe( <= (1.2))
      
    }
    "generate similar cost if distance and loyalty is the same, and small differece in price".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1005), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        //should be around 50 50
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
        
      }
      val ratio = airline1Picked.toDouble / airline2Picked
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if price and loyalty is the same, and small differece in distance".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if everything is the same but small difference in raw link quality".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 51, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 50, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate similar cost if everything is the same, and small differece in raw link quality".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 51, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10100, defaultCapacity, 50, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    
    
    "generate differentiating but overlapping cost if everything is the same, but loyalty at big difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(10), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.5)) //significantly more people should pick airline 2
      ratio.shouldBe( > (0.05)) //yet some will still pick airline 1
    }
    "generate almost no overlapping cost if everything is the same, but loyalty at min vs max".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(0), testAirline2.id -> AirlineAppeal(100)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.1)) //almost everyone should go for airline 2
      ratio.shouldBe( > (0.0)) //yet some will still pick airline 1 due to randomness 
    }
    "generate differentiating but overlapping cost if everything is the same, but price at some difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1100), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (1.5)) //more people should pick airline 1
      ratio.shouldBe( < (4.0)) //yet some will still pick airline 2
    }
    
    "generate differentiating but overlapping cost if everything is the same, but price at big difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1200), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (2.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (20.0)) //yet some will still pick airline 2
    }
    "generate no overlapping cost if everything is the same, but price is at huge difference".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(50), testAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(5000), 10000, defaultCapacity, 0, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1, flightType)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      airline1Picked.shouldBe(0) //noone should pick airline 1
    }
    "generate differentiating but overlapping cost if everything is the same, but quality at small difference".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1, flightType)
      airline1Link.setQuality(55)
      airline2Link.setQuality(50)
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1) //more people should pick airline 1
      assert(ratio < 2) //yet some will still pick airline 2
    }
    "generate almost same cost if everything is the same, but quality at small difference and both very high".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1, flightType)
      airline1Link.setQuality(100)
      airline2Link.setQuality(90)
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      println("EQ " + fromAirport.expectedQuality(flightType, ECONOMY))
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (1.0)) //more people should pick airline 1
      ratio.shouldBe( < (1.5)) //yet some will still pick airline 2
    }
    "generate differentiating but little overlapping cost if everything is the same, but quality at big difference".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1, flightType)
      airline1Link.setQuality(80)
      airline2Link.setQuality(40)
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (5.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (20.0)) //yet a few will still pick airline 2
    }
    "generate almost no overlapping cost if everything is the same, but quality at huge difference (0 vs 100)".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 100, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      
      airline1Link.setQuality(100)
      airline2Link.setQuality(0)
      
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1 , 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (10.0)) //significantly more people should pick airline 1
      //ratio.shouldBe( < (.0)) //yet some will still pick airline 2
    }
    
    "generate no overlap cost if everything is the same, but quality at big difference and the country has high quality expectation".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      val fromAirport = this.fromAirport.copy(baseIncome = 1000000, basePopulation = 1)
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1, flightType)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1, flightType)
      airline1Link.setQuality(60)
      airline2Link.setQuality(10)
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (10.0)) //significantly more people should pick airline 1
    }
    
    "generate similar cost if everything is the same but one has lounge but all passengers are econ".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1 , 0)
        val link1Cost = preference.computeCost(airline1Link, ECONOMY)
        val link2Cost = preference.computeCost(airline2Link, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 0.9)
      assert(ratio < 1.1)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has level 1 lounge and all passengers are business class with some lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1 , 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1)
      assert(ratio < 2)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has level 3 lounge and all passengers are business class with no lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1 , 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1.5)
      assert(ratio < 2.5)
    }
    
    "generate different yet some overlapping cost if everything is the same but one has higher level lounge and all passengers are business class with some lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 2, status = LoungeStatus.ACTIVE, foundedCycle = 0),
                                   Lounge(adjustedAirline2, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 2, status = LoungeStatus.ACTIVE, foundedCycle = 0),
                                 Lounge(adjustedAirline2, allianceId = None, fromAirport, level = 1, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 1 , loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1)
      assert(ratio < 2)
    }
    "generate no overlaps if everything is the same but one has lounge but all passengers are business class with max lounge requirement".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(50), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      airline2Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline1, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      airline1Link.setTestingAssignedAirplanes(scala.collection.immutable.Map(Airplane(model, adjustedAirline2, 0, purchasedCycle = 0, 100, 0, 0) -> 1))
      
      fromAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, fromAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
      toAirport.initLounges(List(Lounge(adjustedAirline1, allianceId = None, toAirport, level = 3, status = LoungeStatus.ACTIVE, foundedCycle = 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 3, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      assert(airline2Picked == 0)
    }
    "favor high loyalty more if loyalty ratio > 1".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(60), adjustedAirline2.id -> AirlineAppeal(50)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      fromAirport.initLounges(List())
      toAirport.initLounges(List())
      
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 10000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val appealRatio = airline1Picked.toDouble / airline2Picked //should be > 1 but not by much
      airline1Picked = 0
      airline2Picked = 0
      for (i <- 0 until 10000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 2, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val loyaltyRatio =  airline1Picked.toDouble / airline2Picked //should be > 1 and bigger than appealRatio
      
      println(appealRatio)
      println(loyaltyRatio)
      assert(appealRatio > 1)
      assert(loyaltyRatio > 1)
      assert(loyaltyRatio / appealRatio > 1.2) 
    }
    "favor high loyalty even more if loyalty ratio > 1 if loyalty difference is obvious".in {
      val adjustedAirline1 = testAirline1.copy()
      val adjustedAirline2 = testAirline2.copy()
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](adjustedAirline1.id -> AirlineAppeal(60), adjustedAirline2.id -> AirlineAppeal(40)))
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline1Link.setQuality(100)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1, flightType)
      airline2Link.setQuality(100)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 10000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val appealRatio = airline1Picked.toDouble / airline2Picked //should be > 1 but not by much
      airline1Picked = 0
      airline2Picked = 0
      for (i <- 0 until 10000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 2, 0)
        val link1Cost = preference.computeCost(airline1Link, BUSINESS)
        val link2Cost = preference.computeCost(airline2Link, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val loyaltyRatio =  airline1Picked.toDouble / airline2Picked //should be > 1 and bigger than appealRatio
      
      println(appealRatio)
      println(loyaltyRatio)
      assert(appealRatio > 1)
      assert(appealRatio < 3)
      assert(loyaltyRatio > 4)
      assert(loyaltyRatio < 8)
      assert(loyaltyRatio / appealRatio > 1.5) 
    }
    
    "generate higher cost for higher link class if the quality is the same". in {
      var economyTotalCost : Long = 0
      var businessTotalCost : Long = 0
      var firstTotalCost : Long = 0
      for (i <- 0 until 100000) {
        val economyCost = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 1, 0).computeCost(airline1Link, ECONOMY)
        val businessCost = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 1, 0).computeCost(airline1Link, BUSINESS)
        val firstCost = AppealPreference(fromAirport, FIRST, loungeLevelRequired = 0, loyaltyRatio = 1, 0).computeCost(airline1Link, FIRST)
        economyTotalCost += economyCost.toLong
        businessTotalCost += businessCost.toLong
        firstTotalCost += firstCost.toLong
      }
      
      economyTotalCost.should(be < businessTotalCost)
      businessTotalCost.should(be < firstTotalCost)
    }
    
    "some overlap even if frequency diff is huge". in {
      val link1 = airline1Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(frequency = 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 0, 0)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1)
      assert(ratio < 2.5)
     }
     
      "mostly overlap if frequency diff is small (low frequency)". in {
      val link1 = airline1Link.copy(frequency = 3)
      val link2 = airline2Link.copy(frequency = 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 0, 0)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 1)
      assert(ratio < 1.2)
     }

    "mostly overlap if frequency diff is small". in {
      val link1 = airline1Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD + 4)
      val link2 = airline2Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 0, 0)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked
      assert(ratio > 1)
      assert(ratio < 1.2)
    }

    "consider frequency by class".in {
      val link1 = airline1Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val config1 = AirplaneConfiguration(100, 0, 0, testAirline1, model, false)
      val config2 = AirplaneConfiguration(0, 50, 0, testAirline1, model, false)
      link1.setAssignedAirplanes(
        scala.collection.immutable.Map(
          Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config1) -> LinkAssignment(Link.HIGH_FREQUENCY_THRESHOLD - 2, 6000) //more freq econ
          , Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config2) -> LinkAssignment(2, 6000)))
      link2.setAssignedAirplanes(
        scala.collection.immutable.Map(
          Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config2) -> LinkAssignment(Link.HIGH_FREQUENCY_THRESHOLD - 2, 6000) //more freq business
          , Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config1) -> LinkAssignment(2, 6000)))

      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, ECONOMY, loungeLevelRequired = 0, loyaltyRatio = 0, 0)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      var ratio = airline1Picked.toDouble / airline2Picked //more airline1 as it has more freq econ services
      assert(ratio > 1.4)
      assert(ratio < 1.8)

      airline1Picked = 0
      airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport, BUSINESS, loungeLevelRequired = 0, loyaltyRatio = 0, 0)
        val link1Cost = preference.computeCost(link1, BUSINESS)
        val link2Cost = preference.computeCost(link2, BUSINESS)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      ratio = airline2Picked.toDouble / airline1Picked //more airline2 as it has more freq econ services
      assert(ratio > 1.4)
      assert(ratio < 1.8)

    }

    "higher link class should care less about price".in {
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, Link.HIGH_FREQUENCY_THRESHOLD, flightType)
      val airline2Link = Link(fromAirport, toAirport, testAirline1, defaultPrice * (1.2), 10000, defaultCapacity, 0, 600, Link.HIGH_FREQUENCY_THRESHOLD, flightType)

      val ratios = mutable.HashMap[LinkClass, Double]()
      var airline1Picked = 0
      var airline2Picked = 0
      LinkClass.values.foreach { linkClass =>
        for (i <- 0 until 100000) {
          val preference = AppealPreference(fromAirport, linkClass, loungeLevelRequired = 0, loyaltyRatio = 1, 0)
          val link1Cost = preference.computeCost(airline1Link, linkClass)
          val link2Cost = preference.computeCost(airline2Link, linkClass)
          if (link1Cost < link2Cost) airline1Picked += 1 else airline2Picked += 1
        }
        ratios.put(linkClass, airline1Picked.toDouble / airline2Picked)
      }

      println(ratios)
      assert(ratios(ECONOMY) > ratios(BUSINESS))
      assert(ratios(BUSINESS) > ratios(FIRST))
      assert(ratios(FIRST) < 8)
      assert(ratios(ECONOMY) > 3)


    }
  }
   "A SimplePreference".must {
      "adjust price accordingly due to price weight ". in {
      val expensiveLink = airline1Link.copy(price = LinkClassValues.getInstance(10000, 10000, 10000))
      val cost1 = SimplePreference(fromAirport, 0.8, ECONOMY).computeCost(expensiveLink, ECONOMY)
      val cost2 = SimplePreference(fromAirport, 1.2, ECONOMY).computeCost(expensiveLink, ECONOMY)
      val standardPrice = expensiveLink.standardPrice(ECONOMY)
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
   
   "A SpeedPreference".must {
     "almost no overlap if frequency diff is huge". in {
      val link1 = airline1Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(frequency = 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = SpeedPreference(fromAirport, ECONOMY)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 10)
     }
     
      "some overlap if frequency diff is small". in {
      val link1 = airline1Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(frequency = Link.HIGH_FREQUENCY_THRESHOLD - 4)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = SpeedPreference(fromAirport, ECONOMY)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 2)
      assert(ratio < 4)
     }
     
   }
   
  
  //this is from DemandGenerator... 
  "Get FlightPreference pool".must {
    "generate some preference to compute to a cost lower than suggested price if the link is priced at standard price".in {
       var lowerCount = 0
       for (i <- 0 until 100) {
         DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.foreach {
           case (linkClass, flightPreferences) => flightPreferences.foreach { flightPreference =>
             val cost = flightPreference.computeCost(airline1Link, linkClass)
             if (cost <= airline1Link.price(linkClass)) {
               lowerCount += 1
             }
           }
         }
       }
       assert(lowerCount > 0)
    }
    "not generate preference to compute to a cost lower than standard price with extremely overpriced ticket even with perfect link/airline".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(AirlineAppeal.MAX_LOYALTY)))

      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
      val extremePrice = suggestedPrice * 5
      val extremeLink = airline1Link.copy(price = extremePrice, rawQuality = Link.MAX_QUALITY)
      for (i <- 0 until 100) {
        DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.foreach {
          case (linkClass, flightPreferences) => flightPreferences.foreach { flightPreference =>
            val cost = flightPreference.computeCost(extremeLink, linkClass)
            assert(cost >= suggestedPrice(linkClass)) //the cost should not be reduced to lower than the standard price no matter how good it is
          }
         
        }
      }
    }
    "generate some preference to compute to a cost lower than standard price with overpriced ticket with perfect link/airline".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal](testAirline1.id -> AirlineAppeal(AirlineAppeal.MAX_LOYALTY)))
      val suggestedPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)
      val highPrice = suggestedPrice * 1.5
      val highPriceLink = airline1Link.copy(price = highPrice)
      highPriceLink.setQuality(Link.MAX_QUALITY)
      
      
      for (i <- 0 until 100) {
        val lowerCostOption = DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport).pool.find {
           case (linkClass, flightPreferences) => flightPreferences.find{ flightPreference =>
             val cost = flightPreference.computeCost(highPriceLink, linkClass)
             cost < suggestedPrice(linkClass)
           }.isDefined
        }
        
        assert(lowerCostOption.isDefined)
      }
    }
    
    "generate preference that compute to similar cost if price is balanced with quality/loyalty difference (high income country)".in { 
      val clonedFromAirport = fromAirport.copy(baseIncome = Country.HIGH_INCOME_THRESHOLD, basePopulation = 1)
      
      clonedFromAirport.initAirlineAppeals(scala.collection.immutable.Map(testAirline1.id -> AirlineAppeal(loyalty = 50),
                                               testAirline2.id -> AirlineAppeal(loyalty = 0)))
      
      val link1 = airline1Link.copy(price = defaultPrice * 1.4, frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(price = defaultPrice, frequency = 1)
      link1.setQuality(70)
      link2.setQuality(40)
      val pool = DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport)
      
      var airline1Picked = 0
      var airline2Picked = 0
      
      for (i <- 0 until 100000) {
        val preference = pool.draw(ECONOMY, clonedFromAirport, link1.to)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 0.8)
      assert(ratio < 1.2)
    }
    
    "generate preference that compute to similar cost if price is balanced with quality/loyalty difference (low income country)".in { 
      val clonedFromAirport = fromAirport.copy(baseIncome = Country.HIGH_INCOME_THRESHOLD / 10, basePopulation = 1)
      
      clonedFromAirport.initAirlineAppeals(scala.collection.immutable.Map(testAirline1.id -> AirlineAppeal(loyalty = 50),
                                               testAirline2.id -> AirlineAppeal(loyalty = 0)))
      
      val link1 = airline1Link.copy(price = defaultPrice * 1.2, frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(price = defaultPrice, frequency = 1)
      link1.setQuality(80)    
      link2.setQuality(40)
      val pool = DemandGenerator.getFlightPreferencePoolOnAirport(clonedFromAirport)
      
      var airline1Picked = 0
      var airline2Picked = 0
      
      for (i <- 0 until 100000) {
        val preference = pool.draw(ECONOMY, clonedFromAirport, link1.to)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      
      val ratio = airline1Picked.toDouble / airline2Picked 
      assert(ratio > 0.8)
      assert(ratio < 1.2)
    }

    "generate preference that compute to some overlapping but differentiating cost if frequency has considerable difference)".in {
      val link1 = airline1Link.copy(price = defaultPrice, frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(price = defaultPrice, frequency = 5)
      link1.setQuality(50)
      link2.setQuality(50)
      val pool = DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport)

      var airline1Picked = 0
      var airline2Picked = 0

      for (i <- 0 until 100000) {
        val preference = pool.draw(ECONOMY, fromAirport, link1.to)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }

      val ratio = airline1Picked.toDouble / airline2Picked
      assert(ratio > 5)
      assert(ratio < 10)
    }

    "generate preference that compute to some overlapping but differentiating cost if frequency/quality has huge difference but make up in ticket price)".in {
      val link1 = airline1Link.copy(price = defaultPrice, frequency = Link.HIGH_FREQUENCY_THRESHOLD)
      val link2 = airline2Link.copy(price = defaultPrice * 0.7, frequency = 1)
      link1.setQuality(70)
      link2.setQuality(20)
      val pool = DemandGenerator.getFlightPreferencePoolOnAirport(fromAirport)

      var airline1Picked = 0
      var airline2Picked = 0

      for (i <- 0 until 100000) {
        val preference = pool.draw(ECONOMY, fromAirport, link1.to)
        val link1Cost = preference.computeCost(link1, ECONOMY)
        val link2Cost = preference.computeCost(link2, ECONOMY)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }

      val ratio = airline1Picked.toDouble / airline2Picked
      assert(ratio > 1.5)
      assert(ratio < 2.5)
    }
  }
}
