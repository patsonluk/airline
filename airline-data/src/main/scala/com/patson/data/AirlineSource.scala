package com.patson.data

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import com.patson.data.Constants._
import com.patson.model._
import com.patson.MainSimulation
import java.sql.Statement
import java.io.ByteArrayInputStream
import java.sql.Blob

import com.patson.util.{AirlineCache, AirportCache}


object AirlineSource {
  private[this] val BASE_QUERY = "SELECT a.id AS id, a.name AS name, a.is_generated AS is_generated, ai.* FROM " + AIRLINE_TABLE + " a JOIN " + AIRLINE_INFO_TABLE + " ai ON a.id = ai.airline "
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
      loadAirlinesByQueryString(queryString, criteria.map(_._2), fullLoad)
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
          val airline = Airline(resultSet.getString("name"), isGenerated = resultSet.getBoolean("is_generated"))
          airline.id = resultSet.getInt("id")
          airline.setBalance(resultSet.getLong("balance"))
          airline.setReputation(resultSet.getDouble("reputation"))
          airline.setCurrentServiceQuality(resultSet.getDouble("service_quality"))
          airline.setTargetServiceQuality(resultSet.getInt("target_service_quality"))
          airline.setMaintenanceQuality(resultSet.getDouble("maintenance_quality"))
          airline.setAirlineCode(resultSet.getString("airline_code"))
          val countryCode = resultSet.getString("country_code")
          if (countryCode != null) {
            airline.setCountryCode(countryCode)
          }
          airline.setSkipTutorial(resultSet.getBoolean("skip_tutorial"))
          airline.setInitialized(resultSet.getBoolean("initialized"))
          
