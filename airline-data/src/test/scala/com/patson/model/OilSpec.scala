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
import scala.collection.mutable.ListBuffer
import com.patson.OilSimulation
import com.patson.model.oil.OilPrice
 
class OilSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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
  "compute oil price".must {
    "never generate price lower than min or max".in {
      val prices = ListBuffer[Double]()
      for (i <- 0 until 10000) {
        val nextPrice = OilSimulation.getNextPrice(prices.toList)
        assert(nextPrice >= OilSimulation.MIN_PRICE && nextPrice <= OilSimulation.MAX_PRICE)
        prices += nextPrice
      }
    }
    
    "generate average price close to default in long run".in {
      val prices = ListBuffer[Double]()
      for (i <- 0 until 10000) {
        val nextPrice = OilSimulation.getNextPrice(prices.toList)
        prices += nextPrice
      }
      
      val priceAverage = prices.sum / prices.length
      
      println("average " + priceAverage)
      assert(priceAverage >= OilPrice.DEFAULT_PRICE - 3)
      assert(priceAverage <= OilPrice.DEFAULT_PRICE + 3)
    }
    
    "generate price mostly in the boundary zone".in {
      val prices = ListBuffer[Double]()
      var withinBoundryCount = 0
      for (i <- 0 until 10000) {
        val nextPrice = OilSimulation.getNextPrice(prices.toList)
        prices += nextPrice
        if (nextPrice <= OilSimulation.HIGH_PRICE_THRESHOLD && nextPrice >= OilSimulation.LOW_PRICE_THRESHOLD) {
          withinBoundryCount += 1
        }
      }
      println("within range ratio : " + (withinBoundryCount.toDouble / prices.length))
      val sortedPrices = prices.sorted
      println("lowest " + sortedPrices.take(1) + " highest " + sortedPrices.takeRight(1))
      assert(withinBoundryCount.toDouble / prices.length > 0.8)
      assert(withinBoundryCount.toDouble / prices.length < 0.95) //yet some of them should be outside the boundary 
    }
    
  }
}
