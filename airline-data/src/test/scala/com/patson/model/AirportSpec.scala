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
import org.scalatest.BeforeAndAfterEach
 
class AirportSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
 
  def this() = this(ActorSystem("MySpec"))
 
  val airport : Airport = Airport("A", "", "Airport A", 0, 0, countryCode = "A", "", "", 1, 0, 0, slots = 100)
  airport.country = Some(Country(countryCode = "A", name = "Country A", airportPopulation = 1000000, income = 500000, openness = 10))
  val otherAirport : Airport = Airport("B", "", "Airport B", 0, 0, countryCode = "B", "", "", 1, 0, 0, slots = 100)
  otherAirport.country = Some(Country(countryCode = "B", name = "Country B", airportPopulation = 1000000, income = 500000, openness = 3))
  
  val highReputationLocalHqAirline = Airline("airline 1", id = 1)
  highReputationLocalHqAirline.setReputation(Airline.MAX_REPUTATION)
  highReputationLocalHqAirline.setCountryCode("A")
  val base1 = AirlineBase(highReputationLocalHqAirline, airport, countryCode = "A", scale = 1, foundedCycle = 1, headquarter = true) 
  highReputationLocalHqAirline.setBases(List[AirlineBase](base1))
  val lowReputationLocalHqAirline = Airline("airline 2", id = 2)
  lowReputationLocalHqAirline.setReputation(0)
  val base2 = AirlineBase(lowReputationLocalHqAirline, airport, countryCode = "A", scale = 1, foundedCycle = 1, headquarter = true)
  lowReputationLocalHqAirline.setBases(List[AirlineBase](base2))
  lowReputationLocalHqAirline.setCountryCode("A")
  
  val highReputationForeignHqAirline = Airline("high rep foreign", id = 3)
  highReputationForeignHqAirline.setReputation(100)
  val base3 = AirlineBase(highReputationForeignHqAirline, otherAirport, countryCode = "B", scale = 1, foundedCycle = 1, headquarter = true)
  highReputationForeignHqAirline.setBases(List[AirlineBase](base3))
  highReputationForeignHqAirline.setCountryCode("B")
  val lowReputationForeignHqAirline = Airline("low rep foreign", id = 4)
  lowReputationForeignHqAirline.setReputation(0)
  val base4 = AirlineBase(lowReputationForeignHqAirline, otherAirport, countryCode = "B", scale = 1, foundedCycle = 1, headquarter = true)
  lowReputationForeignHqAirline.setBases(List[AirlineBase](base4))
  lowReputationForeignHqAirline.setCountryCode("B")
  
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  
  override def beforeEach {
    airport.initAirlineBases(List(base1, base2))
    airport.initSlotAssignments(Map())
    airport.initAirlineAppeals(Map())
    otherAirport.initAirlineBases(List(base3, base4))
    otherAirport.initSlotAssignments(Map())
    otherAirport.initAirlineAppeals(Map())
  }
 
  
  
  "Airport.availableSlots()".must {
    "return 0 if all slots are taken".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 100))
      airport.availableSlots.shouldBe(0)
    }
    "return all slots if no slots are taken".in {
      airport.initSlotAssignments(Map())
      airport.availableSlots.shouldBe(100)
    }
    "return correct slots if some slots are taken".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 10, lowReputationLocalHqAirline.id -> 20))
      airport.availableSlots.shouldBe(70)
    }
  }
  "Airport.getAirlineSlotAssignment()".must {
    "return correct slots assigned to corresponding airline".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 50, lowReputationLocalHqAirline.id -> 30))
      airport.getAirlineSlotAssignment(highReputationLocalHqAirline.id).shouldBe(50)
      airport.getAirlineSlotAssignment(lowReputationLocalHqAirline.id).shouldBe(30)
    }
    "return no slots if none assigned".in {
      airport.initSlotAssignments(Map())
      airport.getAirlineSlotAssignment(highReputationLocalHqAirline.id).shouldBe(0)
      airport.getAirlineSlotAssignment(lowReputationLocalHqAirline.id).shouldBe(0)
    }
  }
  "Airport.getMaxSlotAssignment()".must {
    "no slots changes if all slots are taken".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 100))
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(100)
      airport.getMaxSlotAssignment(lowReputationLocalHqAirline).shouldBe(0)
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe(0)
      airport.getMaxSlotAssignment(lowReputationForeignHqAirline).shouldBe(0)
    }
    "no slots changes if the airline is no longer loved :<".in {
      airport.initAirlineAppeals(Map())
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 50))
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(50)
    }
    "get more slots based on reputation".in {
      airport.initSlotAssignments(Map())
      airport.initAirlineAppeals(Map())
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(airport.getMaxSlotAssignment(lowReputationLocalHqAirline)) //doesn't matter for local HQ airlines with low loyalty, they both get 10 at the minimum
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe( > (airport.getMaxSlotAssignment(lowReputationForeignHqAirline))) //doesn't matter for local HQ airlines with low loyalty, they both get 10 at the minimum
    }
    "get some slots based on HQ". in {
      airport.initSlotAssignments(Map())
      airport.initAirlineAppeals(Map())
      assert(airport.getMaxSlotAssignment(lowReputationLocalHqAirline) == Airport.HQ_GUARANTEED_SLOTS)
    }
    "get more slots based on loyalty".in {
      airport.initSlotAssignments(Map())
      airport.initAirlineAppeals(Map())
      airport.setAirlineLoyalty(highReputationForeignHqAirline.id, 100)
      airport.setAirlineLoyalty(lowReputationForeignHqAirline.id, 20)
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe( > (airport.getMaxSlotAssignment(lowReputationForeignHqAirline)))
      assert(airport.getMaxSlotAssignment(highReputationForeignHqAirline) == Airport.NON_BASE_MAX_SLOT)
    }
    "get correct slot when in reserved range yet with available slots".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 50, lowReputationLocalHqAirline.id -> 45))
      airport.initAirlineAppeals(Map())
      airport.setAirlineLoyalty(highReputationLocalHqAirline.id, 100)
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(50) //no change, into reserved range
      airport.getMaxSlotAssignment(lowReputationLocalHqAirline).shouldBe(45) //no change, into reserved range
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe(5) //new airline in open country
      airport.getMaxSlotAssignment(lowReputationForeignHqAirline).shouldBe(5) //new airline in open country
    }
    "get correct slot when close to reserved range".in {
      airport.initSlotAssignments(Map(highReputationLocalHqAirline.id -> 5, lowReputationLocalHqAirline.id -> 70))
      airport.initAirlineAppeals(Map())
      airport.setAirlineLoyalty(highReputationLocalHqAirline.id, 100)
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(10) //can only give 5 more  
      airport.getMaxSlotAssignment(lowReputationLocalHqAirline).shouldBe(70) //keep what u had...
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe(5) //new airline in open country
      airport.getMaxSlotAssignment(lowReputationForeignHqAirline).shouldBe(5) //new airline in open country
    }
    "get max slot limited by base type".in {
      airport.initSlotAssignments(Map())
      airport.initAirlineAppeals(Map())
      airport.setAirlineLoyalty(highReputationLocalHqAirline.id, 100)
      airport.setAirlineLoyalty(highReputationForeignHqAirline.id, 100)
      airport.getMaxSlotAssignment(highReputationLocalHqAirline).shouldBe(80)  // give up to reserved range
      airport.getMaxSlotAssignment(highReputationForeignHqAirline).shouldBe(Airport.NON_BASE_MAX_SLOT)
    }
    "get slot base on openness".in {
      assert(airport.getMaxSlotAssignment(highReputationForeignHqAirline) > otherAirport.getMaxSlotAssignment(highReputationLocalHqAirline))
      assert(airport.getMaxSlotAssignment(lowReputationForeignHqAirline) > otherAirport.getMaxSlotAssignment(lowReputationLocalHqAirline))
    }
  }
}