          airlines += airline
        }
        
        if (fullLoad) {
          val airlineBases : scala.collection.immutable.Map[Int, List[AirlineBase]] = loadAirlineBasesByAirlines(airlines.toList).groupBy(_.airline.id)
          airlines.foreach { airline =>
            airline.setBases(airlineBases.getOrElse(airline.id, List.empty))
          }
          
          val allianceMembers = AllianceSource.loadAllianceMemberByAirlines(airlines.toList)
          airlines.foreach { airline =>
            allianceMembers.get(airline) match { //i don't like foreach or fold... hard to read
              case Some(allianceMember) => airline.setAllianceId(allianceMember.allianceId)
              case None => //do nothing
            }
          }
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
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_TABLE + "(name, is_generated) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
          
      airlines.foreach { 
        airline =>
          preparedStatement.setString(1, airline.name)
          preparedStatement.setBoolean(2, airline.isGenerated)
          preparedStatement.executeUpdate()
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            println("Id is " + generatedId)
            airline.id = generatedId
            
            //insert airline info too
            val infoStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_INFO_TABLE + "(airline, balance, service_quality, target_service_quality, maintenance_quality, reputation, country_code, airline_code) VALUES(?,?,?,?,?,?,?,?)")
            infoStatement.setInt(1, airline.id)
            infoStatement.setLong(2, airline.getBalance())
            infoStatement.setDouble(3, airline.getCurrentServiceQuality())
            infoStatement.setInt(4, airline.getTargetServiceQuality())
            infoStatement.setDouble(5, airline.getMaintenanceQuality())
            infoStatement.setDouble(6, airline.getReputation())
            infoStatement.setString(7, airline.getCountryCode().getOrElse(null))
            infoStatement.setString(8, airline.getAirlineCode())
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
          AirlineCache.invalidateAirline(airlineId)
	      } finally {
  	      connection.close()
	      }
	    }
	  }

  def adjustAirlineReputation(airlineId : Int, delta : Double) = {
    this.synchronized {
      val connection = Meta.getConnection()
      try {
        val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET reputation = reputation + ? WHERE airline = ?")
        updateStatement.setDouble(1, delta)
        updateStatement.setInt(2, airlineId)
        updateStatement.executeUpdate()
        updateStatement.close()
        AirlineCache.invalidateAirline(airlineId)
      } finally {
        connection.close()
      }
    }
  }
  
  
  def saveAirlineInfo(airline : Airline, updateBalance : Boolean = true) = {
    this.synchronized {
      val connection = Meta.getConnection()
      var query = "UPDATE " + AIRLINE_INFO_TABLE + " SET "
      if (updateBalance) {
        query += "balance = ?, "
      }
      query += "service_quality = ?, target_service_quality = ?, maintenance_quality = ?, reputation = ?, country_code = ?, airline_code = ?, skip_tutorial = ?, initialized = ?  WHERE airline = ?"
      
      try {
        val updateStatement = connection.prepareStatement(query)
        var index = 0
          
        if (updateBalance) {
          index += 1
          updateStatement.setLong(index, airline.getBalance())
        }
        index += 1
        updateStatement.setDouble(index, airline.getCurrentServiceQuality())
        index += 1
        updateStatement.setInt(index, airline.getTargetServiceQuality())
        index += 1
        updateStatement.setDouble(index, airline.getMaintenanceQuality())
        index += 1
        updateStatement.setDouble(index, airline.getReputation())
        index += 1
        updateStatement.setString(index, airline.getCountryCode().getOrElse(null))
        index += 1
        updateStatement.setString(index, airline.getAirlineCode())
        index += 1
        updateStatement.setBoolean(index, airline.isSkipTutorial)
        index += 1
        updateStatement.setBoolean(index, airline.isInitialized)
        index += 1

        updateStatement.setInt(index, airline.id)
        updateStatement.executeUpdate()
        updateStatement.close()
        AirlineCache.invalidateAirline(airline.id)
      } finally {
        connection.close()
      }
    }
  }
  
  def saveAirlineCode(airlineId : Int, airlineCode : String) = {
    this.synchronized {
      val connection = Meta.getConnection()
      try {
        val updateStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET airline_code = ? WHERE airline = ?")
        updateStatement.setString(1, airlineCode)
        updateStatement.setInt(2, airlineId)
        updateStatement.executeUpdate()
        updateStatement.close()

        AirlineCache.invalidateAirline(airlineId)
      } finally {
        connection.close()
      }
    }
  }
  
  def saveAirlinesInfo(airlines : List[Airline]) = {
    this.synchronized {
      val connection = Meta.getConnection()
      
      var query = "UPDATE " + AIRLINE_INFO_TABLE + " SET "
      query += "service_quality = ?, target_service_quality = ?, maintenance_quality = ?, reputation = ?  WHERE airline = ?"
      
      
      try {
        connection.setAutoCommit(false)
        val updateStatement = connection.prepareStatement(query)
          
        airlines.foreach { airline =>
          var index = 0
          
          index += 1
          updateStatement.setDouble(index, airline.getCurrentServiceQuality())
          index += 1
          updateStatement.setInt(index, airline.getTargetServiceQuality())
          index += 1
          updateStatement.setDouble(index, airline.getMaintenanceQuality())
          index += 1
          updateStatement.setDouble(index, airline.getReputation())
          index += 1
          updateStatement.setInt(index, airline.id)
          
          //updateStatement.executeUpdate()
          updateStatement.addBatch()
          AirlineCache.invalidateAirline(airline.id)
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
    AirlineCache.invalidateAirline(airlineId)
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
  
  def loadAirlineBasesByAirlines(airlines : scala.collection.immutable.List[Airline]) : List[AirlineBase] = {
    if (airlines.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder("SELECT * FROM " + AIRLINE_BASE_TABLE + " where airline IN (");
      for (i <- 0 until airlines.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadAirlineBasesByQueryString(queryString.toString(), airlines.map(_.id), airlines = collection.mutable.Map(airlines.map(airline => (airline.id, airline)).toMap.toSeq: _*))
    }
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
  
  /**
   * Provide the airlines map for quicker load
   */
  def loadAirlineBasesByCriteria(criteria : List[(String, Any)], airlines : Map[Int, Airline] = Map()) : List[AirlineBase] = {
    var queryString = "SELECT * FROM " + AIRLINE_BASE_TABLE
        
    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadAirlineBasesByQueryString(queryString, criteria.map(_._2), airlines)
  }
  
  def loadAirlineBasesByQueryString(queryString : String, parameters : List[Any], airlines : Map[Int, Airline] = Map()) : List[AirlineBase] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        
        
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val bases = new ListBuffer[AirlineBase]()
        
        val airportIds = scala.collection.mutable.Set[Int]()
        while (resultSet.next()) {
          airportIds.add(resultSet.getInt("airport"))
        }
        
        val airports = AirportSource.loadAirportsByIds(airportIds.toList, false).map(airport => (airport.id, airport)).toMap
        
        resultSet.beforeFirst()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          //val airport = Airport.fromId(resultSet.getInt("airport"))
          val airportId = resultSet.getInt("airport")
          val airport = airports(airportId)
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

      AirlineCache.invalidateAirline(airlineBase.airline.id)
      AirportCache.invalidateAirport(airlineBase.airport.id)
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
      val deletingBases = loadAirlineBasesByCriteria(criteria)

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
      deletingBases.foreach{ base =>
        AirlineCache.invalidateAirline(base.airline.id)
        AirportCache.invalidateAirport(base.airport.id)
        println(s"Purged from cache base record $base")
      }

      println("Deleted " + deletedCount + " airline base records")
      deletedCount

    } finally {
      connection.close()
    }
      
  }
  
  //
  def loadAllLounges() : List[Lounge] = {
    loadLoungesByCriteria(List())
  }
  
  
  def loadLoungesByAirportId(airportId : Int) : List[Lounge] = {
    loadLoungesByCriteria(List(("airport", airportId)))
  }
   
  def loadLoungesByAirport(airport : Airport) : List[Lounge] = {
    loadLoungesByCriteria(List(("airport", airport.id)), Map(airport.id -> airport))
  }
  
  def loadLoungesByAirline(airlineId : Int) : List[Lounge] = {
    loadLoungesByCriteria(List(("airline", airlineId)))
  }
  
  
  def loadLoungesByCountryCode(countryCode : String) : List[Lounge] = {
    loadLoungesByCriteria(List(("country", countryCode)))
  }
  
  def loadLoungeByAirlineAndAirport(airlineId : Int, airportId : Int) : Option[Lounge] = {
    val result = loadLoungesByCriteria(List(("airline", airlineId), ("airport", airportId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }

  def loadLoungesByCriteria(criteria : List[(String, Any)], airports : Map[Int, Airport] = Map[Int, Airport]()) : List[Lounge] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        var queryString = "SELECT * FROM " + LOUNGE_TABLE
        
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
        
        val lounges = new ListBuffer[Lounge]()
        
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          AllianceSource.loadAllianceMemberByAirline(airline).foreach { member =>
            airline.setAllianceId(member.allianceId)
          }
          
          //val airport = Airport.fromId(resultSet.getInt("airport"))
          val airportId = resultSet.getInt("airport")
          val airport = airports.getOrElseUpdate(airportId, AirportCache.getAirport(airportId, false).get)
          val name = resultSet.getString("name")
          val level = resultSet.getInt("level")
          val foundedCycle = resultSet.getInt("founded_cycle")
          val status = resultSet.getString("status")
          
          
          lounges += Lounge(airline, airline.getAllianceId(), airport, name, level, LoungeStatus.withName(status), foundedCycle)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        lounges.toList
      } finally {
        connection.close()
      }
  }
  
  
  //case class AirlineBase(airline : Airline, airport : Airport, scale : Int, headQuarter : Boolean = false, var id : Int = 0) extends IdObject
  def saveLounge(lounge : Lounge) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + LOUNGE_TABLE + "(airline, airport, name, level, status, founded_cycle) VALUES(?, ?, ?, ?, ?, ?)")
          
      preparedStatement.setInt(1, lounge.airline.id)
      preparedStatement.setInt(2, lounge.airport.id)
      preparedStatement.setString(3, lounge.name)
      preparedStatement.setInt(4, lounge.level)
      preparedStatement.setString(5, lounge.status.toString())
      preparedStatement.setInt(6, lounge.foundedCycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()

      AirlineCache.invalidateAirline(lounge.airline.id)
      AirportCache.invalidateAirport(lounge.airport.id)
    } finally {
      connection.close()
    }
  }
  
  def deleteLounge(lounge : Lounge) = {
    deleteLoungeByCriteria(List(("airline", lounge.airline.id), ("airport", lounge.airport.id)))
    AirlineCache.invalidateAirline(lounge.airline.id)
    AirportCache.invalidateAirport(lounge.airport.id)
  }
  
  def deleteLoungeByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + LOUNGE_TABLE
      
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
      println("Deleted " + deletedCount + " lounge records")
      deletedCount
      
    } finally {
      connection.close()
    }
      
  }

  def loadShuttleServiceByAirlineAndAirport(airlineId : Int, airportId : Int) : Option[ShuttleService] = {
    val result = loadShuttleServiceByCriteria(List(("airline", airlineId), ("airport", airportId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }

  def loadShuttleServiceByCriteria(criteria : List[(String, Any)], airports : Map[Int, Airport] = Map[Int, Airport]()) : List[ShuttleService] = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "SELECT * FROM " + SHUTTLE_SERVICE_TABLE

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

      val services = new ListBuffer[ShuttleService]()

      val airlines = Map[Int, Airline]()
      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
        AllianceSource.loadAllianceMemberByAirline(airline).foreach { member =>
          airline.setAllianceId(member.allianceId)
        }

        //val airport = Airport.fromId(resultSet.getInt("airport"))
        val airportId = resultSet.getInt("airport")
        val airport = airports.getOrElseUpdate(airportId, AirportCache.getAirport(airportId, false).get)
        val name = resultSet.getString("name")
        val level = resultSet.getInt("level")
        val foundedCycle = resultSet.getInt("founded_cycle")


        services += ShuttleService(airline, airline.getAllianceId(), airport, name, level, foundedCycle)
      }

      resultSet.close()
      preparedStatement.close()

      services.toList
    } finally {
      connection.close()
    }
  }

  //case class AirlineBase(airline : Airline, airport : Airport, scale : Int, headQuarter : Boolean = false, var id : Int = 0) extends IdObject
  def saveShuttleService(shuttleService : ShuttleService) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + SHUTTLE_SERVICE_TABLE + "(airline, airport, name, level, founded_cycle) VALUES(?, ?, ?, ?, ?)")

      preparedStatement.setInt(1, shuttleService.airline.id)
      preparedStatement.setInt(2, shuttleService.airport.id)
      preparedStatement.setString(3, shuttleService.name)
      preparedStatement.setInt(4, shuttleService.level)
      preparedStatement.setInt(5, shuttleService.foundedCycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()

      AirlineCache.invalidateAirline(shuttleService.airline.id)
      AirportCache.invalidateAirport(shuttleService.airport.id)
    } finally {
      connection.close()
    }
  }

  def deleteShuttleService(shuttleService : ShuttleService) = {
    deleteShuttleServiceByCriteria(List(("airline", shuttleService.airline.id), ("airport", shuttleService.airport.id)))
    AirlineCache.invalidateAirline(shuttleService.airline.id)
    AirportCache.invalidateAirport(shuttleService.airport.id)
  }

  def deleteShuttleServiceByCriteria(criteria : List[(String, Any)]) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + SHUTTLE_SERVICE_TABLE

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
      println("Deleted " + deletedCount + " shuttleService records")
      deletedCount

    } finally {
      connection.close()
    }

  }
  
  
  def saveTransaction(transaction : AirlineTransaction) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_TRANSACTION_TABLE + " VALUES(?, ?, ?, ?)")
        preparedStatement.setInt(1, transaction.airlineId)
        preparedStatement.setInt(2, transaction.transactionType.id)
        preparedStatement.setDouble(3, transaction.amount)
        //cannot use MainSimulation.currentWeek as this could be called from other projects apart from simulation
        preparedStatement.setInt(4,CycleSource.loadCycle())
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def loadTransactions(cycle : Int) : List[AirlineTransaction] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement("SELECT * FROM " + AIRLINE_TRANSACTION_TABLE + " WHERE cycle = ?")
        
        preparedStatement.setInt(1, cycle)
        
        val resultSet = preparedStatement.executeQuery()
        
        val transactions = new ListBuffer[AirlineTransaction]()
        
        while (resultSet.next()) {
          transactions += AirlineTransaction(resultSet.getInt("airline"), TransactionType(resultSet.getInt("transaction_type")), resultSet.getLong("amount"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        transactions.toList
      } finally {
        connection.close()
      }
  }
  
  def deleteTransactions(cycleAndBefore : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_TRANSACTION_TABLE + " WHERE cycle <= ?")
        preparedStatement.setInt(1, cycleAndBefore)
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  
  def saveCashFlowItem(cashFlowItem : AirlineCashFlowItem) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_CASH_FLOW_ITEM_TABLE + " VALUES(?, ?, ?, ?)")
        preparedStatement.setInt(1, cashFlowItem.airlineId)
        preparedStatement.setInt(2, cashFlowItem.cashFlowType.id)
        preparedStatement.setDouble(3, cashFlowItem.amount)
        //cannot use MainSimulation.currentWeek as this could be called from other projects apart from simulation
        preparedStatement.setInt(4,CycleSource.loadCycle())
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def loadCashFlowItems(cycle : Int) : List[AirlineCashFlowItem] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement("SELECT * FROM " + AIRLINE_CASH_FLOW_ITEM_TABLE + " WHERE cycle = ?")
        
        preparedStatement.setInt(1, cycle)
        
        val resultSet = preparedStatement.executeQuery()
        
        val transactions = new ListBuffer[AirlineCashFlowItem]()
        
        while (resultSet.next()) {
          transactions += AirlineCashFlowItem(resultSet.getInt("airline"), CashFlowType(resultSet.getInt("cash_flow_type")), resultSet.getLong("amount"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        transactions.toList
      } finally {
        connection.close()
      }
  }
  
  def deleteCashFlowItems(cycleAndBefore : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_CASH_FLOW_ITEM_TABLE + " WHERE cycle <= ?")
        preparedStatement.setInt(1, cycleAndBefore)
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
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
  
  def saveLogo(airlineId : Int, airlineLogo : Array[Byte]) = {
    val connection = Meta.getConnection()
    val logoStream = new ByteArrayInputStream(airlineLogo)
    try {    
        val preparedStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_LOGO_TABLE + " VALUES(?, ?)")
        preparedStatement.setInt(1, airlineId)
        preparedStatement.setBlob(2, logoStream)
        preparedStatement.executeUpdate()
        
        preparedStatement.close()

    } finally {
      connection.close()
      logoStream.close()
    }
  }
  
  def loadLogos() : scala.collection.immutable.Map[Int, Array[Byte]] = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("SELECT * FROM " + AIRLINE_LOGO_TABLE)
        val resultSet = preparedStatement.executeQuery()
        val logos = new ListBuffer[(Int, Array[Byte])]()
        
        while (resultSet.next()) {
          val blob = resultSet.getBlob("logo")
          logos += Tuple2(resultSet.getInt("airline"), blob.getBytes(1, blob.length.toInt))
          blob.free()
        }
        
        resultSet.close()
        preparedStatement.close()
        
        logos.toMap
    } finally {
      connection.close()
    }
  }

  def saveLivery(airlineId : Int, livery : Array[Byte]) = {
    val connection = Meta.getConnection()
    val stream = new ByteArrayInputStream(livery)
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_LIVERY_TABLE + " VALUES(?, ?)")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.setBlob(2, stream)
      preparedStatement.executeUpdate()

      preparedStatement.close()

    } finally {
      connection.close()
      stream.close()
    }
  }

  def deleteLivery(airlineId : Int) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRLINE_LIVERY_TABLE WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.executeUpdate()

      preparedStatement.close()

    } finally {
      connection.close()
    }
  }

  def loadLivery(airlineId : Int) : Option[Array[Byte]] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $AIRLINE_LIVERY_TABLE WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)
      val resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        val blob = resultSet.getBlob("livery")
        val result = Some(blob.getBytes(1, blob.length.toInt))
        blob.free()
        result
      } else {
        None
      }
    } finally {
      connection.close()
    }
  }

  def saveSlogan(airlineId : Int, slogan : String) = {
    val connection = Meta.getConnection()

    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_SLOGAN_TABLE + " VALUES(?, ?)")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.setString(2, slogan)
      preparedStatement.executeUpdate()

      preparedStatement.close()

    } finally {
      connection.close()
    }
  }


  def loadSlogan(airlineId : Int) : Option[String] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $AIRLINE_SLOGAN_TABLE WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)
      val resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        Some(resultSet.getString("slogan"))
      } else {
        None
      }
    } finally {
      connection.close()
    }
  }
  
  def saveColor(airlineId : Int, color : String) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("UPDATE " + AIRLINE_INFO_TABLE + " SET color = ? WHERE airline = ?")
        preparedStatement.setString(1, color)
        preparedStatement.setInt(2, airlineId)
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
        AirlineCache.invalidateAirline(airlineId)
    } finally {
      connection.close()
    }
  }
  
  def getColors() = {
   val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("SELECT airline, color FROM " + AIRLINE_INFO_TABLE + " WHERE color IS NOT NULL")
        val resultSet = preparedStatement.executeQuery()
        val colors = scala.collection.mutable.Map[Int, String]()
        
        while (resultSet.next()) {
          colors.put(resultSet.getInt("airline"), resultSet.getString("color"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        colors.toMap
    } finally {
      connection.close()
    }
  }
  
  def deleteAirplaneRenewal(airlineId : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRPLANE_RENEWAL_TABLE + " WHERE airline = ?")
        preparedStatement.setInt(1, airlineId)
        
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def saveAirplaneRenewal(airlineId : Int, threshold : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("REPLACE INTO " + AIRPLANE_RENEWAL_TABLE + "(airline, threshold) VALUES(?, ?)")
        preparedStatement.setInt(1, airlineId)
        preparedStatement.setInt(2, threshold)
                
        preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def loadAirplaneRenewal(airlineId : Int) : Option[Int] = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("SELECT threshold FROM " + AIRPLANE_RENEWAL_TABLE + " WHERE airline = ?")
        preparedStatement.setInt(1, airlineId)
        val resultSet = preparedStatement.executeQuery()
        val result : Option[Int] = 
          if (resultSet.next()) {
            Some(resultSet.getInt("threshold"))
          } else {
            None
          }
        
        resultSet.close()
        preparedStatement.close()
        
        result
    } finally {
      connection.close()
    }
  }
  
  def loadAirplaneRenewals() : scala.collection.immutable.Map[Int, Int] = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("SELECT * FROM " + AIRPLANE_RENEWAL_TABLE)
        
        val resultSet = preparedStatement.executeQuery()
        val result : Map[Int, Int] = Map[Int, Int]() 
          while (resultSet.next()) {
            result.put(resultSet.getInt("airline"), resultSet.getInt("threshold"))
          }
        
        resultSet.close()
        preparedStatement.close()
        
        result.toMap
    } finally {
      connection.close()
    }
  }

  def loadReputationBreakdowns(airlineId : Int) = {
    val connection = Meta.getConnection()
    try {

      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $AIRLINE_REPUTATION_BREAKDOWN WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)

      val result = ListBuffer[ReputationBreakdown]()
      val resultSet = preparedStatement.executeQuery()
      while (resultSet.next()) {
        val breakdown = ReputationBreakdown(
          ReputationType.withName(resultSet.getString("reputation_type")),
          resultSet.getDouble("value")
        )
        result.append(breakdown)
      }
      resultSet.close()
      preparedStatement.close()
      ReputationBreakdowns(result.toList)
    } finally {
      connection.close()
    }
  }

  def deleteReputationBreakdowns(airlineId : Int) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRLINE_REPUTATION_BREAKDOWN WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def updateReputationBreakdowns(airlineId : Int, breakdowns : ReputationBreakdowns): Unit = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRLINE_REPUTATION_BREAKDOWN(airline, reputation_type, value) VALUES(?,?,?)")
      breakdowns.breakdowns.foreach { breakdown =>
        preparedStatement.setInt(1, airlineId)
        preparedStatement.setString(2, breakdown.reputationType.toString)
        preparedStatement.setDouble(3, breakdown.value)
        preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
}