package com.patson.model.notice

abstract class Notice(val category: NoticeCategory.Value) {
  val id : String
}
case class LevelNotice(level : Int) extends Notice(NoticeCategory.LEVEL_UP) {
  override val id = level.toString
}
case class LoyalistNotice(level : Int) extends Notice(NoticeCategory.LOYALIST) {
  override val id = level.toString
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
  val LEVEL_UP, LOYALIST = Value
}

object Notice {
  def fromCategoryAndId(category : NoticeCategory.Value, id : String) = {
    import NoticeCategory._
    category match {
      case LEVEL_UP => LevelNotice(id.toInt)
      case LOYALIST => LoyalistNotice(id.toInt)
    }
  }

}