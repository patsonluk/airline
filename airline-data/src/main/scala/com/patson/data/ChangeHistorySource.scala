package com.patson.data

import java.sql.Types

import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.airplane.Model
import com.patson.model.history.LinkChange
import com.patson.util._

import scala.collection.mutable.ListBuffer


object ChangeHistorySource {
  private[this] val BASE_LINK_CHANGE_QUERY = "SELECT * FROM " + LINK_CHANGE_HISTORY_TABLE
  val RESULT_SIZE = 500
  
  def loadLinkChangeByAirline(airlineId : Int) : List[LinkChange] = {
    loadLinkChangeByCriteria(List(("airline", "=", airlineId)))
  }
  
  def loadLinkChangeByAllianceName(allianceName : String) : List[LinkChange] = {
    loadLinkChangeByCriteria(List(("alliance_name", "=", allianceName)))
  }
  
  def loadLinkChangeByCriteria(criteria : List[(String, String, Any)]) = {
      var queryString = BASE_LINK_CHANGE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + criteria(i)._2 + " ? AND "
        }
        queryString += criteria.last._1 + criteria.last._2 + " ?"
      }
      loadLinkChangeByQueryString(queryString, criteria.map(_._3))
  }
  
  def loadLinkChangeByQueryString(queryString : String, parameters : List[Any]) : List[LinkChange]= {
    val connection = Meta.getConnection()
    try {
      val finalQueryString = queryString + " ORDER BY id DESC LIMIT 0, " + RESULT_SIZE
        val preparedStatement = connection.prepareStatement(finalQueryString)
        
        for (i <- 0 until parameters.size) {
          preparedStatement.setObject(i + 1, parameters(i))
        }

        
        val resultSet = preparedStatement.executeQuery()
        
        val entries = ListBuffer[LinkChange]()
        
        while (resultSet.next()) {
          val price = LinkClassValues.getInstance(resultSet.getInt("price_economy"), resultSet.getInt("price_business"), resultSet.getInt("price_first"))
          val priceDelta = LinkClassValues.getInstance(resultSet.getInt("price_economy_delta"), resultSet.getInt("price_business_delta"), resultSet.getInt("price_first_delta"))
          val capacity = LinkClassValues.getInstance(resultSet.getInt("capacity_economy"), resultSet.getInt("capacity_business"), resultSet.getInt("capacity_first"))
          val capacityDelta = LinkClassValues.getInstance(resultSet.getInt("capacity_economy_delta"), resultSet.getInt("capacity_business_delta"), resultSet.getInt("capacity_first_delta"))
          val fromAirport = AirportCache.getAirport(resultSet.getInt("from_airport")).get
          val toAirport = AirportCache.getAirport(resultSet.getInt("to_airport")).get
          val fromCountry = CountryCache.getCountry(resultSet.getString("from_country")).get
          val toCountry = CountryCache.getCountry(resultSet.getString("to_country")).get
          val airline = AirlineCache.getAirline(resultSet.getInt("airline")).getOrElse(Airline.fromId(resultSet.getInt("airline")))

          val allianceObject = resultSet.getObject("alliance")
          val alliance : Option[Alliance] =
            if (allianceObject == null) {
              None
            } else {
              AllianceCache.getAlliance(resultSet.getInt("alliance"))
            }
          val airplaneModel = AirplaneModelCache.getModel(resultSet.getInt("airplane_model")).getOrElse(Model.fromId(resultSet.getInt("airplane_model")))


          val entry = LinkChange(
            linkId = resultSet.getInt("link"),
            price = price,
            priceDelta = priceDelta,
            capacity = capacity,
            capacityDelta = capacityDelta,
            fromAirport = fromAirport,
            toAirport = toAirport,
            fromCountry = fromCountry,
            toCountry = toCountry,
            fromZone = resultSet.getString("from_zone"),
            toZone = resultSet.getString("to_zone"),
            airline = airline,
            alliance = alliance,
            frequency = resultSet.getInt("frequency"),
            flightNumber = resultSet.getInt("flight_number"),
            airplaneModel = airplaneModel,
            rawQuality = resultSet.getInt("raw_quality"),
            cycle =  resultSet.getInt("cycle"),
            id = resultSet.getInt("id"))

          entries.append(entry)
        }
        
        resultSet.close()
        preparedStatement.close()
        
        entries.toList
      } finally {
        connection.close()
      }
  }


  def saveLinkChange(change : LinkChange) : Unit = {
    saveLinkChanges(List(change))
  }
    
  def saveLinkChanges(changes : List[LinkChange]) : Unit = {
    val connection = Meta.getConnection()
    connection.setAutoCommit(false)
    val preparedStatement = connection.prepareStatement("INSERT INTO " + LINK_CHANGE_HISTORY_TABLE + "(link, price_economy, price_business, price_first, price_economy_delta, price_business_delta, price_first_delta, capacity_economy, capacity_business, capacity_first, capacity, capacity_economy_delta, capacity_business_delta, capacity_first_delta, capacity_delta, from_airport, to_airport, from_country, to_country, from_zone, to_zone, airline, alliance, frequency, flight_number, airplane_model, raw_quality, cycle) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
    try {
      changes.foreach { change =>
        preparedStatement.setInt(1, change.linkId)
        preparedStatement.setInt(2, change.price.economyVal)
        preparedStatement.setInt(3, change.price.businessVal)
        preparedStatement.setInt(4, change.price.firstVal)
        preparedStatement.setInt(5, change.priceDelta.economyVal)
        preparedStatement.setInt(6, change.priceDelta.businessVal)
        preparedStatement.setInt(7, change.priceDelta.firstVal)
        preparedStatement.setInt(8, change.capacity.economyVal)
        preparedStatement.setInt(9, change.capacity.businessVal)
        preparedStatement.setInt(10, change.capacity.firstVal)
        preparedStatement.setInt(11, change.capacity.total)
        preparedStatement.setInt(12, change.capacityDelta.economyVal)
        preparedStatement.setInt(13, change.capacityDelta.businessVal)
        preparedStatement.setInt(14, change.capacityDelta.firstVal)
        preparedStatement.setInt(15, change.capacityDelta.total)
        preparedStatement.setInt(16, change.fromAirport.id)
        preparedStatement.setInt(17, change.toAirport.id)
        preparedStatement.setString(18, change.fromCountry.countryCode)
        preparedStatement.setString(19, change.toCountry.countryCode)
        preparedStatement.setString(20, change.fromZone)
        preparedStatement.setString(21, change.toZone)
        preparedStatement.setInt(22, change.airline.id)
        change.alliance match {
          case Some(alliance) =>
            preparedStatement.setInt(23, alliance.id)
          case None =>
            preparedStatement.setNull(23, Types.INTEGER)
        }

        preparedStatement.setInt(24, change.frequency)
        preparedStatement.setInt(25, change.flightNumber)
        preparedStatement.setInt(26, change.airplaneModel.id)
        preparedStatement.setInt(27, change.rawQuality)
        preparedStatement.setInt(28, change.cycle)

        preparedStatement.executeUpdate()
      }
      
      preparedStatement.close()
      connection.commit()
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }
 

  
  
  def deleteLinkChangeByCriteria(criteria : List[(String, String, Any)]) = {
      //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + LINK_CHANGE_HISTORY_TABLE
      
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
      println("Deleted " + deletedCount + " link change history records")
      deletedCount
    } finally {
      connection.close()
    }
  }
}