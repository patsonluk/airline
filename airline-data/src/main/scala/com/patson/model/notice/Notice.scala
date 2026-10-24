package com.patson.model.notice

import com.patson.AirlineSimulation
import com.patson.model.{Airline, HighSpeedRail}

abstract class Notice(val category: NoticeCategory.Value) {
  val id : String
}
case class LevelNotice(level : Int) extends Notice(NoticeCategory.LEVEL_UP) {
  override val id = level.toString
}
case class LoyalistNotice(level : Int) extends Notice(NoticeCategory.LOYALIST) {
  override val id = level.toString
}

abstract class TrackingNotice(override val category: NoticeCategory.Value, trackingId : Int) extends Notice(category){
  override val id = trackingId.toString
  val descriptions : (Airline => String)
}

case class GameOverNotice(var trackingId : Int = 0) extends TrackingNotice(NoticeCategory.GAME_OVER, trackingId) {
  override val descriptions : Airline => String = airline => {
    s"Unfortunately ${airline.name} is forced into bankruptcy due to balance below $$${AirlineSimulation.BANKRUPTCY_CASH_THRESHOLD / 1_000_000}M and negative flight profit!"
  }
}

/**
  * Sent to airlines with HQ/base at either end of a HSR once its construction completes. The description is generated on creation and persisted,
  * so it remains valid even if the HSR is removed afterward
  */
case class HighSpeedRailNotice(description : String, var trackingId : Int = 0) extends TrackingNotice(NoticeCategory.HIGH_SPEED_RAIL, trackingId) {
  override val descriptions : Airline => String = _ => description
}

object HighSpeedRailNotice {
  def apply(rail : HighSpeedRail) : HighSpeedRailNotice = {
    HighSpeedRailNotice(s"High speed rail between ${rail.from.displayText} and ${rail.to.displayText}, funded by ${rail.airline.name}, is now open!")
  }
}


object LoyalistNotice {
  val getLevel = (loyalist : Long) => {
    Math.log10(loyalist).toInt
  }

  val getThreshold = (level : Int) => {
    Math.pow(10, level)
  }
}

object NoticeCategory extends Enumeration {
  type NoticeCategory = Value
  val LEVEL_UP, LOYALIST, GAME_OVER, HIGH_SPEED_RAIL = Value
}

object Notice {
  /**
    * @param description only used by tracking notice that carries its own description
    */
  def fromCategoryAndId(category : NoticeCategory.Value, id : String, description : String = "") = {
    import NoticeCategory._
    category match {
      case LEVEL_UP => LevelNotice(id.toInt)
      case LOYALIST => LoyalistNotice(id.toInt)
      case GAME_OVER => GameOverNotice(id.toInt)
      case HIGH_SPEED_RAIL => HighSpeedRailNotice(description, id.toInt)
    }
  }

}