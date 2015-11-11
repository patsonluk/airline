package com.patson.data

import scala.collection.mutable.ListBuffer

import com.patson.data.Constants.AIRLINE_TABLE
import com.patson.model.Airline

object AirlineSource {
  def loadAllAirlines() = {
      loadAirlinesByCriteria(List.empty)
  }
  
  def loadAirlinesByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      
      var queryString = "SELECT id, name FROM airline"
      
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
        val airline = Airline( 
          resultSet.getString("name")
          )
        airline.id = resultSet.getInt("id")
        airlines += airline
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      airlines.toList
  }
  
  
  def loadAirlineById(id : Int) = {
      val result = loadAirlinesByCriteria(List(("id", id)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  
  def saveAirlines(airlines : List[Airline]) = {
    val connection = Meta.getConnection()
    
    val preparedStatement = connection.prepareStatement("INSERT INTO airline(name) VALUES(?)")
        
    airlines.foreach { 
      airline =>
        preparedStatement.setString(1, airline.name)
        preparedStatement.executeUpdate()
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          println("Id is " + generatedId)
          airline.id = generatedId
        } 
    }
    
    preparedStatement.close()
    connection.close()
    
    airlines
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