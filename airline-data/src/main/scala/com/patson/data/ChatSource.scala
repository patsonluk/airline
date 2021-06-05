package com.patson.data

import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.Calendar

import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.chat.ChatMessage
import com.patson.util.{AirlineCache, UserCache}

import scala.collection.mutable.ListBuffer


object ChatSource {
  val dateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue() = {
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
  }

  val updateLastChatId = (userId : Int, lastChatId : Long) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("REPLACE INTO " + LAST_CHAT_ID_TABLE + "(user, last_chat_id) VALUES(?,?)")
    

    try {
      statement.setInt(1, userId)
      statement.setLong(2, lastChatId)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  val getLastChatId = (userId : Int) => {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + LAST_CHAT_ID_TABLE + " WHERE user = ?")

      preparedStatement.setInt(1, userId)

      val resultSet = preparedStatement.executeQuery()

      val result : Option[Long] =
        if (resultSet.next()) {
          Some(resultSet.getLong("last_chat_id"))
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

  val insertChatMessage = (chatMessage : ChatMessage) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("INSERT INTO " + CHAT_MESSAGE_TABLE + "(airline, user, room_id, text, time) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)


    try {
      statement.setInt(1, chatMessage.airline.id)
      statement.setInt(2, chatMessage.user.id)
      statement.setInt(3, chatMessage.roomId)
      statement.setString(4, chatMessage.text)
      statement.setString(5, dateFormat.get().format(chatMessage.time.getTime))

      statement.executeUpdate()

      val generatedKeys = statement.getGeneratedKeys
      if (generatedKeys.next()) {
        val generatedId = generatedKeys.getInt(1)
        chatMessage.id = generatedId
      }

    } finally {
      statement.close()
      connection.close()
    }
  }



  def loadChatMessagesByCriteria(criteria : List[(String, String, Any)]) = {
    var queryString = "SELECT * FROM " + CHAT_MESSAGE_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " " + criteria(i)._2 + " ? AND "
      }
      queryString += criteria.last._1 + " " + criteria.last._2 + " ?"
    }
    loadChatMessagesByQueryString(queryString, criteria.map(_._3))
  }

  def loadLatestChatMessagesWithLimit(countLimit : Int) = {
    val queryString = s"SELECT * FROM $CHAT_MESSAGE_TABLE ORDER BY id DESC LIMIT $countLimit"
    loadChatMessagesByQueryString(queryString, List.empty).reverse
  }

  private def loadChatMessagesByQueryString(queryString : String, parameters : List[Any]) : List[ChatMessage] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val messages = ListBuffer[ChatMessage]()
        
        
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
          val userId = resultSet.getInt("user")
          val user = UserCache.getUser(userId).getOrElse(User.fromId(userId))
          val text = resultSet.getString("text")
          val roomId = resultSet.getInt("room_id")
          val time = Calendar.getInstance()
          time.setTime(dateFormat.get().parse(resultSet.getString("time")))
          val id = resultSet.getInt("id")
          messages += ChatMessage(airline, user, roomId, text, time, id);
        }
        
        resultSet.close()
        preparedStatement.close()
        
        messages.toList
      } finally {
        connection.close()
      }
  }
  
  def deleteChatMessagesBeforeId(cutoffId : Long) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + CHAT_MESSAGE_TABLE + " WHERE id < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffId)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}