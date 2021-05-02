package controllers

import com.patson.data.{CountrySource, CycleSource, EventSource}
import com.patson.model.{Airline, Airport, Country}
import com.patson.model.event._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

import scala.collection.MapView
import scala.collection.mutable.ListBuffer


class OlympicsApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object OlympicsWrites extends Writes[Olympics] {
    def writes(olympics: Olympics): JsValue = {
      val currentCycle = CycleSource.loadCycle()
      val remainingDuration =
        if (currentCycle > olympics.startCycle + olympics.duration) {
          0
        } else {
          olympics.startCycle + olympics.duration - currentCycle
        }

      val currentYear = olympics.currentYear(currentCycle)
      import OlympicsStatus._
      val status = olympics.status(currentCycle) match {
        case VOTING => "Voting for Host City"
        case HOST_CITY_SELECTED => "Host City Voted"
        case PREPARATION => "Preparation for the Games"
        case OLYMPICS_YEAR =>
          val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - olympics.currentWeek(currentCycle)
          s"$weeksBeforeGames week(s) before the Games"
        case IN_PROGRESS =>
          "Olympic Games in Progress"
        case CONCLUDED =>
          "Concluded"
        case _ => "Unknown"
      }
      val votingActive = remainingDuration > 0 && currentYear == 1
      var result = JsObject(List(
        "id" -> JsNumber(olympics.id),
        "startCycle" -> JsNumber(olympics.startCycle),
        "votingActive" -> JsBoolean(votingActive),
        "remainingDuration" -> JsNumber(remainingDuration),
        "status" -> JsString(status)))

      Olympics.getSelectedAirport(olympics.id).foreach { airport =>
        result = result + ("hostCity" -> JsString(airport.city)) + ("hostCountryCode" -> JsString(airport.countryCode))
      }

      if (remainingDuration > 0) {
        result = result + ("currentYear" -> JsNumber(currentYear))
      }

      result
    }
  }

  def getAll() = Action {
    val allOlympics : List[Olympics] = EventSource.loadEvents().filter(_.eventType == EventType.OLYMPICS).map(_.asInstanceOf[Olympics])

    Ok(Json.toJson(allOlympics))
  }

  def getOlympicsDetails(eventId : Int) = Action {

    var result = Json.obj()

    var candidatesJson = Json.arr()

    EventSource.loadOlympicsAffectedAirports(eventId).foreach {
      case(principalAirport, affectedAirports) =>
        val airportJson = Json.toJson(principalAirport).asInstanceOf[JsObject] + ("affectedAirports" -> Json.toJson(affectedAirports))
        candidatesJson = candidatesJson.append(airportJson)
    }

    result = result + ("candidates" -> candidatesJson)

    val votingRounds = EventSource.loadOlympicsVoteRounds(eventId)
    if (!votingRounds.isEmpty) {
      val eliminatedCandidatesByRound = ListBuffer[Airport]()

      for (i <- 1 until votingRounds.length) {
        val previousCandidates = votingRounds(i - 1).votes.keys
        val currentCandidates = votingRounds(i).votes.keys
        eliminatedCandidatesByRound.append(previousCandidates.toSet.removedAll(currentCandidates).iterator.next())
      }

      var votingRoundsJson = Json.arr()


      votingRounds.foreach { round =>
        var votesJson = Json.obj()
        round.votes.foreach {
          case (airport, vote) => votesJson = votesJson + (airport.id.toString -> JsNumber(vote))
        }

        var votingRoundJson = Json.obj("votes" -> votesJson)

        val eliminatedIndex = round.round - 1
        if (eliminatedIndex < eliminatedCandidatesByRound.size) {
          val eliminatedAirport = eliminatedCandidatesByRound(eliminatedIndex)
          votingRoundJson = votingRoundJson +  ("eliminatedAirport" -> Json.toJson(eliminatedAirport))
        }

        votingRoundsJson = votingRoundsJson.append(votingRoundJson)
      }

      result = result + ("votingRounds" -> votingRoundsJson)
      Olympics.getSelectedAirport(eventId).foreach { selectedAirport =>
        result = result + ("selectedAirport", Json.toJson(selectedAirport))
      }
    }
    EventSource.loadEventById(eventId) match {
      case Some(olympics: Olympics) =>
        val currentCycle = CycleSource.loadCycle()
        if (!olympics.isActive(currentCycle)) { //only load total pax after the olympics is over
          val stats = EventSource.loadOlympicsCountryStats(eventId)
          val totalPassengers : Int = stats.view.values.map {
            case statsOfAnCountry => statsOfAnCountry.map(_.transported).sum
          }.sum
          result = result + ("totalPassengers" -> JsNumber(totalPassengers))
        }
      case _ =>
    }

    Ok(result)
  }

  def getOlympicsAirlineVotes(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj("weight" -> Olympics.getVoteWeight(request.user))
    EventSource.loadOlympicsAirlineVotes(eventId, airlineId) match {
      case Some(vote) =>
        var precedenceJson = Json.obj()
        var precedenceIndex = 1
        vote.voteList.foreach { airport =>
          precedenceJson = precedenceJson + (airport.id.toString -> JsNumber(precedenceIndex))
          precedenceIndex += 1
        }
        result = result + ("precedence" -> precedenceJson)
        if (!vote.voteList.isEmpty) {
          result = result + ("votedAirport" -> Json.toJson(vote.voteList(0)))
        }
        EventSource.loadPickedRewardOption(eventId, airlineId, RewardCategory.OLYMPICS_VOTE).foreach { claimedReward =>
          result = result + ("claimedVoteReward" -> Json.toJson(claimedReward))
        }

      case None =>
    }

    Ok(result)
  }

  val PASSENGER_AWARD_CLAIM_DURATION = 52 //52 weeks
  def getOlympicsAirlinePassengerDetails(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj()

    EventSource.loadOlympicsAirlineGoal(eventId, airlineId) match {
      case Some(goal) =>
        result = result + ("goal" -> JsNumber(goal))
      case None =>
    }

    val currentCycle = CycleSource.loadCycle ()
    EventSource.loadEventById(eventId) match {
      case Some(olympics : Olympics) =>

        val stats: Map[Int, BigDecimal] = EventSource.loadOlympicsAirlineStats (eventId, airlineId).toMap

        if (olympics.isActive(currentCycle) && olympics.currentYear(currentCycle) == 4) {
          val previousCycle = currentCycle - 1
          stats.get(previousCycle).foreach {
            previousCycleScore =>
              result = result + ("previousCycleScore" -> JsNumber(previousCycleScore))
          }
          val preciousCycleGoal = Olympics.getGoalByCycle(eventId, airlineId, previousCycle)
          preciousCycleGoal.foreach {
            previousCycleGoal =>
              result = result + ("previousCycleGoal" -> JsNumber(previousCycleGoal))
          }
          val currentCycleGoal = Olympics.getGoalByCycle(eventId, airlineId, currentCycle)
          currentCycleGoal.foreach {
            currentCycleGoal =>
              result = result + ("currentCycleGoal" -> JsNumber(currentCycleGoal))
          }
        }

        val totalScore = stats.view.values.sum
        result = result + ("totalScore" -> JsNumber (totalScore) )

        EventSource.loadPickedRewardOption(eventId, airlineId, RewardCategory.OLYMPICS_PASSENGER) match {
          case Some(claimedReward) =>
            result = result + ("claimedPassengerReward" -> Json.toJson(claimedReward))
          case None => //then see if a reward can be claimed
            hasUnclaimedPassengerAward(eventId, airlineId, currentCycle) match {
              case Right(_) =>
                result = result + ("unclaimedPassengerReward" -> JsBoolean(true))
              case Left(rejection) =>
            }
        }

      case _ =>
    }

    Ok(result)
  }

  def hasUnclaimedVoteAward(eventId : Int, airlineId : Int, currentCycle : Int) : Either[String, Unit] = {
    EventSource.loadEventById(eventId) match {
      case Some(olympics : Olympics) =>
        EventSource.loadPickedRewardOption(eventId, airlineId, RewardCategory.OLYMPICS_VOTE) match {
          case Some(existingReward) => Left("Reward has already been claimed")
          case None =>
            if (olympics.isActive(currentCycle)) {
              if (isOlympicsVoteMatch(airlineId, eventId)) {
                Right(())
              } else {
                Left("vote does not match")
              }
            } else {
              Left("no longer active")
            }
        }
      case _ =>
        Left("event not found")

    }
  }

  def isOlympicsVoteMatch(airlineId: Int, eventId: Int): Boolean = {
    EventSource.loadOlympicsAirlineVotes(eventId, airlineId) match {
      case Some(vote) =>
        Olympics.getSelectedAirport(eventId) match {
          case Some(selectedAirport) =>
            if (!vote.voteList.isEmpty) {
              //vote.voteList(0).id == selectedAirport.id
              true //as far as a vote is casted it's considered okay now
            } else {
              false
            }
          case None => false
        }
      case None => false
    }
  }

  def hasUnclaimedPassengerAward(eventId : Int, airlineId : Int, currentCycle : Int) : Either[String, Unit] = {
    EventSource.loadEventById(eventId) match {
      case Some(olympics : Olympics) =>
        val stats: List[(Int, BigDecimal)] = EventSource.loadOlympicsAirlineStats(eventId, airlineId)
        val totalScore = stats.map(_._2).sum
        EventSource.loadOlympicsAirlineGoal(eventId, airlineId) match {
          case Some(goal) =>
            if (totalScore >= goal) {
              if (olympics.startCycle + olympics.duration + PASSENGER_AWARD_CLAIM_DURATION < currentCycle) {
                Left("Cannot redeem reward, it has already been expired")
              } else if (olympics.isActive(currentCycle)) {
                Left("Cannot yet claim reward, olympics still active")
              } else {
                EventSource.loadPickedRewardOption(eventId, airlineId, RewardCategory.OLYMPICS_PASSENGER) match {
                  case Some(pickedReward) =>
                    Left(s"Already picked reward $pickedReward")
                  case None =>
                    Right(())
                }
              }
            } else {
              Left(s"Did not reach the goal")
            }
          case _ =>
            Left(s"No goal set for airline $airlineId")
        }
      case _ => Left("event not found")
    }

  }

  def putOlympicsAirlineVotes(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    val precedenceJson = request.body.asInstanceOf[AnyContentAsJson].json.asInstanceOf[JsObject]
    val airportIdPrecedences = precedenceJson.fields.sortBy(_._2.as[Int]).map(_._1.toInt)

    val candidateAirportIds = EventSource.loadOlympicsCandidates(eventId).map(_.id)
    //validates
    if (!airportIdPrecedences.filter(id => !candidateAirportIds.contains(id)).isEmpty) {
      BadRequest("Voted airport that is not a valid candidate")
    } else {
      val airportPrecedences = airportIdPrecedences.map(airportId => AirportCache.getAirport(airportId).get)
      if (airportPrecedences.isEmpty) {
        EventSource.deleteOlympicsAirlineVote(eventId, airlineId)
      } else {
        EventSource.saveOlympicsAirlineVote(eventId, OlympicsAirlineVote(request.user, airportPrecedences.toList))
      }
      Ok(precedenceJson)
    }
  }

  val TOP_OLYMPICS_RANK_COUNT = 10


  object PassengerRankingWrites extends Writes[List[(String, Int, Double)]] {
    val countryMap : Map[String, Country] = CountrySource.loadAllCountries().map(country => (country.countryCode, country)).toMap
    override def writes(entries: List[(String, Int, Double)]): JsValue = {
      var result = Json.arr()
      entries.foreach { entry =>
        val countryCode = entry._1
        val country = countryMap.getOrElse(countryCode, Country.fromCode(countryCode))
        result = result.append(Json.obj(
          "countryCode" -> JsString(countryCode),
          "countryName" -> country.name,
          "count" -> JsNumber(entry._2),
          "percentage" -> JsNumber(entry._3)))
      }
      result
    }
  }



  def getOlympicsRanking(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj()

    case class PassengerStats(transported : Int, missed : Int)
    //top 10 lowest/highest fulfillment
    //Map[countryCode, (transported, missed))
    val countryStats: MapView[String, PassengerStats] = EventSource.loadOlympicsCountryStats(eventId).view.mapValues(entries => {
      val transportedSum = entries.map(_.transported).sum
      val totalSum = entries.map(_.total).sum
      PassengerStats(transportedSum, totalSum - transportedSum)
    })


    //List[(countryCode, transported, transportedPercentage)]
    val topTransportedCountry: List[(String, Int, Double)] = countryStats.toList.sortBy(_._2.transported).reverse.take(TOP_OLYMPICS_RANK_COUNT).map {
      case (countryCode, PassengerStats(transported, missed)) => (countryCode, transported, (transported.toDouble / (transported + missed) * 100))
    }

    result = result + ("topTransportedCountries" -> Json.toJson(topTransportedCountry)(PassengerRankingWrites))

    //List[(countryCode, missed, transportedPercentage)]
    val topMissedCountry: List[(String, Int, Double)] = countryStats.toList.sortBy(entry => entry._2.missed).reverse.take(TOP_OLYMPICS_RANK_COUNT).map {
      case (countryCode, PassengerStats(transported, missed)) => (countryCode, missed, (missed.toDouble / (transported + missed) * 100))
    }

    result = result + ("topMissedCountries" -> Json.toJson(topMissedCountry)(PassengerRankingWrites))

    val airlineStats : MapView[Airline, BigDecimal] = EventSource.loadOlympicsAirlineStats(eventId).view.mapValues { entries => //from weekly to accumulative
      entries.map(_._2).sum
    }
    val rankedAirlineStats : List[(Airline, BigDecimal)] = airlineStats.toList.sortBy(_._2).reverse

    val topAirlines = rankedAirlineStats.take(TOP_OLYMPICS_RANK_COUNT)

    var topAirlinesJson = Json.arr()
    topAirlines.foreach {
      case (airline, score) =>
        topAirlinesJson = topAirlinesJson.append(Json.obj("airlineId" -> airline.id, "airlineName" -> airline.name, "score" -> score))
    }
    result = result + ("topAirlines" -> topAirlinesJson)

    var rank = 0
    rankedAirlineStats.find { entry =>
      rank += 1
      entry._1.id == airlineId
    } match {
      case Some(matchedEntry) =>
        result = result + ("currentAirline" -> Json.obj("airlineId" -> matchedEntry._1.id, "airlineName" -> matchedEntry._1.name, "score" -> matchedEntry._2, "rank" -> rank))
      case None =>
    }
    Ok(result)
  }

  def getOlympicsVoteRewardOptions(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    hasUnclaimedVoteAward(eventId, airlineId, CycleSource.loadCycle()) match {
      case Left(rejection) =>
        BadRequest(rejection)
      case Right(_) =>
        Ok(Json.obj("title" -> "Reward of voting for the host city", "options" -> Olympics.voteRewardOptions))
    }
  }

  def getOlympicsPassengerRewardOptions(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    hasUnclaimedPassengerAward(eventId, airlineId, CycleSource.loadCycle()) match {
      case Right(_) =>
        Ok(Json.obj("title" -> "Reward of fulfilling passenger goal", "options" -> Olympics.passengerRewardOptions))
      case Left(rejection) => BadRequest(rejection)
    }
  }

  def pickOlympicsRewardOption(airlineId : Int, eventId: Int, categoryId : Int, optionId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet

    EventReward.fromId(categoryId, optionId) match {
      case None => BadRequest("Reward option not valid")
      case Some(pickedReward) =>
        val rewardCategory = pickedReward.rewardCategory
        val currentCycle = CycleSource.loadCycle()
        val (validationResult, validOptions) = rewardCategory match {
          case RewardCategory.OLYMPICS_VOTE =>
            (hasUnclaimedVoteAward(eventId, airlineId, currentCycle), Olympics.voteRewardOptions)
          case RewardCategory.OLYMPICS_PASSENGER =>
            (hasUnclaimedPassengerAward(eventId, airlineId, currentCycle), Olympics.passengerRewardOptions)
        }
        validationResult match {
          case Left(rejection) =>
            BadRequest(rejection)
          case Right(_) =>
            if (validOptions.contains(pickedReward)) {
              pickedReward.apply(EventSource.loadEventById(eventId).get, request.user)
              EventSource.savePickedRewardOption(eventId, airlineId, pickedReward)
              Ok(Json.toJson(pickedReward))
            } else {
              BadRequest("Reward option not valid")
            }
        }
    }
  }
}


