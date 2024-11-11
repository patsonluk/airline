package com.patson.model

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import com.patson.Util
import com.patson.model.airplane.{Airplane, AirplaneConfiguration, LinkAssignment, Model}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.collection.immutable.Map

class LinkSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
 
  def this() = this(ActorSystem("MySpec"))

  val testAirline1 = Airline("airline 1", id = 1)
  val fromAirport = Airport("", "", "From Airport", 0, 0, "", "", "", 1, baseIncome = 40000, basePopulation = 1, 0, 0)
  val toAirport = Airport("", "", "To Airport", 0, 180, "", "", "", 1, baseIncome = 40000, basePopulation = 1, 0, 0)
  val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  val defaultPrice = Pricing.computeStandardPriceForAllClass(distance, fromAirport, toAirport)

  val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
  val model = Model.modelByName("Boeing 737 MAX 9")




  "frequencyByClass".must {
    "compute correct frequency".in {

      val config1 = AirplaneConfiguration(100, 0, 0, testAirline1, model, false)
      val config2 = AirplaneConfiguration(50, 25, 0, testAirline1, model, false)
      val config3 = AirplaneConfiguration(50, 10, 5, testAirline1, model, false)
      val airline1Link = Link(fromAirport, toAirport, testAirline1, defaultPrice, distance = distance, LinkClassValues.getInstance(200, 35, 5) * 10, rawQuality = 0, 600, frequency = 30, flightType)

      airline1Link.setAssignedAirplanes(
        scala.collection.immutable.Map(
          Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config1) -> LinkAssignment(10, 6000)
        , Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config2) -> LinkAssignment(10, 6000)
        , Airplane(model, testAirline1, 0, purchasedCycle = 0, 100, 0, 0, configuration = config3) -> LinkAssignment(10, 6000)))

      assert(airline1Link.frequencyByClass(ECONOMY) == 30)
      assert(airline1Link.frequencyByClass(BUSINESS) == 20)
      assert(airline1Link.frequencyByClass(FIRST) == 10)
    }
  }
}
