package com.patson.data
import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer
import java.sql.DriverManager

import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement

import com.patson.model._
import java.sql.Statement
import java.sql.ResultSet

import com.patson.util.AirlineCache

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map


object LogSource {
  val insertLogs = (logs : List[Log]) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("INSERT INTO " + LOG_TABLE + "(airline, message, category, severity, cycle) VALUES(?,?,?,?,?)")
    
    connection.setAutoCommit(false)
    
    try {
      logs.foreach {  
        case Log(airline : Airline, message : String, category : LogCategory.Value, severity : LogSeverity.Value, cycle : Int) => {
          statement.setInt(1, airline.id)
          statement.setString(2, message)
          statement.setInt(3, category.id)
          statement.setInt(4, severity.id)
          statement.setInt(5, cycle)
          statement.addBatch()
        }
      }
      statement.executeBatch()
      
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadLogsByAirline(airlineId : Int, fromCycle : Int, fullLoad : Boolean = false) = {
    var queryString = "SELECT * FROM " + LOG_TABLE + " WHERE airline = ? AND cycle >= ?"
     loadLogsByQueryString(queryString, List(airlineId, fromCycle), fullLoad)
  }
  
  
  def loadLogsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = "SELECT * FROM " + LOG_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadLogsByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadLogsByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[Log] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val logs = ListBuffer[Log]()
        
        
        val airlines = Map[Int, Airline]()
        
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, fullLoad).getOrElse(Airline.fromId(airlineId)))
          val message = resultSet.getString("message")
          val category = LogCategory(resultSet.getInt("category"))
          val severity = LogSeverity(resultSet.getInt("severity"))
          val cycle = resultSet.getInt("cycle")
          logs += Log(airline, message, category, severity, cycle)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        logs.toList
      } finally {
        connection.close()
      }
  }
  
  def deleteLogsBeforeCycle(cutoffCycle : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + LOG_TABLE + " WHERE cycle < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffCycle)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}