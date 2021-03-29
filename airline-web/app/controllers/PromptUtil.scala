package controllers

import com.patson.data.{AirportSource, LoyalistSource, NoticeSource, TutorialSource}
import com.patson.model.Airline
import com.patson.model.notice.{NoticeCategory, _}
import com.patson.model.tutorial.Tutorial

import scala.collection.mutable.ListBuffer

case class Prompts(notices : ListBuffer[AirlineNotice], tutorials : ListBuffer[AirlineTutorial]) {
  def append(that : Prompts) = {
    this.notices.appendAll(that.notices)
    this.tutorials.appendAll(that.tutorials)
  }
}
case class AirlineTutorial(airline : Airline, tutorial : Tutorial)

object PromptUtil {

  def getPrompts(airline : Airline) : Prompts = {
    val completedNoticesByCategory : Map[NoticeCategory.Value, List[Notice]] = NoticeSource.loadCompletedNoticesByAirline(airline.id).groupBy(_.category)
    val completedTutorials = TutorialSource.loadCompletedTutorialsByAirline(airline.id)


    val result = Prompts(ListBuffer(), ListBuffer())
    result.append(getLevelUpPrompts(airline, completedNoticesByCategory.get(NoticeCategory.LEVEL_UP), completedTutorials.filter(_.category == "airlineGrade")))

    //check loyalist
    result.append(getLoyalistPrompts(airline, completedNoticesByCategory.get(NoticeCategory.LOYALIST), completedTutorials.filter(_.category == "loyalist")))

    result
  }

  def getLevelUpPrompts(airline : Airline, completedNotices : Option[List[Notice]], completedTutorials : List[Tutorial]) : Prompts = {
    val notices = ListBuffer[AirlineNotice]()
    val tutorials = ListBuffer[AirlineTutorial]()

    //check level up
    val notifiedLevel = completedNotices match {
      case Some(completedLevelUpNotices) => completedLevelUpNotices.map(_.asInstanceOf[LevelNotice].level).max
      case None => -1
    }
    if (airline.airlineGrade.value > notifiedLevel) { //level up!
      notices.append(AirlineNotice(airline, LevelNotice(airline.airlineGrade.value), airline.airlineGrade.description))
      if (!airline.isSkipTutorial && airline.airlineGrade.value > 1) {
        if (completedTutorials.isEmpty) {
          tutorials.append(AirlineTutorial(airline, Tutorial("airlineGrade", "")))
        }
      }
    }
    Prompts(notices, tutorials)
  }

  def getLoyalistPrompts(airline : Airline, completedNotices : Option[List[Notice]], completedTutorials : List[Tutorial]) : Prompts = {
    val totalLoyalist = LoyalistSource.loadLoyalistsByCriteria(List(("airline", airline.id))).map(_.amount).sum

    val completedLevel = completedNotices match {
      case Some(completedLevelUpNotices) => completedLevelUpNotices.map(_.asInstanceOf[LoyalistNotice].level).max
      case None => 0
    }
    val currentLevel = LoyalistNotice.getLevel(totalLoyalist)
    val threshold = LoyalistNotice.getThreshold(currentLevel).toLong
    if (currentLevel > completedLevel) {
      val notice = AirlineNotice(airline, LoyalistNotice(currentLevel), s"Reaching $threshold Loyalists!")
      val tutorial = AirlineTutorial(airline, Tutorial("loyalist", ""))
      if (!airline.isSkipTutorial && !completedTutorials.contains(tutorial)) {
        Prompts(ListBuffer(notice), ListBuffer(tutorial))
      } else {
        Prompts(ListBuffer(notice), ListBuffer())
      }


    } else {
      Prompts(ListBuffer(), ListBuffer())
    }
  }
}
