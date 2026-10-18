package com.patson.model

import scala.collection.mutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.ImplicitSender
import org.apache.pekko.testkit.TestKit
import com.patson.Util
import com.patson.model.airplane.Model
 
class ComputationSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  import FlightType._
  val smallAirplaneModel = Model.modelByName("Cessna Caravan")
  val mediumAirplaneModel = Model.modelByName("Bombardier CS100")
  val largeAirplaneModel = Model.modelByName("Boeing 787-8 Dreamliner")
  val extraLargeAirplaneModel = Model.modelByName("Boeing 777-300")
  
  "calculateDuration".must {
    "returns longer duration for slower speed craft".in {
      val smallAirplaneDuration = Computation.calculateDuration(smallAirplaneModel, 500)
      val largeAirplaneDuration = Computation.calculateDuration(largeAirplaneModel, 500)
      smallAirplaneDuration.shouldBe( > (largeAirplaneDuration))
    }
    "slower average speed for shorter distance (due to takeoff)".in {
      val shortAverageSpeed = 500 / Computation.calculateDuration(smallAirplaneModel, 500)
      val longAverageSpeed = 1000 / Computation.calculateDuration(largeAirplaneModel, 1000)
      longAverageSpeed.shouldBe( > (shortAverageSpeed))
    }
  }
  "calculateMaxFrequency".must {
    "returns higher frequency for smaller craft on very short route".in {
      val small = Computation.calculateMaxFrequency(smallAirplaneModel, 500)
      val medium = Computation.calculateMaxFrequency(mediumAirplaneModel, 500)
      val large = Computation.calculateMaxFrequency(largeAirplaneModel, 500)
      val extraLarge = Computation.calculateMaxFrequency(extraLargeAirplaneModel, 500)
      small.shouldBe( > (medium))
      medium.shouldBe( > (large))
      large.shouldBe( > (extraLarge))
    }
    "returns highest frequency for medium craft on short route".in {
      val small = Computation.calculateMaxFrequency(smallAirplaneModel, 2000)
      val medium = Computation.calculateMaxFrequency(mediumAirplaneModel, 2000)
      val large = Computation.calculateMaxFrequency(largeAirplaneModel, 2000)
      val extraLarge = Computation.calculateMaxFrequency(extraLargeAirplaneModel, 2000)
      medium.shouldBe( > (small))
      medium.shouldBe( > (large))
      large.shouldBe( > (extraLarge))
    }
    "returns highest frequency for large+ craft on long+ route".in {
      val small = Computation.calculateMaxFrequency(smallAirplaneModel, 10000)
      val medium = Computation.calculateMaxFrequency(mediumAirplaneModel, 10000)
      val large = Computation.calculateMaxFrequency(largeAirplaneModel, 10000)
      val extraLarge = Computation.calculateMaxFrequency(extraLargeAirplaneModel, 10000)
      small.shouldBe(0)
      medium.shouldBe(0)
      large.shouldBe( > (0))
      extraLarge.shouldBe( > (0))
      println(large)
      println(extraLarge)
    }

  }

  "getAirportsWithinRange".must {
    "return airports within range across countries, excluding the principal airport".in {
      //~111.19 km per degree at the equator
      val principal = Airport.fromId(1).copy(latitude = 0, longitude = 0, countryCode = "C1")
      val near = Airport.fromId(2).copy(latitude = 0, longitude = 0.5, countryCode = "C1") //~56km
      val nearOtherCountry = Airport.fromId(3).copy(latitude = 0.8, longitude = 0, countryCode = "C2") //~89km
      val far = Airport.fromId(4).copy(latitude = 0, longitude = 1, countryCode = "C1") //~111km
      val farLatitude = Airport.fromId(5).copy(latitude = 5, longitude = 0, countryCode = "C1")
      val farSouthLatitude = Airport.fromId(6).copy(latitude = -5, longitude = 0, countryCode = "C1")
      val nearSouth = Airport.fromId(7).copy(latitude = -0.85, longitude = 0, countryCode = "C1") //~95km
      val farSouth = Airport.fromId(8).copy(latitude = -0.95, longitude = 0, countryCode = "C1") //~106km, within latitude band but out of range

      val candidatesByLatitude = List(principal, near, nearOtherCountry, far, farLatitude, farSouthLatitude, nearSouth, farSouth).sortBy(_.latitude).toIndexedSeq
      val result = Computation.getAirportsWithinRange(principal, 100, candidatesByLatitude)
      result.map(_.id).toSet.shouldBe(Set(near.id, nearOtherCountry.id, nearSouth.id))
    }
  }
}
