package com.patson.data

import com.patson.data.Constants._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object DelegateSource {
  private[this] val BASE_LINK_CHANGE_QUERY = "SELECT * FROM " + BUSY_DELEGATE_TABLE
  
  def loadBusyDelegateByAirline(airlineId : Int) : List[Int] = {
    loadBusyDelegateByCriteria(List(("airline", "=", airlineId))).get(airlineId).getOrElse(List.empty)
  }
  

  def loadBusyDelegateByCriteria(criteria : List[(String, String, Any)]) = {
      var queryString = BASE_LINK_CHANGE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + criteria.last._2 + " ?"
      }
      loadBusyDelegateByQueryString(queryString, criteria.map(_._3))
  }
  
  def loadBusyDelegateByQueryString(queryString : String, parameters : List[Any]) : Map[Int, List[Int]]= {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }

        
        val resultSet = preparedStatement.executeQuery()
        
        val result = mutable.Map[Int, ListBuffer[Int]]()
        
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val busyUntilCycle = resultSet.getInt("busy_until_cycle")
          result.getOrElseUpdate(airlineId, ListBuffer[Int]()).append(busyUntilCycle)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        result.view.mapValues(_.toList).toMap
      } finally {
        connection.close()
      }
  }


  def saveBusyDelegate(airlineId : Int, busyUntilCycle : Int) : Unit = {
    saveBusyDelegates(airlineId, List(busyUntilCycle))
  }

  def saveBusyDelegates(airlineId : Int, entries : List[Int]) : Unit = {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + BUSY_DELEGATE_TABLE + "(airline, busy_until_cycle) VALUES(?,?)")
    try {
      entries.foreach { busyUntilCycle =>
        preparedStatement.setInt(1, airlineId)
        preparedStatement.setInt(2, busyUntilCycle)

        preparedStatement.executeUpdate()
      }
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }
 

  
  
  def deleteBusyDelegateByCriteria(criteria : List[(String, String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + BUSY_DELEGATE_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + criteria.last._2 + " ?"
      }
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._3)
      }
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " busy delegate records")
      deletedCount
    } finally {
      connection.close()
    }
  }
}