package com.patson.data

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import com.patson.data.Constants._
import com.patson.model._
import com.patson.MainSimulation
import java.sql.Statement
import java.io.ByteArrayInputStream
import java.sql.Blob

import com.patson.model.oil.OilContract
import com.patson.model.oil.OilPrice
import com.patson.model.oil.OilInventoryPolicy
import com.patson.model.oil.OilConsumptionHistory
import com.patson.model.oil.OilConsumptionType
import com.patson.util.AirlineCache


object OilSource {
  //
  def loadAllOilContracts() : List[OilContract] = {
    loadOilContractsByCriteria(List())
  }
  
  
  def loadOilContractsByAirline(airlineId : Int) : List[OilContract] = {
    loadOilContractsByCriteria(List(("airline", airlineId)))
  }
  
  def loadOilContractById(id : Int) : Option[OilContract] = {
    val result = loadOilContractsByCriteria(List(("id", id)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  
  def loadOilContractsByCriteria(criteria : List[(String, Any)], airports : Map[Int, Airport] = Map[Int, Airport]()) : List[OilContract] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        var queryString = "SELECT * FROM " + OIL_CONTRACT_TABLE
        
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
        
        val contracts = new ListBuffer[OilContract]()
        
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          
          //airline : Airline, contractPrice : OilPrice, volume : Int, contractCost : Long, startCycle : Int, contractDuration : Int
          contracts += OilContract(airline = airline, contractPrice = resultSet.getDouble("price"), volume = resultSet.getInt("volume"), startCycle = resultSet.getInt("start_cycle"), contractDuration = resultSet.getInt("duration"), id = resultSet.getInt("id"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        contracts.toList
      } finally {
        connection.close()
      }
  }
  
  
  //case class AirlineBase(airline : Airline, airport : Airport, scale : Int, headQuarter : Boolean = false, var id : Int = 0) extends IdObject
  def saveOilContract(contract : OilContract) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + OIL_CONTRACT_TABLE + "(airline, price, volume, start_cycle, duration) VALUES(?, ?, ?, ?, ?)")
          
      preparedStatement.setInt(1, contract.airline.id)
      preparedStatement.setDouble(2, contract.contractPrice)
      preparedStatement.setInt(3, contract.volume)
      preparedStatement.setInt(4, contract.startCycle)
      preparedStatement.setInt(5, contract.contractDuration)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def deleteOilContract(contract : OilContract) = {
    deleteOilContractByCriteria(List(("id", contract.id)))
  }
  
  def deleteOilContractByCriteria(criteria : List[(String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + OIL_CONTRACT_TABLE
      
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
      
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
      
    } finally {
      connection.close()
    }
      
  }
  
  def loadOilPricesFromCycle(fromCycle : Int) : List[OilPrice] = {
    var queryString = "SELECT * FROM " + OIL_PRICE_TABLE + " WHERE cycle >= ?"
    loadOilPricesByQueryString(queryString, List(fromCycle))
  }
  
  def loadOilPriceByCycle(cycle : Int) : Option[OilPrice] = {
    var queryString = "SELECT * FROM " + OIL_PRICE_TABLE + " WHERE cycle = ?"
    val result = loadOilPricesByQueryString(queryString, List(cycle))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
  
  def loadOilPricesByQueryString(queryString : String, parameters : List[Any]) : List[OilPrice] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val prices = new ListBuffer[OilPrice]()
        
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          prices += OilPrice(price = resultSet.getDouble("price"), cycle = resultSet.getInt("cycle"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        prices.toList
      } finally {
        connection.close()
      }
  }
  def saveOilPrice(oilPrice : OilPrice) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + OIL_PRICE_TABLE + "(price, cycle) VALUES(?, ?)")
          
      preparedStatement.setDouble(1, oilPrice.price)
      preparedStatement.setInt(2, oilPrice.cycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  def deleteOilPricesUpToCycle(toCycle : Int) : Int = {
    var queryString = "DELETE FROM " + OIL_PRICE_TABLE + " WHERE cycle < ?"
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        preparedStatement.setInt(1, toCycle)
        val updateCount = preparedStatement.executeUpdate()
        preparedStatement.close()
        updateCount
      } finally {
        connection.close()
      }
  }
  
  def loadAllOilInventoryPolicies() = {
    loadOilInventoryPoliciesByCriteria(List.empty)
  }
  
  def loadOilInventoryPolicyByAirlineId(airlineId : Int) : Option[OilInventoryPolicy] = {
    val result = loadOilInventoryPoliciesByCriteria(List(("airline", airlineId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  
   def loadOilInventoryPoliciesByCriteria(criteria : List[(String, Any)]) : List[OilInventoryPolicy] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        var queryString = "SELECT * FROM " + OIL_INVENTORY_POLICY_TABLE
        
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
        
        val result = new ListBuffer[OilInventoryPolicy]()
        
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          
          //airline : Airline, contractPrice : OilPrice, volume : Int, contractCost : Long, startCycle : Int, contractDuration : Int
          result += OilInventoryPolicy(airline = airline, factor = resultSet.getDouble("factor"), startCycle = resultSet.getInt("start_cycle"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        result.toList
      } finally {
        connection.close()
      }
  }
   
  def saveOilInventoryPolicy(policy : OilInventoryPolicy) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("REPLACE INTO " + OIL_INVENTORY_POLICY_TABLE + "(airline, factor, start_cycle) VALUES(?, ?, ?)")
          
      preparedStatement.setInt(1, policy.airline.id)
      preparedStatement.setDouble(2, policy.factor)
      preparedStatement.setInt(3, policy.startCycle)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
  
  
  
   def loadOilConsumptionHistoryByAirlineId(airlineId : Int, cycle : Int) : List[OilConsumptionHistory] = {
    val result = loadOilConsumptionHistoryByCriteria(List(("airline", airlineId), ("cycle", cycle)))
    result
  }
  
   def loadOilConsumptionHistoryByCriteria(criteria : List[(String, Any)]) : List[OilConsumptionHistory] = {
      //open the hsqldb
      val connection = Meta.getConnection() 
      try {
        var queryString = "SELECT * FROM " + OIL_CONSUMPTION_HISTORY_TABLE
        
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
        
        val result = new ListBuffer[OilConsumptionHistory]()
        
        val airlines = Map[Int, Airline]()
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId, false).getOrElse(Airline.fromId(airlineId)))
          
          //(airline : Airline, price : Double, volume : Int, consumptionType : OilConsumptionType.Value, cycle : Int
          result += OilConsumptionHistory(airline = airline, 
              price = resultSet.getDouble("price"), 
              volume = resultSet.getInt("volume"), 
              consumptionType = OilConsumptionType(resultSet.getInt("consumption_type")), 
              cycle = resultSet.getInt("cycle"))
        }
        
        resultSet.close()
        preparedStatement.close()
        
        result.toList
      } finally {
        connection.close()
      }
  }
   
  def saveOilConsumptionHistory(historyEntires : List[OilConsumptionHistory]) = {
     val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + OIL_CONSUMPTION_HISTORY_TABLE + "(airline, price, volume, consumption_type, cycle) VALUES (?,?,?,?,?)")
    
      connection.setAutoCommit(false)
      historyEntires.foreach { 
        entry =>
          preparedStatement.setInt(1, entry.airline.id)
          preparedStatement.setDouble(2, entry.price)
          preparedStatement.setInt(3, entry.volume)
          preparedStatement.setInt(4, entry.consumptionType.id)
          preparedStatement.setInt(5, entry.cycle)
          preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def deleteOilConsumptionHistoryBeforeCycle(cycle : Int) = {
    var queryString = "DELETE FROM " + OIL_CONSUMPTION_HISTORY_TABLE + " WHERE cycle < ?"
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        preparedStatement.setInt(1, cycle)
        val updateCount = preparedStatement.executeUpdate()
        preparedStatement.close()
        updateCount
      } finally {
        connection.close()
      }
  }
}  
  
 