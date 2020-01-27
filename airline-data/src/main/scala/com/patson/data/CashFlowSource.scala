package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement
import scala.collection.mutable.HashSet
import java.sql.Connection
import scala.collection.mutable.HashMap
import com.patson.MainSimulation
 


object CashFlowSource {
  def saveCashFlows(cashFlows: List[AirlineCashFlow]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val cashFlowPreparedStatement = connection.prepareStatement("REPLACE INTO " + CASH_FLOW_TABLE + "(airline, cash_flow, operation, loan_interest, loan_principle, base_construction, buy_airplane, sell_airplane, create_link, facility_construction, oil_contract, period, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")
    
    try {
      connection.setAutoCommit(false)
      cashFlows.foreach { cashFlow =>
          val period = cashFlow.period
          cashFlowPreparedStatement.setInt(1, cashFlow.airlineId)
          cashFlowPreparedStatement.setLong(2, cashFlow.cashFlow)
          cashFlowPreparedStatement.setLong(3, cashFlow.operation)
          cashFlowPreparedStatement.setLong(4, cashFlow.loanInterest)
          cashFlowPreparedStatement.setLong(5, cashFlow.loanPrincipal)
          cashFlowPreparedStatement.setLong(6, cashFlow.baseConstruction)
          cashFlowPreparedStatement.setLong(7, cashFlow.buyAirplane)
          cashFlowPreparedStatement.setLong(8, cashFlow.sellAirplane)
          cashFlowPreparedStatement.setLong(9, cashFlow.createLink)
          cashFlowPreparedStatement.setLong(10, cashFlow.facilityConstruction)
          cashFlowPreparedStatement.setLong(11, cashFlow.oilContract)
          cashFlowPreparedStatement.setInt(12, period.id)
          cashFlowPreparedStatement.setInt(13, cashFlow.cycle)
          cashFlowPreparedStatement.addBatch()
      }
      
      cashFlowPreparedStatement.executeBatch()
      cashFlowPreparedStatement.close()
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  def deleteCashFlows(cycle : Int, period : Period.Value) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      var deleteStatement = connection.prepareStatement("DELETE FROM " + CASH_FLOW_TABLE + " WHERE cycle = ? AND period = ?")
      deleteStatement.setInt(1, cycle)
      deleteStatement.setInt(2, period.id)
      deleteStatement.executeUpdate()
      
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  
  def deleteCashFlowsBefore(cycleAndBefore : Int, period : Period.Value) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      var deleteStatement = connection.prepareStatement("DELETE FROM " + CASH_FLOW_TABLE + " WHERE cycle <= ? AND period = ?")
      deleteStatement.setInt(1, cycleAndBefore)
      deleteStatement.setInt(2, period.id)
      deleteStatement.executeUpdate()
      
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  def loadCashFlowByAirline(airlineId : Int, cycle: Int, period : Period.Value) : Option[AirlineCashFlow] = {
    val cashFlows = loadCashFlowByCriteria(List(("airline", airlineId), ("cycle", cycle), ("period", period.id)))
    if (cashFlows.length > 0) {
      Some(cashFlows(0)) 
    } else {
      None
    }
  }
  
  def loadCashFlowsByAirline(airlineId : Int) : List[AirlineCashFlow] = {
    loadCashFlowByCriteria(List(("airline", airlineId)))
  }
  
  
  def loadCashFlowByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    val cashFlows = ListBuffer[AirlineCashFlow]()  
    try {
      val cashFlowStatement = getCashFlowStatement(connection, criteria)
      val resultSet = cashFlowStatement.executeQuery()
      
      while (resultSet.next()) {
          val airlineId = resultSet.getInt("i.airline")
          val cashFlow = resultSet.getLong("i.cash_flow")
          val operation = resultSet.getLong("i.operation") 
          val loanInterest = resultSet.getLong("i.loan_interest")
          val loanPrincipal = resultSet.getLong("i.loan_principle")
          val baseConstruction = resultSet.getLong("i.base_construction")
          val buyAirplane = resultSet.getLong("i.buy_airplane")
          val sellAirplane = resultSet.getLong("i.sell_airplane")
          val createLink= resultSet.getLong("i.create_link")
          val facilityConstruction = resultSet.getLong("i.facility_construction")
          val oilContract = resultSet.getLong("i.oil_contract")
          val period = Period(resultSet.getInt("i.period"))
          val cycle = resultSet.getInt("i.cycle")
           
            //case class AirlineCashFlow(airlineId : Int, cashFlow : Long = 0, operation : Long = 0, loanInterest : Long = 0, loanPrinciple : Long = 0, baseConstruction : Long = 0, buyAirplane : Long = 0, sellAirplane : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
          cashFlows += AirlineCashFlow(airlineId, cashFlow, operation, loanInterest, loanPrincipal, baseConstruction, buyAirplane, sellAirplane, createLink, facilityConstruction, oilContract, period, cycle)
      }
       
       cashFlows.toList
    } finally {
      connection.close()
    }
  }
  
  def getCashFlowStatement(connection: Connection, criteria : List[(String, Any)]) = {
    val queryString = new StringBuilder("SELECT i.* FROM " + CASH_FLOW_TABLE + " i")
    
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
}