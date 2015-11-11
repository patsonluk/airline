package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.Airport
import com.patson.model.Link
import com.patson.model.Airline

object LinkSource {
  def loadLinksByCriteria(criteria : List[(String, Any)]) = {
      val connection = Meta.getConnection()
      
      var queryString = "SELECT from_airport, to_airport, airline, price, distance, capacity, id FROM link"
      
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
      
      val links = new ListBuffer[Link]()
      while (resultSet.next()) {
        val fromAirport = AirportSource.loadAirportById(resultSet.getInt("from_airport"))
        val toAirport = AirportSource.loadAirportById(resultSet.getInt("to_airport"))
        val airline = AirlineSource.loadAirlineById(resultSet.getInt("airline"))
        
        if (fromAirport.isEmpty || toAirport.isEmpty) {
          println("Cannot load link, airports not found !")
        } else {
          val link = Link( 
              fromAirport.get,
              toAirport.get,
              airline.get,
              resultSet.getDouble("price"),
              resultSet.getDouble("distance"),
              resultSet.getInt("capacity"))
          link.id = resultSet.getInt("id")
          links += link    
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      println("Loaded " + links.length + " link records")
      links.toList
  }
  def loadLinkById(linkId : Int) = {
    loadLinksByCriteria(List(("id", linkId)))
  }
  
  def loadAllLinks() = {
      loadLinksByCriteria(List.empty)
  }
  
  def saveLink(link : Link) : Option[Link] = {
     saveLink(link.from.id, link.to.id, link.airline.id, link.price, link.distance, link.capacity) match { 
       case Some(generatedId) => 
         link.id = generatedId
         Some(link)
       case None =>
         None
     }
  }
  
  def saveLink(fromAirportId : Int, toAirportId : Int, airlineId : Int, price : Double, distance : Double, capacity : Int) : Option[Int] = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO link(from_airport, to_airport, airline, price, distance, capacity) VALUES(?,?,?,?,?,?)")

    try {
      preparedStatement.setInt(1, fromAirportId)
      preparedStatement.setInt(2, toAirportId)
      preparedStatement.setInt(3, airlineId)
      preparedStatement.setDouble(4, price)
      preparedStatement.setDouble(5, distance)
      preparedStatement.setInt(6, capacity)
      
      val updateCount = preparedStatement.executeUpdate()
      println("Saved " + updateCount + " link!")
      
      if (updateCount > 0) {
        val generatedKeys = preparedStatement.getGeneratedKeys
        while (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          println("Id is " + generatedId)
          return Some(generatedId)
        }
      }
      None
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }
  
  def deleteLink(linkId : Int) = {
    deleteLinksByCriteria(List(("id", linkId)))
  }
  
  def deleteAllLinks() = {
    deleteLinksByCriteria(List.empty)
  }
  
  def deleteLinksByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
      var queryString = "DELETE FROM link "
      
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
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      connection.close()
      
      println("Deleted " + deletedCount + " link records")
      deletedCount
  }
  
  def saveLinkConsumptions(linkConsumptions: List[(Link, Int)]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("REPLACE INTO link_consumption(link, consumption) VALUES(?,?)")

    connection.setAutoCommit(false)
    linkConsumptions.foreach {
      case(link, consumption) => 
        preparedStatement.setInt(1, link.id)
        preparedStatement.setInt(2, consumption)
        preparedStatement.executeUpdate()
    }
    connection.commit
    preparedStatement.close()
    connection.close()
  }
  
  def loadLinkConsumptions() = {
    loadLinkConsumptionsByAirline(None)
  }
  def loadLinkConsumptionsByAirline(airline : Option[Airline]) = {
    val connection = Meta.getConnection()
      
      var queryString = "SELECT l.id, from_airport, to_airport, airline, price, distance, capacity, lc.consumption FROM link l LEFT JOIN link_consumption lc ON l.id = lc.link"
      
      if (airline != None) {
        queryString += " WHERE airline = ?" 
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      if (airline != None) {
        preparedStatement.setInt(1, airline.get.id)
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val links = new ListBuffer[(Link, Int)]()
      while (resultSet.next()) {
        val fromAirport = AirportSource.loadAirportById(resultSet.getInt("from_airport"))
        val toAirport = AirportSource.loadAirportById(resultSet.getInt("to_airport"))
        val airline = AirlineSource.loadAirlineById(resultSet.getInt("airline"))
        
        if (fromAirport.isEmpty || toAirport.isEmpty) {
          println("Cannot load link, airports not found !")
        } else {
          val link = Link( 
              fromAirport.get,
              toAirport.get,
              airline.get,
              resultSet.getDouble("price"),
              resultSet.getDouble("distance"),
              resultSet.getInt("capacity"))
          link.id = resultSet.getInt("id")
          links += Tuple2(link, resultSet.getInt("consumption"))
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      println("Loaded " + links.length + " link records")
      links.toList
  }

}