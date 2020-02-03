package controllers

import com.patson.data.{CycleSource, EventSource, LogSource}
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

      val status =
        if (remainingDuration > 0) {
          olympics.currentYear(currentCycle) match {
            case 1 => "Voting for Host City"
            case 2 => "Host City Voted"
            case 3 => "Preparation for the Games"
            case 4 =>
              val week = olympics.duration % Olympics.WEEKS_PER_YEAR
              val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - week
              if (weeksBeforeGames > 0) {
                weeksBeforeGames + " week(s) before the Games"
              } else {
                "Olympic Games in Progress"
              }
            case _ => "Unknown"
          }
        } else {
          "Concluded"
        }
      var result = JsObject(List(
        "id" -> JsNumber(olympics.id),
        "startCycle" -> JsNumber(olympics.startCycle),
        "remainingDuration" -> JsNumber(remainingDuration),
        "status" -> JsString(status)))

      Olympics.getSelectedAirport(olympics.id).foreach { airport =>
        result = result + ("hostCity" -> JsString(airport.city))
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

      var roundsJson = Json.arr()
      for (i <- 0 until votingRounds.length) {

      }

      result = result + ("votingRounds" -> roundsJson)
    }

    Ok(result)
  }

  def getOlympicsAirlineVotes(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj("weight" -> 1) //TODO
    EventSource.loadOlympicsAirlineVotes(eventId, airlineId) match {
      case Some(vote) =>
        var precedenceJson = Json.obj()
        var precedenceIndex = 1
        vote.voteList.foreach { airport =>
          precedenceJson = precedenceJson + (airport.id.toString -> JsNumber(precedenceIndex))
          precedenceIndex += 1
        }
        result = result + ("precedence" -> precedenceJson)
      case None =>
    }

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
        EventSource.saveOlympicsAirlineVote(eventId, OlympicsAirlineVote(request.user, 1, airportPrecedences.toList)) //TODO vote weight
      }
      Ok(precedenceJson)
    }
  }
}
