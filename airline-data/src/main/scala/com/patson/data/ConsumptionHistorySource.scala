package com.patson.data
import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer


import com.patson.model._
import java.util


object ConsumptionHistorySource {
  val updateConsumptions = (consumptions : Map[(PassengerGroup, Airport, Route), Int]) => {
    val connection = Meta.getConnection()
    val passengerHistoryStatement = connection.prepareStatement("INSERT INTO " + PASSENGER_HISTORY_TABLE + "(passenger_type, passenger_count, route_id, link, link_class, inverted, home_country, home_airport, destination_airport, preference_type) VALUES(?,?,?,?,?,?,?,?,?,?)")
    
    connection.setAutoCommit(false)
    
    connection.createStatement().executeUpdate("TRUNCATE TABLE " + PASSENGER_HISTORY_TABLE);
    
    var routeId = 0
    val batchSize = 1000
    
    try {
      consumptions.foreach { 
        case((passengerGroup, _, route), passengerCount) => {
          routeId += 1
          passengerHistoryStatement.setInt(1, passengerGroup.passengerType.id)
          passengerHistoryStatement.setInt(2, passengerCount)
          passengerHistoryStatement.setInt(3, routeId)
          passengerHistoryStatement.setString(7, passengerGroup.fromAirport.countryCode)
          passengerHistoryStatement.setInt(8, passengerGroup.fromAirport.id)
          passengerHistoryStatement.setInt(9, route.links.last.to.id)
          passengerHistoryStatement.setInt(10, passengerGroup.preference.getPreferenceType.id)
          route.links.foreach { linkConsideration =>  
            passengerHistoryStatement.setInt(4, linkConsideration.link.id)
            passengerHistoryStatement.setString(5, linkConsideration.linkClass.code)
            passengerHistoryStatement.setBoolean(6, linkConsideration.inverted)
            //passengerHistoryStatement.executeUpdate()
            passengerHistoryStatement.addBatch()
          }
          if (routeId % batchSize == 0) {
            passengerHistoryStatement.executeBatch()
            //println("inserted " + routeId)
          }
        }
      }
      passengerHistoryStatement.executeBatch()
      connection.commit()
    } finally {
      passengerHistoryStatement.close()
      connection.close()
    }
  }
  
//  def loadAllConsumptions() : List[(PassengerType.Value, Int, Route)] = {
//    val connection = Meta.getConnection()
//    val linkMap = LinkSource.loadAllLinks(LinkSource.SIMPLE_LOAD).map { link => (link.id , link) }.toMap
//    try {
//      val preparedStatement = connection.prepareStatement("SELECT * FROM " + PASSENGER_HISTORY_TABLE)
//
//      val resultSet = preparedStatement.executeQuery()
//
//      val routeConsumptions = new util.HashMap[Int, (PassengerType.Value, Int)]()
//      val linkConsiderations = new util.ArrayList[(Int, LinkConsideration)] //route_id, linkConsideration
//
//      println("Loaded all pax history")
//
//      while (resultSet.next()) {
//        linkMap.get(resultSet.getInt("link")).foreach { link =>
//          val routeId = resultSet.getInt("route_id")
//          val passengerType = PassengerType.apply(resultSet.getInt("passenger_type"))
//          val passengerCount = resultSet.getInt("passenger_count")
//          val linkConsideration = new LinkConsideration(link, 0, LinkClass.fromCode(resultSet.getString("link_class")), resultSet.getBoolean("inverted"))
//          linkConsiderations.add((routeId,  linkConsideration))
//          routeConsumptions.put(routeId, (passengerType, passengerCount))
//        }
//      }
//
//      resultSet.close()
//
//
//      println("Created all route consumptions")
//
//      val linkConsiderationsByRouteId = new util.HashMap[Int, ListBuffer[LinkConsideration]]()
//      for (linkConsideration <- linkConsiderations.asScala) {
//        var considerations = linkConsiderationsByRouteId.get(linkConsideration._1)
//        if (considerations == null) {
//          considerations = ListBuffer[LinkConsideration]()
//          linkConsiderationsByRouteId.put(linkConsideration._1, considerations)
//        }
//        considerations += linkConsideration._2
//      }
//
//      println("Finished grouping considerations as route")
//
//      val allRoutes = linkConsiderationsByRouteId.asScala.map {
//        case ((routeId, considerations : ListBuffer[LinkConsideration])) => new Route(considerations.toList, 0 , routeId)
//      }
//
//
////      val allRoutes = linkConsiderations.groupBy(_._1).map {
////        case (routeId, linkConsiderationsByRoute) => new Route(linkConsiderationsByRoute.map(_._2).toList, 0, routeId)
////      }
//      println("Finished rebuilding all routes")
//
//      allRoutes.map { route =>
//        val consumption = routeConsumptions.get(route.id)
//        (consumption._1, consumption._2, route)
//      }.toList
//    } finally {
//      connection.close()
//    }
//  }

