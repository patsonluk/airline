package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.Airport
import com.patson.model.Link
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Airplane
import com.patson.data.airplane.ModelSource

object AirplaneSource {
  def loadAirplanesCriteria(criteria : List[(String, Any)]) = {
      val connection = Meta.getConnection()
      
      var queryString = "SELECT owner, m.id as id, model, name, capacity, fuel_burn, speed, range, price  FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id" 
      
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
      
      val airplanes = new ListBuffer[Airplane]()
      while (resultSet.next()) {
        val model = ModelSource.getModelFromRow(resultSet)
        val airplane = Airplane(model, Airline.fromId(resultSet.getInt("owner")))
        airplanes.append(airplane)
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      println("Loaded " + airplanes.length + " airplane records")
      airplanes.toList
  }
  def loadAllAirplanes() = {
    loadAirplanesCriteria(List.empty)
  }
  
  def loadAirplanesByOwner(ownerId : Int) = {
    loadAirplanesCriteria(List(("owner", ownerId)))
  }
  
 def deleteAllAirplanes() = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
      var queryString = "DELETE FROM  " + AIRPLANE_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      connection.close()
      
      println("Deleted " + deletedCount + " airplane records")
      deletedCount
  }
  
  
  def saveAirplanes(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
        
    val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_TABLE + "(owner, model) VALUES(?,?)")
    
    var updateCount = 0
    airplanes.foreach { 
      airplane =>
        println("???" + airplane)
        preparedStatement.setInt(1, airplane.owner.id)
        preparedStatement.setInt(2, airplane.model.id)
        updateCount += preparedStatement.executeUpdate()
    }
    preparedStatement.close()
    connection.close()
    
    println("!!!!" + loadAllAirplanes.size)
    
    updateCount
  }
}