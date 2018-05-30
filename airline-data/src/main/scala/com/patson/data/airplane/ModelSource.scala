package com.patson.data.airplane

import scala.collection.mutable.ListBuffer
import com.patson.data.Constants._
import com.patson.model.airplane.Model
import com.patson.data.Meta
import java.sql.ResultSet

object ModelSource {
  private[this] val BASE_QUERY = "SELECT id, name, capacity, fuel_burn, speed, fly_range, price FROM " + AIRPLANE_MODEL_TABLE 
  
  def loadAllModels() = {
      loadModelsByCriteria(List.empty)
  }
  
  def loadModelsByCriteria(criteria : List[(String, Any)]) = {
    val queryString = new StringBuilder(BASE_QUERY) 
      
    if (!criteria.isEmpty) {
      queryString.append(" WHERE ")
      for (i <- 0 until criteria.size - 1) {
        queryString.append(criteria(i)._1 + " = ? AND ")
      }
      queryString.append(criteria.last._1 + " = ?")
    }
    loadModelsByQuery(queryString.toString, criteria.map(_._2))
  }
  def loadModelsByQuery(queryString : String, parameters : Seq[Any] = Seq.empty) = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }
      
      
      val resultSet = preparedStatement.executeQuery()
      
 
//  "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                                             "name VARCHAR(256), " +
//                                             "capacity INTEGER, " + 
//                                             "fuel_burn INTEGER, " +
//                                             "speed INTEGER, " +
//                                             "fly_range INTEGER, " +
//                                             "price INTEGER)")
      val models = new ListBuffer[Model]()
      while (resultSet.next()) {
        models += getModelFromRow(resultSet)
      }
      
      resultSet.close()
      preparedStatement.close()
      connection.close()
      
      models.toList
  }
  
  def getModelFromRow(resultSet : ResultSet) = {
     val model = Model( 
          resultSet.getString("name"),
          resultSet.getInt("capacity"),
          resultSet.getInt("fuel_burn"),
          resultSet.getInt("speed"),
          resultSet.getInt("fly_range"),
          resultSet.getInt("price")
          )
     model.id = resultSet.getInt("id")
     model
  }
  
  def loadModelById(id : Int) = {
      val result = loadModelsByCriteria(List(("id", id)))
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  
  def loadModelsWithinRange(range : Int) = {
    val queryString = new StringBuilder(BASE_QUERY) 
      
    queryString.append(" WHERE fly_range >= ?")
    loadModelsByQuery(queryString.toString, Seq(range))
  }
   
  def deleteAllModels() = {
      //open the hsqldb
      val connection = Meta.getConnection()
      
      var queryString = "DELETE FROM  " + AIRPLANE_MODEL_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      connection.close()
      
      println("Deleted " + deletedCount + " model records")
      deletedCount
  }
  
  
  def saveModels(models : List[Model]) = {
    val connection = Meta.getConnection()
        
        val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPLANE_MODEL_TABLE + "(name, capacity, fuel_burn, speed, fly_range, price) VALUES(?,?,?,?,?,?)")
        
        connection.setAutoCommit(false)
        models.foreach { 
          model =>
            preparedStatement.setString(1, model.name)
            preparedStatement.setInt(2, model.capacity)
            preparedStatement.setInt(3, model.fuelBurn)
            preparedStatement.setInt(4, model.speed)
            preparedStatement.setInt(5, model.range)
            preparedStatement.setInt(6, model.price)
            preparedStatement.executeUpdate()
        }
        connection.commit()
        
        connection.close()
  }
}