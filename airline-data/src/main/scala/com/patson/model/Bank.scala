package com.patson.model

import com.patson.data.BankSource
import com.patson.data.IncomeSource
import com.patson.data.CycleSource
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource

object Bank {
  val LOAN_TERMS = Map(52 -> 0.2 , 2 * 52 -> 0.18, 3 *52 -> 0.16, 5 * 52 -> 0.14)
  val MAX_LOANS = 10
  val MIN_LOAN_AMOUNT = 10000
  def getMaxLoan(airlineId : Int) : Long = {
    val existingLoans = BankSource.loadLoansByAirline(airlineId)
    
    if (existingLoans.size >= MAX_LOANS) {
      return 0
    }
    
    val currentCycle = CycleSource.loadCycle()
    
    //base on previous month
    val previousMonthCycle = currentCycle - currentCycle % 4 - 1
    
    val creditFromProfit : Option[Long] = IncomeSource.loadIncomeByAirline(airlineId, previousMonthCycle, Period.MONTHLY).map(_.profit * 13 * 2)  //2 * yearly profit  
    
    var totalAssets = 0L
    AirlineSource.loadAirlineBasesByAirline(airlineId).foreach { base =>
      totalAssets = totalAssets + base.getUpgradeCost(base.scale) //take the upgrade to current base as the assets value
    }
    
    AirplaneSource.loadAirplanesByOwner(airlineId).foreach { airplane =>
      totalAssets = totalAssets + airplane.value
    }
    
    //offer 50% of the assets as credit
    val creditFromAssets = (totalAssets * 0.5).toLong
    
    val totalCredit = creditFromAssets + creditFromProfit.getOrElse(0L)
    
    val liability = existingLoans.map(_.remainingAmount).sum
    
    val availableLoanAmount = totalCredit - liability 
    if (availableLoanAmount >= MIN_LOAN_AMOUNT) {
      return availableLoanAmount
    } else {
      return 0
    }
  }
 
  def getLoanOptions(loanAmount : Long) : List[Loan] = {
    LOAN_TERMS.map {
      case(term, interestRate) => {
        val interest = (loanAmount * interestRate).toLong
        val total = loanAmount + interest
        Loan(airlineId = 0, borrowedAmount = loanAmount, interest = interest, remainingAmount = total, creationCycle = 0, loanTerm = term)   
      }
    }.toList
  }
  
  
}