package com.patson.data

import com.patson.data.Constants._
import com.patson.data.UserSource.dateFormat
import com.patson.model._
import com.patson.util.{AirlineCache, UserCache}

import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import scala.collection.mutable.ListBuffer

object IpSource {
  val dateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue() = {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
  }

  def saveUserIp(userId : Int, ip : String) : Boolean = {
    val connection = Meta.getConnection() 

    val selectStatement = connection.prepareStatement(s"SELECT * FROM $USER_IP_TABLE WHERE user = ? AND ip = ?")
    selectStatement.setInt(1, userId)
    selectStatement.setString(2, ip)

    val existResultSet = selectStatement.executeQuery()

    val occurrence =
      if (existResultSet.next()) {
        existResultSet.getInt("occurrence") + 1
      } else {
        1
      }

    try {
      val statement = connection.prepareStatement(s"REPLACE INTO $USER_IP_TABLE (user, ip, occurrence) VALUES(?,?,?)");
      statement.setInt(1, userId)
      statement.setString(2, ip)
      statement.setInt(3, occurrence)
      statement.executeUpdate()
      statement.close()
      true
    } finally {
      connection.close()
    }
  }

  case class IpDetails(occurrence : Int, lastUpdated : Date)

  def loadUsersByIp(ip : String) : Map[User, IpDetails] = {
    val connection = Meta.getConnection()

    try {
      val queryString = s"SELECT * FROM $USER_IP_TABLE WHERE ip = ?"
      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setString(1, ip)

      val resultSet = preparedStatement.executeQuery()

      val result = scala.collection.mutable.HashMap[User, IpDetails]()
      while (resultSet.next()) {
        UserCache.getUser(resultSet.getInt("user")).foreach { user =>
          result.put(user, IpDetails(resultSet.getInt("occurrence"), new Date(resultSet.getTimestamp("last_update").getTime)))
        }
      }

      resultSet.close()
      preparedStatement.close()
      result.toMap
    } finally {
      connection.close()
    }
  }

  def loadUserIps(userId : Int) : Map[String, IpDetails] = {
    val connection = Meta.getConnection()
    
    try {
      val queryString = s"SELECT * FROM $USER_IP_TABLE WHERE user = ?"
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setInt(1, userId)
      
      val resultSet = preparedStatement.executeQuery()
      
      val result = scala.collection.mutable.HashMap[String, IpDetails]()
      while (resultSet.next()) {
        result.put(resultSet.getString("ip"), IpDetails(resultSet.getInt("occurrence"), new Date(resultSet.getTimestamp("last_update").getTime)))
      }
      
      resultSet.close()
      preparedStatement.close()
      result.toMap
    } finally {
      connection.close()
    }
  }

  def loadBannedIps() = {
    val connection = Meta.getConnection()

    try {
      val queryString = s"SELECT * FROM $BANNED_IP_TABLE"
      val preparedStatement = connection.prepareStatement(queryString)

      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[String]()
      while (resultSet.next()) {
        result.append(resultSet.getString("ip"))
      }

      resultSet.close()
      preparedStatement.close()
      result.toList
    } finally {
      connection.close()
    }
  }
  
  def deleteBannedIps(userId : Int) = {
    val connection = Meta.getConnection()
    try {
      loadUserIps(userId).toList.foreach { case(ip, _) =>
        val preparedStatement = connection.prepareStatement(s"DELETE FROM $BANNED_IP_TABLE  WHERE ip = ?")
        preparedStatement.setString(1, ip)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      }
    } finally {
      connection.close()
    }
  }

  def saveBannedIps(userId : Int) = {
    val connection = Meta.getConnection()
    try {
      loadUserIps(userId).toList.foreach { case(ip, _) =>
        val preparedStatement = connection.prepareStatement(s"REPLACE INTO $BANNED_IP_TABLE WHERE ip = ?")
        preparedStatement.setString(1, ip)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      }

    } finally {
      connection.close()
    }
  }
}