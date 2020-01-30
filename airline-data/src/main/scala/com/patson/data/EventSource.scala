package com.patson.data

import java.sql.Statement

import com.patson.data.Constants._
import com.patson.model.{Airline, Airport}
import com.patson.model.event.{Event, Olympics, OlympicsAirlineVote, OlympicsVoteRound}
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map}
import scala.collection.immutable


object EventSource {
  def saveOlympicsVoteRounds(eventId: Int, rounds : List[OlympicsVoteRound]) = ???

  def saveOlympicsAirlineVote(eventId: Int, vote : OlympicsAirlineVote) = ???

  def saveOlympicsCandidates(eventId: Int): List[Airport] = ???


  def loadOlympicsVoteRounds(eventId: Int): List[OlympicsVoteRound] = ???

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

  val insertEvents = (events : List[Event]) => {
    val connection = Meta.getConnection()
    //case class Alert(airline : Airline, message : String, category : AlertCategory.Value, targetId : Option[Int], cycle : Int, duration : Int, var id : Int)
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
      //open the hsqldb
    val connection = Meta.getConnection()
    try {  
      var queryString = "DELETE FROM " + EVENT_TABLE + " WHERE start_cycle < ?"
      
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