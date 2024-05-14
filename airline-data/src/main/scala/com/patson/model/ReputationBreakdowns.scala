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

  val MILESTONES = new AbstractReputationType {
    override val label = "Milestones"
  }

  val AIRPORT_LOYALIST_RANKING = new AbstractReputationType {
    override val label = "Airports"
  }

  val TOURISTS = new AbstractReputationType {
    override val label = "Tourists ticketed"
  }

  val ELITES = new AbstractReputationType {
    override val label = "Elites ticketed"
  }

  val STOCK_PRICE = new AbstractReputationType {
    override val label = "Stock Price"
  }

  val LEADERBOARD_BONUS = new AbstractReputationType {
    override val label = "Leaderboard Bonus"
  }

  val ALLIANCE_BONUS = new AbstractReputationType {
    override val label = "Alliance Bonus"
  }
}

