package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement


object LinkSource {
  val FULL_LOAD = Map(DetailType.AIRLINE -> true, DetailType.AIRPORT -> true, DetailType.AIRPLANE -> true)
  val SIMPLE_LOAD = Map(DetailType.AIRLINE -> false, DetailType.AIRPORT -> false, DetailType.AIRPLANE -> false)
  val ID_LOAD : Map[DetailType.Type, Boolean] = Map.empty
  
  def loadLinksByCriteria(criteria : List[(String, Any)], loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    val connection = Meta.getConnection()
    val airportCache = scala.collection.mutable.Map[Int, Airport]()
    val airlineCache = scala.collection.mutable.Map[Int, Airline]()
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
      
      val loadAirportFunction : (Int => Airport) = loadDetails.get(DetailType.AIRPORT) match {
        case Some(fullLoad) => (airportId : Int) => AirportSource.loadAirportById(airportId, fullLoad).get 
        case None => (airportId : Int) => Airport.fromId(airportId) 
      }
      
      val loadAirlineFunction : (Int => Airline) = loadDetails.get(DetailType.AIRLINE) match {
        case Some(fullLoad) => (airlineId : Int) => AirlineSource.loadAirlineById(airlineId, fullLoad).get 
        case None => (airlineId : Int) => Airline.fromId(airlineId) 
      }
      
      val loadAirplaneFunction : (Int => Airline) = loadDetails.get(DetailType.AIRLINE) match {
        case Some(fullLoad) => (airlineId : Int) => AirlineSource.loadAirlineById(airlineId, fullLoad).get 
        case None => (airlineId : Int) => Airline.fromId(airlineId) 
      }
      
      val links = new ListBuffer[Link]()
      while (resultSet.next()) {
        val fromAirportId = resultSet.getInt("from_airport")
        val toAirportId = resultSet.getInt("to_airport")
        val airlineId = resultSet.getInt("airline")
        
        val fromAirport = airportCache.getOrElseUpdate(fromAirportId, loadAirportFunction(fromAirportId))
        val toAirport = airportCache.getOrElseUpdate(toAirportId, loadAirportFunction(toAirportId))
        val airline = airlineCache.getOrElseUpdate(airlineId, loadAirlineFunction(airlineId))
        
        val link = Link( 
            fromAirport,
            toAirport,
            airline,
            LinkClassValues(Map(ECONOMY -> resultSet.getInt("price_economy"), BUSINESS -> resultSet.getInt("price_business"), FIRST -> resultSet.getInt("price_first"))),
            resultSet.getInt("distance"),
            LinkClassValues(Map(ECONOMY -> resultSet.getInt("capacity_economy"), BUSINESS -> resultSet.getInt("capacity_business"), FIRST -> resultSet.getInt("capacity_first"))),
            resultSet.getInt("quality"),
            resultSet.getInt("duration"),
            resultSet.getInt("frequency"))
        link.id = resultSet.getInt("id")
        
        loadDetails.get(DetailType.AIRPLANE) match {
          case Some(fullLoad) => //fulload doesnt make a diff here...
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
            linkAssignmentStatement.close()
          case None => //do not load assigned airplanes
        }
        links += link   
      }
      
      resultSet.close()
      preparedStatement.close()
      links.toList
    } finally {
      connection.close()
    }
  }
  
  def loadLinkById(linkId : Int, loadDetails : Map[DetailType.Value, Boolean] = FULL_LOAD) : Option[Link] = {
    val result = loadLinksByCriteria(List(("id", linkId)), loadDetails)
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  def loadLinkByAirportsAndAirline(fromAirportId : Int,  toAirportId : Int, airlineId : Int, loadDetails : Map[DetailType.Value, Boolean] = FULL_LOAD) : Option[Link] = {
    val result = loadLinksByCriteria(List(("from_airport", fromAirportId), ("to_airport", toAirportId), ("airline", airlineId)), loadDetails)
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  def loadLinksByAirports(fromAirportId : Int, toAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = FULL_LOAD) : List[Link] = {
    loadLinksByCriteria(List(("from_airport", fromAirportId), ("to_airport", toAirportId)), loadDetails)
  }
  
  def loadAllLinks(loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
      loadLinksByCriteria(List.empty, loadDetails)
  }
  
  def loadLinksByAirlineId(airlineId : Int, loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    loadLinksByCriteria(List(("airline", airlineId)), loadDetails)
  }
  
  def loadLinksByFromAirport(fromAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    loadLinksByCriteria(List(("from_airport", fromAirportId)), loadDetails)
  }
  
  def loadLinksByToAirport(toAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    loadLinksByCriteria(List(("to_airport", toAirportId)), loadDetails)
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
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price_economy, price_business, price_first, distance, capacity_economy, capacity_business, capacity_first, quality, duration, frequency) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)

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
      preparedStatement.setInt(10, capacity(FIRST))
      preparedStatement.setInt(11, rawQuality)
      preparedStatement.setInt(12, duration)
      preparedStatement.setInt(13, frequency)
      
      val updateCount = preparedStatement.executeUpdate()
      //println("Saved " + updateCount + " link!")
      
      if (updateCount > 0) {
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
        //  println("Id is " + generatedId)
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
  
  def saveLinks(links : List[Link]) : Int = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price_economy, price_business, price_first, distance, capacity_economy, capacity_business, capacity_first, quality, duration, frequency) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    var updateCount = 0
    connection.setAutoCommit(false)
    try {
      links.foreach { link =>
        preparedStatement.setInt(1, link.from.id)
        preparedStatement.setInt(2, link.to.id)
        preparedStatement.setInt(3, link.airline.id)
        preparedStatement.setInt(4, link.price(ECONOMY))
        preparedStatement.setInt(5, link.price(BUSINESS))
        preparedStatement.setInt(6, link.price(FIRST))
        preparedStatement.setDouble(7, link.distance)
        preparedStatement.setInt(8, link.capacity(ECONOMY))
        preparedStatement.setInt(9, link.capacity(BUSINESS))
        preparedStatement.setInt(10, link.capacity(FIRST))
        preparedStatement.setInt(11, link.rawQuality)
        preparedStatement.setInt(12, link.duration)
        preparedStatement.setInt(13, link.frequency)
        
        updateCount += preparedStatement.executeUpdate()
        //println("Saved " + updateCount + " link!")
        
        if (updateCount > 0) {
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            link.id = generatedId
          }
        }
      }
      connection.commit()
    } finally {
      preparedStatement.close()
      connection.close()
    }
    links.foreach { link =>
      updateAssignedPlanes(link.id, link.getAssignedAirplanes())
    }
    updateCount
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
    val preparedStatement = connection.prepareStatement("INSERT INTO link_consumption(link, price_economy, price_business, price_first, capacity_economy, capacity_business, capacity_first, sold_seats_economy, sold_seats_business, sold_seats_first, quality, fuel_cost, crew_cost, airport_fees, inflight_cost, maintenance_cost, depreciation, revenue, profit, from_airport, to_airport, airline, distance, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")

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
  
  object DetailType extends Enumeration {
    type Type = Value
    val AIRPORT, AIRLINE, AIRPLANE = Value
  }
}