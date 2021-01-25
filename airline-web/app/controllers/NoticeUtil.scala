package controllers

import com.patson.data.NoticeSource
import com.patson.model.Airline
import com.patson.model.notice.{NoticeCategory, _}

import scala.collection.mutable.ListBuffer

object NoticeUtil {
  def getNotices(airline : Airline) : List[Notice] = {
    val completedNoticesByCategory : Map[NoticeCategory.Value, List[Notice]] = NoticeSource.loadCompletedNoticesByAirline(airline.id).groupBy(_.category)

    val result = ListBuffer[Notice]()
    //check level up
    val notifiedLevel = completedNoticesByCategory.get(NoticeCategory.LEVEL_UP) match {
      case Some(completedLevelUpNotices) => completedLevelUpNotices.map(_.asInstanceOf[LevelNotice].level).max
      case None => -1
    }
    if (airline.airlineGrade.value > notifiedLevel) { //level up!
      result.append(LevelNotice(airline.airlineGrade.value))
    }

    result.toList
  }
}
