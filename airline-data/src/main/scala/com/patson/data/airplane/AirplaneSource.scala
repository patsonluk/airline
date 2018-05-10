package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.Airport
import com.patson.model.Link
import com.patson.model.Airline
import com.patson.model.airplane.Airplane
import com.patson.model.airplane.Model
import com.patson.data.airplane.ModelSource
import java.sql.Statement

object AirplaneSource {
  val LINK_FULL_LOAD = Map(DetailType.LINK -> true)
  val LINK_SIMPLE_LOAD = Map(DetailType.LINK -> false)
  val LINK_ID_LOAD : Map[DetailType.Value, Boolean] = Map.empty
  
  private[this] val BASE_QUERY = "SELECT owner, a.id as id, model, name, capacity, fuel_burn, speed, fly_range, price, constructed_cycle, airplane_condition, a.depreciation_rate, a.value FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id" 
  
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
  
  def loadAirplanesByQueryString(queryString : String, parameters : List[Any]) = {
      val connection = Meta.getConnection()
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val airplanes = new ListBuffer[Airplane]()
      while (resultSet.next()) {
        val model = Model( 
          resultSet.getString("name"),
          resultSet.getInt("capacity"),
          resultSet.getInt("fuel_burn"),
          resultSet.getInt("speed"),
          resultSet.getInt("fly_range"),
          resultSet.getInt("price"),
          resultSet.getInt("model")
          )
        val airplane = Airplane(model, Airline.fromId(resultSet.getInt("owner")), resultSet.getInt("constructed_cycle"), resultSet.getDouble("airplane_condition"), depreciationRate = resultSet.getInt("depreciation_rate"), value = resultSet.getInt("value"))
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
  
  def loadAirplanesWithAssignedLinkByOwner(ownerId : Int, loadDetails : Map[DetailType.Value, Boolean] = LINK_ID_LOAD)  : List[(Airplane, Option[Link])] = {
    loadAirplanesWithAssignedLinkByCriteria(List(("owner", ownerId)), loadDetails)
  }
  
  def loadAirplanesWithAssignedLinkByAirplaneId(airplaneId : Int, loadDetails : Map[DetailType.Value, Boolean] = LINK_ID_LOAD) : Option[(Airplane, Option[Link])] = {
    val result = loadAirplanesWithAssignedLinkByCriteria(List(("a.id", airplaneId)), loadDetails)
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
   }
  
  def loadAirplanesWithAssignedLinkByCriteria(criteria : List[(String, Any)], loadDetails : Map[DetailType.Value, Boolean] = LINK_ID_LOAD) : List[(Airplane, Option[Link])]= {
    val connection = Meta.getConnection()
      var queryString = "SELECT owner, a.id as id, model, name, capacity, fuel_burn, speed, fly_range, price, constructed_cycle, airplane_condition, depreciation_rate, value, la.link  FROM " + AIRPLANE_TABLE + " a LEFT JOIN " + AIRPLANE_MODEL_TABLE + " m ON a.model = m.id LEFT JOIN " + LINK_ASSIGNMENT_TABLE + " la ON a.id = la.airplane"  
      
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
      
      val loadLinkFunction : (Int => Link) = loadDetails.get(DetailType.LINK) match {
        case Some(fullLoad) => (linkId : Int) => 
          if (fullLoad) {
            LinkSource.loadLinkById(linkId, LinkSource.FULL_LOAD).get 
          } else {
            LinkSource.loadLinkById(linkId, LinkSource.SIMPLE_LOAD).get
          }
        case None => (linkId : Int) => Link.fromId(linkId) 
      }
      
      val resultSet = preparedStatement.executeQuery()
      
      val airplanesWithAssignedLink = new ListBuffer[(Airplane, Option[Link])]()
      while (resultSet.next()) {
        val model = Model( 
          resultSet.getString("name"),
          resultSet.getInt("capacity"),
          resultSet.getInt("fuel_burn"),
          resultSet.getInt("speed"),
          resultSet.getInt("fly_range"),
          resultSet.getInt("price"),
          resultSet.getInt("model")
          )
          
      

        val airplane = Airplane(model, Airline.fromId(resultSet.getInt("owner")), resultSet.getInt("constructed_cycle"), resultSet.getDouble("airplane_condition"), depreciationRate = resultSet.getInt("depreciation_rate"), value = resultSet.getInt("value"))
        airplane.id = resultSet.getInt("id")
        if (resultSet.getObject("link") != null) {
          val linkId = resultSet.getInt("link")
          val assignedLink = loadLinkFunction(linkId)
            
          airplanesWithAssignedLink.append((airplane, Some(assignedLink)))
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
    var updateCount = 0
      
    try {
      connection.setAutoCommit(false)    
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_TABLE + "(owner, model, constructed_cycle, airplane_condition, depreciation_rate, value) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
      
      airplanes.foreach { 
        airplane =>
          preparedStatement.setInt(1, airplane.owner.id)
          preparedStatement.setInt(2, airplane.model.id)
          preparedStatement.setInt(3, airplane.constructedCycle)
          preparedStatement.setDouble(4, airplane.condition)
          preparedStatement.setInt(5, airplane.depreciationRate)
          preparedStatement.setInt(6, airplane.value)
          updateCount += preparedStatement.executeUpdate()
          
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            airplane.id = generatedId //assign id back to the airplane
          }
      }
      connection.commit()
      preparedStatement.close()
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
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPLANE_TABLE + " SET owner = ?, airplane_condition = ?, depreciation_rate = ?, value = ? WHERE id = ?")
      
      airplanes.foreach { 
        airplane =>
          preparedStatement.setInt(1, airplane.owner.id)
          preparedStatement.setDouble(2, airplane.condition)
          preparedStatement.setInt(3, airplane.depreciationRate)
          preparedStatement.setInt(4, airplane.value)
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