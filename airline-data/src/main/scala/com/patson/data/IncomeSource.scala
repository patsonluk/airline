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
 


object IncomeSource {
  val FULL_LOAD = Map(DetailType.AIRLINE -> true, DetailType.AIRPORT -> true, DetailType.AIRPLANE -> true)
  val SIMPLE_LOAD = Map(DetailType.AIRLINE -> false, DetailType.AIRPORT -> false, DetailType.AIRPLANE -> false)
  val ID_LOAD : Map[DetailType.Type, Boolean] = Map.empty

//  case class AirlineTransaction(airlineId : Int, transactionType : TransactionType.Value, amount : Long)
//case class AirlineBalanceData(profit : Long, revenue: Long, expense: Long, links : LinksBalanceData, transactions : TransactionsBalance, others : OthersBalance)
//case class LinksBalanceData(profit : Long, revenue : Long, expense : Long, airportFee : Long, fuelCost : Long, crewCost : Long, depreciation : Long, inflightCost : Long, maintenanceCost: Long)
//case class TransactionsBalance(profit : Long, revenue: Long, expense: Long, transactionSummary : Map[TransactionType.Value, Long])
//case class OthersBalance(profit : Long, revenue: Long, expense: Long, othersSummary : Map[OtherBalanceItemType.Value, Long])
  
