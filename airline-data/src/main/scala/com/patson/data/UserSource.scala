package com.patson.data

import com.patson.model._
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.util.Calendar
import java.text.SimpleDateFormat
import java.sql.Statement

object UserSource {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  def loadUserSecret(userName : String) : Option[UserSecret] = {
    val connection = Meta.getConnection() 
    try {  
      var queryString = "SELECT * FROM " + USER_SECRET_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
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
  
  
  def loadUsersByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    
    try {  
      var queryString = "SELECT * FROM " +  USER_TABLE 
      
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
      
      val userList = new ListBuffer[User]()
      
      while (resultSet.next()) {
        val userName = resultSet.getString("user_name")
        val userAirlines = ListBuffer[Airline]()
        val userAirlineStatment = connection.prepareStatement("SELECT * FROM " + USER_AIRLINE_TABLE + " WHERE user_name = ?")
        userAirlineStatment.setString(1, userName)
        val userAirlineResultSet = userAirlineStatment.executeQuery()
        while (userAirlineResultSet.next()) {
          val airlineId = userAirlineResultSet.getInt("airline")
          AirlineSource.loadAirlineById(airlineId, true).foreach { airline => 
            userAirlines.append(airline)
          }
        }
        userAirlineResultSet.close()
        userAirlineStatment.close()
        val creationTime = Calendar.getInstance()
        
        creationTime.setTime(dateFormat.parse(resultSet.getString("creation_time")))
        val status = UserStatus.withName(resultSet.getString("status"))
        
        val user = User(userName, resultSet.getString("email"), creationTime, status, resultSet.getInt("id"))
        user.setAccesibleAirlines(userAirlines.toList)
        userList.append(user)
      }
      
      resultSet.close()
      preparedStatement.close()
      userList.toList
    } finally {
      connection.close()
    }
  }
  
  
  def loadUserById(id : Int) = {
      val result = loadUsersByCriteria(List(("id", id)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def loadUserByUserName(userName : String) = {
      val result = loadUsersByCriteria(List(("user_name", userName)))
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
        
        updateCount == 1
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
}