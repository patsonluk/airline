package com.patson.data

import scala.collection.mutable.ListBuffer

import com.patson.data.Constants._
import com.patson.model.Airport
import com.patson.model.Airline

object AirportSource {
  def loadAllAirports(fullLoad : Boolean = false) = {
      loadAirportsByCriteria(List.empty, fullLoad)
  }
  
  def loadAirportsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
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
      val airlineMap : Map[Int, Airline] = AirlineSource.loadAllAirlines().foldLeft(Map[Int, Airline]())( (container, airline) => container + Tuple2(airline.id, airline))
      
      
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
          val loyaltyStatement = connection.prepareStatement("SELECT airline, loyalty, awareness FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ?")
          loyaltyStatement.setInt(1, airport.id)
          val loyaltyResultSet = loyaltyStatement.executeQuery()
          while (loyaltyResultSet.next()) {
            val airlineId = loyaltyResultSet.getInt("airline")
             airlineMap.get(airlineId) match {
              case Some(airline) => 
                airport.setAirlineLoyalty(airline, loyaltyResultSet.getDouble("loyalty"))
                airport.setAirlineAwareness(airline, loyaltyResultSet.getDouble("awareness"))
              case None => println("Unexpected! Cannot load airline with id " + airlineId)
            }
          }
          loyaltyStatement.close()
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      airportData.toList
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
     val connection = Meta.getConnection()
        
     connection.setAutoCommit(false)
     airports.foreach { airport => 
      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ?")
      purgeStatement.setInt(1, airport.id)
      purgeStatement.executeUpdate()
      
      airport.airlineAppeals.foreach { 
        case(airline, airlineAppeal) =>
          val insertStatement = connection.prepareStatement("INSERT INTO " + AIRLINE_APPEAL_TABLE + "(airport, airline, loyalty, awareness) VALUES (?,?,?,?)")
          //println( airport.id + "  " + airline.id)
          insertStatement.setInt(1, airport.id)
          insertStatement.setInt(2, airline.id)
          insertStatement.setDouble(3, airlineAppeal.loyalty)
          insertStatement.setDouble(4, airlineAppeal.awareness)
          insertStatement.executeUpdate()
          insertStatement.close()
        }
     }
     connection.commit()
              
     connection.close()
  }
  
  def saveAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    
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
//            println("airport : " + airport.id + " city : " + city.id)
            infoStatement.executeUpdate()
          }
        } 
    }
    connection.commit()
    connection.close()
  }
  
  def updateAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    
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
    connection.commit()
    connection.close()
  }
  
  def deleteAllAirports() = {
    //open the hsqldb
    val connection = Meta.getConnection()
    
    var queryString = "DELETE FROM " + AIRPORT_TABLE
    
    val preparedStatement = connection.prepareStatement(queryString)
    val deletedCount = preparedStatement.executeUpdate()
    
    preparedStatement.close()
    connection.close()
    
    println("Deleted " + deletedCount + " airport records")
    deletedCount
  }
}