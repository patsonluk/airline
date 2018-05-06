package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement
import java.sql.ResultSet


object ConsumptionHistorySource {
  val updateConsumptions = (consumptions : List[(PassengerGroup, Airport, Int, Route)]) => {
     //open the hsqldb
    val connection = Meta.getConnection()
    val passengerHistoryStatement = connection.prepareStatement("INSERT INTO " + PASSENGER_HISTORY_TABLE + "(passenger_type, passenger_count) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
    val routeStatement = connection.prepareStatement("INSERT INTO " + ROUTE_CONSUMPTION_TABLE + "(passenger_group, cost) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS)
    val linkConsiderationStatement = connection.prepareStatement("INSERT INTO " + LINK_CONSIDERATION_TABLE + "(route, link, cost, link_class, inverted) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    
    connection.setAutoCommit(false)
    
    connection.createStatement().executeUpdate("DELETE FROM " + PASSENGER_HISTORY_TABLE);
    
    try {
      consumptions.foreach { consumption =>
        passengerHistoryStatement.setInt(1, consumption._1.passengerType.id)
        passengerHistoryStatement.setInt(2, consumption._3)
        passengerHistoryStatement.executeUpdate()
        val passengerHistoryKeys = passengerHistoryStatement.getGeneratedKeys
        
        if (passengerHistoryKeys.next()) {
          val passengerGroupId = passengerHistoryKeys.getInt(1)
          val route = consumption._4
          routeStatement.setInt(1, passengerGroupId)
          routeStatement.setDouble(2, route.totalCost)
          routeStatement.executeUpdate()
          
          val routeKeys = routeStatement.getGeneratedKeys
          
          if (routeKeys.next()) {
            route.id = routeKeys.getInt(1)
          }
          
          route.links.foreach { linkConsideration =>
            linkConsiderationStatement.setInt(1, route.id)
            linkConsiderationStatement.setInt(2, linkConsideration.link.id)
            linkConsiderationStatement.setDouble(3, linkConsideration.cost)
            linkConsiderationStatement.setString(4, linkConsideration.linkClass.code)
            linkConsiderationStatement.setBoolean(5, linkConsideration.inverted)
            linkConsiderationStatement.executeUpdate()
  
            val linkConsiderationKeys = linkConsiderationStatement.getGeneratedKeys
            if (linkConsiderationKeys.next()) {
              val generatedId = linkConsiderationKeys.getInt(1)
              linkConsideration.id = generatedId
            }
          }
        }
      }
      
      connection.commit()
    } finally {
      linkConsiderationStatement.close()
      connection.close()
    }
  }
  
  
  val loadConsumptionByLink : (Link => List[(PassengerType.Value, Int, Route)]) = (link : Link) => {
    val connection = Meta.getConnection()
    try {  
      val preparedStatement = connection.prepareStatement("SELECT rc.id as route_id, ph.passenger_count as passenger_count, ph.passenger_type as passenger_type FROM " + LINK_CONSIDERATION_TABLE + " lc "
          + " JOIN " + ROUTE_CONSUMPTION_TABLE + " rc ON lc.link = ? AND lc.route = rc.id "
          + " JOIN " + PASSENGER_HISTORY_TABLE + " ph ON rc.passenger_group = ph.id")
      preparedStatement.setInt(1, link.id)
      val resultSet = preparedStatement.executeQuery()
      
      val routeConsumptions = new ListBuffer[(PassengerType.Value, Int, Route)]
      while (resultSet.next()) {
        val passengerType = PassengerType.apply(resultSet.getInt("passenger_type"))
        val passengerCount = resultSet.getInt("passenger_count")
        val routeId = resultSet.getInt("route_id")
        
        loadRouteById(routeId).foreach { route =>
          val routeConsumption = (passengerType, passengerCount, route)
          routeConsumptions += routeConsumption  
        }
        
      }
      routeConsumptions.toList
    } finally {
      connection.close()
    }
  }
  
  val loadRouteById : Int => Option[Route] = (routeId : Int) => {
    val result = loadRoutesByCriteria(List(("id", routeId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  val loadRoutesByCriteria : List[(String, Any)] => List[Route] = (criteria : List[(String, Any)]) => {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT id, cost FROM " + LINK_CONSIDERATION_TABLE   
      
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
      
      val routes = new ListBuffer[Route]
      
      while (resultSet.next()) {
        val routeId = resultSet.getInt(1)
        val cost = resultSet.getDouble(2)
        //now load all link considerations
        val queryString = "SELECT * FROM " + LINK_CONSIDERATION_TABLE + " WHERE route=?"
        val linkConsiderationStatement = connection.prepareStatement(queryString)
        linkConsiderationStatement.setInt(1, routeId)
        val linkConsiderationResult = linkConsiderationStatement.executeQuery()
        
        val linkConsiderations = new ListBuffer[LinkConsideration]
        while (linkConsiderationResult.next()) {
          val linkId = linkConsiderationResult.getInt("link") 
          val link = LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD)
          link.foreach { 
            val linkClass = LinkClass.fromCode(linkConsiderationResult.getString("link_class"))
            linkConsiderations += LinkConsideration(_, linkConsiderationResult.getDouble("cost"), linkClass, linkConsiderationResult.getBoolean("inverted")) 
          }
        }
        
        routes += Route(linkConsiderations.toList, cost, routeId);
      }
      
      routes.toList
    } finally {
      connection.close()
    }
  }
  
}