package com.patson.data

import scala.collection.mutable.ListBuffer

import com.patson.data.Constants.AIRPORT_LOYALTY_TABLE
import com.patson.data.Constants.AIRPORT_TABLE
import com.patson.data.Constants.DB_DRIVER
import com.patson.model.Airport
import com.patson.model.Airline

object AirportSource {
  def loadAllAirports(fullLoad : Boolean = false) = {
      loadAirportsByCriteria(List.empty, fullLoad)
  }
  
  def loadAirportsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
      var queryString = "SELECT iata, icao, name, latitude, longitude, country_code, city, airport_size, power, id FROM airport"
      
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
          resultSet.getLong("power"))
        airport.id = resultSet.getInt("id")
        airportData += airport
        if (fullLoad) {
          val loyaltyStatement = connection.prepareStatement("SELECT airline, value FROM " + AIRPORT_LOYALTY_TABLE + " WHERE airport = ?")
          loyaltyStatement.setInt(1, airport.id)
          val loyaltyResultSet = loyaltyStatement.executeQuery()
          while (loyaltyResultSet.next()) {
            val airlineId = loyaltyResultSet.getInt("airline")
             airlineMap.get(airlineId) match {
              case Some(airline) => airport.setAirlineLoyalty(airline, loyaltyResultSet.getInt("value"))
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
  
  def updateLoyalty(airports: List[Airport]) = {
     val connection = Meta.getConnection()
        
     connection.setAutoCommit(false)
     airports.foreach { airport => 
      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_LOYALTY_TABLE + " WHERE airport = ?")
      purgeStatement.setInt(1, airport.id)
      purgeStatement.executeUpdate()
      
      airport.airlineLoyalties.foreach { 
        case(airline, value) =>
          val insertStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_LOYALTY_TABLE + "(airport, airline, value) VALUES (?,?,?)")
          //println( airport.id + "  " + airline.id)
          insertStatement.setInt(1, airport.id)
          insertStatement.setInt(2, airline.id)
          insertStatement.setInt(3, value)
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
        
        val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_TABLE + "(iata, icao, name, latitude, longitude, country_code, city, airport_size, power) VALUES(?,?,?,?,?,?,?,?,?)")
        
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
            preparedStatement.executeUpdate()
        }
        connection.commit()
        
        connection.close()
        
        
  }
}