package com.patson.model

import org.scalatest.{Matchers, WordSpecLike}

class HighSpeedRailSpec extends WordSpecLike with Matchers {
  def airportOfSize(size : Int) = Airport.fromId(1).copy(size = size)

  "carCount".must {
    "be between 3 and 8 based on airport size".in {
      HighSpeedRail.carCount(airportOfSize(1)).shouldBe(3)
      HighSpeedRail.carCount(airportOfSize(3)).shouldBe(3)
      HighSpeedRail.carCount(airportOfSize(5)).shouldBe(5)
      HighSpeedRail.carCount(airportOfSize(8)).shouldBe(8)
      HighSpeedRail.carCount(airportOfSize(10)).shouldBe(8)
    }
  }

  "capacity".must {
    "have 400,000 max capacity with 8 cars".in {
      HighSpeedRail.maxCapacity(airportOfSize(8)).shouldBe(400000)
    }
    "convert between cars and capacity".in {
      val cars : Map[LinkClass, Int] = Map(ECONOMY -> 5, BUSINESS -> 2, FIRST -> 1)
      val capacity = HighSpeedRail.capacityByCars(cars)
      capacity(ECONOMY).shouldBe(250000)
      capacity(BUSINESS).shouldBe(40000)
      capacity(FIRST).shouldBe(8333)
      HighSpeedRail.carsByCapacity(capacity).shouldBe(cars)
    }
  }

  "buildCost".must {
    "be about 0.5B per km with 8 cars".in {
      HighSpeedRail.buildCost(1, airportOfSize(8)).shouldBe(500000000L)
      HighSpeedRail.buildCost(100, airportOfSize(8)).shouldBe(50000000000L)
    }
  }

  "carsRejection".must {
    "reject invalid car assignment".in {
      HighSpeedRail.carsRejection(Map(ECONOMY -> -1, BUSINESS -> 2, FIRST -> 0), 5).isDefined.shouldBe(true)
      HighSpeedRail.carsRejection(Map(ECONOMY -> 0, BUSINESS -> 0, FIRST -> 0), 5).isDefined.shouldBe(true)
      HighSpeedRail.carsRejection(Map(ECONOMY -> 4, BUSINESS -> 1, FIRST -> 1), 5).isDefined.shouldBe(true)
    }
    "accept valid car assignment".in {
      HighSpeedRail.carsRejection(Map(ECONOMY -> 3, BUSINESS -> 1, FIRST -> 1), 5).shouldBe(None)
      HighSpeedRail.carsRejection(Map(ECONOMY -> 1, BUSINESS -> 0, FIRST -> 0), 5).shouldBe(None)
    }
  }

  "reconfigureCooldown".must {
    "count down in cycles from the last update cycle".in {
      HighSpeedRail.reconfigureCooldown(100, 100).shouldBe(HighSpeedRail.RECONFIGURE_COOLDOWN)
      HighSpeedRail.reconfigureCooldown(100, 101).shouldBe(HighSpeedRail.RECONFIGURE_COOLDOWN - 1)
      HighSpeedRail.reconfigureCooldown(100, 100 + HighSpeedRail.RECONFIGURE_COOLDOWN).shouldBe(0)
      HighSpeedRail.reconfigureCooldown(100, 200 + HighSpeedRail.RECONFIGURE_COOLDOWN).shouldBe(0)
    }
  }

  "construction".must {
    val rail = HighSpeedRail(airportOfSize(8), Airport.fromId(2), Airline.fromId(1), 50, LinkClassValues.getInstance(100, 0, 0), HighSpeedRail.WEEKLY_FREQUENCY, fundedCycle = 100)

    "take 5 years (5 * 52 cycles) to complete".in {
      rail.completionCycle.shouldBe(100 + 5 * 52)
    }
    "not be completed before the completion cycle".in {
      rail.isCompleted(100).shouldBe(false)
      rail.isCompleted(rail.completionCycle - 1).shouldBe(false)
      rail.remainingConstructionCycles(100).shouldBe(5 * 52)
      rail.remainingConstructionCycles(rail.completionCycle - 1).shouldBe(1)
    }
    "be completed on and after the completion cycle".in {
      rail.isCompleted(rail.completionCycle).shouldBe(true)
      rail.isCompleted(rail.completionCycle + 1).shouldBe(true)
      rail.remainingConstructionCycles(rail.completionCycle).shouldBe(0)
      rail.remainingConstructionCycles(rail.completionCycle + 100).shouldBe(0)
    }
  }
}
