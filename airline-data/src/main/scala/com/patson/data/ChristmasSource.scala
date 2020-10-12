package com.patson.data

import java.sql.{Statement, Types}

import com.patson.data.Constants._
import com.patson.model._
import com.patson.model.christmas.{SantaClausAwardType, SantaClausGuess, SantaClausInfo}
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable.{ListBuffer, Map}


object ChristmasSource {
  def saveSantaClausInfo(entries: List[SantaClausInfo]): List[SantaClausInfo] = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      //case class SantaClausInfo(airport : Airport, airline : Airline, attemptsLeft : Int, found : Boolean, pickedAward : Option[SantaClausAwardType.Value] )
      val preparedStatement = connection.prepareStatement("INSERT INTO " + SANTA_CLAUS_INFO_TABLE + "(airport, airline, attempts_left, found, picked_award) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)

      entries.foreach {
        entry =>
          preparedStatement.setInt(1, entry.airport.id)
          preparedStatement.setInt(2, entry.airline.id)
          preparedStatement.setInt(3, entry.attemptsLeft)
          preparedStatement.setBoolean(4, entry.found)
          entry.pickedAward match {
            case Some(pickedOption) => preparedStatement.setInt(5, pickedOption.id)
            case None => preparedStatement.setNull(5, Types.INTEGER)
          }

          preparedStatement.executeUpdate()
          val generatedKeys = preparedStatement.getGeneratedKeys
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            entry.id = generatedId
          }
      }

      preparedStatement.close()
      connection.commit()
      entries
    } finally {
      connection.close()
    }
  }

   def updateSantaClausInfo(entry: SantaClausInfo) = {
    val connection = Meta.getConnection()
    try {
      var preparedStatement = connection.prepareStatement("UPDATE " + SANTA_CLAUS_INFO_TABLE + " SET attempts_left = ?, found = ?, picked_award = ? WHERE id = ?")
      preparedStatement.setInt(1, entry.attemptsLeft)
      preparedStatement.setBoolean(2, entry.found)
      entry.pickedAward match {
        case Some(pickedOption) => preparedStatement.setInt(3, pickedOption.id)
        case None => preparedStatement.setNull(3, Types.INTEGER)
      }
      preparedStatement.setInt(4, entry.id)
      preparedStatement.executeUpdate()
    } finally {
      connection.close()
    }
  }

  def saveSantaClausGuess(guess : SantaClausGuess) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + SANTA_CLAUS_GUESS_TABLE + "(airport, airline) VALUES(?,?)")
      preparedStatement.setInt(1, guess.airport.id)
      preparedStatement.setInt(2, guess.airline.id)
      preparedStatement.executeUpdate()
    } finally {
      connection.close()
    }
  }
  
  def deleteAllSantaClausInfo() = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + SANTA_CLAUS_INFO_TABLE)
    try {
      statement.execute()
    } finally {
      statement.close()
      connection.close()
    }
  }
  
  def loadSantaClausInfoByAirline(airlineId : Int) = {
    val result = loadSantaClausInfoByCriteria(List(("airline", airlineId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result(0))
    }
  }
  

  def loadSantaClausInfoByCriteria(criteria : List[(String, Any)]) = {
      var queryString = "SELECT * FROM " + SANTA_CLAUS_INFO_TABLE
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
    loadSantaClausInfoByQueryString(queryString, criteria.map(_._2))
  }

  def loadSantaClausGuessesByAirline(airline : Airline) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + SANTA_CLAUS_GUESS_TABLE + " WHERE airline = ?")
      preparedStatement.setInt(1, airline.id)
      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[SantaClausGuess]()


      while (resultSet.next()) {
        val airport = AirportCache.getAirport(resultSet.getInt("airport")).getOrElse(Airport.fromId(0))
        entries += SantaClausGuess(airport, airline)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  private def loadSantaClausInfoByQueryString(queryString : String, parameters : List[Any]) : List[SantaClausInfo] = {
    val connection = Meta.getConnection()
    try {
        val infoPreparedStatement = connection.prepareStatement(queryString)

        for (i <- 0 until parameters.size) {
          infoPreparedStatement.setObject(i + 1, parameters(i))
        }


        val resultSet = infoPreparedStatement.executeQuery()

        val entries = ListBuffer[SantaClausInfo]()


        val airlines = Map[Int, Airline]()

        while (resultSet.next()) {
          val airlineId = resultSet.getInt("airline")
          val airline = airlines.getOrElseUpdate(airlineId, AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId)))
          val airport = AirportCache.getAirport(resultSet.getInt("airport")).getOrElse(Airport.fromId(0))
          val found = resultSet.getBoolean("found")
          val attemptsLeft = resultSet.getInt("attempts_left")
          val pickedAwardValue = resultSet.getInt("picked_award")
          val pickedAward =
            if (resultSet.wasNull()) {
              None
            } else {
              Some(SantaClausAwardType(pickedAwardValue))
            }
          val id =  resultSet.getInt("id")
          val guesses = loadSantaClausGuessesByAirline(airline)

          entries += SantaClausInfo(airport, airline, attemptsLeft, guesses, found, pickedAward, id)
        }
        
        resultSet.close()
        infoPreparedStatement.close()
        
        entries.toList
      } finally {
        connection.close()
      }
  }
}