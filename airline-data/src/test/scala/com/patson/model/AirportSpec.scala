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
 
  val airport : Airport = Airport("A", "", "Airport A", 0, 0, countryCode = "A", "", "", 1, 0, population = 100, slots = 100)
  airport.country = Some(Country(countryCode = "A", name = "Country A", airportPopulation = 1000000, income = 500000, openness = 10))
  val otherAirport : Airport = Airport("B", "", "Airport B", 0, 0, countryCode = "B", "", "", 1, 0, 0, slots = 100)
  otherAirport.country = Some(Country(countryCode = "B", name = "Country B", airportPopulation = 1000000, income = 500000, openness = 3))
  
  val highReputationLocalHqAirline = Airline("airline 1", id = 1)
  highReputationLocalHqAirline.setReputation(AirlineGrade.CONTINENTAL.reputationCeiling)
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



  "computeLoyaltyByLoyalist".must {
    "give no loyalty if loyalist is empty".in {
      val airport = this.airport.copy()
      assert(airport.computeLoyaltyByLoyalist(List.empty).isEmpty)
    }
    "give 0 loyalty if loyalist is 0".in {
      val airport = this.airport.copy()
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), 0)))(1) == 0.0)
    }
    "give 100 loyalty if loyalist is same as pop".in {
      val airport = this.airport.copy()
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), airport.population.toInt)))(1) == 100.0)
    }
    "give x loyalty if loyalist is 0.5 * pop, which 70 < x < 80".in {
      val airport = this.airport.copy()
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), airport.population.toInt / 2)))(1) > 70.0)
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), airport.population.toInt / 2)))(1) < 80.0)
    }
    "give x loyalty if loyalist is 0.2 * pop, which 40 < x < 50".in {
      val airport = this.airport.copy()
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), airport.population.toInt / 5)))(1) > 40.0)
      assert(airport.computeLoyaltyByLoyalist(List(Loyalist(airport, Airline.fromId(1), airport.population.toInt / 5)))(1) < 50.0)
    }

  }
  
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  
  override def beforeEach {
    airport.initAirlineBases(List(base1, base2))
    //airport.initSlotAssignments(Map())
    airport.initAirlineAppeals(Map())
    otherAirport.initAirlineBases(List(base3, base4))
    //otherAirport.initSlotAssignments(Map())
    otherAirport.initAirlineAppeals(Map())
  }
}
