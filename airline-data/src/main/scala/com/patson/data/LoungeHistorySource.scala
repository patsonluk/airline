package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement
import java.sql.ResultSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map


object LoungeHistorySource {
  def deleteConsumptionsBeforeCycle(cycle: Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LOUNGE_CONSUMPTION_TABLE + " WHERE cycle < ?")
    statement.setInt(1, cycle)

    try {
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  val updateConsumptions = (consumptions : List[LoungeConsumptionDetails]) => {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("REPLACE INTO " + LOUNGE_CONSUMPTION_TABLE + "(airport, airline, self_visitors, alliance_visitors, cycle) VALUES(?,?,?,?,?)")
    
    connection.setAutoCommit(false)
    
    try {
      consumptions.foreach {  
        case LoungeConsumptionDetails(lounge : Lounge, selfVisitors : Int, allianceVisitors : Int, cycle : Int) => {
          statement.setInt(1, lounge.airport.id)
          statement.setInt(2, lounge.airline.id)
          statement.setInt(3, selfVisitors)
          statement.setInt(4, allianceVisitors)
          statement.setInt(5, cycle)
          statement.addBatch()
        }
      }
      statement.executeBatch()
      
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadLoungeConsumptionsByAirportId(airportId : Int, fullLoad : Boolean = false) = {
    loadLoungeConsumptionsByCriteria(List(("airport", airportId)))
  }
  
  def loadAll() = {
    loadLoungeConsumptionsByCriteria(List())
  }
  
  def loadLoungeConsumptionsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false) = {
      var queryString = "SELECT * FROM " + LOUNGE_CONSUMPTION_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      loadLoungesConsumptionsByQueryString(queryString, criteria.map(_._2), fullLoad)
  }
  
  private def loadLoungesConsumptionsByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false) : List[LoungeConsumptionDetails] = {
    val connection = Meta.getConnection()
    try {
        val preparedStatement = connection.prepareStatement(queryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }
        
        
        val resultSet = preparedStatement.executeQuery()
        
        val details = ListBuffer[LoungeConsumptionDetails]()
        
        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airportId = resultSet.getInt("airport")
          AirlineSource.loadLoungeByAirlineAndAirport(airlineId, airportId).foreach { lounge =>
              details += LoungeConsumptionDetails(lounge = lounge, selfVisitors = resultSet.getInt("self_visitors"), allianceVisitors = resultSet.getInt("alliance_visitors"), cycle = resultSet.getInt("cycle"))
          }
        }
        
        resultSet.close()
        preparedStatement.close()
        
        details.toList
      } finally {
        connection.close()
      }
  }
}
