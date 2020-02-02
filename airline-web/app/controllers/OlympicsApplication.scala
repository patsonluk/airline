package controllers

import com.patson.data.{CycleSource, EventSource, LogSource}
import com.patson.model.Airport
import com.patson.model.event._
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
  
  
  val LOG_RANGE = 100 //load 100 weeks worth of logs
  
  
  def getAll() = Action {
    val allOlympics : List[Olympics] = EventSource.loadEvents().filter(_.eventType == EventType.OLYMPICS).map(_.asInstanceOf[Olympics])

    Ok(Json.toJson(allOlympics))
  }

  def getOlympicsDetails(eventId : Int) = Action {
    val candidates = EventSource.loadOlympicsCandidates(eventId)

    var result = Json.obj()
    result = result + ("candidates" -> Json.toJson(candidates))

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
  
  

  
}
