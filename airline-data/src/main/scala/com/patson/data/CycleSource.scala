package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model.Airline
import com.patson.model.AirlineInfo

object CycleSource {
  def loadCycle() = {
    val connection = Meta.getConnection() 
    try {  
      var queryString = "SELECT cycle FROM " + CYCLE_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      val resultSet = preparedStatement.executeQuery()
      val cycle = if (resultSet.next()) { resultSet.getInt("cycle") } else 0
      
      resultSet.close()
      preparedStatement.close()
      cycle
    } finally {
      connection.close()
    }
  }
  
  def setCycle(cycle : Int) = {
    val connection = Meta.getConnection() 
    
    try {
      connection.setAutoCommit(false)
      var queryString = "DELETE FROM " + CYCLE_TABLE
      val deleteStatement = connection.prepareStatement(queryString)
      deleteStatement.executeUpdate()
      deleteStatement.close()
      
      val insertStatement = connection.prepareStatement("INSERT INTO " + CYCLE_TABLE + "(cycle) VALUES(?)");
      insertStatement.setInt(1, cycle)
      insertStatement.executeUpdate()
      insertStatement.close()
      
      connection.commit()
    } finally {
      connection.close()
    }
  }
}