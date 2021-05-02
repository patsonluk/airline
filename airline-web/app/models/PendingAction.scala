package models

import com.patson.model.Airline

case class PendingAction(airline : Airline, category : PendingActionCategory.Value)

object PendingActionCategory extends Enumeration {
  type AlertCategory = Value
  val OLYMPICS_VOTE = Value
}




