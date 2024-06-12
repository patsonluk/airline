package com.patson.model

import com.patson.data.BankSource
import com.patson.data.IncomeSource
import com.patson.data.CycleSource
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource

object Bank {
  val WEEKS_PER_YEAR = 52
  val LOAN_TERMS = List[Int](26, WEEKS_PER_YEAR, 2 * WEEKS_PER_YEAR, 4 * WEEKS_PER_YEAR, 8 * WEEKS_PER_YEAR)
  val MAX_LOANS = 10
  val MIN_LOAN_AMOUNT = 10000
  val MAX_LOAN_AMOUNT = 1500000000 //1.5b max
  val LOAN_REAPPLY_MIN_INTERVAL = 2
  def getMaxLoan(airlineId : Int) : LoanReply = {
    val existingLoans = BankSource.loadLoansByAirline(airlineId)
    
    if (existingLoans.size >= MAX_LOANS) {
      return LoanReply(0, Some("Only up to " + MAX_LOANS + " loans are allowed"))
    }
    
    val currentCycle = CycleSource.loadCycle()
    
    existingLoans.sortBy(_.creationCycle).lastOption.foreach { previousLoan =>//check the last loan if there's one
      val weeksFromLastLoan = currentCycle - previousLoan.creationCycle
      if (weeksFromLastLoan < LOAN_REAPPLY_MIN_INTERVAL) {
        return LoanReply(0, Some("Can only apply next loan in " + (LOAN_REAPPLY_MIN_INTERVAL - weeksFromLastLoan) + " weeks"))
      }
    }
    
    //base on previous month
    val previousMonthCycle = currentCycle - currentCycle % 4 - 1
    
    val creditFromProfit : Option[Long] = IncomeSource.loadIncomeByAirline(airlineId, previousMonthCycle, Period.MONTHLY).map(_.links.profit * 13 * 2)  //2 * yearly link profit
    
    val totalAssets = Computation.getResetAmount(airlineId).overall
    val creditFromAssets = (totalAssets * 0.2).toLong //offer 20% of the assets as credit
    val totalCredit = creditFromAssets + creditFromProfit.getOrElse(0L)
    
    val liability = existingLoans.map(_.remainingPayment(currentCycle)).sum
    
    var availableLoanAmount = totalCredit - liability
    
    if (availableLoanAmount >= MAX_LOAN_AMOUNT) {
      availableLoanAmount = MAX_LOAN_AMOUNT
    }
    
    if (availableLoanAmount >= MIN_LOAN_AMOUNT) {
      return LoanReply(availableLoanAmount, None)
    } else {
      return LoanReply(0, Some("Lending you money is considered too high risk."))
    }
  }
 
  def getLoanOptions(principal : Long) : List[Loan] = {
    val currentCycle = CycleSource.loadCycle()
    BankSource.loadLoanInterestRateByCycle(currentCycle) match {
      case Some(currentRate) =>
        getLoanOptions(principal, currentRate.annualRate, currentCycle)
      case None =>
        List.empty[Loan]
    }
  }

  def getLoanOptions(principal : Long, annualRate : BigDecimal, currentCycle : Int) = {
      val rateIncrementPerYear = 0.004 //0.5% more every extra year
      LOAN_TERMS.map { term =>
        //Payment = P x (r / n) x (1 + r / n)^n(t)] / ((1 + r / n)^n(t) - 1)
        val years = term / WEEKS_PER_YEAR
        val baseAnnualRate = annualRate
        val annualRateByTerm = baseAnnualRate + (years - 1) * rateIncrementPerYear
        Loan(airlineId = 0, principal = principal, annualRate = annualRateByTerm, creationCycle = currentCycle, lastPaymentCycle = currentCycle, term = term)
      }
  }

  case class LoanReply(maxLoan : Long, rejectionOption : Option[String])
}