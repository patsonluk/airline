package com.patson.init

import com.patson.data.{AirportAnimationSource, AirportSource}
import com.patson.model.animation._

object AirportAnimationPatcher extends App {
  import AirportAnimationType._
  val animations = Array(
    ("JFK", AIRPORT, "https://player.vimeo.com/video/506825925"),
    ("JFK", CITY, "https://player.vimeo.com/video/506868914"),
    ("LAX", AIRPORT, "https://player.vimeo.com/video/506879495"),
    ("LAX", CITY, "https://player.vimeo.com/video/506879854"),
    ("LAX", SCENERY, "https://player.vimeo.com/video/506879191"),
    ("DUS", AIRPORT, "https://player.vimeo.com/video/506896956"),
    ("BOI", AIRPORT, "https://player.vimeo.com/video/506881395"),
  )

  def patchAirportAnimations() = {
    val animationList = animations.map {
      case (iata, animationType, url) => AirportAnimation(AirportSource.loadAirportByIata(iata).get, animationType, url)
    }.toList
    AirportAnimationSource.updateAirportAnimations(animationList)
  }

  patchAirportAnimations()
}
