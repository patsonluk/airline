package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model._
import java.sql.Statement

import com.patson.util.{AirportCache}


object DestinationSource {
  def loadAllDestinations() = {
      loadDestinationsByCriteria(List.empty)
  }

  def loadDestinations() = {
    loadDestinationsByCriteria(List.empty)
  }

  def loadDestinationsByAirport(airportId : Int) : Option[List[Destination]] = {

    val result = loadDestinationsByCriteria(List(("airport", airportId)))

    if (result.isEmpty) {
      None
    } else {
      Some(result)
    }
  }

  def loadAllEliteDestinations() = {
    //todo load only elite
    loadDestinationsByCriteria(List.empty)
  }
  
  def loadDestinationsByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "SELECT * FROM " + DESTINATIONS_TABLE

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

      val airportIds = scala.collection.mutable.Set[Int]()
      while (resultSet.next()) {
        airportIds.add(resultSet.getInt("airport"))
      }
      val airports = AirportCache.getAirports(airportIds.toList, true)

      val destinationList = new ListBuffer[Destination]()

      resultSet.beforeFirst()
      while (resultSet.next()) {
        val destination = Destination(
          resultSet.getInt("id"),
          airports(resultSet.getInt("airport")),
          resultSet.getString("name"),
          DestinationType(resultSet.getInt("destination_type")),
          resultSet.getInt("strength"),
          resultSet.getString("description"),
          resultSet.getDouble("latitude"),
          resultSet.getDouble("longitude"),
          resultSet.getString("country_code")
        )
        destinationList += destination
      }


      
      resultSet.close()
      preparedStatement.close()
      destinationList.toList
    } finally {
      connection.close()
    }
  }
  
  def deleteAllDestinations() = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + DESTINATIONS_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " destination records")
      deletedCount
    } finally {
      connection.close()
    }
      
  }
  
  def saveAllDestinations(destinations : List[Destination]) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + DESTINATIONS_TABLE + "(id, airport, name, destination_type, strength, description, latitude, longitude, country_code) VALUES(?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)

        connection.setAutoCommit(false)
        destinations.foreach {
          val generatedKeys = preparedStatement.getGeneratedKeys
          var generatedId = 1
          if (generatedKeys.next()) {
            generatedId = generatedKeys.getInt(1)
          }
          destination =>
            preparedStatement.setInt(1, destination.id)
            preparedStatement.setInt(2, destination.airport.id)
            preparedStatement.setString(3, destination.name)
            preparedStatement.setInt(4, destination.destinationType.id)
            preparedStatement.setInt(5, destination.strength)
            preparedStatement.setString(6, destination.description)
            preparedStatement.setDouble(7, destination.latitude)
            preparedStatement.setDouble(8, destination.longitude)
            preparedStatement.setString(9, destination.countryCode)
            preparedStatement.executeUpdate()
        }
        preparedStatement.close()
        connection.commit()
    } finally {
      connection.close()
    }        
        
  }
}