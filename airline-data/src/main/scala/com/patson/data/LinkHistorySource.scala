package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import java.sql.DriverManager
import com.patson.model._
import java.sql.PreparedStatement

object LinkHistorySource {
  def loadWatchedLinkIdsByCriteria(criteria : List[(String, Any)]) : List[Int]= {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + WATCHED_LINK_TABLE 
      
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
      
      val watchedLinkIds = ListBuffer[Int]()
      while (resultSet.next()) {
        watchedLinkIds += resultSet.getInt("watched_link")
      }
      
      resultSet.close()
      preparedStatement.close()
      
      watchedLinkIds.toList      
    } finally {
      connection.close()
    }
  }
  
  def loadAllWatchedLinkIds() = {
    loadWatchedLinkIdsByCriteria(List.empty)
  }
  
  def loadWatchedLinkIdByAirline(airlineId : Int) : Option[Int]= {
    val result = loadWatchedLinkIdsByCriteria(List(("airline", airlineId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def updateWatchedLinkId(airlineId : Int, watchedLinkId : Int) = {
    val connection = Meta.getConnection()
    try {  
       //add
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + WATCHED_LINK_TABLE + "(airline, watched_link) VALUES(?,?)")
      preparedStatement.setInt(1, airlineId)
      preparedStatement.setInt(2, watchedLinkId)
      preparedStatement.executeUpdate()  
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  
  
  
  def loadLinkHistoryByCriteria(criteria : List[(String, Any)]) : List[LinkHistory]= {
    val connection = Meta.getConnection()
    try {  
      val allAirports = Map[Int, Airport]() 
      val allAirlines = Map[Int, Airline]()
      AirportSource.loadAllAirports(false).foreach( airport => allAirports.put(airport.id, airport))
      AirlineSource.loadAllAirlines(false).foreach( airline => allAirlines.put(airline.id, airline))
      
      var queryString = "SELECT * FROM " + LINK_HISTORY_TABLE 
      
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
      
      val linkHistoryMap = Map[Int, Set[RelatedLink]]()
      val invertedLinkHistoryMap = Map[Int, Set[RelatedLink]]()
      while (resultSet.next()) {
        val watchedLinkId = resultSet.getInt("watched_link")
        val relatedLinkId = resultSet.getInt("related_link")
        val fromAirport = allAirports(resultSet.getInt("from_airport"))
        val toAirport = allAirports(resultSet.getInt("to_airport"))
        val airline = allAirlines(resultSet.getInt("airline"))
        
        val relatedLink = RelatedLink(relatedLinkId, fromAirport, toAirport, airline, resultSet.getInt("passenger"))
        if (!resultSet.getBoolean("inverted")) {
          linkHistoryMap.getOrElseUpdate(watchedLinkId, Set[RelatedLink]()) += relatedLink
        } else {
          invertedLinkHistoryMap.getOrElseUpdate(watchedLinkId, Set[RelatedLink]()) += relatedLink
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      
      linkHistoryMap.keySet.union(invertedLinkHistoryMap.keySet).map { watchedLinkId =>
        LinkHistory(watchedLinkId, linkHistoryMap.getOrElse(watchedLinkId, Set.empty).toSet, invertedLinkHistoryMap.getOrElse(watchedLinkId, Set.empty).toSet)
      }.toList
    } finally {
      connection.close()
    }
  }
  
  /**
   * Only load the latest cycle for now
   */
  def loadLinkHistoryByWatchedLinkId(linkId : Int) : Option[LinkHistory]= {
    val result = loadLinkHistoryByCriteria(List(("watched_link", linkId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
 
  
  def updateLinkHistory(linkHistory : List[LinkHistory]) = {
    val connection = Meta.getConnection()
    try {
        connection.setAutoCommit(false)
      
        val deleteStatement = connection.prepareStatement("DELETE FROM " + LINK_HISTORY_TABLE)
        val deletedCount = deleteStatement.executeUpdate()
        deleteStatement.close()
        println("Deleted " + deletedCount + " link history records")
        
        //add
        val insertStatement = connection.prepareStatement("INSERT INTO " + LINK_HISTORY_TABLE + "(watched_link, inverted, related_link, from_airport, to_airport, airline, passenger) VALUES(?,?,?,?,?,?,?)")
        linkHistory.foreach { 
          linkHistoryEntry =>
            linkHistoryEntry.relatedLinks.foreach { relatedLink => 
              insertStatement.setInt(1, linkHistoryEntry.watchedLinkId)
              insertStatement.setBoolean(2, false)
              insertStatement.setInt(3, relatedLink.relatedLinkId)
              insertStatement.setInt(4, relatedLink.fromAirport.id)
              insertStatement.setInt(5, relatedLink.toAirport.id)
              insertStatement.setInt(6, relatedLink.airline.id)
              insertStatement.setInt(7, relatedLink.passengers)
              insertStatement.executeUpdate()  
            }
            linkHistoryEntry.invertedRelatedLinks.foreach { invertedRelatedLink => 
              insertStatement.setInt(1, linkHistoryEntry.watchedLinkId)
              insertStatement.setBoolean(2, true)
              insertStatement.setInt(3, invertedRelatedLink.relatedLinkId)
              insertStatement.setInt(4, invertedRelatedLink.fromAirport.id)
              insertStatement.setInt(5, invertedRelatedLink.toAirport.id)
              insertStatement.setInt(6, invertedRelatedLink.airline.id)
              insertStatement.setInt(7, invertedRelatedLink.passengers)
              insertStatement.executeUpdate()  
            }
        }
        insertStatement.close()
        connection.commit()
    } finally {
      connection.close()
    }        
  } 
  
  
  
//  /**
//   * Deletes all link before this cycle (exclusive)
//   */
//  def deleteLinkHistoryBeforeCycle(cutoffCycle : Int) = {
//      //open the hsqldb
//    val connection = Meta.getConnection()
//    try {  
//      var queryString = "DELETE FROM " + LINK_STATISTICS_TABLE + " WHERE cycle < ?"
//      
//      val preparedStatement = connection.prepareStatement(queryString)
//      
//      preparedStatement.setObject(1, cutoffCycle)
//      val deletedCount = preparedStatement.executeUpdate()
//      
//      preparedStatement.close()
//      println("Deleted " + deletedCount + " link statistics records")
//      deletedCount
//    } finally {
//      connection.close()
//    }
//  }
  
}