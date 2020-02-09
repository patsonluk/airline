package com.patson.data

import java.sql.Statement

import com.patson.LinkSimulation.PassengerTransportStats
import com.patson.data.Constants._
import com.patson.model.{Airline, Airport}
import com.patson.model.event.{Event, EventReward, EventType, Olympics, OlympicsAirlineVote, OlympicsVoteRound, RewardCategory, RewardOption}
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer


object EventSource {
  def saveOlympicsAirlinePassengerGoals(eventId : Int, goals: Map[Airline, Int]) = {
    val connection = Meta.getConnection()

    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_AIRLINE_GOAL_TABLE + "(event, airline, goal) VALUES(?,?,?)")

    connection.setAutoCommit(false)

    try {
      goals.foreach {
        case (airline, goal) =>
          statement.setInt(1, eventId)
          statement.setInt(2, airline.id)
          statement.setInt(3, goal)
          statement.executeUpdate()
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsCountryStats(eventId : Int, olympicsCountryStats: Map[String, PassengerTransportStats]) = {
    val connection = Meta.getConnection()

    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_COUNTRY_STATS_TABLE + "(event, cycle, country_code, transported, total) VALUES(?,?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      olympicsCountryStats.foreach {
        case (countryCode, PassengerTransportStats(cycle, transported, total)) =>
            statement.setInt(1, eventId)
            statement.setInt(2, cycle)
            statement.setString(3, countryCode)
            statement.setInt(4, transported)
            statement.setInt(5, total)
            statement.executeUpdate()
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsVoteRounds(eventId: Int, rounds : List[OlympicsVoteRound]) = {
    val connection = Meta.getConnection()

    val purgeStatement = connection.prepareStatement("DELETE FROM " + OLYMPIC_VOTE_ROUND_TABLE + " WHERE event = ?")
    purgeStatement.setInt(1, eventId)
    purgeStatement.execute()

    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_VOTE_ROUND_TABLE + "(event, airport, round, vote) VALUES(?,?,?,?)")

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
      purgeStatement.close()
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsAirlineStats(eventId : Int, olympicsAirlineStats: Map[Airline, (Int, BigDecimal)]) = {
    val connection = Meta.getConnection()

    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_AIRLINE_STATS_TABLE + "(event, cycle, airline, score) VALUES(?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      olympicsAirlineStats.foreach {
        case (airline, (cycle, score)) =>
          statement.setInt(1, eventId)
          statement.setInt(2, cycle)
          statement.setInt(3, airline.id)
          statement.setBigDecimal(4, score.bigDecimal)
          statement.executeUpdate()
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def saveOlympicsAirlineVote(eventId: Int, vote : OlympicsAirlineVote) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_AIRLINE_VOTE_TABLE + "(event, airline, airport, precedence) VALUES(?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      var precedence = 1
      vote.voteList.foreach { airport =>
        statement.setInt(1, eventId)
        statement.setInt(2, vote.airline.id)
        statement.setInt(3, airport.id)
        statement.setInt(4, precedence)

        statement.executeUpdate()
        precedence += 1
      }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }


  def deleteOlympicsAirlineVote(eventId: Int, airlineId : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + OLYMPIC_AIRLINE_VOTE_TABLE + " WHERE event = ? AND airline = ?")

    connection.setAutoCommit(false)

    try {
      statement.setInt(1, eventId)
      statement.setInt(2, airlineId)
      statement.executeUpdate()
      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }


  def saveOlympicsCandidates(eventId: Int, airports : List[Airport]) = {
    val connection = Meta.getConnection()

    val purgeStatement = connection.prepareStatement("DELETE FROM " + OLYMPIC_CANDIDATE_TABLE + " WHERE event = ?")
    purgeStatement.setInt(1, eventId)
    purgeStatement.execute()

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

  def saveOlympicsAffectedAirports(eventId: Int, airports : Map[Airport, List[Airport]]) = {
    val connection = Meta.getConnection()

    val purgeStatement = connection.prepareStatement("DELETE FROM " + OLYMPIC_AFFECTED_AIRPORT_TABLE + " WHERE event = ?")
    purgeStatement.setInt(1, eventId)
    purgeStatement.execute()

    val statement = connection.prepareStatement("REPLACE INTO " + OLYMPIC_AFFECTED_AIRPORT_TABLE + "(event, principal_airport, affected_airport) VALUES(?,?,?)")

    connection.setAutoCommit(false)

    try {
      airports.foreach {
        case(principalAirport, affectedAirports) =>
          affectedAirports.foreach { affectedAirport =>
            statement.setInt(1, eventId)
            statement.setInt(2, principalAirport.id)
            statement.setInt(3, affectedAirport.id)
            statement.executeUpdate()
          }
     }

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def savePickedRewardOption(eventId: Int, airlineId : Int, option : EventReward) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("REPLACE INTO " + EVENT_PICKED_REWARD_TABLE + "(event, airline, reward_category, reward_option) VALUES(?,?,?,?)")

    connection.setAutoCommit(false)

    try {
      statement.setInt(1, eventId)
      statement.setInt(2, airlineId)
      statement.setInt(3, option.rewardCategory.id)
      statement.setInt(4, option.rewardOption.id)
      statement.executeUpdate()

      connection.commit()
    } finally {
      statement.close()
      connection.close()
    }
  }

  def loadPickedRewardOption(eventId: Int, airlineId : Int, rewardCategory: RewardCategory.Value) : Option[EventReward] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + EVENT_PICKED_REWARD_TABLE + " WHERE event = ? AND airline = ? AND reward_category = ?")

      preparedStatement.setInt(1, eventId)
      preparedStatement.setInt(2, airlineId)
      preparedStatement.setInt(3, rewardCategory.id)
      val resultSet = preparedStatement.executeQuery()
      val result : Option[EventReward] =
        if (resultSet.next()) {
          EventReward.fromId(resultSet.getInt("reward_category"), resultSet.getInt("reward_option"))
        } else {
          None
        }


      resultSet.close()
      preparedStatement.close()

      result
    } finally {
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
        val vote = resultSet.getInt("vote")
        val airportId = resultSet.getInt("airport")
        val airport = AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId))
        result.getOrElseUpdate(round, mutable.HashMap()).put(airport, vote)
      }

      resultSet.close()
      preparedStatement.close()

      result.view.map {
        case (round, airportVotes) => OlympicsVoteRound(round, airportVotes.toMap)
      }.toList.sortBy(_.round)
    } finally {
      connection.close()
    }

  }

  def loadOlympicsAirlineVotes(eventId: Int, airlineId : Int): Option[OlympicsAirlineVote] = {
    val result = loadOlympicsAirlineVotesByCriteria(List(("event", eventId), ("airline", airlineId)))
    if (result.isEmpty) {
      None
    } else {
      Some(result.iterator.next()._2)
    }
  }

  def loadOlympicsAirlineVotes(eventId: Int): immutable.Map[Airline, OlympicsAirlineVote] = {
    loadOlympicsAirlineVotesByCriteria(List(("event", eventId)))
  }

  def loadOlympicsAirlineVotesByCriteria(criteria : List[(String, Any)]): immutable.Map[Airline, OlympicsAirlineVote] = {

    var queryString = "SELECT * FROM " + OLYMPIC_AIRLINE_VOTE_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }

      val resultSet = preparedStatement.executeQuery()

      val votesByAirline : mutable.Map[Airline, ListBuffer[(Int, Airport)]] = mutable.HashMap() //List is (precedence, airport)
      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val precedence = resultSet.getInt("precedence")
        val airport = AirportCache.getAirport(airportId).getOrElse(Airport.fromId(airportId))
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
        val votesOfThisAirline = votesByAirline.getOrElseUpdate(airline, ListBuffer())
        votesOfThisAirline.append((precedence, airport))
      }

      val result = votesByAirline.view.map {
        case (airline, votesWithPrecedence) =>
          val sortedVotes = votesWithPrecedence.sortBy(_._1).map(_._2)
          (airline, OlympicsAirlineVote(airline, sortedVotes.toList))
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

  /**
    *
    * @param eventId
    * @return key as principal airport, value as the list of affected airports
    */
  def loadOlympicsAffectedAirports(eventId: Int): immutable.Map[Airport, List[Airport]] =  {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AFFECTED_AIRPORT_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()
      val result = mutable.HashMap[Airport, ListBuffer[Airport]]()
      while (resultSet.next()) {
        val principalAirportId = resultSet.getInt("principal_airport")
        val affectedAirportId = resultSet.getInt("affected_airport")

        val principalAirport = AirportCache.getAirport(principalAirportId).getOrElse(Airport.fromId(principalAirportId))
        val affectedAirport = AirportCache.getAirport(affectedAirportId).getOrElse(Airport.fromId(affectedAirportId))
        val airportsOfThisPrincipal = result.getOrElseUpdate(principalAirport, ListBuffer[Airport]())
        airportsOfThisPrincipal.append(affectedAirport)
      }

      resultSet.close()
      preparedStatement.close()

      result.view.mapValues(_.toList).toMap
    } finally {
      connection.close()
    }
  }

  def loadOlympicsCountryStats(eventId: Int): Map[String, List[PassengerTransportStats]] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_COUNTRY_STATS_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.HashMap[String, ListBuffer[PassengerTransportStats]]()
      while (resultSet.next()) {
        val countryCode = resultSet.getString("country_code")
        val cycle = resultSet.getInt("cycle")
        val transported = resultSet.getInt("transported")
        val total = resultSet.getInt("total")
        val existing = result.getOrElseUpdate(countryCode, ListBuffer())
        existing.append(PassengerTransportStats(cycle, transported, total))
      }

      resultSet.close()
      preparedStatement.close()

      result.view.mapValues(_.toList).toMap
    } finally {
      connection.close()
    }
  }

  /**
    *
    * @param eventId
    * @return Map[airline, (cycle, score)]
    */
  def loadOlympicsAirlineStats(eventId: Int): Map[Airline, List[(Int, BigDecimal)]] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AIRLINE_STATS_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.HashMap[Airline, ListBuffer[(Int, BigDecimal)]]()
      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val cycle = resultSet.getInt("cycle")
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
        val score = resultSet.getBigDecimal("score")
        val existing = result.getOrElseUpdate(airline, ListBuffer())
        existing.append((cycle, score))
      }

      resultSet.close()
      preparedStatement.close()

      result.view.mapValues(_.toList).toMap
    } finally {
      connection.close()
    }

  }

  def loadOlympicsAirlineStats(eventId: Int, airlineId : Int): List[(Int, BigDecimal)] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AIRLINE_STATS_TABLE + " WHERE event = ? AND airline = ?")

      preparedStatement.setInt(1, eventId)
      preparedStatement.setInt(2, airlineId)
      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[(Int, BigDecimal)]()
      while (resultSet.next()) {
        val cycle = resultSet.getInt("cycle")
        val score = resultSet.getBigDecimal("score")
        result.append((cycle, score))
      }

      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }

  }

  def loadOlympicsAirlineGoals(eventId: Int): Map[Airline, Int] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AIRLINE_GOAL_TABLE + " WHERE event = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val result = mutable.HashMap[Airline, Int]()
      while (resultSet.next()) {
        val airlineId = resultSet.getInt("airline")
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
        val goal = resultSet.getInt("goal")
        result.put(airline, goal)
      }

      resultSet.close()
      preparedStatement.close()

      result.toMap
    } finally {
      connection.close()
    }

  }

  def loadOlympicsAirlineGoal(eventId: Int, airlineId : Int): Option[Int] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + OLYMPIC_AIRLINE_GOAL_TABLE + " WHERE event = ? AND airline = ?")

      preparedStatement.setInt(1, eventId)
      preparedStatement.setInt(2, airlineId)
      val resultSet = preparedStatement.executeQuery()

      val result =
        if (resultSet.next()) {
          Some(resultSet.getInt("goal"))
        } else {
          None
        }

      resultSet.close()
      preparedStatement.close()

      result
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

  def loadEventById(eventId : Int): Option[Event] =  {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + EVENT_TABLE + " WHERE id = ?")

      preparedStatement.setInt(1, eventId)
      val resultSet = preparedStatement.executeQuery()

      val result : Option[Event] =
        if (resultSet.next()) {
          val eventType = EventType(resultSet.getInt("event_type"))
          eventType match {
            case EventType.OLYMPICS =>
              Some(Olympics(resultSet.getInt("start_cycle"), resultSet.getInt("duration"), resultSet.getInt("id")))
          }
        } else {
          None
        }

      resultSet.close()
      preparedStatement.close()

      result
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