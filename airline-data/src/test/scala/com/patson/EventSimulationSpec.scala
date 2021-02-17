package com.patson

import com.patson.model.event.{OlympicsAirlineVote, OlympicsAirlineVoteWithWeight, OlympicsVoteRound}
import com.patson.model.{PassengerType, _}
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable.ListBuffer

class EventSimulationSpec extends WordSpecLike with Matchers {
  val DEFAULT_RELATIONSHIP = 0
  val countries = List(Country.fromCode("A"), Country.fromCode("B"), Country.fromCode("C"), Country.fromCode("D"), Country.fromCode("E"), Country.fromCode("F"))

  val airports = ListBuffer[Airport]() //60 airports
  var airportId = 1
  countries.foreach { country =>
    val countryCode = country.countryCode
    for (i <- 1 to 10) {
      val airport = Airport("", "", "", latitude = 0, longitude = 0, countryCode = countryCode, "", "", size = i, power = 1000, population = EventSimulation.CANDIDATE_MIN_POPULATION, 0, id = airportId)
      airports.append(airport)
      airportId += 1

    }
  }

  "olympicsSimulation".must {
    "select 6 random candidates with different country".in {
       val airports = this.airports.toList
       val candidates = EventSimulation.selectCandidates(airports)
       
       assert(candidates.length == EventSimulation.MAX_CANDIDATES_COUNT)
       val countries = candidates.map(_.countryCode).toSet
       assert(countries.size == EventSimulation.MAX_CANDIDATES_COUNT)

       candidates.foreach { airport =>
         assert(airport.size >= EventSimulation.CANDIDATE_MIN_SIZE)
       }
    }
    "tally the votes correctly".in {
      val airports = this.airports.toList
      val candidates = EventSimulation.selectCandidates(airports)

      //4 airlines votes (from highest precedence to lowest)
      //Airline 1 : 0 1 2 3 4 5
      //Airline 2 : 5 4 3 2 1 0
      //Airline 3 : 1 2 3 4 5 0
      //Airline 4 : 2 1 4 3 5 0
      val airline1 = Airline.fromId(1)
      val airline2 = Airline.fromId(2)
      val airline3 = Airline.fromId(3)
      val airline4 = Airline.fromId(4)
      var airline1Vote = OlympicsAirlineVoteWithWeight(airline1, 1, List(candidates(0), candidates(1), candidates(2), candidates(3), candidates(4), candidates(5)))
      var airline2Vote = OlympicsAirlineVoteWithWeight(airline2, 1, List(candidates(5), candidates(4), candidates(3), candidates(2), candidates(1), candidates(0)))
      var airline3Vote = OlympicsAirlineVoteWithWeight(airline3, 1, List(candidates(1), candidates(2), candidates(3), candidates(4), candidates(5), candidates(0)))
      var airline4Vote = OlympicsAirlineVoteWithWeight(airline4, 1, List(candidates(2), candidates(1), candidates(4), candidates(3), candidates(5), candidates(0)))

      var result = EventSimulation.simulateOlympicsVoteRounds(candidates, Map(airline1 -> airline1Vote, airline2 -> airline2Vote, airline3 -> airline3Vote, airline4 -> airline4Vote))
      //first round: 0, 5, 1, 2 => 3 is dropped  (0, 1, 2, 4, 5)
      //second round: 0, 5, 1, 2 => 4 is dropped (0, 1, 2, 5)
      //third round: 0, 5, 1, 2 => 0 is dropped (1, 2, 5)
      //forth round: 1, 5, 1, 2 => 2 is dropped (1, 5)
      //firth round: 1, 5, 1, 1 => 0 is dropped, 1 is winner
      assert(result.length == 6)
      assert(result(0) == OlympicsVoteRound(1, Map(candidates(0) -> 1, candidates(5) -> 1, candidates(1) -> 1, candidates(2) -> 1, candidates(3) -> 0, candidates(4) -> 0)))
      assert(result(1) == OlympicsVoteRound(2, Map(candidates(0) -> 1, candidates(5) -> 1, candidates(1) -> 1, candidates(2) -> 1, candidates(4) -> 0)))
      assert(result(2) == OlympicsVoteRound(3, Map(candidates(0) -> 1, candidates(5) -> 1, candidates(1) -> 1, candidates(2) -> 1)))
      assert(result(3) == OlympicsVoteRound(4, Map(candidates(1) -> 2, candidates(5) -> 1, candidates(2) -> 1)))
      assert(result(4) == OlympicsVoteRound(5, Map(candidates(1) -> 3, candidates(5) -> 1)))
      assert(result(5) == OlympicsVoteRound(6, Map(candidates(1) -> 4)))

      airline1Vote = OlympicsAirlineVoteWithWeight(airline1, 1, List(candidates(0), candidates(1), candidates(2), candidates(3), candidates(4), candidates(5)))
      airline2Vote = OlympicsAirlineVoteWithWeight(airline2, 2, List(candidates(5), candidates(4), candidates(3), candidates(2), candidates(1), candidates(0))) //2 VOTES
      airline3Vote = OlympicsAirlineVoteWithWeight(airline3, 1, List(candidates(0), candidates(1), candidates(2), candidates(3), candidates(4), candidates(5)))
      airline4Vote = OlympicsAirlineVoteWithWeight(airline4, 1, List(candidates(1), candidates(5), candidates(4), candidates(3), candidates(2), candidates(0)))

      result = EventSimulation.simulateOlympicsVoteRounds(candidates, Map(airline1 -> airline1Vote, airline2 -> airline2Vote, airline3 -> airline3Vote, airline4 -> airline4Vote))
      //first round: 0, 5, 5, 0, 1 => 2 is dropped (0, 1, 3, 4, 5)
      //second round: 0, 5, 5, 0, 1 => 3 is dropped (0, 1, 4, 5)
      //third round: 0, 5, 5, 0, 1 => 4 is dropped (0, 1, 5)
      //forth round: 0, 5, 5, 0, 1 => 1 is dropped (0, 5)
      //fifth round: 0, 5, 5, 0, 5 => 0 is dropped, 5 is winner
      assert(result.length == 6)
      assert(result(0) == OlympicsVoteRound(1, Map(candidates(0) -> 2, candidates(5) -> 2, candidates(1) -> 1, candidates(2) -> 0, candidates(3) -> 0, candidates(4) -> 0)))
      assert(result(1) == OlympicsVoteRound(2, Map(candidates(0) -> 2, candidates(5) -> 2, candidates(1) -> 1, candidates(3) -> 0, candidates(4) -> 0)))
      assert(result(2) == OlympicsVoteRound(3, Map(candidates(0) -> 2, candidates(5) -> 2, candidates(1) -> 1, candidates(4) -> 0)))
      assert(result(3) == OlympicsVoteRound(4, Map(candidates(0) -> 2, candidates(5) -> 2, candidates(1) -> 1)))
      assert(result(4) == OlympicsVoteRound(5, Map(candidates(0) -> 2, candidates(5) -> 3)))
      assert(result(5) == OlympicsVoteRound(6, Map(candidates(5) -> 5)))
    }
    "do not error out if no votes".in {
      val airports = this.airports.toList
      val candidates = EventSimulation.selectCandidates(airports)


      val result = EventSimulation.simulateOlympicsVoteRounds(candidates, Map())
      assert(result.length == 6)
      assert(result(0) == OlympicsVoteRound(1, Map(candidates(0) -> 0, candidates(1) -> 0, candidates(2) -> 0, candidates(3) -> 0, candidates(4) -> 0, candidates(5) -> 0)))
      assert(result(1) == OlympicsVoteRound(2, Map(candidates(1) -> 0, candidates(2) -> 0, candidates(3) -> 0, candidates(4) -> 0, candidates(5) -> 0)))
      assert(result(2) == OlympicsVoteRound(3, Map(candidates(2) -> 0, candidates(3) -> 0, candidates(4) -> 0, candidates(5) -> 0)))
      assert(result(3) == OlympicsVoteRound(4, Map(candidates(3) -> 0, candidates(4) -> 0, candidates(5) -> 0)))
      assert(result(4) == OlympicsVoteRound(5, Map(candidates(4) -> 0, candidates(5) -> 0)))
      assert(result(5) == OlympicsVoteRound(6, Map(candidates(5) -> 0)))
    }
  }
}
