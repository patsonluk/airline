package com.patson.data
import com.patson.data.Constants._

import scala.collection.mutable.{HashMap, HashSet, ListBuffer, Map, Set}
import com.patson.model._
import java.sql.Connection

import com.patson.model.bank.LoanInterestRate



object BankSource {
//case class Loan(owner : Airline, borrowedAmount : Long, interest : Long, remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject  
  def saveLoan(loan: Loan) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LOAN_TABLE + "(airline, borrowed_amount, interest, remaining_amount, creation_cycle, loan_term) VALUES(?,?,?,?,?,?)")
    
    try {
      preparedStatement.setInt(1, loan.airlineId)
      preparedStatement.setLong(2, loan.borrowedAmount)
      preparedStatement.setLong(3, loan.interest)
      preparedStatement.setLong(4, loan.remainingAmount)
      preparedStatement.setInt(5, loan.creationCycle)
      preparedStatement.setInt(6, loan.loanTerm)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def updateLoan(loan: Loan) = {
    val connection = Meta.getConnection()
    try {
      var deleteStatement = connection.prepareStatement("UPDATE " + LOAN_TABLE + " SET remaining_amount = ? WHERE id = ?")
      deleteStatement.setLong(1, loan.remainingAmount)
      deleteStatement.setInt(2, loan.id)
      deleteStatement.executeUpdate()
    } finally {
      connection.close()
    }
  }
  
  
  
  def deleteLoan(loanId : Int) = {
    val connection = Meta.getConnection()
    try {
      var deleteStatement = connection.prepareStatement("DELETE FROM " + LOAN_TABLE + " WHERE id = ?")
      deleteStatement.setInt(1, loanId)
      deleteStatement.executeUpdate()
    } finally {
      connection.close()
    }
  }
  
  def loadLoansByAirline(airlineId : Int) : List[Loan] = {
    loadLoansByCriteria(List(("airline", airlineId)))
  }
  
  def loadLoanById(loanId : Int) : Option[Loan] = {
    val loans = loadLoansByCriteria(List(("id", loanId)))
    if (loans.isEmpty) {
      None
    } else {
      Some(loans(0))
    }
  }
  
  
  def loadLoansByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    val loans = ListBuffer[Loan]()  
    try {
      val statement = getQueryStatement(connection, criteria)
      val resultSet = statement.executeQuery()
      
      while (resultSet.next()) {
        loans += Loan(
            airlineId = resultSet.getInt("airline"),
            borrowedAmount = resultSet.getLong("borrowed_amount"),
            interest = resultSet.getLong("interest"), 
            remainingAmount = resultSet.getLong("remaining_amount"),
            creationCycle = resultSet.getInt("creation_cycle"),
            loanTerm = resultSet.getInt("loan_term"),
            id = resultSet.getInt("id"))
      }
       
       loans.toList
    } finally {
      connection.close()
    }
  }
  
  def getQueryStatement(connection: Connection, criteria : List[(String, Any)]) = {
    val queryString = new StringBuilder("SELECT * FROM " + LOAN_TABLE + " ")
    
    if (!criteria.isEmpty) {
      queryString.append(" WHERE ")
      for (i <- 0 until criteria.size - 1) {
          queryString.append(criteria(i)._1 + " = ? AND ")
        }
        queryString.append(criteria.last._1 + " = ?")
    }
    
    
    val preparedStatement = connection.prepareStatement(queryString.toString())
    
    for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
    }
    preparedStatement
  }

  def loadLoanInterestRatesFromCycle(fromCycle : Int) : List[LoanInterestRate] = {
    var queryString = "SELECT * FROM " + LOAN_INTEREST_RATE_TABLE + " WHERE cycle >= ?"
    loadLoanInterestRatesByQueryString(queryString, List(fromCycle))
  }

  def loadLoanInterestRateByCycle(cycle : Int) : Option[LoanInterestRate] = {
    var queryString = "SELECT * FROM " + LOAN_INTEREST_RATE_TABLE + " WHERE cycle = ?"
    val result = loadLoanInterestRatesByQueryString(queryString, List(cycle))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }


  def loadLoanInterestRatesByQueryString(queryString : String, parameters : List[Any]) : List[LoanInterestRate] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()

      val rates = new ListBuffer[LoanInterestRate]()

      while (resultSet.next()) {
        rates += LoanInterestRate(annualRate = resultSet.getDouble("rate"), cycle = resultSet.getInt("cycle"))
      }

      resultSet.close()
      preparedStatement.close()

      rates.toList
    } finally {
      connection.close()
    }
  }
  def saveLoanInterestRate(rate : LoanInterestRate) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + LOAN_INTEREST_RATE_TABLE + "(rate, cycle) VALUES(?, ?)")

      preparedStatement.setBigDecimal(1, rate.annualRate.bigDecimal)
      preparedStatement.setInt(2, rate.cycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def deleteLoanInterestRatesUpToCycle(toCycle : Int) : Int = {
    var queryString = "DELETE FROM " + LOAN_INTEREST_RATE_TABLE + " WHERE cycle < ?"
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, toCycle)
      val updateCount = preparedStatement.executeUpdate()
      preparedStatement.close()
      updateCount
    } finally {
      connection.close()
    }
  }
  
}