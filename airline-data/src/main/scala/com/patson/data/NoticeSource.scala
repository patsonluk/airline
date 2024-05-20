package com.patson.data

import com.patson.data.Constants._
import com.patson.model.notice._

import java.sql.Statement
import scala.collection.mutable.ListBuffer


object NoticeSource {

  /**
    * Notice that we want tracking on (ie not leveling/loyalist notice)
    * @param airlineId
    * @param notice
    */
  def saveTrackingNotice(airlineId : Int, notice : TrackingNotice) : Notice = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement(s"INSERT INTO $TRACKING_NOTICE_TABLE (airline, category) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)

    try {
      statement.setInt(1, airlineId)
      statement.setString(2, notice.category.toString)

      val updateCount = statement.executeUpdate()
      if (updateCount > 0) {
        val generatedKeys = statement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          Notice.fromCategoryAndId(notice.category, generatedId.toString)
        }
      }
      notice

    } finally {
      statement.close()
      connection.close()
    }
  }

  val updateCompletedNotice = (airlineId : Int, notice : Notice) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement(s"REPLACE INTO $COMPLETED_NOTICE_TABLE (airline, category, id) VALUES(?,?,?)")

    try {
      statement.setInt(1, airlineId)
      statement.setString(2, notice.category.toString)
      statement.setString(3, notice.id)

      statement.executeUpdate()

    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadCompletedNoticesByAirline(airlineId : Int) = {
    val connection = Meta.getConnection()
    val queryString = s"SELECT * FROM $COMPLETED_NOTICE_TABLE  WHERE airline = ?"
    val preparedStatement = connection.prepareStatement(queryString)
    try {
      preparedStatement.setInt(1, airlineId)

      val resultSet = preparedStatement.executeQuery()

      val completedNotices = ListBuffer[Notice]()
      while (resultSet.next()) {
        val id = resultSet.getString("id")
        val category = NoticeCategory.withName(resultSet.getString("category"))
        import NoticeCategory._
        val notice = category match {
          case LEVEL_UP =>
            LevelNotice(id.toInt)
          case LOYALIST =>
            LoyalistNotice(id.toInt)
        }

        completedNotices += notice
      }
      resultSet.close()
      preparedStatement.close()

      completedNotices.toList
    } finally {
      connection.close()
    }
  }

  def loadTrackingNotices(airlineId : Int) : List[Notice] = {
    val connection = Meta.getConnection()
    val queryString = s"SELECT * FROM $TRACKING_NOTICE_TABLE  WHERE airline = ?"
    val preparedStatement = connection.prepareStatement(queryString)
    try {
      preparedStatement.setInt(1, airlineId)

      val resultSet = preparedStatement.executeQuery()

      val trackingNotices = ListBuffer[Notice]()
      while (resultSet.next()) {
        val id = resultSet.getString("id")
        val category = NoticeCategory.withName(resultSet.getString("category"))
        trackingNotices += Notice.fromCategoryAndId(category, id)

      }
      resultSet.close()
      preparedStatement.close()

      trackingNotices.toList
    } finally {
      connection.close()
    }
  }
  
  def deleteNoticesByAirline(airlineId : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var preparedStatement = connection.prepareStatement("DELETE FROM " + COMPLETED_NOTICE_TABLE + " WHERE airline = ?")
      
      preparedStatement.setObject(1, airlineId)
      preparedStatement.executeUpdate()
      preparedStatement.close()

      preparedStatement = connection.prepareStatement("DELETE FROM " + TRACKING_NOTICE_TABLE + " WHERE airline = ?")
      preparedStatement.setObject(1, airlineId)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def deleteTrackingNotice(airlineId : Int, trackingId : Int) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var preparedStatement = connection.prepareStatement("DELETE FROM " + TRACKING_NOTICE_TABLE + " WHERE id = ? AND airline = ?")

      preparedStatement.setObject(1, trackingId) //tracking notice should have unique id
      preparedStatement.setObject(2, airlineId) //tracking notice should have unique id
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
}