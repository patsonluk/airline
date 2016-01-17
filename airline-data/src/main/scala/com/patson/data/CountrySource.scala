package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model._
import scala.collection.mutable.Map
import com.patson.model.AirlineAppeal
import java.sql.Statement

object CountrySource {
  def loadAllCountries() = {
      loadCountriesByCriteria(List.empty)
  }
  
  def loadCountriesByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + COUNTRY_TABLE
      
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
      
      val countryData = new ListBuffer[Country]()
      //val airlineMap : Map[Int, Airline] = AirlineSource.loadAllAirlines().foldLeft(Map[Int, Airline]())( (container, airline) => container + Tuple2(airline.id, airline))
      
      
      while (resultSet.next()) {
        val country = Country( 
          resultSet.getString("code"),
          resultSet.getString("name"),
          resultSet.getInt("airport_population"),
          resultSet.getInt("income"),
          resultSet.getInt("openness"))
          countryData += country
      }    
      resultSet.close()
      preparedStatement.close()
      countryData.toList
    } finally {
      connection.close()
    }
      
  }
  
  
  def loadCountryByCode(countryCode : String) = {
      val result = loadCountriesByCriteria(List(("code", countryCode)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def saveCountries(countries : List[Country]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + COUNTRY_TABLE + "(code, name, airport_population, income, openness) VALUES (?,?,?,?,?)")
    
      connection.setAutoCommit(false)
      countries.foreach { 
        country =>
          preparedStatement.setString(1, country.countryCode)
          preparedStatement.setString(2, country.name)
          preparedStatement.setInt(3, country.airportPopulation)
          preparedStatement.setInt(4, country.income)
          preparedStatement.setInt(5, country.openness)
          preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def saveCountryRelationships(relationships : Map[Country, Map[Airline, Int]]) = {
     val connection = Meta.getConnection()
     try {  
       connection.setAutoCommit(false)
       val purgeStatement = connection.prepareStatement("DELETE FROM " + COUNTRY_AIRLINE_RELATIONSHIP_TABLE + " WHERE country = ? AND airline = ?")
       val replaceStatement = connection.prepareStatement("REPLACE INTO " + COUNTRY_AIRLINE_RELATIONSHIP_TABLE + "(country, airline, relationship) VALUES (?,?,?)")
       relationships.foreach { 
         case (country, airlineRelationShips) => 
           airlineRelationShips.foreach {
             case (airline, relationship) =>
               if (relationship <= 0) { //remove the entry for now
                 purgeStatement.setString(1, country.countryCode)
                 purgeStatement.setInt(2, airline.id)
                 purgeStatement.executeUpdate()
               } else {
                 replaceStatement.setString(1, country.countryCode)
                 replaceStatement.setInt(2, airline.id)
                 replaceStatement.setInt(3, relationship)
                 replaceStatement.executeUpdate()
               }
           }
       }
       connection.commit()
       replaceStatement.close()
       purgeStatement.close()
     } finally {
       connection.close()
     }
  }
  
  def loadCountryRelationshipsByCriteria(criteria : List[(String, Any)]) : scala.collection.immutable.Map[Country, scala.collection.immutable.Map[Airline, Int]] = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + COUNTRY_AIRLINE_RELATIONSHIP_TABLE
      
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
      
      val relationShipData = Map[Country, Map[Airline, Int]]()
      
      val countries = Map[String, Country]()
      val airlines = Map[Int, Airline]()
      while (resultSet.next()) {
        val countryCode = resultSet.getString("country")
        val country = countries.getOrElseUpdate(countryCode, loadCountryByCode(countryCode).get)
        val airlineId = resultSet.getInt("airline")
        val airline = airlines.getOrElseUpdate(airlineId, AirlineSource.loadAirlineById(airlineId, false).getOrElse(Airline.fromId(airlineId)))
        
        relationShipData.getOrElseUpdate(country, Map()).put(airline, resultSet.getInt("relationship"))
      }    
      resultSet.close()
      preparedStatement.close()
      relationShipData.mapValues(_.toMap).toMap //make immutable
    } finally {
      connection.close()
    }  
  }
  
  def loadCountryRelationshipsByCountry(countryCode : String) : scala.collection.immutable.Map[Airline, Int] = {
    loadCountryRelationshipsByCriteria(List(("country", countryCode))).find( _._1.countryCode == countryCode) match {
      case Some((_, relationships)) => relationships
      case None => scala.collection.immutable.Map.empty
    }
  }
  
  def loadCountryRelationshipsByAirline(airlineId : Int) : scala.collection.immutable.Map[Country, Int] = {
    loadCountryRelationshipsByCriteria(List(("airline", airlineId))).mapValues { airlineToRelationship =>
      airlineToRelationship.toIterable.head._2
    }
  }
}