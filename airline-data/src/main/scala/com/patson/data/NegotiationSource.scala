package com.patson.data

import com.patson.data.Constants._

import com.patson.model.negotiation.LinkNegotiationDiscount
import com.patson.util.{AirlineCache, AirportCache}

import java.sql.Statement
import scala.collection.mutable.ListBuffer


object NegotiationSource {
  val saveLinkDiscount = (entry : LinkNegotiationDiscount) => {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement(s"INSERT INTO $LINK_NEGOTIATION_DISCOUNT_TABLE(airline, from_airport, to_airport, discount, expiration_cycle) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    try {
      preparedStatement.setInt(1, entry.airline.id)
      preparedStatement.setInt(2, entry.fromAirport.id)
      preparedStatement.setInt(3, entry.toAirport.id)
      preparedStatement.setBigDecimal(4, entry.discount.bigDecimal)
      preparedStatement.setInt(5, entry.expiry)

      val updateCount = preparedStatement.executeUpdate()
      if (updateCount > 0) {
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          entry.id = generatedId
        }
      }
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }




  def loadLinkDiscounts(airlineId : Int, fromAirportId : Int, toAirportId : Int) = {
    loadLinkDiscountsByCriteria(List(("airline", airlineId), ("from_airport", fromAirportId), ("to_airport", toAirportId)))
  }

  def loadLinkDiscountsByCriteria(criteria : List[(String, Any)], loadArea : Boolean = false) = {
    var queryString = "SELECT * FROM " + LINK_NEGOTIATION_DISCOUNT_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadLinkDiscountsByQueryString(queryString, criteria.map(_._2), loadArea)
  }

  def loadLinkDiscountsByQueryString(queryString : String, parameters : List[Any], loadArea : Boolean = false) : List[LinkNegotiationDiscount] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[LinkNegotiationDiscount]()

      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val fromAirport = AirportCache.getAirport(resultSet.getInt("from_airport")).get
        val toAirport = AirportCache.getAirport(resultSet.getInt("to_airport")).get
        val discount = resultSet.getBigDecimal("discount")
        val expiry = resultSet.getInt("expiration_cycle")
        val airline = AirlineCache.getAirline(resultSet.getInt("airline")).get

        entries += LinkNegotiationDiscount(airline, fromAirport, toAirport, discount, expiry, id)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  
  def deleteLinkDiscountsByAirline(airlineId : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LINK_NEGOTIATION_DISCOUNT_TABLE + " WHERE airline = ?")

    try {
      statement.setInt(1, airlineId)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def deleteLinkDiscountsByAirlineAndAirport(airlineId : Int, fromAirportId : Int, toAirportId : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LINK_NEGOTIATION_DISCOUNT_TABLE + " WHERE airline = ? AND from_airport =? AND to_airport = ?")

    try {
      statement.setInt(1, airlineId)
      statement.setInt(2, fromAirportId)
      statement.setInt(3, toAirportId)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }


  def deleteLinkDiscountBeforeExpiry(cutoff : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + LINK_NEGOTIATION_DISCOUNT_TABLE + " WHERE expiration_cycle < ?")

    try {
      statement.setInt(1, cutoff)
      statement.executeUpdate()
    } finally {
      statement.close()
      connection.close()
    }
  }
}