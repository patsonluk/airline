package controllers

import com.patson.data.{CycleSource, EventSource}
import com.patson.model.{Airline, AllianceRole}
import com.patson.model.event.{EventType, Olympics, OlympicsStatus}
import com.patson.util.AllianceCache
import models.{PendingAction, PendingActionCategory}

import scala.collection.mutable.ListBuffer

object PendingActionUtil {
  def getPendingActions(airline : Airline) : List[PendingAction] = {
    val result = ListBuffer[PendingAction]()
    result.appendAll(getOlympicsPendingActions(airline))
    result.appendAll(getAlliancePendingActions(airline))
    result.toList
  }

  private def getOlympicsPendingActions(airline : Airline) = {
    if (Olympics.getVoteWeight(airline) > 0) {
      EventSource.loadEvents().filter(_.eventType == EventType.OLYMPICS).map(_.asInstanceOf[Olympics]).sortBy(_.startCycle).lastOption match {
        case Some(latestOlympics) =>
          val currentCycle = CycleSource.loadCycle()
          if (latestOlympics.status(currentCycle) == OlympicsStatus.VOTING) {
            if (EventSource.loadOlympicsAirlineVotes(latestOlympics.id, airline.id).isEmpty) {
              List(PendingAction(airline, PendingActionCategory.OLYMPICS_VOTE))
            } else {
              List.empty
            }
          } else {
            List.empty
          }
        case None => List.empty
      }
    } else {
      List.empty
    }

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
