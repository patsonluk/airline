package com.patson.data

import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable.{ListBuffer}


object LoyalistSource {
  val updateLoyalists = (loyalistEntries : List[Loyalist]) => {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("REPLACE INTO " + LOYALIST_TABLE + "(airport, airline, amount) VALUES(?,?,?)")

    connection.setAutoCommit(false)

    try {
      loyalistEntries.foreach {
        case Loyalist(airport : Airport, airline : Airline, amount : Int) => {
          statement.setInt(1, airport.id)
          statement.setInt(2, airline.id)
          statement.setInt(3, amount)
          statement.addBatch()
        }
      }
      statement.executeBatch()

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def loadLoyalistsByCriteria(criteria : List[(String, Any)]) = {
    var queryString = "SELECT * FROM " + LOYALIST_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadLoyalistsByQueryString(queryString, criteria.map(_._2))
  }


  def loadLoyalistsByAirportId(airportId : Int) = {
    loadLoyalistsByCriteria(List(("airport", airportId)))
  }

  private def loadLoyalistsByQueryString(queryString : String, parameters : List[Any]) : List[Loyalist] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[Loyalist]()

      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val airport = AirportCache.getAirport(airportId).get
        val airline = AirlineCache.getAirline(airlineId).get
        val amount = resultSet.getInt("amount")
        entries += Loyalist(airport, airline, amount)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }
  def deleteLoyalistsByAirline(airlineId : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LOYALIST_TABLE + " WHERE airline = ?")

    try {
      statement.setInt(1, airlineId)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  val deleteLoyalists = (loyalistEntries : List[Loyalist]) => {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LOYALIST_TABLE + " WHERE airport = ? AND airline = ?")

    connection.setAutoCommit(false)

    try {
      loyalistEntries.foreach {
        case Loyalist(airport : Airport, airline : Airline, amount : Int) => {
          statement.setInt(1, airport.id)
          statement.setInt(2, airline.id)
          statement.addBatch()
        }
      }
      statement.executeBatch()

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def deleteLoyalist(airportId : Int, airlineId : Int) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + LOYALIST_TABLE + " WHERE airport = ? AND airline = ?"

      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(1, airlineId)
      val deletedCount = preparedStatement.executeUpdate()

      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }

  val updateLoyalistHistory = (entries : List[LoyalistHistory]) => {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("REPLACE INTO " + LOYALIST_HISTORY_TABLE + "(airport, airline, amount, cycle) VALUES(?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      entries.foreach {
        case LoyalistHistory(Loyalist(airport : Airport, airline : Airline, amount : Int), cycle) => {
          statement.setInt(1, airport.id)
          statement.setInt(2, airline.id)
          statement.setInt(3, amount)
          statement.setInt(4, cycle)
          statement.addBatch()
        }
      }
      statement.executeBatch()

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def loadLoyalistsHistoryByAirportId(airportId : Int) : Map[Int, List[LoyalistHistory]] = { //returns key cycle
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + LOYALIST_HISTORY_TABLE + " WHERE airport = ?")

      preparedStatement.setInt(1, airportId)

      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[LoyalistHistory]()

      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val airport = AirportCache.getAirport(airportId).get
        val airline = AirlineCache.getAirline(airlineId).get
        val amount = resultSet.getInt("amount")
        val cycle =  resultSet.getInt("cycle")
        entries += LoyalistHistory(Loyalist(airport, airline, amount), cycle)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList.groupBy(_.cycle)
    } finally {
      connection.close()
    }
  }

  def loadLoyalistHistoryByCriteria(criteria : List[(String, Any)]) : List[LoyalistHistory] = {
    var queryString = "SELECT * FROM " + LOYALIST_HISTORY_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }


      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[LoyalistHistory]()

      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val airport = AirportCache.getAirport(airportId).get
        val airline = AirlineCache.getAirline(airlineId).get
        val amount = resultSet.getInt("amount")
        val cycle =  resultSet.getInt("cycle")
        entries += LoyalistHistory(Loyalist(airport, airline, amount), cycle)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  def loadLoyalistHistoryByCycleAndAirline(cycle : Int, airlineId : Int) : List[LoyalistHistory] = {
    loadLoyalistHistoryByCriteria(List(("cycle", cycle), ("airline", airlineId)))
  }
  def loadLoyalistHistoryByCycle(cycle : Int) : List[LoyalistHistory] = {
    loadLoyalistHistoryByCriteria(List(("cycle", cycle)))
  }


  def deleteLoyalistHistoryBeforeCycle(cutoff : Int) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + LOYALIST_HISTORY_TABLE + " WHERE cycle < ?"

      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setInt(1, cutoff)
      val deletedCount = preparedStatement.executeUpdate()

      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}