package com.patson.data

import java.sql.Statement

import com.patson.data.Constants._
import com.patson.model.{Airline, Airport}
import com.patson.model.event.{Event, EventType, Olympics, OlympicsAirlineVote, OlympicsVoteRound}
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map}
import scala.collection.immutable


object EventSource {
  def saveOlympicsVoteRounds(eventId: Int, rounds : List[OlympicsVoteRound]) = {
    val connection = Meta.getConnection()

    val statement = connection.prepareStatement("INSERT INTO " + OLYMPIC_VOTE_ROUND_TABLE + "(event, airport, round, vote) VALUES(?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      rounds.foreach { voteRound =>
        voteRound.votes.foreach {
          case(airport, vote) =>
            statement.setInt(1, eventId)
            statement.setInt(2, airport.id)
            statement.setInt(3, voteRound.round)
            statement.setInt(4, vote)
            statement.executeUpdate()
        }
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsAirlineVote(eventId: Int, vote : OlympicsAirlineVote) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("INSERT INTO " + OLYMPIC_AIRLINE_VOTE_TABLE + "(event, airline, airport, vote_weight, priority) VALUES(?,?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      var priority = 1
      vote.voteList.foreach { airport =>
        statement.setInt(1, eventId)
        statement.setInt(2, vote.airline.id)
        statement.setInt(3, airport.id)
        statement.setInt(4, vote.voteWeight)
        statement.setInt(5, priority)

        statement.executeUpdate()
        priority += 1
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsCandidates(eventId: Int, airports : List[Airport]) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("INSERT INTO " + OLYMPIC_CANDIDATE_TABLE + "(event, airport) VALUES(?,?)")

    connection.setAutoCommit(false)

    try {
      airports.foreach { airport =>
        statement.setInt(1, eventId)
        statement.setInt(2, airport.id)

        statement.executeUpdate()
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsAffectedAirports(eventId: Int, airports : List[Airport]) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("INSERT INTO " + OLYMPIC_AFFECTED_AIRPORT_TABLE + "(event, airport) VALUES(?,?)")

    connection.setAutoCommit(false)

    try {
      airports.foreach { airport =>
        statement.setInt(1, eventId)
        statement.setInt(2, airport.id)

        statement.executeUpdate()
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }


  def loadOlympicsVoteRounds(eventId: Int): List[OlympicsVoteRound] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_VOTE_ROUND_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.HashMap[Int, mutable.HashMap[Airport, Int]]()
      while (resultSet.next()) {
        val round = resultSet.getInt("round")
        val vote = resultSet.getInt("priority")
        val airportId = resultSet.getInt("airport")
        val airport = AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId))
        result.getOrElseUpdate(round, mutable.HashMap()).put(airport, vote)
      }

      resultSet.close()
      preparedStatement.close()

      result.view.map {
        case (round, airportVotes) => OlympicsVoteRound(round, airportVotes.toMap)
      }.toList.orderBy(_.round)
    } finally {
      connection.close()
    }

  }

  def loadOlympicsAirlineVotes(eventId: Int): immutable.Map[Airline, OlympicsAirlineVote] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AIRLINE_VOTE_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val votesByAirline : Map[Airline, ListBuffer[(Int, Airport)]] = mutable.HashMap() //List is (priority, airport)
      val weightsByAirline : Map[Airline, Int] = mutable.HashMap()
      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val priority = resultSet.getInt("priority")
        val weight = resultSet.getInt("weight")
        val airport = AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId))
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
        weightsByAirline.put(airline, weight) //put multiple times of same value...okay for now...
        val votesOfThisAirline = votesByAirline.getOrElseUpdate(airline, ListBuffer())
        votesOfThisAirline.append((priority, airport))
      }

      val result = votesByAirline.view.map {
        case (airline, votesWithPriority) =>
          val sortedVotes = votesWithPriority.sortBy(_._1).map(_._2)
          (airline, OlympicsAirlineVote(airline, weightsByAirline(airline), sortedVotes.toList))
      }.toMap

      resultSet.close()
      preparedStatement.close()

      result
    } finally {
      connection.close()
    }
  }

  def loadOlympicsCandidates(eventId: Int): List[Airport] =  {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_CANDIDATE_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()
      val airports = ListBuffer[Airport]()
      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        airports.append(AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId)))
      }

      resultSet.close()
      preparedStatement.close()

      airports.toList
    } finally {
      connection.close()
    }
  }

  def loadOlympicsAffectedAirports(eventId: Int): List[Airport] =  {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AFFECTED_AIRPORT_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()
      val airports = ListBuffer[Airport]()
      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        airports.append(AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId)))
      }

      resultSet.close()
      preparedStatement.close()

      airports.toList
    } finally {
      connection.close()
    }
  }


  def loadEvents(): List[Event] =  {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + EVENT_TABLE)

      val resultSet = preparedStatement.executeQuery()
      val events = ListBuffer[Event]()

      while (resultSet.next()) {
        val eventType = EventType(resultSet.getInt("event_type"))
        val event = eventType match {
          case EventType.OLYMPICS =>
            Olympics(resultSet.getInt("start_cycle"), resultSet.getInt("duration"), resultSet.getInt("id"))
        }
        events.append(event)
      }

      resultSet.close()
      preparedStatement.close()

      events.toList
    } finally {
      connection.close()
    }
  }

  val saveEvents = (events : List[Event]) => {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("INSERT INTO " + EVENT_TABLE + "(event_type, start_cycle, duration) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)
    
    connection.setAutoCommit(false)
    
    try {
      events.foreach { event => event match {
          case olympics : Olympics => {
            statement.setInt(1, olympics.eventType.id)
            statement.setInt(2, olympics.startCycle)
            statement.setInt(3, olympics.duration)

            statement.executeUpdate()
        
            val generatedKeys = statement.getGeneratedKeys
            if (generatedKeys.next()) {
              val generatedId = generatedKeys.getInt(1)
              event.id = generatedId
            }
          }
        }
      }
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }


  
  def deleteEventsBeforeCycle(cutoffCycle : Int) = {
    val connection = Meta.getConnection()
    try {  
      val queryString = "DELETE FROM " + EVENT_TABLE + " WHERE start_cycle < ?"
      
      val preparedStatement = connection.prepareStatement(queryString)
      
      preparedStatement.setObject(1, cutoffCycle)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      deletedCount
    } finally {
      connection.close()
    }
  }
}