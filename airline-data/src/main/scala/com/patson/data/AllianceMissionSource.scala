package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.{AirlineCache, AirportCache}

import java.sql.Statement
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AllianceMissionSource {
  val blueprintQueryColumns = List("airport", "mission_type", "id")

  def loadAllianceMissionsByAllianceId(allianceId : Int) = {
    loadAllianceMissionsByCriteria(List(("alliance", allianceId)))
  }

  def loadBuiltAllianceMissionsByIds(ids : List[Int]) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $ALLIANCE_MISSION_TABLE where id IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      loadAllianceMissionsByQueryString(queryString.toString(), ids)
    }
  }

  def loadAllianceMissionsByCriteria(criteria : List[(String, Any)]) = {
    var queryString = s"SELECT * FROM $ALLIANCE_MISSION_TABLE"

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadAllianceMissionsByQueryString(queryString, criteria.map(_._2))
  }


  private[this] def loadAllianceMissionsByQueryString(queryString : String, parameters : List[Any]) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()
      val missions = ListBuffer[AllianceMission]()

      val ids = ListBuffer[Int]()
      while (resultSet.next()) {
        val id = resultSet.getInt("id") //same id as blueprint
        ids.append(id)
      }

      val idToBoost = loadAirportBoostsByAssetIds(ids.toList)
      val idToProperties = loadAirportPropertiesByAssetIds(ids.toList)

      val currentCycle = CycleSource.loadCycle()
      resultSet.beforeFirst()
      while (resultSet.next()) {
        val missionType = AllianceMissionType.withName(resultSet.getString("mission_type"))
        val id = resultSet.getInt("id") //same id as blueprint
        val airport = AirportCache.getAirport(resultSet.getInt("airport"), false).get

        val airline = AirlineCache.getAirline(resultSet.getInt("airline"))
        val name = resultSet.getString("name")
        val level = resultSet.getInt("level")
        val completionCycle = resultSet.getInt("completion_cycle")
        val revenue = resultSet.getLong("revenue")
        val expense = resultSet.getLong("expense")
        val roi = resultSet.getDouble("roi")
        val upgradeApplied = resultSet.getBoolean("upgrade_applied")

        missions += AllianceMission.getAllianceMission(id, airport, missionType, airline, name, level, Some(completionCycle), idToBoost.getOrElse(id, List.empty), revenue, expense, roi, upgradeApplied, idToProperties.getOrElse(id, Map.empty), currentCycle)
      }

      missions.toList
    } finally {
      connection.close()
    }
  }

  def loadAllianceMissionsByAirport(airportId : Int, loadedAirport : Option[Airport] = None) = {
    loadAllianceMissionsByBlueprintCriteria(List(("airport", airportId)), loadedAirport)
  }


  private[this] def loadAllianceMissionsByBlueprintCriteria(criteria : List[(String, Any)], loadedAirport : Option[Airport] = None) = {
    val connection = Meta.getConnection()
    try {
      var queryString = s"SELECT * FROM $ALLIANCE_MISSION_BLUEPRINT_TABLE"

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


      val currentCycle = CycleSource.loadCycle()
      val resultSet = preparedStatement.executeQuery()
      val idToAssetBlueprint = mutable.HashMap[Int, AllianceMissionBlueprint]()
      while (resultSet.next()) {
        val missionType = AllianceMissionType.withName(resultSet.getString("mission_type"))
        val id = resultSet.getInt("id") //same id as blueprint
        val airportId = resultSet.getInt("airport")
        val airport =
          loadedAirport match {
            case None => AirportCache.getAirport(airportId, false).get
            case Some(loadedAirport) => loadedAirport
          }

        idToAssetBlueprint.put(id, AllianceMissionBlueprint(airport, missionType, id))
      }

      val idToOwnedAssets = loadBuiltAllianceMissionsByIds(idToAssetBlueprint.keys.toList).map(entry => (entry.id, entry)).toMap

      val result = idToAssetBlueprint.map {
        case (id, blueprint) =>
          idToOwnedAssets.getOrElse(id, AllianceMission.getAllianceMission(blueprint, airline = None, name = "", level = 0, completionCycle = None,  boosts = List.empty, revenue = 0, expense = 0, roi = blueprint.missionType.initRoi, upgradeApplied = false, properties = Map.empty, currentCycle))
      }

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportBoostsByAssetIds(ids : List[Int]) : Map[Int, List[AirportBoost]] = {
    if (ids.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $ALLIANCE_MISSION_BOOST_TABLE where mission IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString.toString)

        for (i <- 0 until ids.size) {
          preparedStatement.setObject(i + 1, ids(i))
        }

        val resultSet = preparedStatement.executeQuery()
        val result = mutable.HashMap[Int, ListBuffer[AirportBoost]]()
        while (resultSet.next()) {
          val boostType = AirportBoostType.withName(resultSet.getString("boost_type"))
          val boost = AirportBoost(boostType, resultSet.getDouble("value"))
          val missionId = resultSet.getInt("mission")
          val boosts = result.getOrElseUpdate(missionId, ListBuffer[AirportBoost]())
          boosts.append(boost)
        }
        result.view.mapValues(_.toList).toMap

      } finally {
        connection.close()
      }
    }
  }

  def loadAirportPropertiesByAssetIds(ids : List[Int]) : Map[Int, Map[String, Long]] = {
    if (ids.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $ALLIANCE_MISSION_PROPERTY_TABLE where mission IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString.toString)

        for (i <- 0 until ids.size) {
          preparedStatement.setObject(i + 1, ids(i))
        }

        val resultSet = preparedStatement.executeQuery()
        val result = mutable.HashMap[Int, mutable.Map[String, Long]]()
        while (resultSet.next()) {
          val missionId = resultSet.getInt("mission")
          val property = result.getOrElseUpdate(missionId, mutable.Map[String, Long]())
          property.put(resultSet.getString("property"), resultSet.getLong("value"))
        }
        result.view.mapValues(_.toMap).toMap

      } finally {
        connection.close()
      }
    }

  }

  
  def saveAllianceMissionBlueprints(blueprints : List[AllianceMissionBlueprint]) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"INSERT INTO $ALLIANCE_MISSION_BLUEPRINT_TABLE(airport, mission_type)  VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
      connection.setAutoCommit(false)

      blueprints.foreach { blueprint =>


        preparedStatement.setInt(1, blueprint.airport.id)
        preparedStatement.setString(2, blueprint.missionType.toString)
        preparedStatement.executeUpdate()

        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          blueprint.id = generatedId
        }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def updateAllianceMission(mission : AllianceMission) = {
    val connection = Meta.getConnection()
    try {
      var purgeStatement = connection.prepareStatement(s"DELETE FROM $ALLIANCE_MISSION_BOOST_TABLE WHERE mission = ?")
      purgeStatement.setInt(1, mission.id)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      purgeStatement = connection.prepareStatement(s"DELETE FROM $ALLIANCE_MISSION_PROPERTY_TABLE WHERE mission = ?")
      purgeStatement.setInt(1, mission.id)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      //var preparedStatement = connection.prepareStatement(s"UPDATE $ALLIANCE_MISSION_TABLE SET airline = ?, name = ?, level = ?, completion_cycle = ?, revenue = ?, expense = ? WHERE id = ?")
      var preparedStatement = connection.prepareStatement(s"INSERT INTO $ALLIANCE_MISSION_TABLE (airline, airport, mission_type, name, level, completion_cycle, revenue, expense, roi, upgrade_applied, id) VALUES(?,?,?,?,?,?,?,?,?,?,?) " +
      " ON DUPLICATE KEY UPDATE name=VALUES(name), level=VALUES(level), completion_cycle=VALUES(completion_cycle), revenue=VALUES(revenue), expense=VALUES(expense), roi=VALUES(roi), upgrade_applied=VALUES(upgrade_applied) ")


      preparedStatement.setInt(1, mission.airline.get.id)
      preparedStatement.setInt(2, mission.blueprint.airport.id)
      preparedStatement.setString(3, mission.missionType.toString)
      preparedStatement.setString(4, mission.name)
      preparedStatement.setInt(5, mission.level)
      preparedStatement.setInt(6, mission.completionCycle.get)
      preparedStatement.setLong(7, mission.revenue)
      preparedStatement.setLong(8, mission.expense)
      preparedStatement.setDouble(9, mission.roi)
      preparedStatement.setBoolean(10, mission.upgradeApplied)
      preparedStatement.setInt(11, mission.id)

      preparedStatement.executeUpdate()
      preparedStatement.close()


      mission.boosts.foreach { boost =>
        preparedStatement = connection.prepareStatement(s"INSERT INTO $ALLIANCE_MISSION_BOOST_TABLE (mission, boost_type, value) VALUES(?,?,?)")
        preparedStatement.setInt(1, mission.id)
        preparedStatement.setString(2, boost.boostType.toString)
        preparedStatement.setDouble(3, boost.value)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      }

      mission.properties.foreach {
        case (property, value) =>
          preparedStatement = connection.prepareStatement(s"INSERT INTO $ALLIANCE_MISSION_PROPERTY_TABLE (mission, property, value) VALUES(?,?,?)")
          preparedStatement.setInt(1, mission.id)
          preparedStatement.setString(2, property)
          preparedStatement.setLong(3, value)
          preparedStatement.executeUpdate()
          preparedStatement.close()
      }

      AirportCache.invalidateAirport(mission.blueprint.airport.id)
    } finally {
      connection.close()
    }
  }

  def deleteAllianceMission(id : Int) = {
    val connection = Meta.getConnection()
    val airportId = loadAllianceMissionByAssetId(id).get.blueprint.airport.id
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $ALLIANCE_MISSION_TABLE WHERE id = ?")

      preparedStatement.setInt(1, id)

      preparedStatement.executeUpdate()

      preparedStatement.close()

      AirportCache.invalidateAirport(airportId)
    } finally {
      connection.close()
    }
  }

  def deleteAllAllianceMissionBlueprints() = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $ALLIANCE_MISSION_BLUEPRINT_TABLE")
      preparedStatement.executeUpdate()

      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def loadAirportBoostHistoryByAssetId(missionId : Int) : List[AllianceMissionBoostHistory] = {
    val queryString = s"SELECT * FROM $ALLIANCE_MISSION_BOOST_HISTORY_TABLE where mission = ?";

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setObject(1, missionId)

      val resultSet = preparedStatement.executeQuery()
      val result = ListBuffer[AllianceMissionBoostHistory]()
      while (resultSet.next()) {
        val boostType = AirportBoostType.withName(resultSet.getString("boost_type"))
        val missionId = resultSet.getInt("mission")
        val level = resultSet.getInt("level")
        val value = resultSet.getDouble("value")
        val gain = resultSet.getDouble("gain")
        val upgradeFactor = resultSet.getDouble("upgrade_factor")
        val cycle = resultSet.getInt("cycle")
        result.append(AllianceMissionBoostHistory(missionId, level, boostType, value, gain, upgradeFactor, cycle))
      }

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportPropertyHistoryByAssetId(missionId : Int) : List[AllianceMissionPropertiesHistory] = {
    val queryString = s"SELECT * FROM $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE where mission = ?";

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setObject(1, missionId)

      val resultSet = preparedStatement.executeQuery()
      val result = mutable.Map[Int, mutable.Map[String, Long]]()
      while (resultSet.next()) {
        val missionId = resultSet.getInt("mission")
        val propertyKey = resultSet.getString("property")
        val value = resultSet.getLong("value")
        val cycle = resultSet.getInt("cycle")
        val cycleProperties = result.getOrElseUpdate(cycle, mutable.Map[String, Long]())
        cycleProperties.put(propertyKey, value)
      }

      result.map {
        case(cycle, properties) => AllianceMissionPropertiesHistory(missionId, properties.toMap, cycle)
      }.toList
    } finally {
      connection.close()
    }
  }


  def saveAirportBoostHistory(entries : List[AllianceMissionBoostHistory]) = {
    val queryString = s"INSERT INTO $ALLIANCE_MISSION_BOOST_HISTORY_TABLE (mission, boost_type, level, cycle, value, gain, upgrade_factor) VALUES(?,?,?,?,?,?,?)"
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      entries.foreach { entry =>
        preparedStatement.setInt(1, entry.missionId)
        preparedStatement.setString(2, entry.boostType.toString)
        preparedStatement.setInt(3, entry.level)
        preparedStatement.setInt(4, entry.cycle)
        preparedStatement.setDouble(5, entry.value)
        preparedStatement.setDouble(6, entry.gain)
        preparedStatement.setDouble(7, entry.upgradeFactor)

        preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }


  def saveAirportPropertiesHistory(entries : List[AllianceMissionPropertiesHistory]) = {
    val queryString = s"INSERT INTO $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE (mission, property, cycle, value) VALUES(?,?,?,?)";
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      entries.foreach { entry =>
        entry.properties.foreach { case(property, value) =>
          preparedStatement.setInt(1, entry.missionId)
          preparedStatement.setString(2, property)
          preparedStatement.setInt(3, entry.cycle)
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

  def deleteAirportPropertiesHistory(cycleCutoff : Int) = { //anything before the cutoff will be purged
    val queryString = s"DELETE FROM $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE WHERE cycle < ?";
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString.toString)
      preparedStatement.setInt(1, cycleCutoff)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }


}