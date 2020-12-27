package com.patson.model

import scala.collection.mutable.{ListBuffer, Map}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.Util

import scala.util.Random
 
class UtilSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  "bell random".must {
    "generate numbers within the boundary".in {
      for (i <- 0 until 10000) {
        val generatedValue : Double = Util.getBellRandom(0, 5)
        generatedValue.shouldBe( >= (-5.toDouble))
        generatedValue.shouldBe( <= (5.toDouble))
      }
    }
    "generate numbers evenly on both sides".in {
      var smallerCount = 0
      var biggerCount = 0
      for (i <- 0 until 10000) {
        val generatedValue : Double = Util.getBellRandom(1)
        if (generatedValue < 1) {
          smallerCount += 1
        } else {
          biggerCount += 1
        }
      }
      val ratio = smallerCount.toDouble / biggerCount
        
      ratio.shouldBe( >= (0.9.toDouble))
      ratio.shouldBe( <= (1.1.toDouble))
    }
    "assemble a bell curve".in {
      val scaleFactor = 100
      val boundaryFromCenter = 3 * scaleFactor //(3 as we know the cutoff in the calculation)
      val twoStandardDeviation = 2 * scaleFactor 
      val center = 100
      val twoStandardDeviationMin = 100 - twoStandardDeviation
      val twoStandardDeviationMax = 100 + twoStandardDeviation
      val runCount = 1000000
      
      var outsiderCount = 0
      for (i <- 0 until runCount) {
        val generatedValue : Double = Util.getBellRandom(center, boundaryFromCenter)
        if (generatedValue > twoStandardDeviationMax || generatedValue < twoStandardDeviationMin) {
          outsiderCount += 1
        }
      }
      val outsiderRatio = outsiderCount.toDouble / runCount //statistically it's around 2.1% that is outside 2 standard deviation on each side, so it should be roughly 2 * 2.1%
        
      outsiderRatio.shouldBe( >= (0.040.toDouble))
      outsiderRatio.shouldBe( <= (0.044.toDouble))
    }
    "deterministic if seed is provided".in {
      val runCount = 1000
      val center = 100
      val boundaryFromCenter = 5
      val generatedNumbers = ListBuffer[Double]()
      val random1= new Random(0)
      for (i <- 0 until runCount) {
        val generatedValue : Double = Util.getBellRandom(center, boundaryFromCenter, Some(random1.nextInt()))
        generatedNumbers.append(generatedValue)
      }
      val random2= new Random(0)
      for (i <- 0 until runCount) {
        val generatedValue : Double = Util.getBellRandom(center, boundaryFromCenter, Some(random2.nextInt()))
        assert(generatedValue == generatedNumbers(i))
      }
    }
  }
}