  def saveIncomes(balances: List[AirlineIncome]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val incomePreparedStatement = connection.prepareStatement("REPLACE INTO " + INCOME_TABLE + "(airline, profit, revenue, expense, period, cycle) VALUES(?,?,?,?,?,?)")
    val linksPreparedStatement = connection.prepareStatement("REPLACE INTO " + LINKS_INCOME_TABLE + "(airline, profit, revenue, expense, ticket_revenue, airport_fee, fuel_cost, crew_cost, inflight_cost, maintenance_cost, period, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")
    val transactionsPreparedStatement = connection.prepareStatement("REPLACE INTO " + TRANSACTIONS_INCOME_TABLE + "(airline, profit, revenue, expense, capital_gain, create_link, period, cycle) VALUES(?,?,?,?,?,?,?,?)")
    val othersPreparedStatement = connection.prepareStatement("REPLACE INTO " + OTHERS_INCOME_TABLE + "(airline, profit, revenue, expense, loan_interest, base_upkeep, service_investment, maintenance_investment, advertisement, depreciation,  period, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")
    val cycle = MainSimulation.currentWeek
    try {
      connection.setAutoCommit(false)
      balances.foreach { balance =>
          val period = balance.period
          incomePreparedStatement.setInt(1, balance.airlineId)
          incomePreparedStatement.setLong(2, balance.profit)
          incomePreparedStatement.setLong(3, balance.revenue)
          incomePreparedStatement.setLong(4, balance.expense)
          incomePreparedStatement.setInt(5, period.id)
          incomePreparedStatement.setInt(6, cycle)
          incomePreparedStatement.addBatch()
          
          linksPreparedStatement.setInt(1, balance.airlineId)
          linksPreparedStatement.setLong(2, balance.links.profit)
          linksPreparedStatement.setLong(3, balance.links.revenue)
          linksPreparedStatement.setLong(4, balance.links.expense)
          linksPreparedStatement.setLong(5, balance.links.ticketRevenue)
          linksPreparedStatement.setLong(6, balance.links.airportFee)
          linksPreparedStatement.setLong(7, balance.links.fuelCost)
          linksPreparedStatement.setLong(8, balance.links.crewCost)
          linksPreparedStatement.setLong(9, balance.links.inflightCost)
          linksPreparedStatement.setLong(10, balance.links.maintenanceCost)
          linksPreparedStatement.setInt(11, period.id)
          linksPreparedStatement.setInt(12, cycle)
          linksPreparedStatement.addBatch()
          
          
          transactionsPreparedStatement.setInt(1, balance.airlineId)
          transactionsPreparedStatement.setLong(2, balance.transactions.profit)
          transactionsPreparedStatement.setLong(3, balance.transactions.revenue)
          transactionsPreparedStatement.setLong(4, balance.transactions.expense)
          transactionsPreparedStatement.setLong(5, balance.transactions.capitalGain)
          transactionsPreparedStatement.setLong(6, balance.transactions.createLink)
          transactionsPreparedStatement.setInt(7, period.id)
          transactionsPreparedStatement.setInt(8, cycle)
          transactionsPreparedStatement.addBatch()
          
          
          
          othersPreparedStatement.setInt(1, balance.airlineId)
          othersPreparedStatement.setLong(2, balance.others.profit)
          othersPreparedStatement.setLong(3, balance.others.revenue)
          othersPreparedStatement.setLong(4, balance.others.expense)
          othersPreparedStatement.setLong(5, balance.others.loanInterest)
          othersPreparedStatement.setLong(6, balance.others.baseUpkeep)
          othersPreparedStatement.setLong(7, balance.others.serviceInvestment)
          othersPreparedStatement.setLong(8, balance.others.maintenanceInvestment)
          othersPreparedStatement.setLong(9, balance.others.advertisement)
          othersPreparedStatement.setLong(10, balance.others.depreciation)
          othersPreparedStatement.setInt(11, period.id)
          othersPreparedStatement.setInt(12, cycle)
          othersPreparedStatement.addBatch()
          
          
      }
      
      incomePreparedStatement.executeBatch()
      incomePreparedStatement.close()
      linksPreparedStatement.executeBatch()
      linksPreparedStatement.close()
      transactionsPreparedStatement.executeBatch()
      transactionsPreparedStatement.close()
      othersPreparedStatement.executeBatch()
      othersPreparedStatement.close()
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  def deleteIncomesByCycle(cyclesFromNow : Int) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    try {
      val currentCycle = MainSimulation.currentWeek
      val deleteFrom = if (currentCycle - cyclesFromNow < 0) 0 else currentCycle - cyclesFromNow 
      
      connection.setAutoCommit(false)
      var deleteStatement = connection.prepareStatement("DELETE FROM " + INCOME_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + LINKS_INCOME_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + TRANSACTIONS_INCOME_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + OTHERS_INCOME_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  def loadIncomeByAirline(airlineId : Int, cycle: Int, period : Period.Value) : Option[AirlineIncome] = {
    val incomes = loadIncomeByCriteria(List(("airline", airlineId), ("cycle", cycle), ("period", period.id)))
    if (incomes.length > 0) {
      Some(incomes(0)) 
    } else {
      None
    }
  }
  
  
  def loadIncomeByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    val balances = ListBuffer[AirlineIncome]()  
    try {
      var preparedStatement = getBalanceStatement(connection, INCOME_TABLE, criteria)
      var resultSet = preparedStatement.executeQuery()
      
      if (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val totalProfit = resultSet.getLong("profit")
          val totalRevenue = resultSet.getLong("revenue") 
          val totalExpense = resultSet.getLong("expense")
          val period = Period(resultSet.getInt("period"))
          val cycle = resultSet.getInt("cycle")
           
          preparedStatement = getBalanceStatement(connection, LINKS_INCOME_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          resultSet.next()
          val linksBalance = LinksIncome(airlineId = resultSet.getInt("airline"),
                         profit = resultSet.getLong("profit"),
                         revenue = resultSet.getLong("revenue"), 
                         expense = resultSet.getLong("expense"), 
                         ticketRevenue = resultSet.getLong("ticket_revenue"), 
                         airportFee = resultSet.getLong("airport_fee"), 
                         fuelCost = resultSet.getLong("fuel_cost"), 
                         crewCost = resultSet.getLong("crew_cost"), 
                         inflightCost = resultSet.getLong("inflight_cost"), 
                         maintenanceCost= resultSet.getLong("maintenance_cost"),
                         period = Period(resultSet.getInt("period")),
                         cycle = resultSet.getInt("cycle"))
                         
          preparedStatement = getBalanceStatement(connection, TRANSACTIONS_INCOME_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          resultSet.next()
            
          val transactionsBalance = TransactionsIncome(airlineId,
                         profit = resultSet.getLong("profit"),
                         revenue = resultSet.getLong("revenue"), 
                         expense = resultSet.getLong("expense"), 
                         capitalGain = resultSet.getLong("capital_gain"), 
                         createLink = resultSet.getLong("create_link"), 
                         period = Period(resultSet.getInt("period")),
                         cycle = resultSet.getInt("cycle"))
              
          
          preparedStatement = getBalanceStatement(connection, OTHERS_INCOME_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          resultSet.next()
          val othersBalance = OthersIncome(airlineId,
                         profit = resultSet.getLong("profit"),
                         revenue = resultSet.getLong("revenue"), 
                         expense = resultSet.getLong("expense"), 
                         loanInterest = resultSet.getLong("loan_interest"), 
                         baseUpkeep = resultSet.getLong("base_upkeep"), 
                         serviceInvestment = resultSet.getLong("service_investment"), 
                         maintenanceInvestment = resultSet.getLong("maintenance_investment"),
                         advertisement = resultSet.getLong("advertisement"), 
                         depreciation = resultSet.getLong("depreciation"),
                         period = Period(resultSet.getInt("period")),
                         cycle = resultSet.getInt("cycle"))
          
          balances += AirlineIncome(airlineId, totalProfit, totalRevenue, totalExpense, linksBalance, transactionsBalance, othersBalance, period, cycle)
      }
       
       balances.toList
    } finally {
      connection.close()
    }
  }
  
  def getBalanceStatement(connection: Connection, tableName : String, criteria : List[(String, Any)]) = {
    val queryString = new StringBuilder("SELECT * FROM " + tableName)
      
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
  
  object DetailType extends Enumeration {
    type Type = Value
    val AIRPORT, AIRLINE, AIRPLANE = Value
  }
}