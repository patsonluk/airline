package com.patson.model

case class ReputationBreakdowns(breakdowns : List[ReputationBreakdown]) {
  val total = breakdowns.map(_.value).sum
}

case class ReputationBreakdown(reputationType : ReputationType.Value, value : Double)


object ReputationType extends Enumeration {
  type ReputationType = Value
  implicit def valueToReputationType(x: Value) = x.asInstanceOf[AbstractReputationType]

  abstract class AbstractReputationType() extends super.Val {
    val label: String
  }

  val FLIGHT_PASSENGERS = new AbstractReputationType {
    override val label = "Passengers carried"
  }

  val AIRPORT_LOYALIST_RANKING = new AbstractReputationType {
    override val label = "Airport Loyalist Ranking"
  }

  val ALLIANCE_BONUS = new AbstractReputationType {
    override val label = "Alliance Bonus"
  }
}

