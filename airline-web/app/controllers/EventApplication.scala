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


class EventApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {


  def isOlympicsVoteMatch(airlineId: Int, eventId: Int): Boolean = {
    EventSource.loadOlympicsAirlineVotes(eventId, airlineId) match {
      case Some(vote) =>
        Olympics.getSelectedAirport(eventId) match {
          case Some(selectedAirport) =>
            if (!vote.voteList.isEmpty) {
              vote.voteList(0).id == selectedAirport.id
            } else {
              false
            }
          case None => false
        }
      case None => false

    }
  }

  def getRewardOptions(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    val airline = request.user
    EventSource.loadEventById(eventId) match {
      case Some(event) =>
        event match {
          case EventType.OLYMPICS =>
            if (isOlympicsVoteMatch(airlineId, eventId)) {

            } else {
              BadRequest("No rewards as the vote does not match!")
            }
        }
      case None => NotFound
    }
  }




  def pickRewardOption(airlineId : Int, eventId: Int, optionId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    val airline = request.user
    ChristmasSource.loadSantaClausInfoByAirline(airline.id) match {
      case Some(entry) => {
        if (!entry.found || entry.pickedAward.isDefined) {
          BadRequest("Either not found yet or reward has already been redeemed!")
        } else {
          val pickedAward = SantaClausAward.getRewardByType(entry, SantaClausAwardType(optionId))
          pickedAward.apply

          ChristmasSource.updateSantaClausInfo(entry.copy(pickedAward = Some(pickedAward.getType)))

          Ok(Json.obj())
        }
      }
      case None => NotFound
    }
  }
}
