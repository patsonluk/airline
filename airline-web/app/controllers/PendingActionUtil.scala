package controllers

import com.patson.data.{CycleSource, EventSource}
import com.patson.model.{Airline, AllianceRole}
import com.patson.model.event.{EventType, Olympics, OlympicsStatus}
import com.patson.util.AllianceCache
import models.{PendingAction, PendingActionCategory}

import scala.collection.mutable.ListBuffer

object PendingActionUtil {
  def getPendingActions(airline : Airline) : List[PendingAction] = {
    val result = ListBuffer[PendingAction]() //sorted by precedence, first one has higher precedence for now
    result.appendAll(getOlympicsPendingActions(airline))
    result.appendAll(getAlliancePendingActions(airline))
    result.toList
  }

  private def getOlympicsPendingActions(airline : Airline): List[PendingAction] = {
    //check if current airline has pending pax reward
    val currentCycle = CycleSource.loadCycle()
    val pendingActions = ListBuffer[PendingAction]()

    val previousOlympics: List[Olympics] = EventSource.loadLatestOlympics(2)
    previousOlympics.headOption.foreach { latestOlympics =>
      //pax reward has the highest precedence
      previousOlympics.lift(1).foreach { completedOlympics =>
        Olympics.hasUnclaimedPassengerAward(completedOlympics.id, airline.id, currentCycle) match {
          case Right(duration) => pendingActions.append(PendingAction(airline, PendingActionCategory.OLYMPICS_PAX_REWARD, Map("duration" -> duration.toString)))
          case _ =>
        }
      }

      Olympics.hasUnclaimedVoteAward(latestOlympics.id, airline.id, currentCycle) match {
        case Right(duration) => pendingActions.append(PendingAction(airline, PendingActionCategory.OLYMPICS_VOTE_REWARD, Map("duration" -> duration.toString)))
        case _ =>
      }

      //check if current airline has voted
      if (Olympics.getVoteWeight(airline) > 0) {
        if (latestOlympics.status(currentCycle) == OlympicsStatus.VOTING) {
          if (EventSource.loadOlympicsAirlineVotes(latestOlympics.id, airline.id).isEmpty) {
            pendingActions.append(PendingAction(airline, PendingActionCategory.OLYMPICS_VOTE))
          }
        }
      }
    }
    pendingActions.toList
  }

  private def getAlliancePendingActions(airline : Airline) = {
    val actions = ListBuffer[PendingAction]()
    airline.getAllianceId().foreach {
      allianceId => AllianceCache.getAlliance(allianceId).foreach {
        alliance => {
          val isAdminOption = alliance.members.find(_.airline.id == airline.id).map(thisAirlineMember => AllianceRole.isAdmin(thisAirlineMember.role))
          if (isAdminOption.getOrElse(false)) {
            if (alliance.members.find(_.role == AllianceRole.APPLICANT).isDefined) {
              actions.append(PendingAction(airline, PendingActionCategory.ALLIANCE_PENDING_APPLICATION))
            }
          }
        }
      }
    }
    actions.toList
  }


}
