package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{BankSource, Constants, CycleSource, LinkSource, Meta}
import com.patson.init.actorSystem
import com.patson.model.Loan

import java.sql.Connection
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LoanPatcher extends App {
  mainFlow

  def mainFlow() {
    Class.forName(DB_DRIVER)
    val dataSource = new ComboPooledDataSource()
    dataSource.setUser(DATABASE_USER)
    dataSource.setPassword(DATABASE_PASSWORD)
    dataSource.setJdbcUrl(DATABASE_CONNECTION)
    dataSource.setMaxPoolSize(100)

    val connection = dataSource.getConnection()
    try {
      convert(connection)
    } finally {
      connection.close
    }

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def loadExistingLoans(connection : Connection) = {
    val statement = connection.prepareStatement("SELECT * FROM " + Constants.LOAN_TABLE);
    val resultSet = statement.executeQuery()
    val result = ListBuffer[ExistingLoan]()
    while (resultSet.next()) {
      result.append(
        ExistingLoan(
         resultSet.getInt("airline"),
          resultSet.getLong("borrowed_amount"),
          resultSet.getLong("interest"),
          resultSet.getLong("remaining_amount"),
          resultSet.getInt("creation_cycle"),
          resultSet.getInt("loan_term"),
        )
      )
    }

    result.toList

    //airline | borrowed_amount | interest  | remaining_amount | creation_cycle | loan_term


  }

  def convert(connection : Connection): Unit = {
    val existingLoans = loadExistingLoans(connection)
    Meta.createLoan(connection)

    val currentCycle = CycleSource.loadCycle()
    existingLoans.foreach { existingLoan =>
      val originalRate = existingLoan.interest.toDouble / (existingLoan.loanTerm / 52) / existingLoan.borrowedAmount

      println(s"$existingLoan . Rate: $originalRate")
      //val weeklyPayment : Long = Math.ceil(principal * weeklyRate * Math.pow(1 + weeklyRate, term) / (Math.pow(1 + weeklyRate, term) - 1)).toLong
      //weeklyPayment = principal * weeklyRate * R / (R - 1)

      val creationCycle =
        if (existingLoan.creationCycle == 0) { //a bug on profile, calculate the cycle
          val ratioPaid = 1 - existingLoan.remainingAmount.toDouble / (existingLoan.borrowedAmount + existingLoan.interest)
          val loanTermPaid = (existingLoan.loanTerm * ratioPaid).toInt
          currentCycle - loanTermPaid
        } else {
          existingLoan.creationCycle
        }

      val newLoan = Loan(existingLoan.airlineId, existingLoan.borrowedAmount, originalRate, creationCycle, currentCycle, existingLoan.loanTerm)
      BankSource.saveLoan(newLoan)
    }
  }

  case class ExistingLoan(airlineId : Int, borrowedAmount : Long, interest : Long, remainingAmount : Long, creationCycle : Int, loanTerm : Int)

}