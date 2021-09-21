package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.UserCache

import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable.ListBuffer

object UserUuidSource {
  val dateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue() = {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
  }

  def saveUserUuid(userId : Int, uuid : String) : Boolean = {
    val connection = Meta.getConnection() 

    val selectStatement = connection.prepareStatement(s"SELECT * FROM $USER_UUID_TABLE WHERE user = ? AND uuid = ?")
    selectStatement.setInt(1, userId)
    selectStatement.setString(2, uuid)

    val existResultSet = selectStatement.executeQuery()

    val occurrence =
      if (existResultSet.next()) {
        existResultSet.getInt("occurrence") + 1
      } else {
        1
      }

    try {
      val statement = connection.prepareStatement(s"REPLACE INTO $USER_UUID_TABLE (user, uuid, occurrence) VALUES(?,?,?)");
      statement.setInt(1, userId)
      statement.setString(2, uuid)
      statement.setInt(3, occurrence)
      statement.executeUpdate()
      statement.close()
      true
    } finally {
      connection.close()
    }
  }

  case class UuidDetails(occurrence : Int, lastUpdated : Date)

  def loadUsersByUuid(uuid : String) : Map[User, UuidDetails] = {
    val connection = Meta.getConnection()

    try {
      val queryString = s"SELECT * FROM $USER_UUID_TABLE WHERE uuid = ?"
      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setString(1, uuid)

      val resultSet = preparedStatement.executeQuery()

      val result = scala.collection.mutable.HashMap[User, UuidDetails]()
      while (resultSet.next()) {
        UserCache.getUser(resultSet.getInt("user")).foreach { user =>
          result.put(user, UuidDetails(resultSet.getInt("occurrence"), new Date(resultSet.getTimestamp("last_update").getTime)))
        }
      }

      resultSet.close()
      preparedStatement.close()
      result.toMap
    } finally {
      connection.close()
    }
  }

  def loadUserUuids(userId : Int) : Map[String, UuidDetails] = {
    val connection = Meta.getConnection()
    
    try {
      val queryString = s"SELECT * FROM $USER_UUID_TABLE WHERE user = ?"
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setInt(1, userId)
      
      val resultSet = preparedStatement.executeQuery()
      
      val result = scala.collection.mutable.HashMap[String, UuidDetails]()
      while (resultSet.next()) {
        result.put(resultSet.getString("uuid"), UuidDetails(resultSet.getInt("occurrence"), new Date(resultSet.getTimestamp("last_update").getTime)))
      }
      
      resultSet.close()
      preparedStatement.close()
      result.toMap
    } finally {
      connection.close()
    }
  }

}