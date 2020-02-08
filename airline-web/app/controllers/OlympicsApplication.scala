package controllers

import com.patson.data.{CycleSource, EventSource}
import com.patson.model.Airport
import com.patson.model.event._
import com.patson.util.AirportCache
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._

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
      val status =
        if (remainingDuration > 0) {
          currentYear match {
            case 1 => "Voting for Host City"
            case 2 => "Host City Voted"
            case 3 => "Preparation for the Games"
            case 4 =>
              val week = olympics.duration % Olympics.WEEKS_PER_YEAR
              val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - week
              if (weeksBeforeGames > 0) {
                s"$weeksBeforeGames week(s) before the Games"
              } else {
                "Olympic Games in Progress"
              }
            case _ => "Unknown"
          }
        } else {
          "Concluded"
        }
      val votingActive = remainingDuration > 0 && currentYear == 1
      var result = JsObject(List(
        "id" -> JsNumber(olympics.id),
        "startCycle" -> JsNumber(olympics.startCycle),
        "votingActive" -> JsBoolean(votingActive),
        "remainingDuration" -> JsNumber(remainingDuration),
        "status" -> JsString(status)))

      Olympics.getSelectedAirport(olympics.id).foreach { airport =>
        result = result + ("hostCity" -> JsString(airport.city))
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
      case None =>
    }

    Ok(result)
  }

  def getOlympicsAirlinePassengerDetails(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj()

    EventSource.loadOlympicsAirlineGoal(eventId, airlineId) match {
      case Some(goal) =>
        result = result + ("goal" -> JsNumber(goal))
      case None =>
    }

    val stats : Map[Int, BigDecimal] = EventSource.loadOlympicsAirlineStats(eventId, airlineId).toMap
    val previousCycle = CycleSource.loadCycle() - 1
    stats.get(previousCycle).foreach { previousCycleScore =>
      result = result + ("previousCycleScore" -> JsNumber(previousCycleScore))
    }

    val totalScore = stats.view.values.sum
    result = result + ("totalScore" -> JsNumber(totalScore))

    Ok(result)
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
}
