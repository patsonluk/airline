package com.patson

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.patson.model._
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.patson.model.airplane._
 
class AirlineSimulationSpec extends WordSpecLike with Matchers {
  "getTargetQuality".must {
    "50x capacity to get 50 target quality, 200x capacity to get max 100 target quality".in {
       assert(AirlineSimulation.getTargetQuality(0, 1000) == 0) //X0 capacity funding
       assert(AirlineSimulation.getTargetQuality(5000, 1000) > 0) //X5 capacity funding
       assert(AirlineSimulation.getTargetQuality(50000, 1000) == 100) 
       assert(AirlineSimulation.getTargetQuality(200000, 1000) == 100) //X200 capacity funding
       assert(AirlineSimulation.getTargetQuality(300000, 1000) == 100) //X300 hit max
    }
  }
  "getNewQuality".must {
    "for increment at current quality 0, multiplier 1x; current quality 100, multiplier 0.1x".in {
       assert(AirlineSimulation.getNewQuality(0, Airline.MAX_SERVICE_QUALITY) == 0 + AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //get full increment
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY, Airline.MAX_SERVICE_QUALITY) == Airline.MAX_SERVICE_QUALITY) //no increment
       assert(AirlineSimulation.getNewQuality(50, Airline.MAX_SERVICE_QUALITY) < 50 + AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //slow down
       assert(AirlineSimulation.getNewQuality(50, Airline.MAX_SERVICE_QUALITY) > 50) //but should still have increment
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY - 1, Airline.MAX_SERVICE_QUALITY) < Airline.MAX_SERVICE_QUALITY) //slow down
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY - 1, Airline.MAX_SERVICE_QUALITY) > Airline.MAX_SERVICE_QUALITY - 1) //but should still have increment
    }
    "for decrement at current quality 0, multiplier 0.1x; current quality 100, multiplier 1x".in {
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY, 0) == Airline.MAX_SERVICE_QUALITY - AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //get full decrement
       assert(AirlineSimulation.getNewQuality(0, 0) == 0) //no decrement
       assert(AirlineSimulation.getNewQuality(50, 0) > 50 - AirlineSimulation.MAX_SERVICE_QUALITY_INCREMENT) //slow down
       assert(AirlineSimulation.getNewQuality(50, 0) < 50) //but should still have decrement
       assert(AirlineSimulation.getNewQuality(1, 0) > 0) //slow down
       assert(AirlineSimulation.getNewQuality(1, 0) < 1) //but should still have decrement
    }
  }
  
 
}
