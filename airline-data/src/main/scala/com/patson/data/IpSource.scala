package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.UserCache

import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import scala.collection.mutable.ListBuffer

object IpSource {

  def saveUserIp(userId : Int, ip : String) : Boolean = {
    val connection = Meta.getConnection() 
    
    try {
      val statement = connection.prepareStatement(s"REPLACE INTO $USER_IP_TABLE (user, ip) VALUES(?,?)");
      statement.setInt(1, userId)
      statement.setString(2, ip)
      statement.executeUpdate()
      statement.close()
      true
    } finally {
      connection.close()
    }
  }
  

  def loadUserIps(userId : Int) : List[String] = {
    val connection = Meta.getConnection()
    
    try {  
      val queryString = s"SELECT * FROM $USER_IP_TABLE WHERE user = ?"
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setInt(1, userId)
      
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
      loadUserIps(userId).foreach { ip =>
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
      loadUserIps(userId).foreach { ip =>
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