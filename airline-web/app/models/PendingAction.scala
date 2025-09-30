package models

import com.patson.model.Airline

case class PendingAction(airline : Airline, category : PendingActionCategory.Value, params : Map[String, String] = Map.empty)

object PendingActionCategory extends Enumeration {
  type AlertCategory = Value
  val OLYMPICS_VOTE = Value
  val OLYMPICS_PAX_REWARD = Value
  val OLYMPICS_VOTE_REWARD = Value
  val ALLIANCE_PENDING_APPLICATION = Value
}




