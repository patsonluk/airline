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
 
class FlightPreferenceSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val defaultPrice = LinkClassValues.getInstance((1000 * ECONOMY.priceMultiplier).toInt, (1000 * BUSINESS.priceMultiplier).toInt, (1000 * FIRST.priceMultiplier).toInt)
  val defaultCapacity = LinkClassValues.getInstance(10000, 10000, 10000)
  
  val testAirline1 = Airline("airline 1", 1)
  val testAirline2 = Airline("airline 2", 2)
  val fromAirport = Airport("", "", "From Airport", 0, 0, "", "", "", 1, 0, 0, 0, 0)
  val toAirport = Airport("", "", "To Airport", 0, 0, "", "", "", 1, 0, 0, 0, 0)
  val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, rawQuality = 40, 600, 1)
  val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, rawQuality = 40, 600, 1)
  airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), testAirline1, 0, 100)))
  airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), testAirline2, 0, 100)))
  
  "An AppealPreference".must {
    "generate similar cost if price and distance is the same, and small differece in loyalty".in {
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(testAirline1.id, 30)
      fromAirport.setAirlineLoyalty(testAirline2.id, 32)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1005), 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 51, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10100, defaultCapacity, 50, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1500), 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, testAirline1, LinkClassValues.getInstance(5000), 10000, defaultCapacity, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, LinkClassValues.getInstance(1000), 10000, defaultCapacity, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
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
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 60, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 10, 600, 1)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 100)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (2.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (10.0)) //yet some will still pick airline 2
    }
    "generate almost no overlapping cost if everything is the same, but quality at huge difference (0 vs 100)".in {
      val adjustedAirline1 = testAirline1.copy()
      adjustedAirline1.setServiceQuality(100)
      val adjustedAirline2 = testAirline2.copy()
      adjustedAirline2.setServiceQuality(0)
      fromAirport.initAirlineAppeals(scala.collection.immutable.Map[Int, AirlineAppeal]())
      fromAirport.setAirlineLoyalty(adjustedAirline1.id, 50)
      fromAirport.setAirlineLoyalty(adjustedAirline2.id, 50)
      val airline1Link = Link(fromAirport, toAirport, adjustedAirline1, defaultPrice, 10000, defaultCapacity, 100, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, adjustedAirline2, defaultPrice, 10000, defaultCapacity, 0, 600, 1)
      airline2Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline1, 0, 100)))
      airline1Link.setAssignedAirplanes(List(Airplane(Model.fromId(0), adjustedAirline2, 0, 0)))
       
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (4.0)) //significantly more people should pick airline 1
      //ratio.shouldBe( < (.0)) //yet some will still pick airline 2
    }
    "generate higher cost for higher link class if the quality is the same". in {
      var economyTotalCost : Long = 0
      var businessTotalCost : Long = 0
      var firstTotalCost : Long = 0
      for (i <- 0 until 100000) {
        val economyCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, ECONOMY, 0).computeCost(airline1Link)
        val businessCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, BUSINESS, 0).computeCost(airline1Link)
        val firstCost = AppealPreference(fromAirport.getAirlineAppeals().toMap, FIRST, 0).computeCost(airline1Link)
        economyTotalCost += economyCost.toLong
        businessTotalCost += businessCost.toLong
        firstTotalCost += firstCost.toLong
      }
      
      economyTotalCost.should(be < businessTotalCost)
      businessTotalCost.should(be < firstTotalCost)
    }
    
  }
}
