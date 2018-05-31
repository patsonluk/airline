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
 


object BalanceSource {
  val FULL_LOAD = Map(DetailType.AIRLINE -> true, DetailType.AIRPORT -> true, DetailType.AIRPLANE -> true)
  val SIMPLE_LOAD = Map(DetailType.AIRLINE -> false, DetailType.AIRPORT -> false, DetailType.AIRPLANE -> false)
  val ID_LOAD : Map[DetailType.Type, Boolean] = Map.empty

//  case class AirlineTransaction(airlineId : Int, transactionType : TransactionType.Value, amount : Long)
//case class AirlineBalanceData(profit : Long, revenue: Long, expense: Long, links : LinksBalanceData, transactions : TransactionsBalance, others : OthersBalance)
//case class LinksBalanceData(profit : Long, revenue : Long, expense : Long, airportFee : Long, fuelCost : Long, crewCost : Long, depreciation : Long, inflightCost : Long, maintenanceCost: Long)
//case class TransactionsBalance(profit : Long, revenue: Long, expense: Long, transactionSummary : Map[TransactionType.Value, Long])
//case class OthersBalance(profit : Long, revenue: Long, expense: Long, othersSummary : Map[OtherBalanceItemType.Value, Long])
  
  def saveBalances(balances: List[AirlineBalanceData]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val balancePreparedStatement = connection.prepareStatement("REPLACE INTO " + BALANCE_TABLE + "(airline, profit, revenue, expense, cycle) VALUES(?,?,?,?,?)")
    val linksPreparedStatement = connection.prepareStatement("REPLACE INTO " + LINKS_BALANCE_TABLE + "(airline, profit, revenue, expense, ticket_revenue, airport_fee, fuel_cost, crew_cost, depreciation, inflight_cost, maintenance_cost, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")
    val transactionsPreparedStatement = connection.prepareStatement("REPLACE INTO " + TRANSACTIONS_BALANCE_TABLE + "(airline, transaction_type, amount, cycle) VALUES(?,?,?,?)")
    val othersPreparedStatement = connection.prepareStatement("REPLACE INTO " + OTHERS_BALANCE_TABLE + "(airline, other_balance_item_type, amount, cycle) VALUES(?,?,?,?)")
    val cycle = MainSimulation.currentWeek
    try {
      connection.setAutoCommit(false)
      balances.foreach { balance =>
          balancePreparedStatement.setInt(1, balance.airlineId)
          balancePreparedStatement.setLong(2, balance.profit)
          balancePreparedStatement.setLong(3, balance.revenue)
          balancePreparedStatement.setLong(4, balance.expense)
          balancePreparedStatement.setInt(5, cycle)
          balancePreparedStatement.addBatch()
          
          linksPreparedStatement.setInt(1, balance.airlineId)
          linksPreparedStatement.setLong(2, balance.links.profit)
          linksPreparedStatement.setLong(3, balance.links.revenue)
          linksPreparedStatement.setLong(4, balance.links.expense)
          linksPreparedStatement.setLong(5, balance.links.ticketRevenue)
          linksPreparedStatement.setLong(6, balance.links.airportFee)
          linksPreparedStatement.setLong(7, balance.links.fuelCost)
          linksPreparedStatement.setLong(8, balance.links.crewCost)
          linksPreparedStatement.setLong(9, balance.links.depreciation)
          linksPreparedStatement.setLong(10, balance.links.inflightCost)
          linksPreparedStatement.setInt(11, cycle)
          linksPreparedStatement.addBatch()
          
          balance.transactions.transactionSummary.foreach {
            case(transactionType, amount) => {
              transactionsPreparedStatement.setInt(1, balance.airlineId)
              transactionsPreparedStatement.setInt(2, transactionType.id)
              transactionsPreparedStatement.setLong(3, amount)
              transactionsPreparedStatement.setInt(4, cycle)
              transactionsPreparedStatement.addBatch()
            }
          }
          
          balance.others.othersSummary.foreach {
            case(itemType, amount) => {
              othersPreparedStatement.setInt(1, balance.airlineId)
              othersPreparedStatement.setInt(2, itemType.id)
              othersPreparedStatement.setLong(3, amount)
              othersPreparedStatement.setInt(4, cycle)
              othersPreparedStatement.addBatch()
            }
          }
          
      }
      
      balancePreparedStatement.executeBatch()
      balancePreparedStatement.close()
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
  
  def deleteBalanceConsumptionsByCycle(cyclesFromNow : Int) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    try {
      val currentCycle = MainSimulation.currentWeek
      val deleteFrom = if (currentCycle - cyclesFromNow < 0) 0 else currentCycle - cyclesFromNow 
      
      connection.setAutoCommit(false)
      var deleteStatement = connection.prepareStatement("DELETE FROM " + BALANCE_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + LINKS_BALANCE_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + TRANSACTIONS_BALANCE_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      deleteStatement = connection.prepareStatement("DELETE FROM " + OTHERS_BALANCE_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      deleteStatement.executeUpdate()
      
      connection.commit
    } finally {
      connection.close()
    }
  }
  
  def loadBalanceByAirline(airlineId : Int, cycle: Int)  = {
    loadBalanceByCriteria(List(("airline", airlineId), ("cycle", cycle)))
  }
  
  
  def loadBalanceByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    val balances = ListBuffer[AirlineBalanceData]()  
    try {
      var preparedStatement = getBalanceStatement(connection, BALANCE_TABLE, criteria)
      var resultSet = preparedStatement.executeQuery()
      
      if (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val totalProfit = resultSet.getLong("profit")
          val totalRevenue = resultSet.getLong("revenue") 
          val totalExpense = resultSet.getLong("expense")
          val cycle = resultSet.getInt("cycle")
           
          preparedStatement = getBalanceStatement(connection, LINKS_BALANCE_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          resultSet.next()
          val linksBalance = LinksBalanceData(airlineId = resultSet.getInt("airline"),
                         profit = resultSet.getLong("profit"),
                         revenue = resultSet.getLong("revenue"), 
                         expense = resultSet.getLong("expense"), 
                         ticketRevenue = resultSet.getLong("ticket_revenue"), 
                         airportFee = resultSet.getLong("airport_fee"), 
                         fuelCost = resultSet.getLong("fuel_cost"), 
                         crewCost = resultSet.getLong("crew_cost"), 
                         depreciation = resultSet.getLong("depreciation"), 
                         inflightCost = resultSet.getLong("inflight_cost"), 
                         maintenanceCost= resultSet.getLong("maintenance_cost"),
                         cycle = resultSet.getInt("cycle"))
                         
          preparedStatement = getBalanceStatement(connection, TRANSACTIONS_BALANCE_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          val transactionsSummary = scala.collection.mutable.Map[TransactionType.Value, Long]()
          var revenue = 0L
          var expense = 0L
          while (resultSet.next()) {
            val amount = resultSet.getLong("amount")
            transactionsSummary.put(TransactionType(resultSet.getInt("transaction_type")), amount)
            if (amount >= 0) {
              revenue += amount
            } else {
              expense -= amount
            }
          }
          val transactionsBalance = TransactionsBalance(airlineId, revenue - expense, revenue, expense, transactionsSummary.toMap, cycle)
          
          preparedStatement = getBalanceStatement(connection, OTHERS_BALANCE_TABLE, criteria)
          resultSet = preparedStatement.executeQuery()
          val othersSummary = scala.collection.mutable.Map[OtherBalanceItemType.Value, Long]()
          revenue = 0L
          expense = 0L
          while (resultSet.next()) {
            val amount = resultSet.getLong("amount")
            othersSummary.put(OtherBalanceItemType(resultSet.getInt("other_balance_item_type")), amount)
            if (amount >= 0) {
              revenue += amount
            } else {
              expense -= amount
            }
          }
          val othersBalance = OthersBalance(airlineId, revenue - expense, revenue, expense, othersSummary.toMap, cycle)
          
          balances += AirlineBalanceData(airlineId, totalProfit, totalRevenue, totalExpense, linksBalance, transactionsBalance, othersBalance, cycle)
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