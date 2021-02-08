package com.patson.data

import java.sql.Types

import com.patson.data.Constants._
import com.patson.model.google._

import scala.collection.mutable.ListBuffer


object GoogleResourceSource {
  val insertResource = (resource: GoogleResource) => {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("REPLACE INTO " + GOOGLE_RESOURCE_TABLE + "(resource_id, resource_type, url, max_age_deadline) VALUES(?,?,?,?)")


    try {
      statement.setInt(1, resource.resourceId)
      statement.setInt(2, resource.resourceType.id)
      statement.setString(3, resource.url)
      resource.maxAgeDeadline match {
        case Some(deadline) => statement.setLong(4, deadline)
        case None => statement.setNull(4, Types.BIGINT)
      }
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def deleteResource(resourceId : Int, resourceType : ResourceType.Value): Unit = {
    val connection = Meta.getConnection()
    //case class Log(airline : Airline, message : String, cateogry : LogCategory.Value, severity : LogSeverity.Value, cycle : Int)
    val statement = connection.prepareStatement("DELETE FROM " + GOOGLE_RESOURCE_TABLE + " WHERE resource_id = ? AND resource_type = ?")


    try {
      statement.setInt(1, resourceId)
      statement.setInt(2, resourceType.id)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def loadResource(resourceId : Int, resourceType : ResourceType.Value) = {
    val result = loadResourceByCriteria(List(("resource_id", resourceId), (("resource_type"), resourceType.id)))
    if (result.length > 0) {
      Some(result(0))
    } else {
      None
    }
  }


  def loadResourceByCriteria(criteria: List[(String, Any)]) = {
    var queryString = "SELECT * FROM " + GOOGLE_RESOURCE_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadLogsByQueryString(queryString, criteria.map(_._2))
  }

  private def loadLogsByQueryString(queryString: String, parameters: List[Any]): List[GoogleResource] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }

      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[GoogleResource]()

      while (resultSet.next()) {
        val resourceId = resultSet.getInt("resource_id")
        val resourceType = resultSet.getInt("resource_type")
        val url = resultSet.getString("url")
        val deadlineValue = resultSet.getLong("max_age_deadline")
        val deadline =
          if (resultSet.wasNull()) {
            None
          } else {
            Some(deadlineValue)
          }
        result += GoogleResource(resourceId, ResourceType(resourceType), url, deadline)
      }

      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }
}