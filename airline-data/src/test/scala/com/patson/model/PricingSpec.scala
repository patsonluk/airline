package com.patson.model

import scala.collection.mutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.Util
 
class PricingSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  import FlightType._
  //200 km = 100
  //1000 km = 100 + 100 = 200  (800 * 0.125) 
  //2000 km = 200 + 100 = 300  (1000 * 0.1)
  //10000 km = 300 + 400 = 700 (8000 * 0.05)
  "computeStandardPrice".must {
    "generate expected prices at the bucket (domestic)".in {
      Pricing.computeStandardPrice(200, SHORT_HAUL_DOMESTIC).shouldBe(100)
      Pricing.computeStandardPrice(1000, SHORT_HAUL_DOMESTIC).shouldBe(200)
      Pricing.computeStandardPrice(1500, SHORT_HAUL_DOMESTIC).shouldBe(250)
      Pricing.computeStandardPrice(2000, LONG_HAUL_DOMESTIC).shouldBe(300)
      Pricing.computeStandardPrice(6000, LONG_HAUL_DOMESTIC).shouldBe(500)
      Pricing.computeStandardPrice(10000, LONG_HAUL_DOMESTIC).shouldBe(700)
      Pricing.computeStandardPrice(14000, LONG_HAUL_DOMESTIC).shouldBe(900)
    }
    "generate expected prices at the bucket (international)".in {
      Pricing.computeStandardPrice(200, SHORT_HAUL_INTERNATIONAL).shouldBe((100 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(1000, SHORT_HAUL_INTERNATIONAL).shouldBe((200 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(1500, SHORT_HAUL_INTERNATIONAL).shouldBe((250 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(2000, LONG_HAUL_INTERNATIONAL).shouldBe((300 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(6000, LONG_HAUL_INTERNATIONAL).shouldBe((500 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(10000, EXTRA_LONG_HAUL_INTERNATIONAL).shouldBe((700 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
      Pricing.computeStandardPrice(14000, EXTRA_LONG_HAUL_INTERNATIONAL).shouldBe((900 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
    }
  }
}
