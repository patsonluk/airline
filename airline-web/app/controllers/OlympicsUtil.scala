package controllers

import com.patson.model.event.Olympics
import com.patson.model.event.OlympicsStatus._

object OlympicsUtil {


  val getStatusText = (olympics : Olympics, cycle : Int) => {
    olympics.status(cycle) match {
      case VOTING => "Voting for Host City"
      case HOST_CITY_SELECTED => "Host City Voted"
      case PREPARATION => "Preparation for the Games"
      case OLYMPICS_YEAR =>
        val weeksBeforeGames = Olympics.WEEKS_PER_YEAR - Olympics.GAMES_DURATION - olympics.currentWeek(cycle)
        s"$weeksBeforeGames week(s) before the Games"
      case IN_PROGRESS =>
        "Olympic Games in Progress"
      case CONCLUDED =>
        "Concluded"
      case _ => "Unknown"
    }
  }
}


