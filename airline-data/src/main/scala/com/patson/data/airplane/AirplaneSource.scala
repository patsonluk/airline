package com.patson.data
import com.patson.data.Constants._

import scala.collection.mutable.ListBuffer
import java.sql.DriverManager

import com.patson.model.{Airline, Airport, Link, LinkClassValues}
import com.patson.model.airplane.{Airplane, LinkAssignments, Model}
import com.patson.data.airplane.ModelSource
import java.sql.Statement

import scala.collection.mutable

object AirplaneSource {
  val LINK_FULL_LOAD = Map(DetailType.LINK -> true)
  val LINK_SIMPLE_LOAD = Map(DetailType.LINK -> false)
  val LINK_ID_LOAD : Map[DetailType.Value, Boolean] = Map.empty
  
  private[this] val BASE_QUERY = "SELECT owner, a.id as id, model, name, capacity, fuel_burn, speed, fly_range, price, constructed_cycle, purchased_cycle, airplane_condition, a.depreciation_rate, a.value, is_sold, dealer_ratio, available_flight_minutes, economy, business, first FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id LEFT JOIN " + AIRPLANE_CONFIGURATION_TABLE + " c ON c.airplane = a.id"
  
  val allModels = ModelSource.loadAllModels().map(model => (model.id, model)).toMap
  
   
  def loadAirplanesCriteria(criteria : List[(String, Any)]) = {
    var queryString = BASE_QUERY
    
    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    
    loadAirplanesByQueryString(queryString, criteria.map(_._2))
  }
  
  def loadConstructingAirplanes() : List[Airplane] = {
    val queryString = BASE_QUERY + " WHERE constructed_cycle > ?"
    loadAirplanesByQueryString(queryString, List(CycleSource.loadCycle()))
  }
  
  def loadUsedAirplanes(): List[Airplane] = {
    loadAirplanesCriteria(List(("is_sold", true)))
  }
  
