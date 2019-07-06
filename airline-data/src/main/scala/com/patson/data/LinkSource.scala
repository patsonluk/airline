package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement
import scala.collection.mutable.HashSet
import java.sql.Connection
import scala.collection.mutable.HashMap
 


object LinkSource {
  val FULL_LOAD = Map(DetailType.AIRLINE -> true, DetailType.AIRPORT -> true, DetailType.AIRPLANE -> true)
  val SIMPLE_LOAD = Map(DetailType.AIRLINE -> false, DetailType.AIRPORT -> false, DetailType.AIRPLANE -> false)
  val ID_LOAD : Map[DetailType.Type, Boolean] = Map.empty
  
  private[this]val BASE_QUERY = "SELECT * FROM " + LINK_TABLE
  
  def loadLinksByCriteria(criteria : List[(String, Any)], loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    var queryString = BASE_QUERY 
      
    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    
    loadLinksByQueryString(queryString, criteria.map(_._2), loadDetails)
  }
  
  def loadLinksByIds(ids : List[Int], loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(BASE_QUERY + " where id IN (");
      for (i <- 0 until ids.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadLinksByQueryString(queryString.toString(), ids, loadDetails)
    }
  }
  
  def loadLinksByQueryString(queryString : String, parameters : List[Any], loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) = {
    val connection = Meta.getConnection()
    
    try {  
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val links = new ListBuffer[Link]()
      
      val airportIds : Set[Int] = new HashSet[Int]
      val airlineIds : Set[Int] = new HashSet[Int]
      val linkIds : Set[Int] = new HashSet[Int]
      
      while (resultSet.next()) {
        airportIds += resultSet.getInt("from_airport")
        airportIds += resultSet.getInt("to_airport")
        airlineIds += resultSet.getInt("airline")
        linkIds += resultSet.getInt("id")
      }
      
      val airportCache : Map[Int, Airport] = loadDetails.get(DetailType.AIRPORT) match {
        case Some(fullLoad) => {
          val airports = AirportSource.loadAirportsByIds(airportIds.toList, fullLoad)
          airports.map( airport => (airport.id, airport)).toMap
          
        }
        case None => airportIds.map(id => (id, Airport.fromId(id))).toMap 
      }
      
      val airlineCache : Map[Int, Airline] = loadDetails.get(DetailType.AIRLINE) match {
        case Some(fullLoad) => {
          val airlines = AirlineSource.loadAirlinesByIds(airlineIds.toList, fullLoad)
          airlines.map( airline => (airline.id, airline)).toMap
        }
        case None => airlineIds.map(id => (id, Airline.fromId(id))).toMap 
      }
      
      val assignedAirplaneCache : Map[Int, List[Airplane]] = loadDetails.get(DetailType.AIRPLANE) match {
        case Some(fullLoad) => loadAssignedAirplanesByLinks(connection, linkIds.toList)
        case None => Map.empty
      }
      
      resultSet.beforeFirst()
      while (resultSet.next()) {
        val fromAirportId = resultSet.getInt("from_airport")
        val toAirportId = resultSet.getInt("to_airport")
        val airlineId = resultSet.getInt("airline")
        
        val fromAirport = airportCache.get(fromAirportId)
        val toAirport = airportCache.get(toAirportId)
        val airline = airlineCache.get(airlineId)
        
        if (fromAirport.isDefined && toAirport.isDefined && airline.isDefined) {
          val link = Link( 
            fromAirport.get,
            toAirport.get,
            airline.get,
            LinkClassValues(Map(ECONOMY -> resultSet.getInt("price_economy"), BUSINESS -> resultSet.getInt("price_business"), FIRST -> resultSet.getInt("price_first"))),
            resultSet.getInt("distance"),
            LinkClassValues(Map(ECONOMY -> resultSet.getInt("capacity_economy"), BUSINESS -> resultSet.getInt("capacity_business"), FIRST -> resultSet.getInt("capacity_first"))),
            resultSet.getInt("quality"),
            resultSet.getInt("duration"),
            resultSet.getInt("frequency"),
            FlightType(resultSet.getInt("flight_type")),
            resultSet.getInt("flight_number"))
          link.id = resultSet.getInt("id")
          
          assignedAirplaneCache.get(link.id).foreach {
            link.setAssignedAirplanes(_)
          }
          
          links += link          
        } else {
          println("Failed loading link [" + resultSet.getInt("id") + "] as some details cannot be loaded " + fromAirport + toAirport + airline)
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      links.toList
    } finally {
      connection.close()
    }
  }
  
  def loadFlightNumbers(airlineId : Int) : List[Int] = {
    val connection = Meta.getConnection()
    
    try {  
      val preparedStatement = connection.prepareStatement("SELECT flight_number FROM " + LINK_TABLE + " WHERE airline = ?")
      
      preparedStatement.setInt(1, airlineId)
      
      val resultSet = preparedStatement.executeQuery()
      
      val flightNumbers = ListBuffer[Int]() 
      while (resultSet.next()) {
        flightNumbers.append(resultSet.getInt("flight_number"))
      } 
      resultSet.close()
      preparedStatement.close()
      flightNumbers.toList
    } finally {
      connection.close()
    }
  }
  
  def loadAssignedAirplanesByLinks(connection : Connection, linkIds : List[Int]) : Map[Int, List[Airplane]] = {
    if (linkIds.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder("SELECT link, airplane FROM " + LINK_ASSIGNMENT_TABLE + " WHERE link IN (")
      for (i <- 0 until linkIds.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      val linkAssignmentStatement = connection.prepareStatement(queryString.toString)
      for (i <- 0 until linkIds.size) {
        linkAssignmentStatement.setInt(i + 1, linkIds(i))
      }
      
      val assignmentResultSet = linkAssignmentStatement.executeQuery
      
      val airplaneIds = new HashSet[Int]
      while (assignmentResultSet.next()) {
          airplaneIds += assignmentResultSet.getInt("airplane")
      }
      
      val airplaneCache = AirplaneSource.loadAirplanesByIds(airplaneIds.toList).map { airplane => (airplane.id, airplane) }.toMap
      assignmentResultSet.beforeFirst()
      
      val assignments = new HashMap[Int, ListBuffer[Airplane]]()
      while (assignmentResultSet.next()) {
        val link = assignmentResultSet.getInt("link")
        airplaneCache.get(assignmentResultSet.getInt("airplane")).foreach { airplane =>
          val airplanesForThisLink = assignments.getOrElseUpdate(link, new ListBuffer[Airplane]);
          airplanesForThisLink += airplane
        };
      }
      
      assignmentResultSet.close()
      linkAssignmentStatement.close()
      
      val assignedPlanesByLinkId = assignments.mapValues{ _.toList }.toMap
      
      assignedPlanesByLinkId
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
  def loadLinksByAirports(fromAirportId : Int, toAirportId : Int, loadDetails : Map[DetailType.Value, Boolean] = SIMPLE_LOAD) : List[Link] = {
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
     saveLink(link.from.id, link.to.id, link.airline.id, link.price, link.distance, link.capacity, link.rawQuality, link.duration, link.frequency, link.flightType, link.flightNumber, link.getAssignedAirplanes) match { 
       case Some(generatedId) => 
         link.id = generatedId
         Some(link)
       case None =>
         None
     }
  }
  
  def saveLink(fromAirportId : Int, toAirportId : Int, airlineId : Int, price : LinkClassValues, distance : Double, capacity : LinkClassValues, rawQuality : Int,  duration : Int, frequency : Int, flightType : FlightType.Value, flightNumber : Int, airplanes : List[Airplane] = List.empty) : Option[Int] = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price_economy, price_business, price_first, distance, capacity_economy, capacity_business, capacity_first, quality, duration, frequency, flight_type, flight_number) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)

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
      preparedStatement.setInt(14, flightType.id)
      preparedStatement.setInt(15, flightNumber)
      
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
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_TABLE + "(from_airport, to_airport, airline, price_economy, price_business, price_first, distance, capacity_economy, capacity_business, capacity_first, quality, duration, frequency, flight_type, flight_number) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
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
        preparedStatement.setInt(14, link.flightType.id)
        preparedStatement.setInt(15, link.flightNumber)
        
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
    val preparedStatement = connection.prepareStatement("UPDATE " + LINK_TABLE + " SET price_economy = ?, price_business = ?, price_first = ?, capacity_economy = ?, capacity_business = ?, capacity_first = ?, quality = ?, duration = ?, frequency = ?, flight_type = ?, flight_number = ? WHERE id = ?")

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
      preparedStatement.setInt(10, link.flightType.id)
      preparedStatement.setInt(11, link.flightNumber)
      preparedStatement.setInt(12, link.id)
      
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
  
  def updateLinks(links : List[Link]) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("UPDATE " + LINK_TABLE + " SET price_economy = ?, price_business = ?, price_first = ?, capacity_economy = ?, capacity_business = ?, capacity_first = ?, quality = ?, duration = ?, frequency = ?, flight_type = ?, flight_number = ? WHERE id = ?")

    connection.setAutoCommit(false)
    try {
      links.foreach { link =>
        preparedStatement.setInt(1, link.price(ECONOMY))
        preparedStatement.setInt(2, link.price(BUSINESS))
        preparedStatement.setInt(3, link.price(FIRST))
        preparedStatement.setInt(4, link.capacity(ECONOMY))
        preparedStatement.setInt(5, link.capacity(BUSINESS))
        preparedStatement.setInt(6, link.capacity(FIRST))
        preparedStatement.setInt(7, link.rawQuality)
        preparedStatement.setInt(8, link.duration)
        preparedStatement.setInt(9, link.frequency)
        preparedStatement.setInt(10, link.flightType.id)
        preparedStatement.setInt(11, link.flightNumber)
        preparedStatement.setInt(12, link.id)
        preparedStatement.addBatch()
      }
      
      preparedStatement.executeBatch()
      connection.commit()
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
      val purgingLinkIds = loadLinksByCriteria(criteria, ID_LOAD).map(_.id)

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
      //purge alert records
      val purgingAlerts = AlertSource.loadAlertsByCategory(AlertCategory.LINK_CANCELLATION).filter( alert => purgingLinkIds.contains(alert.targetId.get))
      AlertSource.deleteAlerts(purgingAlerts)

      println("Purged " + purgingAlerts.size + " alert records")

      deletedCount
    } finally {
      connection.close()
    }
      
  }
  
  def saveLinkConsumptions(linkConsumptions: List[LinkConsumptionDetails]) = {
     //open the hsqldb
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("REPLACE INTO link_consumption(link, price_economy, price_business, price_first, capacity_economy, capacity_business, capacity_first, sold_seats_economy, sold_seats_business, sold_seats_first, quality, fuel_cost, crew_cost, airport_fees, inflight_cost, delay_compensation, maintenance_cost, lounge_cost, depreciation, revenue, profit, minor_delay_count, major_delay_count, cancellation_count, from_airport, to_airport, airline, distance, frequency, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")

    try {
      connection.setAutoCommit(false)
      linkConsumptions.foreach { linkConsumption =>
          preparedStatement.setInt(1, linkConsumption.link.id)
          preparedStatement.setInt(2, linkConsumption.link.price(ECONOMY))
          preparedStatement.setInt(3, linkConsumption.link.price(BUSINESS))
          preparedStatement.setInt(4, linkConsumption.link.price(FIRST))
          preparedStatement.setInt(5, linkConsumption.link.capacity(ECONOMY))
          preparedStatement.setInt(6, linkConsumption.link.capacity(BUSINESS))
          preparedStatement.setInt(7, linkConsumption.link.capacity(FIRST))
          preparedStatement.setInt(8, linkConsumption.link.soldSeats(ECONOMY))
          preparedStatement.setInt(9, linkConsumption.link.soldSeats(BUSINESS))
          preparedStatement.setInt(10, linkConsumption.link.soldSeats(FIRST))
          preparedStatement.setInt(11, linkConsumption.link.computedQuality)
          preparedStatement.setInt(12, linkConsumption.fuelCost)
          preparedStatement.setInt(13, linkConsumption.crewCost)
          preparedStatement.setInt(14, linkConsumption.airportFees)
          preparedStatement.setInt(15, linkConsumption.inflightCost)
          preparedStatement.setInt(16, linkConsumption.delayCompensation)
          preparedStatement.setInt(17, linkConsumption.maintenanceCost)
          preparedStatement.setInt(18, linkConsumption.loungeCost)
          preparedStatement.setInt(19, linkConsumption.depreciation)
          preparedStatement.setInt(20, linkConsumption.revenue)
          preparedStatement.setInt(21, linkConsumption.profit)
          preparedStatement.setInt(22, linkConsumption.link.minorDelayCount)
          preparedStatement.setInt(23, linkConsumption.link.majorDelayCount)
          preparedStatement.setInt(24, linkConsumption.link.cancellationCount)
          preparedStatement.setInt(25, linkConsumption.link.from.id)
          preparedStatement.setInt(26, linkConsumption.link.to.id)
          preparedStatement.setInt(27, linkConsumption.link.airline.id)
          preparedStatement.setInt(28, linkConsumption.link.distance)
          preparedStatement.setInt(29, linkConsumption.link.frequency)
          preparedStatement.setInt(30, linkConsumption.cycle)
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
  
  def loadLinkConsumptionsByLinksId(linkIds : List[Int], cycleCount : Int = 1) = {
    
    if (linkIds.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder("SELECT * FROM link_consumption WHERE cycle > ? AND link IN (");
      for (i <- 0 until linkIds.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadLinkConsumptionsByQuery(queryString.toString(), linkIds, cycleCount)
    }
  }
  
  def loadLinkConsumptionsByAirline(airlineId : Int, cycleCount : Int = 1) = {
    loadLinkConsumptionsByCriteria(List(("airline", airlineId)), cycleCount)
  }
  
   def loadLinkConsumptionsByCriteria(criteria : List[(String, Any)], cycleCount : Int) = {
    var queryString = "SELECT * FROM link_consumption WHERE cycle > ?" 
      
    if (!criteria.isEmpty) {
      queryString += " AND "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    queryString += " ORDER BY cycle DESC"
    
    loadLinkConsumptionsByQuery(queryString, criteria.map(_._2), cycleCount)
  }
  
  def loadLinkConsumptionsByQuery(queryString: String, parameters : List[Any], cycleCount : Int) = {
    val connection = Meta.getConnection()
      
    try {
      val latestCycleStatement = connection.prepareStatement("SELECT MAX(cycle) FROM " + LINK_CONSUMPTION_TABLE)
      val latestCycleResultSet = latestCycleStatement.executeQuery()
      val latestCycle = if (latestCycleResultSet.next()) { latestCycleResultSet.getInt(1) } else 0
      latestCycleStatement.close()
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setInt(1, latestCycle - cycleCount)
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 2, parameters(i))
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val linkConsumptions = new ListBuffer[LinkConsumptionDetails]()
      
      val linkIds = ListBuffer[Int]()
      while (resultSet.next()) {
        linkIds.append(resultSet.getInt("link"));
      }
      
      val linksById : Map[Int, Link] = LinkSource.loadLinksByIds(linkIds.toList).map(link => (link.id, link)).toMap 
      
      resultSet.beforeFirst()
      while (resultSet.next()) {
        val linkId = resultSet.getInt("link")
          linksById.get(linkId) match {
          case Some(currentLink) =>
            //need to update current link with history link data
            val frequency = resultSet.getInt("frequency")
            val price = LinkClassValues(Map(ECONOMY -> resultSet.getInt("price_economy"), BUSINESS -> resultSet.getInt("price_business"), FIRST -> resultSet.getInt("price_first")))
            val quality = resultSet.getInt("quality")
            val capacity =  LinkClassValues(Map(ECONOMY -> resultSet.getInt("capacity_economy"), BUSINESS -> resultSet.getInt("capacity_business"), FIRST -> resultSet.getInt("capacity_first")))
            
            val link = currentLink.copy(price = price, frequency = frequency, capacity = capacity)
            link.setQuality(quality)
            link.addSoldSeats(LinkClassValues(Map(ECONOMY -> resultSet.getInt("sold_seats_economy"), BUSINESS -> resultSet.getInt("sold_seats_business"), FIRST -> resultSet.getInt("sold_seats_first"))))
            link.minorDelayCount = resultSet.getInt("minor_delay_count")
            link.majorDelayCount = resultSet.getInt("major_delay_count")
            link.cancellationCount = resultSet.getInt("cancellation_count")
            
            if (link.cancellationCount > 0 && link.frequency > 0) {
              link.addCancelledSeats(capacity * link.cancellationCount / frequency)
            }
            
            linkConsumptions.append(LinkConsumptionDetails(
              link = link,
              fuelCost = resultSet.getInt("fuel_cost"),
              crewCost = resultSet.getInt("crew_cost"),
              airportFees = resultSet.getInt("airport_fees"),
              inflightCost = resultSet.getInt("inflight_cost"),
              delayCompensation = resultSet.getInt("delay_compensation"),
              maintenanceCost = resultSet.getInt("maintenance_cost"),
              loungeCost = resultSet.getInt("lounge_cost"),
              depreciation = resultSet.getInt("depreciation"),
              revenue = resultSet.getInt("revenue"),
              profit = resultSet.getInt("profit"),
              cycle = resultSet.getInt("cycle")))
          case None =>
        }
          
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