  def loadConsumptionsByAirport(airportId : Int) : Map[Link, Int] = {
    val connection = Meta.getConnection()
    try {
      val links = LinkSource.loadLinksByFromAirport(airportId) ++ LinkSource.loadLinksByToAirport(airportId)
      if (links.isEmpty) {
        Map.empty
      } else {
        val linksById = links.map(link => (link.id, link)).toMap
        val queryString = new StringBuilder("SELECT * FROM " + PASSENGER_HISTORY_TABLE + " where link IN (");
        for (i <- 0 until links.size - 1) {
          queryString.append("?,")
        }
        queryString.append("?)")

        val preparedStatement = connection.prepareStatement(queryString.toString())

        for (i <- 0 until links.size) {
          preparedStatement.setInt(i + 1, links(i).id)
        }

        val resultSet = preparedStatement.executeQuery()
        val result = scala.collection.mutable.HashMap[Link, Int]()
        while (resultSet.next()) {
          //        val passengerType = PassengerType.apply(resultSet.getInt("passenger_type"))
          val passengerCount = resultSet.getInt("passenger_count")
          val linkId = resultSet.getInt("link")
          val link = linksById.getOrElse(linkId, Link.fromId(linkId))
          if (result.contains(link)) {
            result.put(link, result(link) + passengerCount)
          } else {
            result.put(link, passengerCount)
          }
        }

        result.toMap
      }
    } finally {
      connection.close()
    }

  }

  def loadConsumptionsByAirportPair(fromAirportId : Int, toAirportId : Int) : Map[Route, (PassengerType.Value, Int)] = {
    val connection = Meta.getConnection()
    try {
      val queryString = new StringBuilder("SELECT * FROM " + PASSENGER_HISTORY_TABLE + " where (home_airport = ? AND destination_airport = ?)")
      val preparedStatement = connection.prepareStatement(queryString.toString())

      preparedStatement.setInt(1, fromAirportId)
      preparedStatement.setInt(2, toAirportId)

      val resultSet = preparedStatement.executeQuery()

      val linkConsiderationsByRouteId = scala.collection.mutable.Map[Int, ListBuffer[LinkConsideration]]()
      val routeConsumptions = new scala.collection.mutable.HashMap[Int, (PassengerType.Value, Int)]()

      val allLinkIds = scala.collection.mutable.HashSet[Int]()

      while (resultSet.next()) {
        val linkId = resultSet.getInt("link")
        allLinkIds += linkId
      }

      val linkConsumptionById: Map[Int, LinkConsumptionDetails] = LinkSource.loadLinkConsumptionsByLinksId(allLinkIds.toList).map(entry => (entry.link.id, entry)).toMap

      //filter out routes that cannot find history (should not happen), and replace the value with LinkConsideration

      resultSet.beforeFirst()
      while (resultSet.next()) {
        val linkId = resultSet.getInt("link")
        val routeId = resultSet.getInt("route_id")
        val passengerType = PassengerType.apply(resultSet.getInt("passenger_type"))
        val passengerCount = resultSet.getInt("passenger_count")
        linkConsumptionById.get(linkId).foreach { linkConsumption =>
          val linkConsideration = new LinkConsideration(linkConsumption.link, 0, LinkClass.fromCode(resultSet.getString("link_class")), resultSet.getBoolean("inverted"))
          val existingConsiderationsForThisRoute = linkConsiderationsByRouteId.getOrElseUpdate(routeId, ListBuffer[LinkConsideration]())

          existingConsiderationsForThisRoute += linkConsideration
        }
        routeConsumptions.put(routeId, (passengerType, passengerCount))
      }

      val result : Map[Route, (PassengerType.Value, Int)] = linkConsiderationsByRouteId.view.map {
        case (routeId: Int, considerations: ListBuffer[LinkConsideration]) => (new Route(considerations.toList, 0, routeId), routeConsumptions(routeId))
      }.toMap

      println(s"Loaded ${result.size} routes for airport pair ${fromAirportId} and ${toAirportId})")

      result
    } finally {
      connection.close()
    }
  }

