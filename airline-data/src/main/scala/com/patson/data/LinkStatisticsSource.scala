package com.patson.data
import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer
import java.sql.DriverManager

import com.patson.model._
import java.sql.PreparedStatement

import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable.Map

object LinkStatisticsSource {
  val FULL_LOAD = Map(DetailType.AIRLINE -> true, DetailType.AIRPORT -> true)
  val SIMPLE_LOAD = Map(DetailType.AIRLINE -> false, DetailType.AIRPORT -> false)
  val ID_LOAD : Map[DetailType.Type, Boolean] = Map.empty
  
  object DetailType extends Enumeration {
    type Type = Value
    val AIRPORT, AIRLINE = Value
  }
  
  def loadLinkStatisticsByCriteria(criteria : List[(String, Any)], loadDetails : Map[DetailType.Value, Boolean] = ID_LOAD) : List[LinkStatistics]= {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + LINK_STATISTICS_TABLE 
      
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
      
      val isSingleFromAirport = criteria.map(_._1).contains("from_airport")
      val isSingleToAirport = criteria.map(_._1).contains("to_airport")
      val isSingleAirline = criteria.map(_._1).contains("airline")
      
      var fromAirport : Airport = null
      var toAirport : Airport = null
      var airline : Airline = null
      
      val links = new ListBuffer[LinkStatistics]()
      
      val airports = Map[Int, Airport]()
      val airlines = Map[Int, Airline]()
      
      while (resultSet.next()) {
        if (!isSingleFromAirport || fromAirport == null) {
          val airportId = resultSet.getInt("from_airport")
          
          fromAirport = airports.getOrElseUpdate(airportId, loadDetails.get(DetailType.AIRPORT) match {
            case Some(fullLoad) => AirportCache.getAirport(airportId, fullLoad).get
            case None => Airport.fromId(airportId)
          })
        }
        if (!isSingleToAirport || toAirport == null) {
          val airportId = resultSet.getInt("to_airport")
          toAirport = airports.getOrElseUpdate(airportId, loadDetails.get(DetailType.AIRPORT) match {
            case Some(fullLoad) => AirportCache.getAirport(airportId, fullLoad).get
            case None => Airport.fromId(airportId)
          })
        }
        if (!isSingleAirline || airline == null) {
          val airlineId = resultSet.getInt("airline")
          airline = airlines.getOrElseUpdate(airlineId, loadDetails.get(DetailType.AIRLINE) match {
            case Some(fullLoad) => AirlineCache.getAirline(airlineId, fullLoad).getOrElse(Airline.fromId(airlineId))
            case None => Airline.fromId(airlineId)
          })
        }
        
        val linkStatistics = LinkStatistics(LinkStatisticsKey(fromAirport, toAirport, resultSet.getBoolean("is_departure"), resultSet.getBoolean("is_destination"), airline), resultSet.getInt("passenger_count"), resultSet.getInt("cycle"))
        links += linkStatistics
      }
      
      resultSet.close()
      preparedStatement.close()
      links.toList
    } finally {
      connection.close()
    }
  }
  
  /**
   * Only load latest for now
   */
  def loadLinkStatisticsByAirline(airlineId : Int, loadDetails : Map[DetailType.Value, Boolean] = ID_LOAD) : List[LinkStatistics]= {
    loadLinkStatisticsByCriteria(List(("airline", airlineId),("cycle", CycleSource.loadCycle() - 1)), loadDetails)
  }
  
  /**
   * Only load latest for now
   */
  def loadLinkStatisticsByFromAirport(fromAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = ID_LOAD) : List[LinkStatistics]= {
    loadLinkStatisticsByCriteria(List(("from_airport", fromAirportId),("cycle", CycleSource.loadCycle() - 1)), loadDetails)
  }
  
  /**
   * Only load latest for now
   */
  def loadLinkStatisticsByToAirport(toAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = ID_LOAD) : List[LinkStatistics]= {
    loadLinkStatisticsByCriteria(List(("to_airport", toAirportId),("cycle", CycleSource.loadCycle() - 1)), loadDetails)
  }
  
  def saveLinkStatistics(linkStatistics : List[LinkStatistics]) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_STATISTICS_TABLE + "(from_airport, to_airport, is_departure, is_destination, airline, passenger_count, cycle) VALUES(?,?,?,?,?,?,?)")
        
        connection.setAutoCommit(false)
        linkStatistics.foreach { 
          linkStatisticsEntry =>
            preparedStatement.setInt(1, linkStatisticsEntry.key.fromAirport.id)
            preparedStatement.setInt(2, linkStatisticsEntry.key.toAirport.id)
            preparedStatement.setBoolean(3, linkStatisticsEntry.key.isDeparture)
            preparedStatement.setBoolean(4, linkStatisticsEntry.key.isDestination)
            preparedStatement.setInt(5, linkStatisticsEntry.key.airline.id)
            preparedStatement.setInt(6, linkStatisticsEntry.passengers)
            preparedStatement.setInt(7, linkStatisticsEntry.cycle)
            //preparedStatement.executeUpdate()
            preparedStatement.addBatch()
        }
        preparedStatement.executeBatch()
        preparedStatement.close()
        connection.commit()
    } finally {
      connection.close()
    }        
  } 
  
  /**
   * Deletes all link before this cycle (exclusive)
   */
  def deleteLinkStatisticsBeforeCycle(cutoffCycle : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + LINK_STATISTICS_TABLE + " WHERE cycle < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffCycle)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " link statistics records")
      deletedCount
    } finally {
      connection.close()
    }
  }
  
}