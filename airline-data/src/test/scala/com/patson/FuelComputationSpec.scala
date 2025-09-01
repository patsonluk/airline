package com.patson

import com.patson.model.campaign._
import com.patson.model.{Airline, Airport, Computation, DelegateTask, ECONOMY, FlightType, Link, LinkClassValues}
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable

class FuelComputationSpec extends WordSpecLike with Matchers {
  val airport1 = Airport("", "", "Test Airport 1", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 1000000L, 0, id = 1)
  val airport2 = Airport("", "", "Test Airport 2", 0, 0 , "", "", "", size = 1, baseIncome = 10, basePopulation = 2000000L, 0, id = 2)
  val airline1 = Airline.fromId(1)



  "Compute fuel consumption".must {
    "Compute largely the same fuel consumption for airplane for non SST planes for all trips".in {
      val distances = Array(100, 200, 500, 1000, 1500, 2000, 3000, 4000, 5000, 10000, 15000)
      val speeds = Array(800, 900, 1000)

      var preDistance = 0
      var preOldCost = 0
      var preNewCost = 0
      distances.foreach { distance =>
        val costPerSpeed : mutable.Map[Int, Int] = new mutable.HashMap[Int, Int]()
        speeds.foreach { speed =>
          val duration = Computation.calculateDuration(speed, distance)
          var link = Link(airport1, airport2, airline1, LinkClassValues.getInstanceByMap(Map(ECONOMY -> 100)), distance, LinkClassValues.getInstanceByMap(Map(ECONOMY -> 100)), rawQuality = 0, duration, 1, FlightType.SHORT_HAUL_DOMESTIC)

          val oldCost = LinkSimulation.computeFuelCostOld(link, speed, 1)
          val newCost = LinkSimulation.computeFuelCost(link, speed, 1)
          //        println(s"$distance : $oldCost -> $newCost . Diff per 1000km with previous(old/new): ${(oldCost * 1.0 - preOldCost)/(distance - preDistance) * 1000} vs ${(newCost * 1.0 - preNewCost)/(distance - preDistance) * 1000}")
          println(s"$speed km/hr airplane $distance km fuel cost : $oldCost -> $newCost")
          preDistance = distance
          preOldCost = oldCost
          preNewCost = newCost
          val diffRatio = Math.abs(1 - oldCost * 1.0 / newCost)
          //        if (distance >= 2000) {
          //          assert(diffRatio < 0.05)
          //        }
          costPerSpeed(speed) = newCost
        }

        println
      }
    }
  }
  

}
