package controllers

import com.patson.data.EventSource
import com.patson.model.event._
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._


class EventApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object EventRewardWrite extends Writes[EventReward] {
    def writes(reward : EventReward): JsValue = JsObject(List(
      "description" -> JsString(reward.description),
      "id" -> JsNumber(reward.rewardOption.id)
    ))
  }

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

  def getOlympicsVoteRewardOptions(airlineId : Int, eventId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet
    EventSource.loadPickedRewardOption(eventId, airlineId, RewardCategory.OLYMPICS_VOTE) match {
      case Some(pickedOption) =>
        Ok(Json.obj("title" -> "Reward of voting for the host city", "pickedOption" -> Json.toJson(pickedOption)))
      case None =>
        if (isOlympicsVoteMatch(airlineId, eventId)) {
          Ok(Json.obj("title" -> "Reward of voting for the host city", "options" -> Olympics.voteRewardOptions))
        } else {
          BadRequest("No rewards as the vote does not match!")
        }

    }

  }




  def pickRewardOption(airlineId : Int, eventId: Int,  optionId : Int) = AuthenticatedAirline(airlineId) { request =>
    //make sure that it is found and reward has NOT been claimed yet

    EventReward.fromOptionId(optionId) match {
      case None => BadRequest("Reward option not valid")
      case Some(pickedReward) =>
        val rewardCategory = pickedReward.rewardCategory
        EventSource.loadPickedRewardOption(eventId, airlineId, rewardCategory) match {
          case Some(existingReward) => BadRequest("Reward has already been claimed")
          case None =>
            rewardCategory match {
              case RewardCategory.OLYMPICS_VOTE =>
                if (isOlympicsVoteMatch(airlineId, eventId)) {
                  val validOptions = Olympics.voteRewardOptions
                  if (validOptions.contains(pickedReward)) {
                    pickedReward.apply(EventSource.loadEventById(eventId).get, request.user)
                    EventSource.savePickedRewardOption(eventId, airlineId, pickedReward)
                    Ok(Json.toJson(pickedReward))
                  } else {
                    BadRequest("Reward option not valid")
                  }
                } else {
                  BadRequest("No rewards as the vote does not match!")
                }
            }


        }
    }
  }
}
