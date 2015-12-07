package com.patson.model

import scala.collection.immutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
 
class AirportSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  val testAirline1 = Airline("airline 1", 1)
  val testAirline2 = Airline("airline 2", 2)
  val airport = Airport("", "", "Airport", 0, 0, "", "", 1, 0, 0, slots = 100)
  
  
  "Airport.availableSlots()".must {
    "return 0 if all slots are taken".in {
      airport.initSlotAssignments(Map(testAirline1.id -> 100))
      airport.availableSlots.shouldBe(0)
    }
    "return all slots if no slots are taken".in {
      airport.initSlotAssignments(Map())
      airport.availableSlots.shouldBe(100)
    }
    "return correct slots if some slots are taken".in {
      airport.initSlotAssignments(Map(testAirline1.id -> 10, testAirline2.id -> 20))
      airport.availableSlots.shouldBe(70)
    }
  }
  "Airport.getAirlineSlotAssignment()".must {
    "return correct slots assigned to corresponding airline".in {
      airport.initSlotAssignments(Map(testAirline1.id -> 50, testAirline2.id -> 30))
      airport.getAirlineSlotAssignment(testAirline1.id).shouldBe(50)
      airport.getAirlineSlotAssignment(testAirline2.id).shouldBe(30)
    }
    "return no slots if none assigned".in {
      airport.initSlotAssignments(Map())
      airport.getAirlineSlotAssignment(testAirline1.id).shouldBe(0)
      airport.getAirlineSlotAssignment(testAirline2.id).shouldBe(0)
    }
  }
  "Airport.getMaxSlotAssignment()".must {
    "no slots changes if all slots are taken".in {
      airport.initSlotAssignments(Map(testAirline1.id -> 100))
      airport.getMaxSlotAssignment(testAirline1.id).shouldBe(100)
      airport.getMaxSlotAssignment(testAirline2.id).shouldBe(0)
    }
    "no slots changes if the airline is no longer loved :<".in {
      val clonedAirport = airport.copy()
      clonedAirport.initAirlineAppeals(Map())
      clonedAirport.initSlotAssignments(Map(testAirline1.id -> 50))
      clonedAirport.getMaxSlotAssignment(testAirline1.id).shouldBe(50)
    }
    "get more slots based on awareness".in {
      val clonedAirport = airport.copy()
      clonedAirport.initSlotAssignments(Map())
      clonedAirport.initAirlineAppeals(Map())
      clonedAirport.setAirlineAwareness(testAirline1.id, 100)
      clonedAirport.setAirlineAwareness(testAirline2.id, 20)
      clonedAirport.getMaxSlotAssignment(testAirline1.id).shouldBe( > (clonedAirport.getMaxSlotAssignment(testAirline2.id)))
    }
    "get more slots based on loyalty".in {
      val clonedAirport = airport.copy()
      clonedAirport.initSlotAssignments(Map())
      clonedAirport.initAirlineAppeals(Map())
      clonedAirport.setAirlineLoyalty(testAirline1.id, 100)
      clonedAirport.setAirlineLoyalty(testAirline2.id, 20)
      clonedAirport.getMaxSlotAssignment(testAirline1.id).shouldBe( > (clonedAirport.getMaxSlotAssignment(testAirline2.id)))
    }
    "get correct slot when in reserved range yet with available slots".in {
      val clonedAirport = airport.copy()
      clonedAirport.initSlotAssignments(Map(testAirline1.id -> 50, testAirline2.id -> 45))
      clonedAirport.initAirlineAppeals(Map())
      clonedAirport.setAirlineLoyalty(testAirline1.id, 100)
      clonedAirport.getMaxSlotAssignment(testAirline1.id).shouldBe(50) //no change, into reserved range
      clonedAirport.getMaxSlotAssignment(3).shouldBe(1) //new airline
    }
    "get correct slot when close to reserved range".in {
      val clonedAirport = airport.copy()
      clonedAirport.initSlotAssignments(Map(testAirline1.id -> 40, testAirline2.id -> 40))
      clonedAirport.initAirlineAppeals(Map())
      clonedAirport.setAirlineLoyalty(testAirline1.id, 100)
      clonedAirport.getMaxSlotAssignment(testAirline1.id).shouldBe(50) //can only give 10 
      clonedAirport.getMaxSlotAssignment(3).shouldBe(1) //new airline
    }
  }
}