  def loadAirplanesByQueryString(queryString : String, parameters : List[Any]) = {
      val connection = Meta.getConnection()
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val airplanes = new ListBuffer[Airplane]()
      
      
      while (resultSet.next()) {
        val configuration = LinkClassValues.getInstance(resultSet.getInt("economy"), resultSet.getInt("business"), resultSet.getInt("first"))
        val airplane = Airplane(allModels(resultSet.getInt("model")), Airline.fromId(resultSet.getInt("owner")), resultSet.getInt("constructed_cycle"), resultSet.getInt("purchased_cycle"), resultSet.getDouble("airplane_condition"), depreciationRate = resultSet.getInt("depreciation_rate"), value = resultSet.getInt("value"), isSold = resultSet.getBoolean("is_sold"), dealerRatio = resultSet.getDouble("dealer_ratio"), availableFlightMinutes = resultSet.getInt("available_flight_minutes"), configuration = configuration)
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
  
  def loadAirplanesByOwner(ownerId : Int, isSold : Boolean = false) = {
    loadAirplanesCriteria(List(("owner", ownerId), ("is_sold", isSold)))
  }
  
  def loadAirplaneById(id : Int) : Option[Airplane] = {
    val result = loadAirplanesCriteria(List(("a.id", id)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
    
  }
  
  def loadAirplanesByIds(ids : List[Int]) : List[Airplane] = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(BASE_QUERY + " where a.id IN (");
      for (i <- 0 until ids.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadAirplanesByQueryString(queryString.toString(), ids)
    }
      
  }
  
  def loadAirplaneLinkAssignmentsByOwner(ownerId : Int)  : Map[Int, LinkAssignments] = {
    loadAirplaneLinkAssignmentsByCriteria(List(("owner", ownerId)))
  }
  
  def loadAirplaneLinkAssignmentsByAirplaneId(airplaneId : Int) : LinkAssignments = {
    val result = loadAirplaneLinkAssignmentsByCriteria(List(("a.id", airplaneId)))
    if (result.isEmpty) {
      LinkAssignments(Map.empty)
    } else {
      result(airplaneId)
    }
   }

  /**
    *
    * @param criteria
    * @param loadDetails
    * @return Map[airplaneId, Map[linkId, frequency]]
    */
  def loadAirplaneLinkAssignmentsByCriteria(criteria : List[(String, Any)], loadDetails : Map[DetailType.Value, Boolean] = LINK_ID_LOAD) : Map[Int, LinkAssignments]= {
    val connection = Meta.getConnection()
      var queryString = "SELECT airplane, link, frequency FROM " + LINK_ASSIGNMENT_TABLE + " l LEFT JOIN " + AIRPLANE_TABLE + " a ON l.airplane = a.id"
      
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
      
      val airplanesWithAssignedLink = new mutable.HashMap[Int, mutable.HashMap[Int, Int]]()
      while (resultSet.next()) {
        val airplaneId = resultSet.getInt("airplane")
        val linkId =  resultSet.getInt("link")

        val assignedLinks = airplanesWithAssignedLink.getOrElseUpdate(airplaneId, new mutable.HashMap[Int, Int]())
        assignedLinks.put(linkId, resultSet.getInt("frequency"))
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      //println("Loaded " + airplanesWithAssignedLink.length + " airplane records (with assigned link)")
      airplanesWithAssignedLink.view.mapValues( mutableMap => LinkAssignments(mutableMap.toMap)).toMap
  }
  
 def deleteAllAirplanes() = {
    deleteAirplanesByCriteria(List.empty)
 }
 
 def deleteAirplane(airplaneId : Int) = {
    deleteAirplanesByCriteria(List(("id", airplaneId))) 
 }
 
 def deleteAirplanesByCriteria (criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    
    var deleteCount = 0
    try {
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
      
      deleteCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
    } finally {
      connection.close()
    }

    deleteCount
  }
  
  
  def saveAirplanes(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
    var updateCount = 0
      
    try {
      connection.setAutoCommit(false)    
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_TABLE + "(owner, model, constructed_cycle, purchased_cycle, airplane_condition, depreciation_rate, value, is_sold, dealer_ratio, available_flight_minutes) VALUES(?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
      val configurationStatement = connection.prepareStatement("REPLACE INTO " + AIRPLANE_CONFIGURATION_TABLE + "(airplane, economy, business, first) VALUES(?,?,?,?)")
      
      airplanes.foreach { 
        airplane =>
          preparedStatement.setInt(1, airplane.owner.id)
          preparedStatement.setInt(2, airplane.model.id)
          preparedStatement.setInt(3, airplane.constructedCycle)
          preparedStatement.setInt(4, airplane.purchasedCycle)
          preparedStatement.setDouble(5, airplane.condition)
          preparedStatement.setInt(6, airplane.depreciationRate)
          preparedStatement.setInt(7, airplane.value)
          preparedStatement.setBoolean(8, airplane.isSold)
          preparedStatement.setDouble(9, airplane.dealerRatio)
          preparedStatement.setInt(10, airplane.availableFlightMinutes)
          updateCount += preparedStatement.executeUpdate()
          
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            airplane.id = generatedId //assign id back to the airplane

            configurationStatement.setInt(1, airplane.id)
            configurationStatement.setInt(2, airplane.configuration.economyVal)
            configurationStatement.setInt(3, airplane.configuration.businessVal)
            configurationStatement.setInt(4, airplane.configuration.firstVal)
            configurationStatement.executeUpdate()
          }
      }
      connection.commit()
      preparedStatement.close()
      configurationStatement.close()
    } finally {
      connection.close()
    }
    
    updateCount
  }
  
  def updateAirplanes(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
    var updateCount = 0
      
    try {
      connection.setAutoCommit(false)    
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_TABLE + " SET owner = ?, airplane_condition = ?, depreciation_rate = ?, value = ?, constructed_cycle = ?, purchased_cycle = ?, is_sold = ?, dealer_ratio = ?, available_flight_minutes = ? WHERE id = ?")
      val configurationStatement = connection.prepareStatement("REPLACE INTO " + AIRPLANE_CONFIGURATION_TABLE + "(airplane, economy, business, first) VALUES(?,?,?,?)")
      airplanes.foreach { 
        airplane =>
          preparedStatement.setInt(1, airplane.owner.id)
          preparedStatement.setDouble(2, airplane.condition)
          preparedStatement.setInt(3, airplane.depreciationRate)
          preparedStatement.setInt(4, airplane.value)
          preparedStatement.setInt(5, airplane.constructedCycle)
          preparedStatement.setInt(6, airplane.purchasedCycle)
          preparedStatement.setBoolean(7, airplane.isSold)
          preparedStatement.setDouble(8, airplane.dealerRatio)
          preparedStatement.setInt(9, airplane.availableFlightMinutes)
          preparedStatement.setInt(10, airplane.id)

          updateCount += preparedStatement.executeUpdate()

          configurationStatement.setInt(1, airplane.id)
          configurationStatement.setInt(2, airplane.configuration.economyVal)
          configurationStatement.setInt(3, airplane.configuration.businessVal)
          configurationStatement.setInt(4, airplane.configuration.firstVal)
          configurationStatement.executeUpdate()
      }
      
      connection.commit()
      preparedStatement.close()
      configurationStatement.close()
    } finally {
      connection.close()
    }
    
    updateCount
  }
  
  /**
   * Update an airplane's details except owner, construction_cycle and isSold information
   */
   def updateAirplanesDetails(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
    var updateCount = 0
      
    try {
      connection.setAutoCommit(false)    
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_TABLE + " SET airplane_condition = ?, depreciation_rate = ?, value = ?, dealer_ratio = ? WHERE id = ?")
      
      airplanes.foreach { 
        airplane =>
          preparedStatement.setDouble(1, airplane.condition)
          preparedStatement.setInt(2, airplane.depreciationRate)
          preparedStatement.setInt(3, airplane.value)
          preparedStatement.setDouble(4, airplane.dealerRatio)
          preparedStatement.setInt(5, airplane.id)
          updateCount += preparedStatement.executeUpdate()
      }
      
      connection.commit()
      preparedStatement.close()
    } finally {
      connection.close()
    }
    
    updateCount
  }
  
  
  object DetailType extends Enumeration {
    val LINK = Value
  }
}
