package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.campaign.Campaign
import com.patson.util.{AirlineCache, AirportCache, CountryCache}

import java.sql.{Statement, Types}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object ColorSource {

  def loadAllianceLabelColorsFromAlliance(currentAllianceId : Int) : Map[Int, String] = {
    loadAllianceLabelColorsFromId("alliance", currentAllianceId, ALLIANCE_LABEL_COLOR_BY_ALLIANCE_TABLE)
  }

  def loadAllianceLabelColorsFromAirline(currentAirlineId : Int) : Map[Int, String] = {
    loadAllianceLabelColorsFromId("airline", currentAirlineId, ALLIANCE_LABEL_COLOR_BY_AIRLINE_TABLE)
  }

  def loadAllianceLabelColorsFromId(idColumn : String, id : Int, table : String) : Map[Int, String] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $table WHERE $idColumn = ?")

      preparedStatement.setInt(1, id)

      val resultSet = preparedStatement.executeQuery()

      val result = mutable.Map[Int, String]() //key: Alliance id


      while (resultSet.next()) {
        val targetAllianceId = resultSet.getInt("target_alliance")
        val color = resultSet.getString("color")
        result.put(targetAllianceId, color)
      }

      resultSet.close()
      preparedStatement.close()

      result.toMap
    } finally {
      connection.close()
    }
  }

  def saveAllianceLabelColorFromAlliance(currentAllianceId : Int, targetAllianceId : Int, color : String) = {
    saveAllianceLabelColor("alliance", currentAllianceId, ALLIANCE_LABEL_COLOR_BY_ALLIANCE_TABLE, targetAllianceId, color)
  }

  def saveAllianceLabelColorFromAirline(currentAirlineId : Int, targetAllianceId : Int, color : String) = {
    saveAllianceLabelColor("airline", currentAirlineId, ALLIANCE_LABEL_COLOR_BY_AIRLINE_TABLE, targetAllianceId, color)
  }


  def saveAllianceLabelColor(idColumn : String, id : Int, table : String, targetAllianceId : Int, color : String) = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement(s"REPLACE INTO $table($idColumn, target_alliance, color) VALUES(?,?,?)")
    try {
      preparedStatement.setInt(1, id)
      preparedStatement.setInt(2, targetAllianceId)
      preparedStatement.setString(3, color)
      preparedStatement.executeUpdate()
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

  def deleteAllianceLabelColorFromAlliance(currentAllianceId : Int, targetAllianceId : Int) = {
    deleteAllianceLabelColor("alliance", currentAllianceId, ALLIANCE_LABEL_COLOR_BY_ALLIANCE_TABLE, targetAllianceId)
  }

  def deleteAllianceLabelColorFromAirline(currentAirlineId : Int, targetAllianceId : Int) = {
    deleteAllianceLabelColor("airline", currentAirlineId, ALLIANCE_LABEL_COLOR_BY_AIRLINE_TABLE, targetAllianceId)
  }

  def deleteAllianceLabelColor(idColumn : String, id : Int, table : String, targetAllianceId : Int) = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement(s"DELETE FROM $table WHERE $idColumn = ? AND target_alliance = ?")
    try {
      preparedStatement.setInt(1, id)
      preparedStatement.setInt(2, targetAllianceId)

      preparedStatement.executeUpdate()
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

}