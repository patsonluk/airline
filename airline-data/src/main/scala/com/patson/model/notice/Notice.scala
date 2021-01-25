package com.patson.model.notice

abstract class Notice(val category: NoticeCategory.Value) {
  val id : String
}
case class LevelNotice(level : Int) extends Notice(NoticeCategory.LEVEL_UP) {
  override val id = level.toString
}

object NoticeCategory extends Enumeration {
  type NoticeCategory = Value
  val LEVEL_UP = Value
}

object Notice {
  def fromCategoryAndId(category : NoticeCategory.Value, id : String) = {
    import NoticeCategory._
    category match {
      case LEVEL_UP => LevelNotice(id.toInt)
    }
  }

}