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
      
      var queryString = "SELECT owner, a.id as id, model, name, capacity, fuel_burn, speed, range, price, constructed_cycle, condition FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id" 
      
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
        val airplane = Airplane(model, Airline.fromId(resultSet.getInt("owner")), resultSet.getInt("constructed_cycle"), resultSet.getBigDecimal("condition"))
        airplane.id = resultSet.getInt("id")
        airplanes.append(airplane)
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      //println("Loaded " + airplanes.length + " airplane records")
      airplanes.toList
  }
  def loadAllAirplanes() = {
    loadAirplanesCriteria(List.empty)
  }
  
  def loadAirplanesByOwner(ownerId : Int) = {
    loadAirplanesCriteria(List(("owner", ownerId)))
  }
  
  def loadAirplaneById(id : Int) : Option[Airplane] = {
    val result = loadAirplanesCriteria(List(("a.id", id)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
    
  }
  
  def loadAirplanesWithAssignedLinkByOwner(ownerId : Int)  : List[(Airplane, Option[Link])] = {
    loadAirplanesWithAssignedLinkByCriteria(List(("owner", ownerId)))
  }
  
  def loadAirplanesWithAssignedLinkByAirplaneId(airplaneId : Int) : Option[(Airplane, Option[Link])] = {
    val result = loadAirplanesWithAssignedLinkByCriteria(List(("a.id", airplaneId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
   }
  
  def loadAirplanesWithAssignedLinkByCriteria(criteria : List[(String, Any)]) : List[(Airplane, Option[Link])]= {
    val connection = Meta.getConnection()
      var queryString = "SELECT owner, a.id as id, model, name, capacity, fuel_burn, speed, range, price, constructed_cycle, condition, la.link  FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id LEFT JOIN " + LINK_ASSIGNMENT_TABLE + " la ON a.id = la.airplane"  
      
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
      
      val airplanesWithAssignedLink = new ListBuffer[(Airplane, Option[Link])]()
      while (resultSet.next()) {
        val model = ModelSource.getModelFromRow(resultSet)
        val airplane = Airplane(model, Airline.fromId(resultSet.getInt("owner")), resultSet.getInt("constructed_cycle"), resultSet.getBigDecimal("condition"))
        airplane.id = resultSet.getInt("id")
        if (resultSet.getObject("link") != null) {
          val linkId = resultSet.getInt("link")
          val assignedLink = LinkSource.loadLinkById(linkId)
          airplanesWithAssignedLink.append((airplane, assignedLink))
        } else{
          airplanesWithAssignedLink.append((airplane, None))
        }
        
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      //println("Loaded " + airplanesWithAssignedLink.length + " airplane records (with assigned link)")
      airplanesWithAssignedLink.toList
  }
  
 def deleteAllAirplanes() = {
    deleteAirplanesByCriteria(List.empty)
 }
 
 def deleteAirplane(airplaneId : Int) = {
    deleteAirplanesByCriteria(List(("id", airplaneId))) 
 }
 
 def deleteAirplanesByCriteria (criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    
    var queryString = "DELETE FROM  " + AIRPLANE_TABLE 
    
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
    
    val deleteCount = preparedStatement.executeUpdate()

    deleteCount
  }
  
  
  def saveAirplanes(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
        
    val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_TABLE + "(owner, model, constructed_cycle, condition) VALUES(?,?,?,?)")
    
    var updateCount = 0
    airplanes.foreach { 
      airplane =>
        preparedStatement.setInt(1, airplane.owner.id)
        preparedStatement.setInt(2, airplane.model.id)
        preparedStatement.setInt(3, airplane.constructedCycle)
        preparedStatement.setBigDecimal(4, airplane.condition.bigDecimal)
        updateCount += preparedStatement.executeUpdate()
    }
    preparedStatement.close()
    connection.close()
    
    updateCount
  }
}