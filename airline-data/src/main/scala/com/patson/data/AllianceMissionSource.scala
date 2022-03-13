package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.alliance._
import com.patson.model.event.EventType
import com.patson.util.{AirlineCache, AirportCache}
import jdk.jfr.internal.Cutoff

import java.sql.Statement
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AllianceMissionSource {
  def savePickedRewardOption(missionId : Int, airlineId : Int, reward : AllianceMissionReward) : Unit = ???

  def saveStats(currentCycle : Int, stats : Long) : Unit = ???

  def loadStatsByCycle(currentCycle : Int) : Option[Long] = {
    ???
  }

  val blueprintQueryColumns = List("airport", "mission_type", "id")

  def loadAllianceMissionsByAllianceId(allianceId : Int) = {
    loadAllianceMissionsByCriteria(List(("alliance", "=", allianceId)))
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
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $EVENT_TABLE WHERE start_cycle >= ?")
      preparedStatement.setInt(1, startCycleCutoff)

      val resultSet = preparedStatement.executeQuery()

      val ids = ListBuffer[Int]()
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        ids.append(id)
      }

      preparedStatement.close()

      loadAllianceMissionsByIds(ids.toList)
    } finally {
      connection.close()
    }
  }

  //criteria List[(key, operator, value)]
  def loadAllianceMissionsByCriteria(criteria : List[(String, String, Any)]) = {
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

      val idToProperties = loadAirportPropertiesByMissionIds(ids.toList)

      resultSet.beforeFirst()
      while (resultSet.next()) {
        val missionType = AllianceMissionType.withName(resultSet.getString("mission_type"))
        val startCycle = resultSet.getInt("startCycle")
        val duration = resultSet.getInt("duration")
        val allianceId = resultSet.getInt("alliance")
        val missionId = resultSet.getInt("id")


        missions += AllianceMission.buildAllianceMission(missionType, startCycle, duration, allianceId, idToProperties.getOrElse(missionId, Map.empty), missionId)
      }

      missions.toList
    } finally {
      connection.close()
    }
  }


  def loadAirportPropertiesByMissionIds(ids : List[Int]) : Map[Int, Map[String, Long]] = {
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


  def updateAllianceMission(mission : AllianceMission) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"REPLACE INTO $ALLIANCE_MISSION_PROPERTY_TABLE (mission, property, value) VALUES(?,?,?)")
      mission.properties.foreach {
        case (property, value) =>
          preparedStatement.setInt(1, mission.id)
          preparedStatement.setString(2, property)
          preparedStatement.setLong(3, value)
          preparedStatement.executeUpdate()
          preparedStatement.close()
      }
    } finally {
      connection.close()
    }
  }



  def loadAirportPropertyHistoryByMissionId(missionId : Int) : List[AllianceMissionPropertiesHistory] = {
    val queryString = s"SELECT * FROM $ALLIANCE_MISSION_PROPERTY_HISTORY_TABLE where mission = ?";

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setObject(1, missionId)

      val resultSet = preparedStatement.executeQuery()
      val result = mutable.Map[Int, mutable.Map[String, Long]]()
      while (resultSet.next()) {
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



  def saveAllianceMissionPropertiesHistory(entries : List[AllianceMissionPropertiesHistory]) = {
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

  def deleteAllianceMissionsByCutoff(cycleCutoff : Int) = { //anything before the cutoff will be purged
    val queryString = s"DELETE FROM $EVENT_TABLE WHERE start_cycle < ? AND event_type = ?";
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, cycleCutoff)
      preparedStatement.setInt(2, EventType.ALLIANCE_MISSION.id)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }


}