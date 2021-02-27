package com.patson.data

import com.patson.data.Constants._
import com.patson.model.notice._

import scala.collection.mutable.ListBuffer


object NoticeSource {
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
  
  def deleteNoticesByAirline(airlineId : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + COMPLETED_NOTICE_TABLE + " WHERE airline = ?"
      
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