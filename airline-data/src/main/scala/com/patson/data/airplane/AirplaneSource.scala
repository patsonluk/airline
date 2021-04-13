package com.patson.data
import java.sql.Statement
import com.patson.data.Constants._
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model.{Airline, Airport}
import com.patson.util.{AirlineCache, AirplaneOwnershipCache}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AirplaneSource {
  val LINK_FULL_LOAD = Map(DetailType.LINK -> true)
  val LINK_SIMPLE_LOAD = Map(DetailType.LINK -> false)
  val LINK_ID_LOAD : Map[DetailType.Value, Boolean] = Map.empty
  
  private[this] val BASE_QUERY = "SELECT owner, a.id as id, a.model as model, name, capacity, fuel_burn, speed, fly_range, price, constructed_cycle, purchased_cycle, airplane_condition, a.depreciation_rate, a.value, is_sold, dealer_ratio, configuration, home, purchase_rate, economy, business, first, is_default FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id LEFT JOIN " + AIRPLANE_CONFIGURATION_TABLE + " c ON c.airplane = a.id LEFT JOIN " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + " t ON c.configuration = t.id"
  
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
      
      val currentCycle = CycleSource.loadCycle()
      while (resultSet.next()) {
        val airlineId = resultSet.getInt("owner")
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
        val model = allModels(resultSet.getInt("a.model"))
        val configuration = AirplaneConfiguration(resultSet.getInt("economy"), resultSet.getInt("business"), resultSet.getInt("first"), airline, model, resultSet.getBoolean("is_default"), id = resultSet.getInt("configuration"))
        val isSold = resultSet.getBoolean("is_sold")
        val constructedCycle = resultSet.getInt("constructed_cycle")
        val isReady = !isSold && currentCycle >= constructedCycle
        val purchaseRate = resultSet.getDouble("purchase_rate")
        val airplane = Airplane(model, airline, constructedCycle, resultSet.getInt("purchased_cycle"), resultSet.getDouble("airplane_condition"), depreciationRate = resultSet.getInt("depreciation_rate"), value = resultSet.getInt("value"), isSold = isSold, dealerRatio = resultSet.getDouble("dealer_ratio"), configuration = configuration, home = Airport.fromId(resultSet.getInt("home")), isReady = isReady, purchaseRate = purchaseRate)
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
    loadAirplaneLinkAssignmentsByCriteria(List(("owner", ownerId)), joinAirplaneTable = true)
  }

  /**
    *
    * @param linkId
    *  @return Map[airplaneId, LinkAssignment]
    */
  def loadAirplaneLinkAssignmentsByLinkId(linkId : Int) : Map[Int, LinkAssignment] = {
    val result: Map[Int, LinkAssignments] = loadAirplaneLinkAssignmentsByCriteria(List(("link", linkId)), joinAirplaneTable = false) //Map[airplaneId, linkAssignments]
    if (result.isEmpty) {
      Map.empty
    } else {
      result.view.mapValues { linkAssignmentsOfThisAirplane =>
        linkAssignmentsOfThisAirplane.assignments(linkId)
      }.toMap
    }
  }
  
  def loadAirplaneLinkAssignmentsByAirplaneId(airplaneId : Int) : LinkAssignments = {
    val result = loadAirplaneLinkAssignmentsByCriteria(List(("airplane", airplaneId)), joinAirplaneTable = false)
    if (result.isEmpty) {
      LinkAssignments(Map.empty)
    } else {
      result(airplaneId)
    }
   }

  /**
    *
    * @param criteria
    * @param joinAirplaneTable whether the query require join on Airplane Table
    * @param loadDetails
    * @return Map[airplaneId, LinkAssignments]
    */
  def loadAirplaneLinkAssignmentsByCriteria(criteria : List[(String, Any)], joinAirplaneTable : Boolean = false, loadDetails : Map[DetailType.Value, Boolean] = LINK_ID_LOAD) : Map[Int, LinkAssignments]= {
    val connection = Meta.getConnection()
      var queryString =
        if (joinAirplaneTable) {
          "SELECT airplane, link, frequency, flight_minutes FROM " + LINK_ASSIGNMENT_TABLE + " l LEFT JOIN " + AIRPLANE_TABLE + " a ON l.airplane = a.id"
        } else {
          "SELECT airplane, link, frequency, flight_minutes FROM " + LINK_ASSIGNMENT_TABLE
        }

      
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
      
      val airplanesWithAssignedLink = new mutable.HashMap[Int, mutable.HashMap[Int, LinkAssignment]]()
      while (resultSet.next()) {
        val airplaneId = resultSet.getInt("airplane")
        val linkId =  resultSet.getInt("link")

        val assignedLinks = airplanesWithAssignedLink.getOrElseUpdate(airplaneId, new mutable.HashMap[Int, LinkAssignment]())
        assignedLinks.put(linkId, LinkAssignment(resultSet.getInt("frequency"), resultSet.getInt("flight_minutes")))
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
    deleteAirplanesByCriteria(List(("a.id", airplaneId)))
 }
 
 def deleteAirplanesByCriteria (criteria : List[(String, Any)]) = {
   val airplanes = loadAirplanesCriteria(criteria)
   val connection = Meta.getConnection()

   if (airplanes.isEmpty) {
     0
   } else {
     var deleteCount = 0
     try {
       val idsString = airplanes.map(_.id).mkString(",")
       val queryString = s"DELETE FROM $AIRPLANE_TABLE WHERE id IN ($idsString)"

       val preparedStatement = connection.prepareStatement(queryString)
       deleteCount = preparedStatement.executeUpdate()

       preparedStatement.close()

       airplanes.map(_.owner.id).distinct.foreach { airlineId =>
         AirplaneOwnershipCache.invalidate(airlineId)
       }
     } finally {
       connection.close()
     }
     deleteCount
    }
  }

  def saveAirplanes(airplanes : List[Airplane]) = {
    val connection = Meta.getConnection()
    var updateCount = 0
      
    try {
      connection.setAutoCommit(false)    
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_TABLE + "(owner, model, constructed_cycle, purchased_cycle, airplane_condition, depreciation_rate, value, is_sold, dealer_ratio, home, purchase_rate) VALUES(?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
      val configurationStatement = connection.prepareStatement("REPLACE INTO " + AIRPLANE_CONFIGURATION_TABLE + "(airplane, configuration) VALUES(?,?)")


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
          preparedStatement.setInt(10, airplane.home.id)
          preparedStatement.setDouble(11, airplane.purchaseRate)
          updateCount += preparedStatement.executeUpdate()
          
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            airplane.id = generatedId //assign id back to the airplane

            if (airplane.configuration.id != 0) {
              configurationStatement.setInt(1, airplane.id)
              configurationStatement.setInt(2, airplane.configuration.id)

              configurationStatement.executeUpdate()
            }
          }
      }
      connection.commit()
      preparedStatement.close()
      configurationStatement.close()

      airplanes.map(_.owner.id).distinct.foreach { airlineId =>
        AirplaneOwnershipCache.invalidate(airlineId)
      }
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
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_TABLE + " SET owner = ?, airplane_condition = ?, depreciation_rate = ?, value = ?, constructed_cycle = ?, purchased_cycle = ?, is_sold = ?, dealer_ratio = ?, home = ?, purchase_rate = ? WHERE id = ?")
      val configurationStatement = connection.prepareStatement("REPLACE INTO " + AIRPLANE_CONFIGURATION_TABLE + "(airplane, configuration) VALUES(?,?)")
      val purgeConfigurationStatement = connection.prepareStatement("DELETE FROM " + AIRPLANE_CONFIGURATION_TABLE + " WHERE airplane = ?")
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
          preparedStatement.setInt(9, airplane.home.id)
          preparedStatement.setDouble(10, airplane.purchaseRate)
          preparedStatement.setInt(11, airplane.id)

          updateCount += preparedStatement.executeUpdate()

          if (airplane.configuration.id == 0) {
            purgeConfigurationStatement.setInt(1, airplane.id)
            purgeConfigurationStatement.executeUpdate()
          } else {
            configurationStatement.setInt(1, airplane.id)
            configurationStatement.setInt(2, airplane.configuration.id)
            configurationStatement.executeUpdate()
          }
      }
      
      connection.commit()
      preparedStatement.close()
      configurationStatement.close()
      purgeConfigurationStatement.close()

      airplanes.map(_.owner.id).distinct.foreach { airlineId =>
        AirplaneOwnershipCache.invalidate(airlineId)
      }
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
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_TABLE + " SET airplane_condition = ?, depreciation_rate = ?, value = ?, dealer_ratio = ?, home = ? WHERE id = ?")
      
      airplanes.foreach { 
        airplane =>
          preparedStatement.setDouble(1, airplane.condition)
          preparedStatement.setInt(2, airplane.depreciationRate)
          preparedStatement.setInt(3, airplane.value)
          preparedStatement.setDouble(4, airplane.dealerRatio)
          preparedStatement.setInt(5, airplane.home.id)
          preparedStatement.setInt(6, airplane.id)
          updateCount += preparedStatement.executeUpdate()
      }
      
      connection.commit()
      preparedStatement.close()

      airplanes.map(_.owner.id).distinct.foreach { airlineId =>
        AirplaneOwnershipCache.invalidate(airlineId)
      }
    } finally {
      connection.close()
    }
    
    updateCount
  }


  def saveAirplaneConfigurations(configurations : List[AirplaneConfiguration]) = {
    val connection = Meta.getConnection()
    var updateCount = 0

    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + "(airline, model, economy, business, first, is_default) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)

      configurations.foreach {
        configuration =>
          preparedStatement.setInt(1, configuration.airline.id)
          preparedStatement.setInt(2, configuration.model.id)
          preparedStatement.setInt(3, configuration.economyVal)
          preparedStatement.setInt(4, configuration.businessVal)
          preparedStatement.setInt(5, configuration.firstVal)
          preparedStatement.setBoolean(6, configuration.isDefault)
          updateCount += preparedStatement.executeUpdate()

          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            configuration.id = generatedId //assign id back to the configuration
          }
      }
      connection.commit()
      preparedStatement.close()
    } finally {
      connection.close()
    }
    updateCount
  }

  def updateAirplaneConfiguration(configuration: AirplaneConfiguration) = {
    val connection = Meta.getConnection()

    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + " SET economy = ?, business = ?, first = ?, is_default = ? WHERE id = ? AND airline = ? AND model = ?")

      preparedStatement.setInt(1, configuration.economyVal)
      preparedStatement.setDouble(2, configuration.businessVal)
      preparedStatement.setInt(3, configuration.firstVal)
      preparedStatement.setBoolean(4, configuration.isDefault)
      preparedStatement.setInt(5, configuration.id)
      preparedStatement.setInt(6, configuration.airline.id) //not necessary but just to play safe...
      preparedStatement.setInt(7, configuration.model.id)//not necessary but just to play safe...
      preparedStatement.executeUpdate()

      connection.commit()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def deleteAirplaneConfiguration(configuration: AirplaneConfiguration) = {
    val connection = Meta.getConnection()

    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE + " WHERE id = ? AND airline = ? AND model = ?")

      preparedStatement.setInt(1, configuration.id)
      preparedStatement.setInt(2, configuration.airline.id) //not necessary but just to play safe...
      preparedStatement.setInt(3, configuration.model.id)//not necessary but just to play safe...
      preparedStatement.executeUpdate()

      connection.commit()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def loadAirplaneConfigurationById(airlineId : Int, modelId : Int) = {
    loadAirplaneConfigurationsByCriteria(List(("airline", airlineId), ("model", modelId)))
  }

  def loadAirplaneConfigurationById(id : Int) = {
    val result = loadAirplaneConfigurationsByCriteria(List(("id", id)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }


  def loadAirplaneConfigurationsByCriteria(criteria : List[(String, Any)]) = {
    var queryString = "SELECT * FROM " + AIRPLANE_CONFIGURATION_TEMPLATE_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }

    loadAirplaneConfigurationsByQueryString(queryString, criteria.map(_._2))
  }



  def loadAirplaneConfigurationsByQueryString(queryString : String, parameters : List[Any]) = {
    val connection = Meta.getConnection()

    val preparedStatement = connection.prepareStatement(queryString)

    try {
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }

      val resultSet = preparedStatement.executeQuery()

      val configurations = new ListBuffer[AirplaneConfiguration]()


      while (resultSet.next()) {
        val configuration = AirplaneConfiguration(resultSet.getInt("economy"), resultSet.getInt("business"), resultSet.getInt("first"), Airline.fromId(resultSet.getInt("airline")), Model.fromId(resultSet.getInt("model")), resultSet.getBoolean("is_default"), id = resultSet.getInt("id"))
        configurations.append(configuration)
      }

      resultSet.close()
      configurations.toList
    } finally {
      preparedStatement.close()
      connection.close()
    }

  }

  
  object DetailType extends Enumeration {
    val LINK = Value
  }
}
