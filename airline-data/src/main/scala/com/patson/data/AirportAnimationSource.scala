package com.patson.data

import com.patson.data.Constants._
import com.patson.model.animation._
import com.patson.util.AirportCache

import java.sql.Statement
import scala.collection.mutable.ListBuffer


object AirportAnimationSource {
  val updateAirportAnimations = (animations : List[AirportAnimation]) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    connection.prepareStatement(s"TRUNCATE $AIRPORT_ANIMATION_TABLE").executeUpdate()

    val statement = connection.prepareStatement(s"REPLACE INTO $AIRPORT_ANIMATION_TABLE (airport, animation_type, url) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)

    connection.setAutoCommit(false)

    try {
      animations.foreach { animation =>
        statement.setInt(1, animation.airport.id)
        statement.setString(2, animation.animationType.toString)
        statement.setString(3, animation.url)
        statement.executeUpdate()

        val generatedKeys = statement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          animation.id = generatedId
        }
      }
      statement.executeBatch()

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def loadAirportAnimationByCriteria(criteria : List[(String, Any)]) : List[AirportAnimation] = {
    var queryString = "SELECT * FROM " + AIRPORT_ANIMATION_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }


      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[AirportAnimation]()

      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airport = AirportCache.getAirport(airportId).get
        val url = resultSet.getString("url")
        val animationType = AirportAnimationType.withName(resultSet.getString("animation_type"))
        val id =  resultSet.getInt("id")
        entries += AirportAnimation(airport, animationType, url, id)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportAnimationByAirportId(airportId : Int) : List[AirportAnimation] = {
    loadAirportAnimationByCriteria(List(("airport", airportId)))
  }
  def loadAirportAnimationByAirportIdAndType(airportId : Int, animationType: AirportAnimationType.Value) : List[AirportAnimation] = {
    loadAirportAnimationByCriteria(List(("airport", airportId), ("animation_type", animationType.toString)))
  }
  def loadAirportAnimationByType(animationType: AirportAnimationType.Value) : List[AirportAnimation] = {
    loadAirportAnimationByCriteria(List(("animation_type", animationType.toString)))
  }

}