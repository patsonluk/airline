package com.patson.data

import java.sql.Connection
import java.sql.Statement
import com.patson.data.Constants._
import com.patson.model._
import com.patson.util.{AirlineCache}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object AirlineStatisticsSource {

  def saveAirlineStats(stats: List[AirlineStat]) = {
    //could add total? pax km?
    val queryString = s"REPLACE INTO $AIRLINE_STATISTICS_TABLE (airline, cycle, tourists, elites, business, total) VALUES(?,?,?,?,?,?)";
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      stats.foreach { entry =>
        preparedStatement.setInt(1, entry.airlineId)
        preparedStatement.setInt(2, entry.cycle)
        preparedStatement.setInt(3, entry.tourists)
        preparedStatement.setInt(4, entry.elites)
        preparedStatement.setInt(5, entry.business)
        preparedStatement.setInt(6, entry.total)
        preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def saveAirlineStat(stat: AirlineStat) = {
    val queryString = s"REPLACE INTO $AIRLINE_STATISTICS_TABLE (airline, cycle, tourists, elites, business, total) VALUES(?,?,?,?,?,?)";
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, stat.airlineId)
      preparedStatement.setInt(2, stat.cycle)
      preparedStatement.setInt(3, stat.tourists)
      preparedStatement.setInt(4, stat.elites)
      preparedStatement.setInt(5, stat.business)
      preparedStatement.setInt(6, stat.total)
      preparedStatement.executeUpdate()
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def deleteAirlineStatsBeforeCycle(cycleCutoff : Int) = {
    if(cycleCutoff >= 0 ){
      val queryString = s"DELETE FROM $AIRLINE_STATISTICS_TABLE WHERE cycle < ?";
      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString)
        preparedStatement.setInt(1, cycleCutoff)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      } finally {
        connection.close()
      }
    }
  }

  def loadAirlineStatsForAirlines(airlines: List[Airline]): List[AirlineStat] = {
    if (airlines.isEmpty) {
      List.empty
    } else {
      val connection = Meta.getConnection()
      val airlineIds = airlines.map(_.id)
      val lastCycle = CycleSource.loadCycle() - 1
      val queryString = new StringBuilder(s"SELECT * FROM $AIRLINE_STATISTICS_TABLE WHERE cycle = ? AND airline IN (");
      for (i <- 0 until airlineIds.size - 1) {
        queryString.append("?,")
      }
      queryString.append("?)") //last item has no comma

      try {
        val preparedStatement = connection.prepareStatement(queryString.toString())
        preparedStatement.setInt(1, lastCycle)
        for (i <- 0 until airlineIds.size) {
          preparedStatement.setObject(i + 2, airlineIds(i))
        }

        val resultSet = preparedStatement.executeQuery()
        val airlineStats = ListBuffer[AirlineStat]()

        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val cycle = resultSet.getInt("cycle")
          val tourists = resultSet.getInt("tourists")
          val elites = resultSet.getInt("elites")
          val business = resultSet.getInt("business")
          val total = resultSet.getInt("total")
          airlineStats += AirlineStat(airlineId, cycle, tourists, elites, business, total)
        }

        airlineStats.toList
      } finally {
        connection.close()
      }
    }
  }

  def loadAirlineStat(airlineId: Int, cycle: Int): Option[AirlineStat] = {
    val airlineStats = loadAirlineStatsByCriteria(List(("airline", airlineId), ("cycle", cycle)))
    if (airlineStats.length > 0) {
      Some(airlineStats(0))
    } else {
      None
    }
  }

  def loadAirlineStats(airlineId: Int): List[AirlineStat] = {
    loadAirlineStatsByCriteria(List(("airline", airlineId)))
  }

  def loadAirlineStatsByCycle(cycle: Int): List[AirlineStat] = {
    loadAirlineStatsByCriteria(List(("cycle", cycle)))
  }

  def loadAirlineStatsByCriteria(criteria: List[(String, Any)]) = {
    val connection = Meta.getConnection()
    val airlineStats = ListBuffer[AirlineStat]()
    try {
      val airlineStatQuery = getAirlineStatistics(connection, criteria)
      val resultSet = airlineStatQuery.executeQuery()

      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val cycle = resultSet.getInt("cycle")
        val tourists = resultSet.getInt("tourists")
        val elites = resultSet.getInt("elites")
        val business = resultSet.getInt("business")
        val total = resultSet.getInt("total")

        airlineStats += AirlineStat(airlineId, cycle, tourists, elites, business, total)
      }

      airlineStats.toList
    } finally {
      connection.close()
    }
  }

  def getAirlineStatistics(connection: Connection, criteria: List[(String, Any)]) = {
    val queryString = new StringBuilder(s"SELECT * FROM $AIRLINE_STATISTICS_TABLE")

    if (!criteria.isEmpty) {
      queryString.append(" WHERE ")
      for (i <- 0 until criteria.size - 1) {
        queryString.append(criteria(i)._1 + " = ? AND ")
      }
      queryString.append(criteria.last._1 + " = ?")
    }

    val preparedStatement = connection.prepareStatement(queryString.toString())

    for (i <- 0 until criteria.size) {
      preparedStatement.setObject(i + 1, criteria(i)._2)
    }
    preparedStatement
  }

}