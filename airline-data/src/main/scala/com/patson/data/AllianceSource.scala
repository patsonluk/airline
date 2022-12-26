package com.patson.data

import java.sql.Statement
import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.alliance.AllianceStats
import com.patson.util.{AirlineCache, AllianceCache}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object AllianceSource {
  private[this] val BASE_ALLIANCE_QUERY = "SELECT * FROM " + ALLIANCE_TABLE
  private[this] val BASE_ALLIANCE_MEMBER_QUERY = "SELECT * FROM " + ALLIANCE_MEMBER_TABLE
  private[this] val BASE_ALLIANCE_HISTORY_QUERY = "SELECT * FROM " + ALLIANCE_HISTORY_TABLE
  
  def loadAllAlliances(fullLoad : Boolean = false) = {
      loadAlliancesByCriteria(List.empty, fullLoad)
  }
  
  def loadAllianceById(allianceId : Int, fullLoad : Boolean = false) : Option[Alliance] = {
    val result = loadAlliancesByCriteria(List(("id", allianceId)), fullLoad)
    if (result.isEmpty) {
      None
    } else {
      Some(result.toList.apply(0))
    }
  }
  
  def loadAlliancesByIds(ids : List[Int], fullLoad : Boolean = false) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(BASE_ALLIANCE_QUERY + " where id IN (");
      for (i <- 0 until ids.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadAlliancesByQueryString(queryString.toString(), ids, fullLoad)
    }
  }
  
  def loadAlliancesByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = BASE_ALLIANCE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadAlliancesByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadAlliancesByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[Alliance] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val alliances = ListBuffer[Alliance]()
        
        while (resultSet.next()) {
          val allianceId = resultSet.getInt("id")
          val alliance = Alliance(name = resultSet.getString("name"), creationCycle = resultSet.getInt("creation_cycle"), members = loadAllianceMembersByAllianceId(allianceId, fullLoad), id = allianceId)
          alliances.append(alliance)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        alliances.toList
      } finally {
        connection.close()
      }
  }

  
  
  private def loadAllianceMembersByAllianceId(allianceId : Int, fullLoad : Boolean = false) : List[AllianceMember] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(BASE_ALLIANCE_MEMBER_QUERY + " WHERE alliance = ? ")
        
        preparedStatement.setObject(1, allianceId)
        
        val resultSet = preparedStatement.executeQuery()
        
        val allianceMembers = new ListBuffer[AllianceMember]()
        val airlineIds = new ListBuffer[Int]()
        
        while (resultSet.next()) {
          airlineIds.append(resultSet.getInt("airline"))
        }
        
        //val airlinesById = AirlineSource.loadAirlinesByIds(airlineIds.toList, fullLoad).map( airline => (airline.id , airline)).toMap
        val airlinesById = AirlineCache.getAirlines(airlineIds.toList, fullLoad)
        
        resultSet.beforeFirst()
        while (resultSet.next()) {
          val allianceMember = AllianceMember(allianceId, airlinesById(resultSet.getInt("airline")), AllianceRole.withName(resultSet.getString("role")), joinedCycle = resultSet.getInt("joined_cycle"))
          allianceMembers.append(allianceMember)    
        }
        resultSet.close()
        preparedStatement.close()
        
        allianceMembers.toList
      } finally {
        connection.close()
      }
  }

  def loadAllianceMemberByAirline(airline : Airline) : Option[AllianceMember] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement("SELECT * FROM " + ALLIANCE_MEMBER_TABLE + " WHERE airline = ? ")
        
        preparedStatement.setObject(1, airline.id)
        
        val resultSet = preparedStatement.executeQuery()
        
        val result = if (resultSet.next()) {
          val allianceMember = AllianceMember(resultSet.getInt("alliance"), airline, role = AllianceRole.withName(resultSet.getString("role")), joinedCycle = resultSet.getInt("joined_cycle"))
          Some(allianceMember)
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
  
  def loadAllianceMemberByAirlines(airlines : List[Airline]) : scala.collection.immutable.Map[Airline, AllianceMember] = {
    if (airlines.isEmpty) {
      scala.collection.immutable.Map.empty
    } else {
      val queryString = new StringBuilder("SELECT * FROM " + ALLIANCE_MEMBER_TABLE + " WHERE airline IN (");
      for (i <- 0 until airlines.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")

      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString.toString)

        for (i <- 0 until airlines.size) {
          preparedStatement.setObject(i + 1, airlines(i).id)
        }

        val result = scala.collection.mutable.Map[Airline, AllianceMember]()
        val resultSet = preparedStatement.executeQuery()
        val airlinesMap = airlines.map(airline => (airline.id, airline)).toMap
        while (resultSet.next()) {
          val airline = airlinesMap(resultSet.getInt("airline"))
          val allianceMember = AllianceMember(resultSet.getInt("alliance"), airline = airline, role = AllianceRole.withName(resultSet.getString("role")), joinedCycle = resultSet.getInt("joined_cycle"))
          result.put(airline, allianceMember)
        }

        resultSet.close()
        preparedStatement.close()

        result.toMap
      } finally {
        connection.close()
      }
    }
  }

  
  def loadAllianceHistoryByAirline(airlineId : Int) : List[AllianceHistory] = {
    loadAllianceHistoryByCriteria(List(("airline", airlineId)))
  }
  
  def loadAllianceHistoryByAllianceName(allianceName : String) : List[AllianceHistory] = {
    loadAllianceHistoryByCriteria(List(("alliance_name", allianceName)))
  }
  
  def loadAllianceHistoryByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = BASE_ALLIANCE_HISTORY_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadAllianceHistoryByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadAllianceHistoryByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[AllianceHistory]= {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val allianceHistoryEntries = ListBuffer[AllianceHistory]()
        
        val airlineIds = new ListBuffer[Int]()
        
        while (resultSet.next()) {
          airlineIds.append(resultSet.getInt("airline"))
        }
        
        //val airlinesById = AirlineSource.loadAirlinesByIds(airlineIds.toList, fullLoad).map( airline => (airline.id , airline)).toMap
        val airlinesById = AirlineCache.getAirlines(airlineIds.toList, fullLoad)
        
        resultSet.beforeFirst()
        
        while (resultSet.next()) {
          val allianceHistoryEntry = AllianceHistory(allianceName = resultSet.getString("alliance_name"), airline = airlinesById(resultSet.getInt("airline")), event = AllianceEvent.withName(resultSet.getString("event")), cycle = resultSet.getInt("cycle"))
          allianceHistoryEntries.append(allianceHistoryEntry)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        allianceHistoryEntries.toList
      } finally {
        connection.close()
      }
  }
  
 
  
  def saveAlliance(alliance : Alliance) : Int = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + ALLIANCE_TABLE + "(name, creation_cycle) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
          
      preparedStatement.setString(1, alliance.name)
      preparedStatement.setInt(2, alliance.creationCycle)
      preparedStatement.executeUpdate()
      val generatedKeys = preparedStatement.getGeneratedKeys
      if (generatedKeys.next()) {
        val generatedId = generatedKeys.getInt(1)
        println("Alliance Id is " + generatedId)
        alliance.id = generatedId
      } 
      
      preparedStatement.close()
      alliance.id
    } finally {
      connection.close()
    }
  }
  
