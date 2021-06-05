package com.patson

import com.patson.model._
import org.scalatest.{Matchers, WordSpecLike}
 
class AirlineSimulationSpec extends WordSpecLike with Matchers {
  "getServiceFunding".must {
    "compute service funding that increase exponentially with quality target".in {
//       assert(AirlineSimulation.getTargetQuality(0, 1000) == 0) //X0 capacity funding
//       assert(AirlineSimulation.getTargetQuality(5000, 1000) > 0) //X5 capacity funding
//       assert(AirlineSimulation.getTargetQuality(50000, 1000) == 100)
//       assert(AirlineSimulation.getTargetQuality(200000, 1000) == 100) //X200 capacity funding
//       assert(AirlineSimulation.getTargetQuality(300000, 1000) == 100) //X300 hit max

      //assert that even with 0 capacity, service funding would still cost something
      assert(AirlineSimulation.getServiceFunding(0, 0) == 0)
      assert(AirlineSimulation.getServiceFunding(50, 0) == 13101)
      assert(AirlineSimulation.getServiceFunding(100, 0) == 74115) //double quality should be power(2,2.5) of cost

      assert(AirlineSimulation.getServiceFunding(0, 2000 * 2000) == 0)
      assert(AirlineSimulation.getServiceFunding(50, 2000 * 2000) == 52407)
      assert(AirlineSimulation.getServiceFunding(100, 2000 * 2000) == 296463) //double quality should be power(2,2.5) of cost

      //AC size
      assert(AirlineSimulation.getServiceFunding(0, 4000000000L) == 0)
      assert(AirlineSimulation.getServiceFunding(50, 4000000000L) == 52407843L)
      assert(AirlineSimulation.getServiceFunding(100, 4000000000L) == 296463530L) //double quality should be power(2,2.5) of cost
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
       assert(AirlineSimulation.getNewQuality(Airline.MAX_SERVICE_QUALITY, 0) == Airline.MAX_SERVICE_QUALITY - AirlineSimulation.MAX_SERVICE_QUALITY_DECREMENT) //get full decrement
       assert(AirlineSimulation.getNewQuality(0, 0) == 0) //no decrement
       assert(AirlineSimulation.getNewQuality(50, 0) > 50 - AirlineSimulation.MAX_SERVICE_QUALITY_DECREMENT) //slow down
       assert(AirlineSimulation.getNewQuality(50, 0) < 50) //but should still have decrement
       assert(AirlineSimulation.getNewQuality(1, 0) == 0)

    }
  }
  
 
}
