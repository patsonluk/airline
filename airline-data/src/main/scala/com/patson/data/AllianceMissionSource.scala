package com.patson.data

import com.patson.data.Constants._
import com.patson.model.alliance.AllianceMissionReward
import com.patson.model.alliance._

import java.sql.Statement
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AllianceMissionSource {
  def savePickedRewardOption(missionId : Int, airlineId : Int, reward : AllianceMissionReward) : Unit = ???


  val blueprintQueryColumns = List("airport", "mission_type", "id")

  def loadAllianceMissionsByAllianceId(allianceId : Int) = {
    loadAllianceMissionsByCriteria(List(("alliance", allianceId)))
  }

  def loadAllianceMissionsByIds(ids : List[Int]) = {
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

  def loadAllianceMissionsAfterCutoff(startCycleCutoff: Int) = {
    var queryString = s"SELECT * FROM $ALLIANCE_MISSION_TABLE WHERE start_cycle >= ?"

    loadAllianceMissionsByQueryString(queryString, List(startCycleCutoff))
  }

  //criteria List[(key, operator, value)]
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
        val id = resultSet.getInt("id")
        ids.append(id)
      }

      val idToProperties = loadPropertiesByMissionIds(ids.toList)

      resultSet.beforeFirst()
      while (resultSet.next()) {
        val missionType = AllianceMissionType.withName(resultSet.getString("mission_type"))
        val startCycle = resultSet.getInt("start_cycle")
        val duration = resultSet.getInt("duration")
        val allianceId = resultSet.getInt("alliance")
        val missionId = resultSet.getInt("id")
        val status = AllianceMissionStatus.withName(resultSet.getString("status"))


        missions += AllianceMission.buildAllianceMission(missionType, startCycle, duration, allianceId, status, idToProperties.getOrElse(missionId, Map.empty), missionId)
      }

      missions.toList
    } finally {
      connection.close()
    }
  }


  def loadPropertiesByMissionIds(ids : List[Int]) : Map[Int, Map[String, Long]] = {
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


  def saveAllianceMissions(missions : List[AllianceMission]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val statement = connection.prepareStatement(s"INSERT INTO $ALLIANCE_MISSION_TABLE(mission_type, start_cycle, duration, status, alliance) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
      missions.foreach { mission =>
        statement.setString(1, mission.missionType.toString)
        statement.setInt(2, mission.startCycle)
        statement.setInt(3, mission.duration)
        statement.setString(4, mission.status.toString)
        statement.setInt(5, mission.allianceId)
        statement.executeUpdate()

        val generatedKeys = statement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          mission.id = generatedId
        }
      }
      statement.close()
      connection.commit()

      missions.foreach { mission =>
        updateAllianceMissionProperties(mission.id, mission.properties)
      }

    } finally {
      connection.close()
    }
  }


  def updateAllianceMission(mission : AllianceMission) = {
    val connection = Meta.getConnection()
    try {
      val missionStatement = connection.prepareStatement(s"UPDATE $ALLIANCE_MISSION_TABLE SET status = ? WHERE id = ?")
      missionStatement.setString(1, mission.status.toString)
      missionStatement.setInt(2, mission.id)
      missionStatement.executeUpdate()
      missionStatement.close()

      updateAllianceMissionProperties(mission.id, mission.properties)
    } finally {
      connection.close()
    }
  }


  def updateAllianceMissionProperties(missionId : Int, properties : Map[String, Long]) = {
    val connection = Meta.getConnection()
    try {
      val propertiesStatement = connection.prepareStatement(s"REPLACE INTO $ALLIANCE_MISSION_PROPERTY_TABLE (mission, property, value) VALUES(?,?,?)")
      properties.foreach {
        case (property, value) =>
          propertiesStatement.setInt(1, missionId)
          propertiesStatement.setString(2, property)
          propertiesStatement.setLong(3, value)
          propertiesStatement.executeUpdate()
      }
      propertiesStatement.close()
    } finally {
      connection.close()
    }
  }



  def loadPropertyHistory(missionId : Int, cycle : Int) : AllianceMissionPropertiesHistory = {
    val queryString = s"SELECT * FROM $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE where mission = ? AND cycle = ?";

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, missionId)
      preparedStatement.setInt(2, cycle)

      val resultSet = preparedStatement.executeQuery()
      val cycleProperties = mutable.Map[String, Long]()
      while (resultSet.next()) {
        val propertyKey = resultSet.getString("property")
        val value = resultSet.getLong("value")
        cycleProperties.put(propertyKey, value)
      }

      AllianceMissionPropertiesHistory(missionId, cycleProperties.toMap, cycle)
    } finally {
      connection.close()
    }
  }



  def saveAllianceMissionPropertiesHistory(entries : List[AllianceMissionPropertiesHistory]) = {
    val queryString = s"REPLACE INTO $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE (mission, property, cycle, value) VALUES(?,?,?,?)";
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

  def deleteAllianceMissionsByCutoff(cycleCutoff : Int) = { //anything before the cutoff will be purged
    val queryString = s"DELETE FROM $ALLIANCE_MISSION_TABLE WHERE start_cycle < ?";
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, cycleCutoff)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def saveRewardOptions(options : List[AllianceMissionReward]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val statement = connection.prepareStatement(s"" +
        s" INTO $ALLIANCE_MISSION_REWARD_TABLE(mission, reward_type) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
      options.foreach { option =>
        statement.setInt(1, option.missionId)
        statement.setString(2, option.rewardType.toString)
        statement.executeUpdate()
        statement.close()

        val generatedKeys = statement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          option.id = generatedId
        }
      }
      connection.commit()

      options.foreach { option =>
        val propertiesStatement = connection.prepareStatement(s"REPLACE INTO $ALLIANCE_MISSION_REWARD_PROPERTY_TABLE (reward, property, value) VALUES(?,?,?)")
        option.properties.foreach {
          case (property, value) =>
            propertiesStatement.setInt(1, option.id)
            propertiesStatement.setString(2, property)
            propertiesStatement.setLong(3, value)
            propertiesStatement.executeUpdate()
        }
        propertiesStatement.close()
      }

    } finally {
      connection.close()
    }
  }

  def loadRewardOptions(missionId : Int) : List[AllianceMissionReward] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $ALLIANCE_MISSION_REWARD_TABLE WHERE mission = ?")

      preparedStatement.setInt(1, missionId)

      val resultSet = preparedStatement.executeQuery()
      preparedStatement.close()
      val options = ListBuffer[AllianceMissionReward]()


      while (resultSet.next()) {
        val rewardType = RewardType.withName(resultSet.getString("reward_type"))
        val missionId = resultSet.getInt("mission")
        val rewardId = resultSet.getInt("id")

        val properties = mutable.HashMap[String, Long]()
        val propertiesStatement = connection.prepareStatement(s"SELECT * FROM $ALLIANCE_MISSION_REWARD_PROPERTY_TABLE WHERE reward = ?")
        propertiesStatement.setInt(1, rewardId)
        val propertiesResultSet = propertiesStatement.executeQuery()
        propertiesStatement.close()
        while (propertiesResultSet.next()) {
          properties.put(propertiesResultSet.getString("property"), propertiesResultSet.getLong("value"))
        }

        options += AllianceMissionReward.buildMissionReward(missionId, rewardType, properties.toMap, rewardId)
      }

      options.toList
    } finally {
      connection.close()
    }
  }


}