//  def updateAlliance(alliance : Alliance) = {
//    val connection = Meta.getConnection()
//    try {
//      val preparedStatement = connection.prepareStatement("UPDATE " + ALLIANCE_TABLE + " SET status = ? WHERE id = ?")
//          
//      preparedStatement.setString(1, alliance.status.toString)
//      preparedStatement.setInt(2, alliance.id)
//      preparedStatement.executeUpdate()
//      preparedStatement.close()
//    } finally {
//      connection.close()
//    }
//  }

  def deleteAlliance(allianceId : Int) = {
    deleteAllianceByCriteria(List(("id", allianceId)))
  }

  
  def deleteAllianceByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {

      var criteriaString = ""
      if (!criteria.isEmpty) {
        criteriaString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          criteriaString += criteria(i)._1 + " = ? AND "
        }
        criteriaString += criteria.last._1 + " = ?"
      }

      val queryString = "SELECT id FROM " + ALLIANCE_TABLE + criteriaString

      val queryStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        queryStatement.setObject(i + 1, criteria(i)._2)
      }
      val resultSet = queryStatement.executeQuery()


      while (resultSet.next()) {
        val allianceId = resultSet.getInt("id")
        AllianceCache.invalidateAlliance(allianceId)
        loadAllianceMembersByAllianceId(allianceId, false).foreach { member =>
          AirlineCache.invalidateAirline(member.airline.id)
        }
      }
      resultSet.close()
      queryStatement.close()


      val deleteString = "DELETE FROM " + ALLIANCE_TABLE + criteriaString
      
      val deleteStatement = connection.prepareStatement(deleteString)
      
      for (i <- 0 until criteria.size) {
        deleteStatement.setObject(i + 1, criteria(i)._2)
      }
      
      val deletedCount = deleteStatement.executeUpdate()
      
      deleteStatement.close()
      println("Deleted " + deletedCount + " airline alliance records")

      deletedCount
      
    } finally {
      connection.close()
    }
  }
  
  def saveAllianceMember(allianceMember : AllianceMember) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + ALLIANCE_MEMBER_TABLE + "(alliance, airline, role, joined_cycle) VALUES(?,?,?,?)")
          
      preparedStatement.setInt(1, allianceMember.allianceId)
      preparedStatement.setInt(2, allianceMember.airline.id)
      preparedStatement.setString(3, allianceMember.role.toString)
      preparedStatement.setInt(4, allianceMember.joinedCycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()

      AllianceCache.invalidateAlliance(allianceMember.allianceId)
      AirlineCache.invalidateAirline(allianceMember.airline.id) //since airline has getAlliance method, not exactly the best design here...
    } finally {
      connection.close()
    }
  }
 

  def deleteAllianceMember(airlineId : Int) = {
    deleteAllianceMemberByCriteria(List(("airline", airlineId)))
  }

  
  def deleteAllianceMemberByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var criteriaString = ""
      
      if (!criteria.isEmpty) {
        criteriaString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          criteriaString += criteria(i)._1 + " = ? AND "
        }
        criteriaString += criteria.last._1 + " = ?"
      }

      val queryString = "SELECT alliance, airline FROM " + ALLIANCE_MEMBER_TABLE + criteriaString

      val queryStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        queryStatement.setObject(i + 1, criteria(i)._2)
      }

      val resultSet = queryStatement.executeQuery()
      while (resultSet.next()) {
        AllianceCache.invalidateAlliance(resultSet.getInt("alliance"))
        AirlineCache.invalidateAirline(resultSet.getInt("airline")) //since airline has getAlliance method, not exactly the best design here...
      }
      resultSet.close()
      queryStatement.close()


      val deleteString = "DELETE FROM " + ALLIANCE_MEMBER_TABLE + criteriaString
      
      val deleteStatement = connection.prepareStatement(deleteString)
      
      for (i <- 0 until criteria.size) {
        deleteStatement.setObject(i + 1, criteria(i)._2)
      }
      
      val deletedCount = deleteStatement.executeUpdate()
      
      deleteStatement.close()
      println("Deleted " + deletedCount + " airline alliance member records")
      deletedCount
      
    } finally {
      connection.close()
    }
  }
  
    
  def saveAllianceHistory(allianceHistory : AllianceHistory) : Int = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + ALLIANCE_HISTORY_TABLE + "(alliance_name, airline, event, cycle) VALUES(?,?,?,?)",  Statement.RETURN_GENERATED_KEYS)
          
      preparedStatement.setString(1, allianceHistory.allianceName)
      preparedStatement.setInt(2, allianceHistory.airline.id)
      preparedStatement.setString(3, allianceHistory.event.toString)
      preparedStatement.setInt(4, allianceHistory.cycle)
      preparedStatement.executeUpdate()
      
      val generatedKeys = preparedStatement.getGeneratedKeys
      generatedKeys.next
      val generatedId = generatedKeys.getInt(1)
        
      preparedStatement.close()
      
      generatedId
    } finally {
      connection.close()
    }
  }
 

  
  
  def deleteAllianceHistoryByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + ALLIANCE_HISTORY_TABLE
      
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
      println("Deleted " + deletedCount + " airline history records")
      deletedCount
    } finally {
      connection.close()
    }
  }

  def saveAllianceStats(stats : List[AllianceStats]) = {
    internalSaveAllianceStat(ALLIANCE_STATS_TABLE, stats)
  }
  def saveAllianceMissionStats(stats : List[AllianceStats]) = {
    internalSaveAllianceStat(ALLIANCE_MISSION_STATS_TABLE, stats)
  }

  def internalSaveAllianceStat(tableName : String, stats : List[AllianceStats]) = {
    val queryString = s"REPLACE INTO $tableName (alliance, cycle, property, value) VALUES(?,?,?,?)";
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      stats.foreach { entry =>
        entry.properties.foreach { case(property, value) =>
          preparedStatement.setInt(1, entry.alliance.id)
          preparedStatement.setInt(2, entry.cycle)
          preparedStatement.setString(3, property)
          preparedStatement.setLong(4, value)
          preparedStatement.executeUpdate()
        }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def deleteAllianceStatsBeforeCutoff(cutoff : Int) = {
    val queryString = s"DELETE FROM $ALLIANCE_STATS_TABLE WHERE cycle < ?";
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, cutoff)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }

  }

  def loadAllianceStatsByCycle(allianceId : Int, cycle : Int) : Option[AllianceStats] = {
    loadAllianceStatsByCriteria(List(("alliance", "=", allianceId), ("cycle", "=", cycle))).headOption
  }

  def loadAllianceStatsByCriteria(criteria : List[(String, String, Any)]) : List[AllianceStats]= {
    internalLoadAllianceStatsByCriteria(ALLIANCE_STATS_TABLE, criteria)
  }

  def loadAllianceMissionStatsByCriteria(criteria : List[(String, String, Any)]) : List[AllianceStats]= {
    internalLoadAllianceStatsByCriteria(ALLIANCE_MISSION_STATS_TABLE, criteria)
  }

  def internalLoadAllianceStatsByCriteria(tableName : String, criteria : List[(String, String, Any)]) : List[AllianceStats]= {
    var queryString = s"SELECT * FROM $tableName";

    val connection = Meta.getConnection()
    try {

      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " " + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + " " + criteria.last._2 + " ? "
      }


      val preparedStatement = connection.prepareStatement(queryString)
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._3)
      }

      val resultSet = preparedStatement.executeQuery()
      val propertiesByAllianceAndCycle = scala.collection.mutable.Map[(Alliance, Int), scala.collection.mutable.Map[String, Long]]()
      while (resultSet.next()) {
        val allianceId = resultSet.getInt("alliance")
        val alliance = AllianceCache.getAlliance(resultSet.getInt("alliance")).getOrElse(Alliance.fromId(allianceId))
        val cycle = resultSet.getInt("cycle")
        val key = (alliance, cycle)

        val propertyKey = resultSet.getString("property")
        val value = resultSet.getLong("value")

        val properties = propertiesByAllianceAndCycle.getOrElseUpdate(key, mutable.HashMap())
        properties.put(propertyKey, value)
      }

      resultSet.close()
      preparedStatement.close()

      propertiesByAllianceAndCycle.map { case ((alliance, cycle), properties) =>
        AllianceStats.buildAllianceStats(alliance, properties.toMap, cycle)
      }.toList
    } finally {
      connection.close()
    }
  }
}