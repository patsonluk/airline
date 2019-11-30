package com.patson

import org.scalatest.{Matchers, WordSpecLike}
import com.patson.LoanInterestRateSimulation._

class LoanInterestSimulationSpec extends WordSpecLike with Matchers {

  "getNextRate".must {
    "Return the default rate if there's no previous rates".in {
       val nextRate = LoanInterestRateSimulation.getNextRate(List.empty[BigDecimal])
       assert(nextRate == DEFAULT_ANNUAL_RATE)
    }
    "Always return a rate within boundary if there's only one rate".in {
      for (i <- 0 until 10000) {
        val nextRate = LoanInterestRateSimulation.getNextRate(List(DEFAULT_ANNUAL_RATE))
        assert(nextRate >= DEFAULT_ANNUAL_RATE - MAX_DELTA)
        assert(nextRate <= DEFAULT_ANNUAL_RATE + MAX_DELTA)
      }
    }
    "Always return a rate within boundary".in {
      var previousRates = List[BigDecimal]()
      for (i <- 0 until 10000) {
        val nextRate = LoanInterestRateSimulation.getNextRate(previousRates)
        assert(nextRate >= MIN_RATE)
        assert(nextRate <= MAX_RATE)

        if (previousRates.length >= PREVIOUS_RATE_ENTRIES_TO_CONSIDER) {
          previousRates = previousRates.drop(1)
        }
        previousRates = previousRates :+ nextRate
      }
    }
    "Most fall within threshold zone, and a few outside".in {
      var previousRates = List[BigDecimal]()
      var withinZoneCount = 0
      val runCount = 100000
      for (i <- 0 until runCount) {
        val nextRate = LoanInterestRateSimulation.getNextRate(previousRates)
        if (nextRate >= LOW_RATE_THRESHOLD && nextRate <= HIGH_RATE_THRESHOLD) { //then within threshold
          withinZoneCount += 1
        }

        if (previousRates.length >= PREVIOUS_RATE_ENTRIES_TO_CONSIDER) {
          previousRates = previousRates.drop(1)
        }
        previousRates = previousRates :+ nextRate
      }

      assert(withinZoneCount.toDouble / runCount >= 0.80) //80% + of the time should be within zone
      assert(withinZoneCount.toDouble / runCount <= 0.95) //but at least 5% of the time should be outside zone

      println("ratio: " + withinZoneCount.toDouble / runCount)
    }

  }

}
