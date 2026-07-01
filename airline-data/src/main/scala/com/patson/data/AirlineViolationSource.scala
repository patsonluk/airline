package com.patson.data

import com.patson.data.Constants._
import com.patson.model.AirlineViolation

import scala.collection.mutable.ListBuffer

object AirlineViolationSource {
  def saveViolation(airlineId : Int, violation : AirlineViolation.Value) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRLINE_VIOLATION_TABLE (airline, violation) VALUES(?, ?)")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.setString(2, violation.toString)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def loadAllViolations() : Map[Int, List[(AirlineViolation.Value, Long)]] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $AIRLINE_VIOLATION_TABLE")
      val resultSet = preparedStatement.executeQuery()
      val result = scala.collection.mutable.HashMap[Int, ListBuffer[(AirlineViolation.Value, Long)]]()
      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val violation = AirlineViolation.withName(resultSet.getString("violation"))
        val timestamp = resultSet.getTimestamp("detection_timestamp").getTime
        result.getOrElseUpdate(airlineId, ListBuffer()).append((violation, timestamp))
      }
      resultSet.close()
      preparedStatement.close()
      result.view.mapValues(_.toList).toMap
    } finally {
      connection.close()
    }
  }

  def deleteViolation(airlineId : Int, violation : AirlineViolation.Value) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRLINE_VIOLATION_TABLE WHERE airline = ? AND violation = ?")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.setString(2, violation.toString)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def loadViolationsByAirlineId(airlineId : Int) : List[(AirlineViolation.Value, Long)] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"SELECT * FROM $AIRLINE_VIOLATION_TABLE WHERE airline = ?")
      preparedStatement.setInt(1, airlineId)
      val resultSet = preparedStatement.executeQuery()
      val result = ListBuffer[(AirlineViolation.Value, Long)]()
      while (resultSet.next()) {
        val violation = AirlineViolation.withName(resultSet.getString("violation"))
        val timestamp = resultSet.getTimestamp("detection_timestamp").getTime
        result.append((violation, timestamp))
      }
      resultSet.close()
      preparedStatement.close()
      result.toList
    } finally {
      connection.close()
    }
  }
}
