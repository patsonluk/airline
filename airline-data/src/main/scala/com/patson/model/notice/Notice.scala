package com.patson.model.notice

import com.patson.AirlineSimulation
import com.patson.model.Airline

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
  val LEVEL_UP, LOYALIST, GAME_OVER = Value
}

object Notice {
  def fromCategoryAndId(category : NoticeCategory.Value, id : String) = {
    import NoticeCategory._
    category match {
      case LEVEL_UP => LevelNotice(id.toInt)
      case LOYALIST => LoyalistNotice(id.toInt)
      case GAME_OVER => GameOverNotice(id.toInt)
    }
  }

}