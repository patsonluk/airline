package com.patson.data

import com.patson.data.Constants._
import com.patson.model.tutorial._

import scala.collection.mutable.ListBuffer


object TutorialSource {
  val updateCompletedTutorial = (airlineId : Int, tutorial : Tutorial) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement(s"REPLACE INTO $COMPLETED_TUTORIAL_TABLE (airline, category, id) VALUES(?,?,?)")

    try {
      statement.setInt(1, airlineId)
      statement.setString(2, tutorial.category)
      statement.setString(3, tutorial.id)

      statement.executeUpdate()

    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadCompletedTutorialsByAirline(airlineId : Int) = {
    val connection = Meta.getConnection()
    val queryString = s"SELECT * FROM $COMPLETED_TUTORIAL_TABLE  WHERE airline = ?"
    val preparedStatement = connection.prepareStatement(queryString)
    try {
      preparedStatement.setInt(1, airlineId)

      val resultSet = preparedStatement.executeQuery()

      val completedTutorials = ListBuffer[Tutorial]()
      while (resultSet.next()) {
        val id = resultSet.getString("id")
        val category = resultSet.getString("category")
        completedTutorials += Tutorial(category, id)
      }
      resultSet.close()
      preparedStatement.close()

      completedTutorials.toList
    } finally {
      connection.close()
    }
  }
  
  def deleteTutorialsByAirline(airlineId : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + COMPLETED_TUTORIAL_TABLE + " WHERE airline = ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, airlineId)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}