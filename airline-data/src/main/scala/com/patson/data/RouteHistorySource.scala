package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model._
import java.sql.PreparedStatement

object RouteHistorySource {
  def loadVipRoutesByCriteria(criteria : List[(String, Any)]) : List[Route]= {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + VIP_ROUTE_TABLE 
      
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
      
      val routes = ListBuffer[Route]()
      while (resultSet.next()) {
        val routeEntryStatement = connection.prepareStatement("SELECT * FROM " + VIP_ROUTE_ENTRY_TABLE + " WHERE route = ?")
        
        routeEntryStatement.setInt(1, resultSet.getInt("id"))
        val routeEntryResultSet = routeEntryStatement.executeQuery()
        val links = ListBuffer[Link]()
        while (routeEntryResultSet.next()) {
          val fromAirportId = routeEntryResultSet.getInt("from_airport")
          val fromAirport = AirportSource.loadAirportById(fromAirportId, false).getOrElse(Airport.fromId(fromAirportId))
          val toAirportId = routeEntryResultSet.getInt("to_airport")
          val toAirport = AirportSource.loadAirportById(toAirportId, false).getOrElse(Airport.fromId(toAirportId))
          val airlineId = routeEntryResultSet.getInt("airline")
          val airline = AirlineSource.loadAirlineById(airlineId, false).getOrElse(Airline.fromId(airlineId))
          links += Link(from = fromAirport, to = toAirport, airline = airline, price = 0, distance = 0, capacity = 0, rawQuality = 0, duration = 0, frequency = 0)
        }
        routeEntryStatement.close()
        routes += Route(links.map(LinkWithCost(_, 0, false)).toList, 0) 
      }
      
      resultSet.close()
      preparedStatement.close()
      routes.toList
    } finally {
      connection.close()
    }
  }
  
  def loadVipRoutes() : List[Route]= {
    loadVipRoutesByCriteria(List.empty)
  }
  
  def saveVipRoutes(routes : List[Route], cycle : Int) = {
    val connection = Meta.getConnection()
    try {    
        
        val routePreparedStatement = connection.prepareStatement("INSERT INTO " + VIP_ROUTE_TABLE + "(cycle) VALUES(?)")
        
        connection.setAutoCommit(false)
        routes.foreach { route =>
          routePreparedStatement.setInt(1, cycle)
          routePreparedStatement.executeUpdate()
          val generatedKeys = routePreparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            
            //insert route entry
            route.links.foreach { 
              linkWithCost =>
                val infoStatement = connection.prepareStatement("INSERT INTO " + VIP_ROUTE_ENTRY_TABLE + "(route, from_airport, to_airport, airline) VALUES(?,?,?,?)")
                infoStatement.setInt(1, generatedId)
                infoStatement.setInt(2, linkWithCost.from.id)
                infoStatement.setInt(3, linkWithCost.to.id)
                infoStatement.setDouble(4, linkWithCost.link.airline.id)
                infoStatement.executeUpdate()
                infoStatement.close()
            }
          }
        }
        routePreparedStatement.close()
        connection.commit()
    } finally {
      connection.close()
    }        
  } 
  
  /**
   * Deletes all link before this cycle (exclusive)
   */
  def deleteVipRouteBeforeCycle(cutoffCycle : Int) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + VIP_ROUTE_TABLE + " WHERE cycle < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffCycle)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " vip route records")
      deletedCount
    } finally {
      connection.close()
    }
  }
  
}