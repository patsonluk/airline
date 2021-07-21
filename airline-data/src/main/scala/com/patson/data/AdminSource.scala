package com.patson.data

import com.patson.data.Constants._
import com.patson.model.notice._

import scala.collection.mutable.ListBuffer


object AdminSource {
  def saveLog(action : String, adminUserName : String, targetUserId : Int) = {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement(s"INSERT INTO $ADMIN_LOG_TABLE (admin_user, admin_action, user_id) VALUES(?,?,?)")

    try {
      statement.setString(1, adminUserName)
      statement.setString(2, action)
      statement.setInt(3, targetUserId)

      statement.executeUpdate()

    } finally {
      statement.close()
      connection.close()
    }
  }
}