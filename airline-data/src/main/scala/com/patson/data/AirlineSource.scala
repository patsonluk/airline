package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model.Airline
import com.patson.model.AirlineInfo

object AirlineSource {
  def loadAllAirlines(fullLoad : Boolean = false) = {
      loadAirlinesByCriteria(List.empty, fullLoad)
  }
  
  def loadAirlinesByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      
      var queryString = "SELECT id, name FROM " + AIRLINE_TABLE + " a"
      if (fullLoad) {
        queryString = "SELECT a.id AS id, a.name AS name, ai.balance AS balance FROM " + AIRLINE_TABLE + " a JOIN " + AIRLINE_INFO_TABLE + " ai ON a.id = ai.airline "
      }
      
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }
      
      
      val resultSet = preparedStatement.executeQuery()
      
      val airlines = new ListBuffer[Airline]()
      while (resultSet.next()) {
        val airline = Airline(resultSet.getString("name"))
        airline.id = resultSet.getInt("id")
        if (fullLoad) {
          airline.setBalance(resultSet.getLong("balance"))
        }
        
        airlines += airline
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      airlines.toList
  }
  
  
  def loadAirlineById(id : Int, fullLoad : Boolean = false) = {
      val result = loadAirlinesByCriteria(List(("id", id)), fullLoad)
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  
  def saveAirlines(airlines : List[Airline]) = {
    val connection = Meta.getConnection()
    
    val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_TABLE + "(name) VALUES(?)")
        
    airlines.foreach { 
      airline =>
        preparedStatement.setString(1, airline.name)
        preparedStatement.executeUpdate()
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          println("Id is " + generatedId)
          airline.id = generatedId
          
          //insert airline info too
          val infoStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_INFO_TABLE + "(airline, balance) VALUES(?, ?)")
          infoStatement.setInt(1, airline.id)
          infoStatement.setLong(2, airline.airlineInfo.balance)
          infoStatement.executeUpdate()
        } 
    }
    
    preparedStatement.close()
    connection.close()
    
    airlines
  }
  
  def updateAirlineInfo(airlines : List[Airline]) = {
    val connection = Meta.getConnection()  
    airlines.foreach { airline => 
      val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET balance = ? WHERE airline = ?")
      updateStatement.setLong(1, airline.airlineInfo.balance)
      updateStatement.setInt(2, airline.id)
      updateStatement.executeUpdate()
      updateStatement.close()
    }
          
    connection.close()
  }
  
  
  def deleteAirline(airlineId : Int) = {
    deleteAirlinesByCriteria(List(("id", airlineId)))
  }
  
  def deleteAllAirlines() = {
    deleteAirlinesByCriteria(List.empty)
  }
  
  def deleteAirlinesByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
      var queryString = "DELETE FROM " + AIRLINE_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      connection.close()
      
      println("Deleted " + deletedCount + " airline records")
      deletedCount
  }
}