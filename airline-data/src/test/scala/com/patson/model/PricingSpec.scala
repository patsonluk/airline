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
  //200 km = 150
  //1000 km = 150 + 100 = 250  (800 * 0.125) 
  //2000 km = 250 + 100 = 350  (1000 * 0.1)
  //10000 km = 350 + 400 = 750 (8000 * 0.05)
  "computeStandardPrice".must {
//    "generate expected prices at the bucket (domestic)".in {
//      Pricing.computeStandardPrice(200, SHORT_HAUL_DOMESTIC, ECONOMY).shouldBe(150)
//      Pricing.computeStandardPrice(1000, SHORT_HAUL_DOMESTIC, ECONOMY).shouldBe(250)
//      Pricing.computeStandardPrice(1500, SHORT_HAUL_DOMESTIC, ECONOMY).shouldBe(300)
//      Pricing.computeStandardPrice(2000, LONG_HAUL_DOMESTIC, ECONOMY).shouldBe(350)
//      Pricing.computeStandardPrice(6000, LONG_HAUL_DOMESTIC, ECONOMY).shouldBe(550)
//      Pricing.computeStandardPrice(10000, LONG_HAUL_DOMESTIC, ECONOMY).shouldBe(750)
//      Pricing.computeStandardPrice(14000, LONG_HAUL_DOMESTIC, ECONOMY).shouldBe(950)
//    }
//    "generate expected prices at the bucket (international)".in {
//      Pricing.computeStandardPrice(200, SHORT_HAUL_INTERNATIONAL, ECONOMY).shouldBe((150 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(1000, SHORT_HAUL_INTERNATIONAL, ECONOMY).shouldBe((250 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(1500, SHORT_HAUL_INTERNATIONAL, ECONOMY).shouldBe((300 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(1500, SHORT_HAUL_INTERCONTINENTAL, ECONOMY).shouldBe((300 * Pricing.INTERCONTINENTAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(2000, LONG_HAUL_INTERNATIONAL, ECONOMY).shouldBe((350 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(6000, LONG_HAUL_INTERNATIONAL, ECONOMY).shouldBe((550 * Pricing.INTERNATIONAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(6000, LONG_HAUL_INTERCONTINENTAL, ECONOMY).shouldBe((550 * Pricing.INTERCONTINENTAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(10000, ULTRA_LONG_HAUL_INTERCONTINENTAL, ECONOMY).shouldBe((750 * Pricing.INTERCONTINENTAL_PRICE_MULTIPLIER).toInt)
//      Pricing.computeStandardPrice(14000, ULTRA_LONG_HAUL_INTERCONTINENTAL, ECONOMY).shouldBe((950 * Pricing.INTERCONTINENTAL_PRICE_MULTIPLIER).toInt)
//    }
  }
}