  def loadRelatedConsumptionByLinkId(linkId : Int) : Map[Route, (PassengerType.Value, Int)] = {
    LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD) match {
      case Some(link) =>
        val connection = Meta.getConnection()
        try {
          val preparedStatement = connection.prepareStatement("SELECT route_id FROM " + PASSENGER_HISTORY_TABLE + " WHERE link = ? ")

          preparedStatement.setInt(1, linkId)
          val resultSet = preparedStatement.executeQuery()

          val relatedRouteIds = new ListBuffer[Int]()
          while (resultSet.next()) {
            relatedRouteIds += resultSet.getInt("route_id")
          }

          if (relatedRouteIds.isEmpty) {
            Map.empty
          } else {
            val queryString = new StringBuilder("SELECT * FROM " + PASSENGER_HISTORY_TABLE + " where route_id IN (");
            for (i <- 0 until relatedRouteIds.size - 1) {
              queryString.append("?,")
            }
            queryString.append("?)")

            val relatedRouteStatement = connection.prepareStatement(queryString.toString())

            for (i <- 0 until relatedRouteIds.size) {
              relatedRouteStatement.setInt(i + 1, relatedRouteIds(i))
            }

            val relatedRouteSet = relatedRouteStatement.executeQuery()

            val linkConsiderationsByRouteId = scala.collection.mutable.Map[Int, ListBuffer[LinkConsideration]]()
            val routeConsumptions = new scala.collection.mutable.HashMap[Int, (PassengerType.Value, Int)]()

            val relatedLinkIds = scala.collection.mutable.HashSet[Int]()
            while (relatedRouteSet.next()) {
              relatedLinkIds += relatedRouteSet.getInt("link")
            }
            val linkMap = LinkSource.loadLinksByIds(relatedLinkIds.toList).map(link => (link.id, link)).toMap

            relatedRouteSet.beforeFirst()
            while (relatedRouteSet.next()) {
              val routeId = relatedRouteSet.getInt("route_id")
              val passengerType = PassengerType.apply(relatedRouteSet.getInt("passenger_type"))
              val passengerCount = relatedRouteSet.getInt("passenger_count")
              val relatedLinkId = relatedRouteSet.getInt("link")
              val relatedLink = linkMap.getOrElse(relatedLinkId, Link.fromId(relatedLinkId))
              val linkConsideration = new LinkConsideration(relatedLink, 0, LinkClass.fromCode(relatedRouteSet.getString("link_class")), relatedRouteSet.getBoolean("inverted"))

              val existingConsiderationsForThisRoute = linkConsiderationsByRouteId.getOrElseUpdate(routeId, ListBuffer[LinkConsideration]())

              existingConsiderationsForThisRoute += linkConsideration
              routeConsumptions.put(routeId, (passengerType, passengerCount))
            }

            val result = linkConsiderationsByRouteId.map {
              case (routeId: Int, considerations: ListBuffer[LinkConsideration]) => (new Route(considerations.toList, 0, routeId), routeConsumptions(routeId))
            }.toMap

            println("Loaded " + result.size + " routes related to link " + link);

            result
          }
        } finally {
          connection.close()
        }
      case None => Map.empty
    }
  }
  
  def loadConsumptionByLinkId(linkId : Int) : List[LinkConsumptionHistory] = {
    LinkSource.loadLinkById(linkId) match {
      case Some(link) => 
        val connection = Meta.getConnection()
        try {  
          val preparedStatement = connection.prepareStatement("SELECT * FROM " + PASSENGER_HISTORY_TABLE + " WHERE link = ? ")
    
          preparedStatement.setInt(1, linkId)
          val resultSet = preparedStatement.executeQuery()
          
          val result = new ListBuffer[LinkConsumptionHistory]()
          while (resultSet.next()) {
            result += LinkConsumptionHistory(link = link, 
                passengerCount = resultSet.getInt("passenger_count"), 
                homeCountryCode = resultSet.getString("home_country"), 
                passengerType = PassengerType(resultSet.getInt("passenger_type")), 
                preferenceType = FlightPreferenceType(resultSet.getInt("preference_type")))
          }
        
          
          result.toList
        } finally {
          connection.close()
        }
      case None => List.empty
    }
  }
}