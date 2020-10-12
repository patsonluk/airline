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


object AlertSource {
  val insertAlerts = (alerts : List[Alert]) => {
    val connection = Meta.getConnection()
    //case class Alert(airline : Airline, message : String, category : AlertCategory.Value, targetId : Option[Int], cycle : Int, duration : Int, var id : Int)
    val statement = connection.prepareStatement("INSERT INTO " + ALERT_TABLE + "(airline, message, category, target_id, duration, cycle) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    
    connection.setAutoCommit(false)
    
    try {
      alerts.foreach { alert => alert match {
          case Alert(airline, message, category, targetId, cycle, duration, id) => {
            statement.setInt(1, airline.id)
            statement.setString(2, message)
            statement.setInt(3, category.id)
            targetId match {
              case Some(targetId) => statement.setInt(4, targetId)
              case None => statement.setNull(4,  java.sql.Types.INTEGER)
            }
            
            statement.setInt(5, duration)
            statement.setInt(6, cycle)
            
            statement.executeUpdate()
        
            val generatedKeys = statement.getGeneratedKeys
            if (generatedKeys.next()) {
              val generatedId = generatedKeys.getInt(1)
              alert.id = generatedId
            }
          }
        }
      }
      
      
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }
  
  val updateAlerts = (alerts : List[Alert]) => {
    val connection = Meta.getConnection()
    //case class Alert(airline : Airline, message : String, category : AlertCategory.Value, targetId : Option[Int], cycle : Int, var id : Int)
    val statement = connection.prepareStatement("REPLACE INTO " + ALERT_TABLE + "(airline, message, category, target_id, cycle, duration, id) VALUES(?,?,?,?,?,?,?)")
    
    connection.setAutoCommit(false)
    
    try {
      alerts.foreach { 
        case Alert(airline, message, category, targetId, cycle, duration, id) => {
          statement.setInt(1, airline.id)
          statement.setString(2, message)
          statement.setInt(3, category.id)
          targetId match {
            case Some(targetId) => statement.setInt(4, targetId)
            case None => statement.setNull(4,  java.sql.Types.INTEGER)
          }
          
          statement.setInt(5, cycle)
          statement.setInt(6, duration)
          statement.setInt(7, id)
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
  
  def deleteAlerts(alerts : List[Alert]) = {
    val connection = Meta.getConnection()
    //case class Alert(airline : Airline, message : String, category : AlertCategory.Value, targetId : Option[Int], cycle : Int, var id : Int)
    val statement = connection.prepareStatement("DELETE FROM " + ALERT_TABLE + " WHERE id = ?")
    
    connection.setAutoCommit(false)
    
    try {
      alerts.foreach { alert =>
        statement.setInt(1, alert.id)
        statement.addBatch()
      }
      
      statement.executeBatch()
      
      
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadAlertsByAirline(airlineId : Int, fullLoad : Boolean = false) = {
     loadAlertsByCriteria(List(("airline", airlineId)), fullLoad)
  }
  
  def loadAlertsByAirlineAndCategory(airlineId : Int, category : AlertCategory.Value, fullLoad : Boolean = false) = {
     loadAlertsByCriteria(List(("airline", airlineId), ("category", category.id)), fullLoad)
  }
  
  def loadAlertsByCategory(category : AlertCategory.Value, fullLoad : Boolean = false) = {
    loadAlertsByCriteria(List(("category", category.id)), fullLoad)
  }

  def loadAlertsByCategoryAndTargetIds(category : AlertCategory.Value, targetIds : List[Int], fullLoad : Boolean = false) : List[Alert] = {
    if (targetIds.isEmpty) {
      List.empty
    } else {
      var queryString = "SELECT * FROM " + ALERT_TABLE + " WHERE category = ? AND target_id IN ("
      for (i <- 0 until targetIds.size - 1) {
        queryString += "?,"
      }
      queryString += "?)"
      val parameters = List(category.id) ++ targetIds
      loadAlertsByQueryString(queryString, parameters, fullLoad)
    }
  }
  
  def loadAlertsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = "SELECT * FROM " + ALERT_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadAlertsByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadAlertsByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[Alert] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val logs = ListBuffer[Alert]()
        
        
        val airlines = Map[Int, Airline]()
        
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, fullLoad).getOrElse(Airline.fromId(airlineId)))
          val message = resultSet.getString("message")
          val category = AlertCategory(resultSet.getInt("category"))
          val targetIdObject = resultSet.getObject("target_id")
          val targetId = if (targetIdObject == null) None else Some(targetIdObject.asInstanceOf[Int]) 
          val cycle = resultSet.getInt("cycle")
          val duration = resultSet.getInt("duration")
          val id =  resultSet.getInt("id")
          logs += Alert(airline, message, category, targetId, cycle, duration, id)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        logs.toList
      } finally {
        connection.close()
      }
  }
  
  def deleteAlertsBeforeCycle(cutoffCycle : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + ALERT_TABLE + " WHERE cycle < ?"
      
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