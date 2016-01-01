package com.patson.data

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model._
import java.sql.Statement

object CitySource {
  def loadAllCities() = {
      loadCitiesByCriteria(List.empty)
  }
  
  def loadCitiesByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "SELECT * FROM " + CITY_TABLE
      
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
      
      val cityList = new ListBuffer[City]()
      
      while (resultSet.next()) {
        val city = City( 
          resultSet.getString("name"),
          resultSet.getDouble("latitude"),
          resultSet.getDouble("longitude"),
          resultSet.getString("country_code"),
          resultSet.getInt("population"),
          resultSet.getInt("income"))
          
        city.id = resultSet.getInt("id")
        cityList += city
      }
      
      resultSet.close()
      preparedStatement.close()
      cityList.toList
    } finally {
      connection.close()
    }
  }
  
  
  def loadCityById(id : Int) = {
      val result = loadCitiesByCriteria(List(("id", id)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def deleteAllCitites() = {
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + CITY_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " city records")
      deletedCount
    } finally {
      connection.close()
    }
      
  }
  
  def saveCities(cities : List[City]) = {
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("INSERT INTO " + CITY_TABLE + "(name, latitude, longitude, country_code, population, income) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
        
        connection.setAutoCommit(false)
        cities.foreach { 
          city =>
            preparedStatement.setString(1, city.name)
            preparedStatement.setDouble(2, city.latitude)
            preparedStatement.setDouble(3, city.longitude)
            preparedStatement.setString(4, city.countryCode)
            preparedStatement.setInt(5, city.population)
            preparedStatement.setInt(6, city.income)
            preparedStatement.executeUpdate()
            
            val generatedKeys = preparedStatement.getGeneratedKeys
            if (generatedKeys.next()) {
              val generatedId = generatedKeys.getInt(1)
              city.id = generatedId
            }
            
        }
        preparedStatement.close()
        connection.commit()
    } finally {
      connection.close()
    }        
        
  } 
  def updateCities(cities : List[City]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    try {    
        val preparedStatement = connection.prepareStatement("UPDATE " + CITY_TABLE + "SET population = ?, income = ? WHERE id = ?")
        
        connection.setAutoCommit(false)
        cities.foreach { 
          city =>
            preparedStatement.setInt(1, city.population)
            preparedStatement.setInt(2, city.income)
            preparedStatement.setInt(3, city.id)
            preparedStatement.executeUpdate()
        }
        preparedStatement.close()
        connection.commit()
    } finally {
        connection.close()
    }
  }
}