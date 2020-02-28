package com.patson.data

import com.patson.model._
import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer
import java.util.Calendar
import java.text.SimpleDateFormat
import java.sql.Statement
import java.util.Date

import com.patson.util.UserCache

object UserSource {
  val dateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue() = {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
  }
  
  def loadUserSecret(userName : String) : Option[UserSecret] = {
    val connection = Meta.getConnection() 
    try {  
      var queryString = "SELECT * FROM " + USER_SECRET_TABLE + " WHERE user_name = ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setString(1, userName)
      val resultSet = preparedStatement.executeQuery()
      
      val userSecret = if (resultSet.next()) { 
          Some(UserSecret(resultSet.getString("user_name"), resultSet.getString("digest"), resultSet.getString("salt"))) 
        } else { 
          None 
        } 
      
      resultSet.close()
      preparedStatement.close()
      userSecret
    } finally {
      connection.close()
    }   
  }
  def saveUserSecret(userSecret : UserSecret) : Boolean = {
    val connection = Meta.getConnection() 
    
    try {
      val statement = connection.prepareStatement("REPLACE INTO " + USER_SECRET_TABLE + "(user_name, digest, salt) VALUES(?,?,?)");
      statement.setString(1, userSecret.userName)
      statement.setString(2, userSecret.digest)
      statement.setString(3, userSecret.salt)
      statement.executeUpdate()
      statement.close()
      true
    } finally {
      connection.close()
    }
  }
  
  
  def loadUsersByCriteria(criteria : List[(String, Any)]) : List[User] = {
      //open the hsqldb
    val connection = Meta.getConnection()
    
    try {  
      var queryString = "SELECT u.*, ua.* FROM " +  USER_TABLE + " u LEFT JOIN " + USER_AIRLINE_TABLE + " ua ON u.user_name = ua.user_name"  
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }
      
      
      val resultSet = preparedStatement.executeQuery()
      
      val userList = scala.collection.mutable.Map[Int, (User, ListBuffer[Int])]() //Map[UserId, (User, List[AirlineId])]
      
      while (resultSet.next()) {
        val userId = resultSet.getInt("u.id")
        val (user, userAirlines) = userList.getOrElseUpdate(userId, {
          val userName = resultSet.getString("u.user_name")
          val creationTime = Calendar.getInstance()
          creationTime.setTime(dateFormat.get().parse(resultSet.getString("u.creation_time")))
          val lastActiveTime = Calendar.getInstance()
          lastActiveTime.setTime(dateFormat.get().parse(resultSet.getString("u.last_active")))
          val status = UserStatus.withName(resultSet.getString("u.status"))
          (User(userName, resultSet.getString("u.email"), creationTime, lastActiveTime, status, level = resultSet.getInt("level"), id = userId), ListBuffer[Int]())  
        })
        
        userAirlines += resultSet.getInt("ua.airline") 
      }
      
      val allAirlineIds : List[Int] = userList.values.map(_._2).flatten.toSet.toList
      
      val airlinesMap = AirlineSource.loadAirlinesByIds(allAirlineIds, true).map(airline => (airline.id, airline)).toMap
      
      userList.values.foreach {
        case(user,userAirlineIds) =>
          user.setAccesibleAirlines(userAirlineIds.map(airlineId => airlinesMap.get(airlineId)).flatten.toList)
      }
      
      resultSet.close()
      preparedStatement.close()
      userList.values.map(_._1).toList
    } finally {
      connection.close()
    }
  }
  
  
  def loadUserById(id : Int) = {
      val result = loadUsersByCriteria(List(("u.id", id)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def loadUserByUserName(userName : String) = {
      val result = loadUsersByCriteria(List(("u.user_name", userName)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def saveUser(user: User) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + USER_TABLE + "(user_name, email, status) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)
        preparedStatement.setString(1, user.userName)
        preparedStatement.setString(2, user.email)
        preparedStatement.setString(3, user.status.toString)
        preparedStatement.executeUpdate()
        
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          user.id = generatedId
        }
        
        preparedStatement.close()
    } finally {
      connection.close()
    }        
  } 
  
  def updateUser(user: User) : Boolean = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("UPDATE " + USER_TABLE + " SET email = ?, status = ? WHERE id = ?")
        preparedStatement.setString(1, user.email)
        preparedStatement.setString(2, user.status.toString)
        preparedStatement.setInt(3, user.id)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
        UserCache.invalidateUser(user.id)
        updateCount == 1
    } finally {
      connection.close()
    }        
  } 
  
  def updateUserLastActive(user: User) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("UPDATE " + USER_TABLE + " SET last_active = ? WHERE id = ?")
        preparedStatement.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()))
        preparedStatement.setInt(2, user.id)
        val updateCount = preparedStatement.executeUpdate()
        UserCache.invalidateUser(user.id)
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def setUserAirline(user: User, airline : Airline) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + USER_AIRLINE_TABLE + "(user_name, airline) VALUES(?,?)")
        preparedStatement.setString(1, user.userName)
        preparedStatement.setInt(2, airline.id)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
        updateCount == 1
    } finally {
      connection.close()
    }        
  }

  def deleteGeneratedUsers(fromId : Int) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + USER_TABLE + " WHERE id >= ?")
        preparedStatement.setInt(1, fromId)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def saveResetUser(username : String, resetToken : String) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("REPLACE INTO " + RESET_USER_TABLE + "(user_name, token) VALUES(?,?)")
        preparedStatement.setString(1, username)
        preparedStatement.setString(2, resetToken)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
        updateCount == 1
    } finally {
      connection.close()
    }
  }
  
  def loadResetUser(resetToken : String) : Option[String] = {
    val connection = Meta.getConnection()
    
    try {  
      var queryString = "SELECT * FROM " +  RESET_USER_TABLE + " WHERE token = ?" 
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setString(1, resetToken)
      
      val resultSet = preparedStatement.executeQuery()
      
      val result =
        if (resultSet.next()) {
          Some(resultSet.getString("user_name"))
        } else {
          None 
        }
      
      resultSet.close()
      preparedStatement.close()
      result
    } finally {
      connection.close()
    }
  }
  
  def deleteResetUser(resetToken : String) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("DELETE FROM " + RESET_USER_TABLE + " WHERE token = ?")
        preparedStatement.setString(1, resetToken)
        val updateCount = preparedStatement.executeUpdate()
        
        preparedStatement.close()
        updateCount == 1
    } finally {
      connection.close()
    }
  }
}