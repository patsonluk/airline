package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._

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
              LinkClassValues(Map(ECONOMY -> resultSet.getInt("price_economy"), BUSINESS -> resultSet.getInt("price_business"), FIRST -> resultSet.getInt("price_first"))),
              resultSet.getInt("distance"),
              LinkClassValues(Map(ECONOMY -> resultSet.getInt("capacity_economy"), BUSINESS -> resultSet.getInt("capacity_business"), FIRST -> resultSet.getInt("capacity_first"))),
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
          link.setAssignedAirplanes(assignedAirplanes.toList)        
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
    val result = loadLinksByCriteria(List(("id", linkId)), true)
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def loadAllLinks(fullLoad : Boolean = false) = {
      loadLinksByCriteria(List.empty, fullLoad)
  }
  
  def loadLinksByAirlineId(airlineId : Int, fullLoad : Boolean = false) = {
    loadLinksByCriteria(List(("airline", airlineId)), fullLoad)
  }
  
  def loadLinksByFromAirport(fromAirportId : Int, fullLoad : Boolean = false) = {
    loadLinksByCriteria(List(("from_airport", fromAirportId)), fullLoad)
  }
  
  def loadLinksByToAirport(toAirportId : Int, fullLoad : Boolean = false) = {
    loadLinksByCriteria(List(("to_airport", toAirportId)), fullLoad)
  }
  
  def saveLink(link : Link) : Option[Link] = {
     saveLink(link.from.id, link.to.id, link.airline.id, link.price, link.distance, link.capacity, link.rawQuality, link.duration, link.frequency, link.getAssignedAirplanes) match { 
       case Some(generatedId) => 
         link.id = generatedId
         Some(link)
       case None =>
         None
     }
  }
  
  def saveLink(fromAirportId : Int, toAirportId : Int, airlineId : Int, price : LinkClassValues, distance : Double, capacity : LinkClassValues, rawQuality : Int,  duration : Int, frequency : Int, airplanes : List[Airplane] = List.empty) : Option[Int] = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price_economy, price_business, price_first, distance, capacity_economy, capacity_business, capacity_first, quality, duration, frequency) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")

    try {
      preparedStatement.setInt(1, fromAirportId)
      preparedStatement.setInt(2, toAirportId)
      preparedStatement.setInt(3, airlineId)
      preparedStatement.setInt(4, price(ECONOMY))
      preparedStatement.setInt(5, price(BUSINESS))
      preparedStatement.setInt(6, price(FIRST))
      preparedStatement.setDouble(7, distance)
      preparedStatement.setInt(8, capacity(ECONOMY))
      preparedStatement.setInt(9, capacity(BUSINESS))
      preparedStatement.setInt(10, capacity(BUSINESS))
      preparedStatement.setInt(11, rawQuality)
      preparedStatement.setInt(12, duration)
      preparedStatement.setInt(13, frequency)
      
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
  def updateLink(link : Link) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("UPDATE " + LINK_TABLE + " SET price_economy = ?, price_business = ?, price_first = ?, capacity_economy = ?, capacity_business = ?, capacity_first = ?, quality = ?, duration = ?, frequency = ? WHERE id = ?")

    try {
      preparedStatement.setInt(1, link.price(ECONOMY))
      preparedStatement.setInt(2, link.price(BUSINESS))
      preparedStatement.setInt(3, link.price(FIRST))
      preparedStatement.setInt(4, link.capacity(ECONOMY))
      preparedStatement.setInt(5, link.capacity(BUSINESS))
      preparedStatement.setInt(6, link.capacity(FIRST))
      preparedStatement.setInt(7, link.rawQuality)
      preparedStatement.setInt(8, link.duration)
      preparedStatement.setInt(9, link.frequency)
      preparedStatement.setInt(10, link.id)
      
      val updateCount = preparedStatement.executeUpdate()
      println("Updated " + updateCount + " link!")
      
      if (updateCount > 0) {
          //try to save assigned airplanes if any
          updateAssignedPlanes(link.id, link.getAssignedAirplanes())
      }
      
      updateCount
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
    val preparedStatement = connection.prepareStatement("REPLACE INTO link_consumption(link, price_economy, price_business, price_first, capacity_economy, capacity_business, capacity_first, sold_seats_economy, sold_seats_business, sold_seats_first, quality, fuel_cost, crew_cost, airport_fees, inflight_cost, maintenance_cost, depreciation, revenue, profit, from_airport, to_airport, airline, distance, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")

    try {
      connection.setAutoCommit(false)
      linkConsumptions.foreach { linkConsumption =>
          preparedStatement.setInt(1, linkConsumption.linkId)
          preparedStatement.setInt(2, linkConsumption.price(ECONOMY))
          preparedStatement.setInt(3, linkConsumption.price(BUSINESS))
          preparedStatement.setInt(4, linkConsumption.price(FIRST))
          preparedStatement.setInt(5, linkConsumption.capacity(ECONOMY))
          preparedStatement.setInt(6, linkConsumption.capacity(BUSINESS))
          preparedStatement.setInt(7, linkConsumption.capacity(FIRST))
          preparedStatement.setInt(8, linkConsumption.soldSeats(ECONOMY))
          preparedStatement.setInt(9, linkConsumption.soldSeats(BUSINESS))
          preparedStatement.setInt(10, linkConsumption.soldSeats(FIRST))
          preparedStatement.setInt(11, linkConsumption.quality)
          preparedStatement.setInt(12, linkConsumption.fuelCost)
          preparedStatement.setInt(13, linkConsumption.crewCost)
          preparedStatement.setInt(14, linkConsumption.airportFees)
          preparedStatement.setInt(15, linkConsumption.inflightCost)
          preparedStatement.setInt(16, linkConsumption.maintenanceCost)
          preparedStatement.setInt(17, linkConsumption.depreciation)
          preparedStatement.setInt(18, linkConsumption.revenue)
          preparedStatement.setInt(19, linkConsumption.profit)
          preparedStatement.setInt(20, linkConsumption.fromAirportId)
          preparedStatement.setInt(21, linkConsumption.toAirportId)
          preparedStatement.setInt(22, linkConsumption.airlineId)
          preparedStatement.setInt(23, linkConsumption.distance)
          preparedStatement.setInt(24, linkConsumption.cycle)
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
          LinkClassValues(Map(ECONOMY -> resultSet.getInt("price_economy"), BUSINESS -> resultSet.getInt("price_business"), FIRST -> resultSet.getInt("price_first"))),
          LinkClassValues(Map(ECONOMY -> resultSet.getInt("capacity_economy"), BUSINESS -> resultSet.getInt("capacity_business"), FIRST -> resultSet.getInt("capacity_first"))),
          LinkClassValues(Map(ECONOMY -> resultSet.getInt("sold_seats_economy"), BUSINESS -> resultSet.getInt("sold_seats_business"), FIRST -> resultSet.getInt("sold_seats_first"))),
          resultSet.getInt("quality"),
          resultSet.getInt("fuel_cost"),
          resultSet.getInt("crew_cost"),
          resultSet.getInt("airport_fees"),
          resultSet.getInt("inflight_cost"),
          resultSet.getInt("maintenance_cost"),
          resultSet.getInt("depreciation"),
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