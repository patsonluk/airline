package controllers

import com.patson.data.{CycleSource, EventSource}
import com.patson.model.Airline
import com.patson.model.event.{EventType, Olympics, OlympicsStatus}
import models.{PendingAction, PendingActionCategory}

import scala.collection.mutable.ListBuffer

object PendingActionUtil {
  def getPendingActions(airline : Airline) : List[PendingAction] = {
    val result = ListBuffer[PendingAction]()
    result.appendAll(getOlympicsPendingActions(airline))
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


}
