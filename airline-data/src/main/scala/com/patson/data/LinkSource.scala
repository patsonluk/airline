package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.Airport
import com.patson.model.Link
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model.LinkConsumptionDetails
import com.patson.model.LinkConsumptionDetails
import com.patson.model.LinkConsumptionDetails

object LinkSource {
  def loadLinksByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + LINK_TABLE 
      
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
        val fromAirport = AirportSource.loadAirportById(resultSet.getInt("from_airport"), fullLoad)
        val toAirport = AirportSource.loadAirportById(resultSet.getInt("to_airport"), fullLoad)
        val airline = AirlineSource.loadAirlineById(resultSet.getInt("airline"), fullLoad)
        if (fromAirport.isEmpty || toAirport.isEmpty) {
          println("Cannot load link, airports not found !")
        } else {
          val link = Link( 
              fromAirport.get,
              toAirport.get,
              airline.get,
              resultSet.getInt("price"),
              resultSet.getInt("distance"),
              resultSet.getInt("capacity"),
              resultSet.getInt("quality"),
              resultSet.getInt("duration"),
              resultSet.getInt("frequency"))
          link.id = resultSet.getInt("id")
          
          val linkAssignmentStatement = connection.prepareStatement("SELECT airplane FROM " + LINK_ASSIGNMENT_TABLE + " WHERE link = ?")
          linkAssignmentStatement.setInt(1, link.id)
          val assignmentResult = linkAssignmentStatement.executeQuery();
          val assignedAirplanes = ListBuffer[Airplane]()
          while (assignmentResult.next()) {
            AirplaneSource.loadAirplaneById(assignmentResult.getInt("airplane")) match {
              case Some(airplane) => assignedAirplanes.append(airplane)
              case None => println("cannot load assigned airplane with id " + assignmentResult.getInt("airplane"))
            }
          }
          link.assignedAirplanes = assignedAirplanes.toList        
          links += link   
          linkAssignmentStatement.close()
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      links.toList
    } finally {
      connection.close()
    }
  }
  
  def loadLinkById(linkId : Int) : Option[Link] = {
    val result = loadLinksByCriteria(List(("id", linkId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def loadAllLinks(fullLoad : Boolean = false) = {
      loadLinksByCriteria(List.empty, fullLoad)
  }
  
  def loadLinksByAirlineId(airlineId : Int) = {
    loadLinksByCriteria(List(("airline", airlineId)))
  }
  
  def saveLink(link : Link) : Option[Link] = {
     saveLink(link.from.id, link.to.id, link.airline.id, link.price, link.distance, link.capacity, link.rawQuality, link.duration, link.frequency, link.assignedAirplanes) match { 
       case Some(generatedId) => 
         link.id = generatedId
         Some(link)
       case None =>
         None
     }
  }
  
  def saveLink(fromAirportId : Int, toAirportId : Int, airlineId : Int, price : Int, distance : Double, capacity : Int, rawQuality : Int,  duration : Int, frequency : Int, airplanes : List[Airplane] = List.empty) : Option[Int] = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price, distance, capacity, quality, duration, frequency) VALUES(?,?,?,?,?,?,?,?,?)")

    try {
      preparedStatement.setInt(1, fromAirportId)
      preparedStatement.setInt(2, toAirportId)
      preparedStatement.setInt(3, airlineId)
      preparedStatement.setInt(4, price)
      preparedStatement.setDouble(5, distance)
      preparedStatement.setInt(6, capacity)
      preparedStatement.setInt(7, rawQuality)
      preparedStatement.setInt(8, duration)
      preparedStatement.setInt(9, frequency)
      
      val updateCount = preparedStatement.executeUpdate()
      println("Saved " + updateCount + " link!")
      
      if (updateCount > 0) {
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          println("Id is " + generatedId)
          //try to save assigned airplanes if any
          updateAssignedPlanes(generatedId, airplanes)
          return Some(generatedId)
        }
      }
      None
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }
  def updateAssignedPlanes(linkId : Int, airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      
      //remove all the existing ones assigned to this link
      val removeStatement = connection.prepareStatement("DELETE FROM " + LINK_ASSIGNMENT_TABLE + " WHERE link = ?")
      removeStatement.setInt(1, linkId)
      removeStatement.executeUpdate()
      removeStatement.close()
      
      
      airplanes.foreach { airplane => 
        val insertStatement = connection.prepareStatement("INSERT INTO " + LINK_ASSIGNMENT_TABLE + "(link, airplane) VALUES(?,?)")
        insertStatement.setInt(1, linkId)
        insertStatement.setInt(2, airplane.id)
        insertStatement.executeUpdate()
        insertStatement.close
      }    
      
      connection.commit()
    } finally {
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
    try {  
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
      println("Deleted " + deletedCount + " link records")
      deletedCount
    } finally {
      connection.close()
    }
      
  }
  
  def saveLinkConsumptions(linkConsumptions: List[LinkConsumptionDetails]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("REPLACE INTO link_consumption(link, price, capacity, sold_seats, fuel_cost, crew_cost, fixed_cost, revenue, profit, from_airport, to_airport, airline, distance, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")

    try {
      connection.setAutoCommit(false)
      linkConsumptions.foreach { linkConsumption =>
          preparedStatement.setInt(1, linkConsumption.linkId)
          preparedStatement.setInt(2, linkConsumption.price)
          preparedStatement.setInt(3, linkConsumption.capacity)
          preparedStatement.setInt(4, linkConsumption.soldSeats)
          preparedStatement.setInt(5, linkConsumption.fuelCost)
          preparedStatement.setInt(6, linkConsumption.crewCost)
          preparedStatement.setInt(7, linkConsumption.fixedCost)
          preparedStatement.setInt(8, linkConsumption.revenue)
          preparedStatement.setInt(9, linkConsumption.profit)
          preparedStatement.setInt(10, linkConsumption.fromAirportId)
          preparedStatement.setInt(11, linkConsumption.toAirportId)
          preparedStatement.setInt(12, linkConsumption.airlineId)
          preparedStatement.setInt(13, linkConsumption.distance)
          preparedStatement.setInt(14, linkConsumption.cycle)
          preparedStatement.executeUpdate()
        }
      preparedStatement.close()
      connection.commit
    } finally {
      connection.close()
    }
  }
  def deleteLinkConsumptionsByCycle(cyclesFromLatest : Int) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    try {
      val latestCycleStatement = connection.prepareStatement("SELECT MAX(cycle) FROM " + LINK_CONSUMPTION_TABLE)
      val resultSet = latestCycleStatement.executeQuery()
      val latestCycle = if (resultSet.next()) { resultSet.getInt(1) } else 0
      latestCycleStatement.close()  
      
      val deleteFrom = if (latestCycle - cyclesFromLatest < 0) 0 else latestCycle - cyclesFromLatest 
      
      val deleteStatement = connection.prepareStatement("DELETE FROM " + LINK_CONSUMPTION_TABLE + " WHERE cycle <= ?")
      deleteStatement.setInt(1, deleteFrom)
      val deletedConsumption = deleteStatement.executeUpdate()
      deleteStatement.close()
      deletedConsumption
    } finally {
      connection.close()
    }
  }
  
  def loadLinkConsumptions(cycleCount : Int = 1) = {
    loadLinkConsumptionsByCriteria(List.empty, cycleCount)
  }
  
  def loadLinkConsumptionsByLinkId(linkId : Int, cycleCount : Int = 1) = {
    loadLinkConsumptionsByCriteria(List(("link", linkId)), cycleCount)
  }
  
  def loadLinkConsumptionsByAirline(airlineId : Int, cycleCount : Int = 1) = {
    loadLinkConsumptionsByCriteria(List(("airline", airlineId)), cycleCount)
  }
  
  
  def loadLinkConsumptionsByCriteria(criteria : List[(String, Any)], cycleCount : Int) = {
    val connection = Meta.getConnection()
      
    try {
      val latestCycleStatement = connection.prepareStatement("SELECT MAX(cycle) FROM " + LINK_CONSUMPTION_TABLE)
      val latestCycleResultSet = latestCycleStatement.executeQuery()
      val latestCycle = if (latestCycleResultSet.next()) { latestCycleResultSet.getInt(1) } else 0
      latestCycleStatement.close()
      
      var queryString = "SELECT * FROM link_consumption WHERE cycle > ?"
      
      if (!criteria.isEmpty) {
        queryString += " AND "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
    
      queryString += " ORDER BY cycle DESC"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setInt(1, latestCycle - cycleCount)
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 2, criteria(i)._2)
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val linkConsumptions = new ListBuffer[LinkConsumptionDetails]()
      while (resultSet.next()) {
          linkConsumptions.append(LinkConsumptionDetails(
          resultSet.getInt("link"),
          resultSet.getInt("price"),
          resultSet.getInt("capacity"),
          resultSet.getInt("sold_seats"),
          resultSet.getInt("fuel_cost"),
          resultSet.getInt("crew_cost"),
          resultSet.getInt("fixed_cost"),
          resultSet.getInt("revenue"),
          resultSet.getInt("profit"),
          resultSet.getInt("from_airport"),
          resultSet.getInt("to_airport"),
          resultSet.getInt("airline"),
          resultSet.getInt("distance"),
          resultSet.getInt("cycle")))
      }
      
      resultSet.close()
      preparedStatement.close()
      linkConsumptions.toList
    } finally {
      connection.close()
    }
  }
}