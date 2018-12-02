package com.patson.model

case class Loan(airlineId : Int, borrowedAmount : Long, interest : Long, var remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject {
  val principalWeeklyPayment = Math.ceil(borrowedAmount.toDouble / loanTerm).toLong
  val interestWeeklyPayment = Math.ceil(interest.toDouble / loanTerm).toLong
  val weeklyPayment = principalWeeklyPayment + interestWeeklyPayment
  val remainingTerm = Math.ceil(remainingAmount.toDouble / weeklyPayment).toLong
  val remainingInterest = interestWeeklyPayment * remainingTerm
  val remainingPrincipal = principalWeeklyPayment * remainingTerm
  val earlyRepaymentFee = remainingInterest / 3
  val earlyRepayment = remainingPrincipal + earlyRepaymentFee
}


