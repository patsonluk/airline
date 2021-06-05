package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model._

import scala.collection.mutable.Map
import com.patson.model.AirlineAppeal
import java.sql.Statement

import com.patson.util.AirlineCache

import scala.collection.mutable

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
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + COUNTRY_TABLE + "(code, name, airport_population, income, openness) VALUES (?,?,?,?,?)")
    
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
  
  def updateCountries(countries : List[Country]) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("UPDATE " + COUNTRY_TABLE + " SET name = ?, airport_population = ?,  income = ?,  openness = ? WHERE code = ?")
    
      connection.setAutoCommit(false)
      countries.foreach { 
        country =>
          preparedStatement.setString(1, country.name)
          preparedStatement.setInt(2, country.airportPopulation)
          preparedStatement.setInt(3, country.income)
          preparedStatement.setInt(4, country.openness)
          preparedStatement.setString(5, country.countryCode)
          preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def purgeAllCountries() = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("DELETE FROM " + COUNTRY_TABLE)
      preparedStatement.executeUpdate()
      preparedStatement.close()
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
  
  def updateCountryMutualRelationships(relationships : Map[(String, String), Int]) = {
     val connection = Meta.getConnection()
     try {
       val purgeStatement = connection.prepareStatement("DELETE FROM " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE)
       purgeStatement.executeUpdate()

       purgeStatement.close()

       println("purged")

       connection.setAutoCommit(false)
       val insertStatement = connection.prepareStatement("INSERT INTO " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE + "(country_1, country_2, relationship) VALUES (?,?,?)")
       relationships.foreach { 
         case ((country1, country2), relationShip) => {
           insertStatement.setString(1, country1)
           insertStatement.setString(2, country2)
           insertStatement.setInt(3, relationShip)
           insertStatement.addBatch()
         }
       }
       
       insertStatement.executeBatch()
       connection.commit()
       insertStatement.close()
     } finally {
       connection.close()
     }
  }
  
  def getCountryMutualRelationship(country1 : String, country2 : String) : Int = {
     val connection = Meta.getConnection()
     val statement = connection.prepareStatement("SELECT relationship FROM " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE + " WHERE country_1 = ? AND country_2 = ?")
     try {
       statement.setString(1, country1)
       statement.setString(2, country2)
       val result = statement.executeQuery();
       
       if (result.next()) {
         result.getInt("relationship")
       } else {
         0
       }
     } finally {
       statement.close()
       connection.close()
     }  
  }
  
  def getCountryMutualRelationships() : scala.collection.immutable.Map[(String, String), Int] = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("SELECT * FROM " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE)
     try {
       
       val result = statement.executeQuery();
       
       val relationships = Map[(String, String), Int]()
       while (result.next()) {
         relationships.put((result.getString("country_1"), result.getString("country_2")), result.getInt("relationship"))
       }
       
       relationships.toMap
     } finally {
       statement.close()
       connection.close()
     }
  }
  
  def getCountryMutualRelationships(country : String) : scala.collection.immutable.Map[String, Int] = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("SELECT * FROM " + COUNTRY_MUTUAL_RELATIONSHIP_TABLE + " WHERE country_1 = ?")
     try {
       
       statement.setString(1, country)
       val result = statement.executeQuery();


       val relationships = Map[String, Int]()
       while (result.next()) {
         relationships.put(result.getString("country_2"), result.getInt("relationship"))
       }
       
       relationships.toMap
     } finally {
       statement.close()
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
        val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
        
        relationShipData.getOrElseUpdate(country, Map()).put(airline, resultSet.getInt("relationship"))
      }    
      resultSet.close()
      preparedStatement.close()
      relationShipData.mapValues(_.toMap).toMap //make immutable
    } finally {
      connection.close()
    }  
  }
  
  def loadAllCountryRelationships(): scala.collection.immutable.Map[Country, scala.collection.immutable.Map[Airline, Int]] = {
    loadCountryRelationshipsByCriteria(List.empty)
  }
  
  def loadCountryRelationshipsByCountry(countryCode : String) : scala.collection.immutable.Map[Airline, Int] = {
    loadCountryRelationshipsByCriteria(List(("country", countryCode))).find( _._1.countryCode == countryCode) match {
      case Some((_, relationships)) => relationships
      case None => scala.collection.immutable.Map.empty
    }
  }
  
  def loadCountryRelationshipsByAirline(airlineId : Int) : scala.collection.immutable.Map[Country, Int] = {
    loadCountryRelationshipsByCriteria(List(("airline", airlineId))).view.mapValues { airlineToRelationship =>
      airlineToRelationship.toIterable.head._2
    }.toMap
  }
  
  def saveMarketShares(marketShares : List[CountryMarketShare]) = {
     val connection = Meta.getConnection()
     try {  
       connection.setAutoCommit(false)
       //purge existing ones
       val truncateStatement = connection.prepareStatement("TRUNCATE TABLE "+ COUNTRY_MARKET_SHARE_TABLE);
       truncateStatement.executeUpdate()       
       
       val replaceStatement = connection.prepareStatement("REPLACE INTO " + COUNTRY_MARKET_SHARE_TABLE + "(country, airline, passenger_count) VALUES (?,?,?)")
       marketShares.foreach { marketShare =>
           replaceStatement.setString(1, marketShare.countryCode)
           marketShare.airlineShares.foreach { 
             case((airline, passenger_count)) =>
               replaceStatement.setInt(2, airline)
               replaceStatement.setDouble(3, passenger_count)
               replaceStatement.addBatch()
           }
           
       }
       
       replaceStatement.executeBatch()
       connection.commit()
       truncateStatement.close()
       replaceStatement.close()
     } finally {
       connection.close()
     }
  }
  def loadMarketSharesByCountryCode(country : String) : Option[CountryMarketShare] = {
    val result = loadMarketSharesByCriteria(List(("country", country)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  def loadMarketSharesByCriteria(criteria : List[(String, Any)]) : List[CountryMarketShare] = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + COUNTRY_MARKET_SHARE_TABLE
      
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
      
      val countryMarketShares = ListBuffer[CountryMarketShare]()
      
      val resultMap = Map[String, Map[Int, Long]]()
      while (resultSet.next()) {
        val countryCode = resultSet.getString("country")
        val airlineId = resultSet.getInt("airline")
        val passengerCount = resultSet.getLong("passenger_count")
        
        val airlinePassengers = resultMap.getOrElseUpdate(countryCode, Map[Int, Long]())
        airlinePassengers.put(airlineId, passengerCount)
      }    
      resultSet.close()
      preparedStatement.close()
      
      resultMap.toList.map {
        case ((countryCode, airlinePassengers)) => CountryMarketShare(countryCode, airlinePassengers.toMap)
      }
    } finally {
      connection.close()
    }  
  }

  def saveCountryAirlineTitles(countryAirlineTitles : List[CountryAirlineTitle]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      //purge existing ones
      val truncateStatement = connection.prepareStatement("TRUNCATE TABLE "+ COUNTRY_AIRLINE_TITLE_TABLE);
      truncateStatement.executeUpdate()

      val replaceStatement = connection.prepareStatement("REPLACE INTO " + COUNTRY_AIRLINE_TITLE_TABLE + "(country, airline, title) VALUES (?,?,?)")
      countryAirlineTitles.foreach { countryAirlineTitle =>
        replaceStatement.setString(1, countryAirlineTitle.country.countryCode)
        replaceStatement.setInt(2, countryAirlineTitle.airline.id)
        replaceStatement.setInt(3, countryAirlineTitle.title.id)
        replaceStatement.addBatch()
      }

      replaceStatement.executeBatch()
      connection.commit()
      truncateStatement.close()
      replaceStatement.close()
    } finally {
      connection.close()
    }
  }

  def loadCountryAirlineTitlesByAirlineAndCountry(airlineId : Int, country : String) : Option[CountryAirlineTitle] = {
    val result = loadCountryAirlineTitlesByCriteria(List(("country", country), ("airline", airlineId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }

  def loadCountryAirlineTitlesByCountryCode(country : String) : List[CountryAirlineTitle] = {
    loadCountryAirlineTitlesByCriteria(List(("country", country)))
  }

  def loadCountryAirlineTitlesByCriteria(criteria : List[(String, Any)]) : List[CountryAirlineTitle] = {
    val connection = Meta.getConnection()
    try {
      var queryString = "SELECT * FROM " + COUNTRY_AIRLINE_TITLE_TABLE

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

      val titles = ListBuffer[CountryAirlineTitle]()
      val airlines = mutable.HashMap[Int, Airline]()
      val countryCache =  mutable.HashMap[String, Country]()

      while (resultSet.next()) {
        val countryCode = resultSet.getString("country")
        val airlineId = resultSet.getInt("airline")
        val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId)))
        val title = Title(resultSet.getInt("title"))
        val country = countryCache.getOrElseUpdate(countryCode, loadCountryByCode(countryCode).getOrElse(Country.fromCode(countryCode)))
        titles.append(CountryAirlineTitle(country, airline, title))
      }
      resultSet.close()
      preparedStatement.close()

      titles.toList
    } finally {
      connection.close()
    }
  }

//  def getCountryRelationshipsByAirline(airline : Airline) : scala.collection.immutable.Map[String, Int] = {
//    //for now only depends on the mother country code of the airline
//    airline.getCountryCode() match {
//      case Some(countryCode) => getCountryMutualRelationships(countryCode)
//      case None => scala.collection.immutable.Map.empty[String, Int]
//    }
//  }

    
}

