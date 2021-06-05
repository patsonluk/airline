package com.patson

import java.math.MathContext

import com.patson.data._
import com.patson.model.bank.LoanInterestRate

import scala.util.Random

object LoanInterestRateSimulation {

  val PREVIOUS_RATE_ENTRIES_TO_CONSIDER = 20

  def simulate(cycle: Int) : Unit = {
    val fromCycle = cycle - PREVIOUS_RATE_ENTRIES_TO_CONSIDER //last 20 weeks have influence on the rate calculation
    val rates = BankSource.loadLoanInterestRatesFromCycle(fromCycle).sortBy(_.cycle).map(_.annualRate)
    val nextRate = getNextRate(rates)
    if (!rates.isEmpty) {
      println(s"Interest rate simulation [previous rate : ${rates.last}, new rate : $nextRate]")
    }

    BankSource.saveLoanInterestRate(LoanInterestRate(nextRate, cycle))
    //purge 200 turns ago
    BankSource.deleteLoanInterestRatesUpToCycle(cycle - 200)
  }

  val toBigDecimal : Double => BigDecimal =  (in: Double) => {
    BigDecimal.valueOf(in).setScale(2)
  }
  
  def getNextRate(previousRates : List[BigDecimal]) : BigDecimal = {
    if (previousRates.isEmpty) {
      return DEFAULT_ANNUAL_RATE
    }
    //check when was the last time a rate changed
    var walker = previousRates.last
    var cycleCount = 0
    val previousChangedRate = previousRates.reverse.find { rate =>
      cycleCount += 1
      if (rate != walker) { //found it
        true
      } else {
        walker = rate
        false
      }
    }

    //cycle count contains either the last change, or the max of the list length - both can be used for the next logic

    //the closer a change has been made the less likely it will change again
    val shouldChange =
      if (cycleCount > 10) { //50% if > 10
        Random.nextBoolean()
      } else if (cycleCount > 5) { //otherwise 1/3
        Random.nextInt(3) == 2
      } else { //low chance
        Random.nextInt(20) <= cycleCount
      }

    if (shouldChange) { //now find out about the velocity
      val previousDelta : BigDecimal = previousChangedRate match {
        case Some(lastChangedRate) =>
          previousRates.last - lastChangedRate
        case None => 0
      }
      simulateNextRate(previousRates.last, previousDelta)
    } else { //same rate as previous one
      previousRates.last
    }
  }

  val DEFAULT_ANNUAL_RATE : Double = 0.12 //12%
  val MAX_DELTA : BigDecimal = 0.01
  val RATE_STEP : BigDecimal= 0.001 // 0.1 % is a step
  val MIN_RATE : BigDecimal= 0.01 //min rate is 1%
  val MAX_RATE = DEFAULT_ANNUAL_RATE * 2 - MIN_RATE //max rate is 23% annual
  val BOUNDARY_ZONE_FACTOR : BigDecimal = 0.25 //top 25% and bottom 25% are considered outside of boundary
  val BOUNDARY_ZONE_DELTA_ADJUSTMENT = 0.003 // 0.3% adjustment if it's considered in abnormal range (ie > HIGH or < LOW threshold)
  val HIGH_RATE_THRESHOLD = MAX_RATE - (MAX_RATE - MIN_RATE) * BOUNDARY_ZONE_FACTOR
  val LOW_RATE_THRESHOLD = MIN_RATE + (MAX_RATE - MIN_RATE) * BOUNDARY_ZONE_FACTOR

  def simulateNextRate(previousRate : BigDecimal, previousDelta : BigDecimal) : BigDecimal = {
     var newDelta = (Random.nextInt((MAX_DELTA / RATE_STEP).toInt) + 1) * RATE_STEP

     if (Random.nextBoolean()) {
       newDelta *= -1 //could either go up or down
     }

     //now adjust it based on previous delta - if it was dropping, then it's more likely to just drop more

     
     //now adjust the delta if it's very close to boundary zone
     if (previousRate <= LOW_RATE_THRESHOLD && newDelta < 0) { //still dropping
       newDelta += BOUNDARY_ZONE_DELTA_ADJUSTMENT
     } else if (previousRate >= HIGH_RATE_THRESHOLD && newDelta > 0) { //still rising
       newDelta -= BOUNDARY_ZONE_DELTA_ADJUSTMENT
     }

    if (newDelta > MAX_DELTA) {
      newDelta = MAX_DELTA
    } else if (newDelta < MAX_DELTA * -1) {
      newDelta = MAX_DELTA * -1
    }

     var newRate = previousRate + newDelta
     if (newRate > MAX_RATE) {
       newRate = MAX_RATE
     } else if (newRate < MIN_RATE) {
       newRate = MIN_RATE
     }
     //BigDecimal(newPrice).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
     newRate
  }
}