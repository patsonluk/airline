package com.patson.model

import scala.collection.mutable.Map

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
 
class FlightPreferenceSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1")
  val testAirline2 = Airline("airline 2")
  val fromAirport = Airport("", "", "From Airport", 0, 0, "", "", 1, 0, 0, 0, 0)
  val toAirport = Airport("", "", "To Airport", 0, 0, "", "", 1, 0, 0, 0, 0)
  val airline1Link = Link(fromAirport, toAirport, testAirline1, 100, 10000, 10000, 0, 600, 1)
  val airline2Link = Link(fromAirport, toAirport, testAirline2, 100, 10000, 10000, 0, 600, 1)
  
  
  
  "An AppealPreference".must {
    "generate similar cost if price and distance is the same, and small differece in loyalty".in {
      fromAirport.setAirlineLoyalty(testAirline1, 30)
      fromAirport.setAirlineLoyalty(testAirline2, 32)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
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
      fromAirport.setAirlineLoyalty(testAirline1, 50)
      fromAirport.setAirlineLoyalty(testAirline2, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 1000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1005, 10000, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
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
      fromAirport.setAirlineLoyalty(testAirline1, 50)
      fromAirport.setAirlineLoyalty(testAirline2, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 1000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1000, 10100, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (0.9))         //should be around 50 50
      ratio.shouldBe( <= (1.1))
    }
    "generate differentiating but overlapping cost if everything is the same, but loyal at big difference".in {
      fromAirport.setAirlineLoyalty(testAirline1, 10)
      fromAirport.setAirlineLoyalty(testAirline2, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 1000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1000, 10000, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.5)) //significantly more people should pick airline 2
      ratio.shouldBe( > (0.1)) //yet some will still pick airline 1
    }
    "generate almost no overlapping cost if everything is the same, but loyal at min vs max".in {
      fromAirport.setAirlineLoyalty(testAirline1, 0)
      fromAirport.setAirlineLoyalty(testAirline2, 100)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 1000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1000, 10000, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( <= (0.1)) //almost everyone should go for airline 2
      ratio.shouldBe( > (0.0)) //yet some will still pick airline 1 due to randomness 
    }
    "generate differentiating but overlapping cost if everything is the same, but price at big difference".in {
      fromAirport.setAirlineLoyalty(testAirline1, 50)
      fromAirport.setAirlineLoyalty(testAirline2, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 1000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1500, 10000, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      val ratio = airline1Picked.toDouble / airline2Picked 
      ratio.shouldBe( >= (2.0)) //significantly more people should pick airline 1
      ratio.shouldBe( < (10.0)) //yet some will still pick airline 2
    }
    "generate no overlapping cost if everything is the same, but price is at huge difference".in {
      fromAirport.setAirlineLoyalty(testAirline1, 50)
      fromAirport.setAirlineLoyalty(testAirline2, 50)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, 5000, 10000, 10000, 0, 600, 1)
      val airline2Link = Link(fromAirport, toAirport, testAirline2, 1000, 10000, 10000, 0, 600, 1)
      var airline1Picked = 0
      var airline2Picked = 0
      for (i <- 0 until 100000) {
        val preference = AppealPreference(fromAirport.airlineAppeals.toMap, 0)
        val link1Cost = preference.computeCost(airline1Link)
        val link2Cost = preference.computeCost(airline2Link)
        if (link1Cost < link2Cost) airline1Picked += 1  else airline2Picked += 1
      }
      airline1Picked.shouldBe(0) //noone should pick airline 1
    }
  }
}
