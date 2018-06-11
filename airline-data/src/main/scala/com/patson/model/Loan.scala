package com.patson.model

case class Loan(airlineId : Int, borrowedAmount : Long, interest : Long, var remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject

