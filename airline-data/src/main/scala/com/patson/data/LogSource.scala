package com.patson.data
import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.AirlineCache

import java.sql.Statement
import scala.collection.immutable
import scala.collection.mutable.{HashMap, ListBuffer, Map}


object LogSource {
  val insertLogs = (logs : List[Log]) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("INSERT INTO " + LOG_TABLE + "(airline, message, category, severity, cycle) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    val propertyStatement = connection.prepareStatement(s"INSERT INTO $LOG_PROPERTY_TABLE(log, property, value) VALUES(?,?,?)")
    connection.setAutoCommit(false)

    val pendingProperties = ListBuffer[(Int, immutable.Map[String, String])]()
    try {
      logs.foreach {  
        case Log(airline : Airline, message : String, category : LogCategory.Value, severity : LogSeverity.Value, cycle : Int, properties : immutable.Map[String, String]) => {
          statement.setInt(1, airline.id)
          statement.setString(2, message)
          statement.setInt(3, category.id)
          statement.setInt(4, severity.id)
          statement.setInt(5, cycle)
          statement.execute()

          val generatedKeys = statement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            pendingProperties.append((generatedId, properties))
          }
        }
      }



      pendingProperties.foreach {
        case(logId, properties) =>
          propertyStatement.setInt(1, logId)
          properties.foreach {
            case(property, value) =>
              propertyStatement.setString(2, property)
              propertyStatement.setString(3, value)
              propertyStatement.addBatch()
          }

      }
      propertyStatement.executeBatch()
      connection.commit()
    } finally {
      statement.close()
      propertyStatement.close()
      connection.close()
    }
  }
  
  def loadLogsByAirline(airlineId : Int, fromCycle : Int, fullLoad : Boolean = false) = {
    var queryString = "SELECT * FROM " + LOG_TABLE + " WHERE airline = ? AND cycle >= ?"
     loadLogsByQueryString(queryString, List(airlineId, fromCycle), fullLoad)
  }
  
  
  def loadLogsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = "SELECT * FROM " + LOG_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadLogsByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadLogsByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[Log] = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement(queryString)

    try {
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }

      val resultSet = preparedStatement.executeQuery()

      val logs = ListBuffer[Log]()


      val airlines = Map[Int, Airline]()

      val ids = ListBuffer[Int]()
      while (resultSet.next()) {
        ids.append(resultSet.getInt("id"))
      }
      if (ids.isEmpty) {
        return List.empty
      }

      resultSet.beforeFirst()

      val propertiesById = HashMap[Int, HashMap[String, String]]()
      if (!ids.isEmpty) {
        val propertyStatement = connection.prepareStatement(s"SELECT * FROM $LOG_PROPERTY_TABLE WHERE log IN (${ids.mkString(",")})")
        try {
          val propertyResultSet = propertyStatement.executeQuery()
          while (propertyResultSet.next()) {
            val logId = propertyResultSet.getInt("log")
            val property = propertyResultSet.getString("property")
            val value = propertyResultSet.getString("value")
            val properties = propertiesById.getOrElseUpdate(logId, HashMap())
            properties.put(property, value)
          }

        } finally {
          propertyStatement.close()
        }

      }


      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, fullLoad).getOrElse(Airline.fromId(airlineId)))
        val message = resultSet.getString("message")
        val category = LogCategory(resultSet.getInt("category"))
        val severity = LogSeverity(resultSet.getInt("severity"))
        val cycle = resultSet.getInt("cycle")
        val id = resultSet.getInt("id")
        logs += Log(
          airline, message, category, severity, cycle,
          propertiesById.get(id) match {
            case Some(properties) => properties.toMap
            case None => scala.collection.immutable.Map.empty
          }
        )
      }


      resultSet.close()
      preparedStatement.close()
        
      logs.toList
    } finally {
      preparedStatement.close()

      connection.close()
    }
  }
  
  def deleteLogsBeforeCycle(cutoffCycle : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + LOG_TABLE + " WHERE cycle < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffCycle)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}