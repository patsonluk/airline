package com.patson.data

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import com.patson.data.Constants._
import com.patson.model._
import java.sql.Statement

object AirlineSource {
  private[this] val BASE_QUERY = "SELECT a.id AS id, a.name AS name, ai.* FROM " + AIRLINE_TABLE + " a JOIN " + AIRLINE_INFO_TABLE + " ai ON a.id = ai.airline "
  def loadAllAirlines(fullLoad : Boolean = false) = {
      loadAirlinesByCriteria(List.empty, fullLoad)
  }
  
  def loadAirlinesByIds(ids : List[Int], fullLoad : Boolean = false) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(BASE_QUERY + " where id IN (");
      for (i <- 0 until ids.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadAirlinesByQueryString(queryString.toString(), ids, fullLoad)
    }
  }
  
  def loadAirlinesByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = BASE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadAirlinesByQueryString(queryString, criteria.map(_._2))
  }
  
  private def loadAirlinesByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[Airline] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val airlines = new ListBuffer[Airline]()
        
        while (resultSet.next()) {
          val airline = Airline(resultSet.getString("name"))
          airline.id = resultSet.getInt("id")
          airline.setBalance(resultSet.getLong("balance"))
          airline.setReputation(resultSet.getDouble("reputation"))
          airline.setServiceQuality(resultSet.getDouble("service_quality"))
          airline.setServiceFunding(resultSet.getInt("service_funding"))
          airline.setMaintainenceQuality(resultSet.getDouble("maintenance_quality"))
          
          airlines += airline
        }
        
        resultSet.close()
        preparedStatement.close()
        
        airlines.toList
      } finally {
        connection.close()
      }
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
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_TABLE + "(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)
          
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
            val infoStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_INFO_TABLE + "(airline, balance, service_quality, service_funding, maintenance_quality, reputation) VALUES(?,?,?,?,?,?)")
            infoStatement.setInt(1, airline.id)
            infoStatement.setLong(2, airline.getBalance())
            infoStatement.setDouble(3, airline.getServiceQuality())
            infoStatement.setInt(4, airline.getServiceFunding())
            infoStatement.setDouble(5, airline.getMaintenanceQuality())
            infoStatement.setDouble(6, airline.getReputation())
            infoStatement.executeUpdate()
          } 
      }
      
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
    
    airlines
  }

  def adjustAirlineBalance(airlineId : Int, delta : Long) = {
	    this.synchronized {
	      val connection = Meta.getConnection()
	      try {
  	      val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET balance = balance + ? WHERE airline = ?")
  	      updateStatement.setLong(1, delta)
  	      updateStatement.setInt(2, airlineId)
  	      updateStatement.executeUpdate()
  	      updateStatement.close()
	      } finally {
  	      connection.close()
	      }
	    }
	  }
  
  
  def saveAirlineInfo(airline : Airline) = {
    this.synchronized {
      val connection = Meta.getConnection()
      try {
        val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET balance = ?, service_quality = ?, service_funding = ?, maintenance_quality = ?, reputation = ? WHERE airline = ?")
        updateStatement.setLong(1, airline.getBalance())
        updateStatement.setDouble(2, airline.getServiceQuality())
        updateStatement.setInt(3, airline.getServiceFunding())
        updateStatement.setDouble(4, airline.getMaintenanceQuality())
        updateStatement.setDouble(5, airline.getReputation())
        updateStatement.setInt(6, airline.id)
        updateStatement.executeUpdate()
        updateStatement.close()
      } finally {
        connection.close()
      }
    }
  }
  
  def saveAirlineInfo(airlines : List[Airline]) = {
    this.synchronized {
      val connection = Meta.getConnection()
      
      try {
        connection.setAutoCommit(false)
        val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET balance = ?, service_quality = ?, service_funding = ?, maintenance_quality = ?, reputation = ? WHERE airline = ?")
          
        airlines.foreach { airline =>
          updateStatement.setLong(1, airline.getBalance())
          updateStatement.setDouble(2, airline.getServiceQuality())
          updateStatement.setInt(3, airline.getServiceFunding())
          updateStatement.setDouble(4, airline.getMaintenanceQuality())
          updateStatement.setDouble(5, airline.getReputation())
          updateStatement.setInt(6, airline.id)
          //updateStatement.executeUpdate()
          updateStatement.addBatch()
        }
        updateStatement.executeBatch()
        updateStatement.close()
        connection.commit()
      } finally {
        connection.close()
      }
    }
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
    try {
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
      println("Deleted " + deletedCount + " airline records")
      deletedCount
      
    } finally {
      connection.close()
    }
      
  }
  def loadAirlineBasesByAirport(airportId : Int) : List[AirlineBase] = {
    loadAirlineBasesByCriteria(List(("airport", airportId)))
  }
  
  def loadAirlineBasesByAirline(airlineId : Int) : List[AirlineBase] = {
    loadAirlineBasesByCriteria(List(("airline", airlineId)))
  }
  
  
  def loadAirlineBasesByCountryCode(countryCode : String) : List[AirlineBase] = {
    loadAirlineBasesByCriteria(List(("country", countryCode)))
  }
  
  def loadAirlineHeadquarter(airlineId : Int) : Option[AirlineBase] = {
    val result = loadAirlineBasesByCriteria(List(("airline", airlineId), ("headquarter", true)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def loadAirlineBaseByAirlineAndAirport(airlineId : Int, airportId : Int) : Option[AirlineBase] = {
    val result = loadAirlineBasesByCriteria(List(("airline", airlineId), ("airport", airportId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def loadAirlineBasesByCriteria(criteria : List[(String, Any)]) : List[AirlineBase] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        var queryString = "SELECT * FROM " + AIRLINE_BASE_TABLE
        
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
        
        val bases = new ListBuffer[AirlineBase]()
        
        val airports = Map[Int, Airport]()
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineSource.loadAirlineById(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          //val airport = Airport.fromId(resultSet.getInt("airport"))
          val airportId = resultSet.getInt("airport")
          val airport = airports.getOrElseUpdate(airportId, AirportSource.loadAirportById(airportId, false).get)
          val scale = resultSet.getInt("scale")
          val foundedCycle = resultSet.getInt("founded_cycle")
          val headquarter = resultSet.getBoolean("headquarter")
          val countryCode = resultSet.getString("country")
          
          bases += AirlineBase(airline, airport, countryCode, scale, foundedCycle, headquarter)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        bases.toList
      } finally {
        connection.close()
      }
  }
  
  
  //case class AirlineBase(airline : Airline, airport : Airport, scale : Int, headQuarter : Boolean = false, var id : Int = 0) extends IdObject
  def saveAirlineBase(airlineBase : AirlineBase) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_BASE_TABLE + "(airline, airport, scale, founded_cycle, headquarter, country) VALUES(?, ?, ?, ?, ?, ?)")
          
      preparedStatement.setInt(1, airlineBase.airline.id)
      preparedStatement.setInt(2, airlineBase.airport.id)
      preparedStatement.setInt(3, airlineBase.scale)
      preparedStatement.setInt(4, airlineBase.foundedCycle)
      preparedStatement.setBoolean(5, airlineBase.headquarter)
      preparedStatement.setString(6, airlineBase.countryCode)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def deleteAirlineBase(airlineBase : AirlineBase) = {
    deleteAirlineBaseByCriteria(List(("airline", airlineBase.airline.id), ("airport", airlineBase.airport.id)))
  }
  
  def deleteAirlineBaseByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + AIRLINE_BASE_TABLE
      
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
      println("Deleted " + deletedCount + " airline base records")
      deletedCount
      
    } finally {
      connection.close()
    }
      
  }
  
  def deleteGeneratedAirlines(fromId : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_TABLE + " WHERE id >= ?")
        preparedStatement.setInt(1, fromId)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
}