package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model.Airport
import com.patson.model.Airline
import scala.collection.mutable.Map
import com.patson.model.AirlineAppeal

object AirportSource {
  def loadAllAirports(fullLoad : Boolean = false) = {
      loadAirportsByCriteria(List.empty, fullLoad)
  }
  
  def loadAirportsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM airport"
      
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
      
      val airportData = new ListBuffer[Airport]()
      //val airlineMap : Map[Int, Airline] = AirlineSource.loadAllAirlines().foldLeft(Map[Int, Airline]())( (container, airline) => container + Tuple2(airline.id, airline))
      
      
      while (resultSet.next()) {
        val airport = Airport( 
          resultSet.getString("iata"),
          resultSet.getString("icao"),
          resultSet.getString("name"),
          resultSet.getDouble("latitude"),
          resultSet.getDouble("longitude"),
          resultSet.getString("country_code"),
          resultSet.getString("city"),
          resultSet.getInt("airport_size"),
          resultSet.getLong("power"),
          resultSet.getLong("population"),
          resultSet.getInt("slots"),
          resultSet.getInt("available_slots"))
        airport.id = resultSet.getInt("id")
        airportData += airport
        if (fullLoad) {
          val airlineAppeals = Map[Int, AirlineAppeal]()
          val loyaltyStatement = connection.prepareStatement("SELECT airline, loyalty, awareness FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ?")
          loyaltyStatement.setInt(1, airport.id)
          val loyaltyResultSet = loyaltyStatement.executeQuery()
          while (loyaltyResultSet.next()) {
            val airlineId = loyaltyResultSet.getInt("airline")
            airlineAppeals.put(airlineId, AirlineAppeal(loyaltyResultSet.getDouble("loyalty"), loyaltyResultSet.getDouble("awareness")))
          }
          airport.initAirlineAppeals(airlineAppeals.toMap)
          loyaltyStatement.close()
          
          val slotAssignments = Map[Int, Int]()
          val slotStatement = connection.prepareStatement("SELECT airline, SUM(frequency) as total_frequency FROM " + LINK_TABLE + " WHERE (from_airport = ? OR to_airport = ?) GROUP BY airline")
          slotStatement.setInt(1, airport.id)
          slotStatement.setInt(2, airport.id)
          
          val slotResultSet = slotStatement.executeQuery()
          while (slotResultSet.next()) {
            val airlineId = slotResultSet.getInt("airline")
            slotAssignments.put(airlineId, slotResultSet.getInt("total_frequency"))
          }
          airport.initSlotAssignments(slotAssignments.toMap)
          slotStatement.close()
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      airportData.toList
    } finally {
      connection.close()
    }
      
  }
  
  
  def loadAirportById(id : Int, fullLoad : Boolean = false) = {
      val result = loadAirportsByCriteria(List(("id", id)), fullLoad)
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  def loadAirportByIata(iata : String) = {
      val result = loadAirportsByCriteria(List(("iata", iata)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def updateAirlineAppeal(airports: List[Airport]) = {
   airports.foreach { airport => //make sure all loaded properly
     if (!airport.isAirlineAppealsInitialized) {
       throw new IllegalStateException("cannot save airline appeal as it's not initialized properly!")
     }
   }
   val connection = Meta.getConnection()
   try {  
     connection.setAutoCommit(false)
     airports.foreach { airport => 
      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ?")
      purgeStatement.setInt(1, airport.id)
      purgeStatement.executeUpdate()
      purgeStatement.close()
      
      
      
      airport.getAirlineAppeals.foreach { 
        case(airlineId, airlineAppeal) =>
          if (airlineAppeal.awareness > 0 || airlineAppeal.loyalty > 0) {
            val insertStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_APPEAL_TABLE + "(airport, airline, loyalty, awareness) VALUES (?,?,?,?)")
            //println( airport.id + "  " + airline.id)
            insertStatement.setInt(1, airport.id)
            insertStatement.setInt(2, airlineId)
            insertStatement.setDouble(3, airlineAppeal.loyalty)
            insertStatement.setDouble(4, airlineAppeal.awareness)
            insertStatement.executeUpdate()
            insertStatement.close()
          }
        }
     }
     connection.commit()
   } finally {
     connection.close()
   }
  }
  
//  def updateSlotAssignments(airports: List[Airport]) = {
//     airports.foreach { airport => //make sure all loaded properly
//       if (!airport.isSlotAssignmentsInitialized) {
//         throw new IllegalStateException("cannot save slot assignments as it's not initialized properly!")
//       }
//     }
//     val connection = Meta.getConnection()
//     try {  
//       connection.setAutoCommit(false)
//       airports.foreach { airport => 
//        val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_SLOT_ASSIGNMENT_TABLE + " WHERE airport = ?")
//        purgeStatement.setInt(1, airport.id)
//        purgeStatement.executeUpdate()
//        purgeStatement.close()
//        
//        var assignedSlots = 0
//        airport.getAirlineSlotAssignments.foreach { 
//          case(airlineId, assignmentValue) =>
//            val insertStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_SLOT_ASSIGNMENT_TABLE + "(airport, airline, assignment_value) VALUES (?,?,?)")
//            //println( airport.id + "  " + airline.id)
//            insertStatement.setInt(1, airport.id)
//            insertStatement.setInt(2, airlineId)
//            insertStatement.setInt(3, assignmentValue)
//            insertStatement.executeUpdate()
//            insertStatement.close()
//            assignedSlots += assignmentValue
//          }
//       //update airport slot
//          val slotStatement = connection.prepareStatement("UPDATE " + AIRPORT_TABLE + " SET available_slots = ? WHERE id = ?")
//          slotStatement.setInt(1, airport.availableSlots)
//          slotStatement.setInt(2, airport.id)
//          slotStatement.executeUpdate()
//          slotStatement.close()
//       }
//
//       
//       connection.commit()
//     } finally {
//       connection.close()
//     }
//  }
  
  def saveAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_TABLE + "(iata, icao, name, latitude, longitude, country_code, city, airport_size, power, population, slots, available_slots) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")
    
      connection.setAutoCommit(false)
      airports.foreach { 
        airport =>
          preparedStatement.setString(1, airport.iata)
          preparedStatement.setString(2, airport.icao)
          preparedStatement.setString(3, airport.name)
          preparedStatement.setDouble(4, airport.latitude)
          preparedStatement.setDouble(5, airport.longitude)
          preparedStatement.setString(6, airport.countryCode)
          preparedStatement.setString(7, airport.city)
          preparedStatement.setInt(8, airport.size)
          preparedStatement.setLong(9, airport.power)
          preparedStatement.setLong(10, airport.population)
          preparedStatement.setInt(11, airport.slots)
          preparedStatement.setInt(12, airport.availableSlots)
          
          preparedStatement.executeUpdate()
          val generatedKeys = preparedStatement.getGeneratedKeys
          
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            airport.id = generatedId
            
            //insert airline info too
            airport.citiesServed.foreach { 
              case (city, share) =>
              val infoStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_CITY_SHARE_TABLE + "(airport, city, share) VALUES(?,?,?)")
              infoStatement.setInt(1, airport.id)
              infoStatement.setInt(2, city.id)
              infoStatement.setDouble(3, share)
              infoStatement.executeUpdate()
              infoStatement.close()
            }
          }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def updateAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    
    try {
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPORT_TABLE + " SET airport_size = ?, power = ?, population = ?, slots = ?, available_slots = ? WHERE id = ?")
      
      connection.setAutoCommit(false)
      airports.foreach { 
        airport =>
          preparedStatement.setInt(1, airport.size)
          preparedStatement.setLong(2, airport.power)
          preparedStatement.setLong(3, airport.population)
          preparedStatement.setInt(4, airport.slots)
          preparedStatement.setInt(5, airport.availableSlots)
          preparedStatement.setInt(6, airport.id)
          preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def deleteAllAirports() = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + AIRPORT_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " airport records")
      deletedCount
    } finally {
      connection.close()
    }
  }
}