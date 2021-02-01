package controllers

import com.patson.data.AirportAnimationSource
import com.patson.model.Airport
import com.patson.model.animation._

import scala.util.Random

object AirportAnimationUtil {
  val random = new Random()
  val sampleAnimationsByAirportSize = AirportAnimationSource.loadAirportAnimationByType(AirportAnimationType.AIRPORT).groupBy(_.airport.size)
  val defaultAnimation = sampleAnimationsByAirportSize.toList.sortBy(_._1).head._2.head //smallest airport

  def getAnimation(candidates : Airport*) : AirportAnimation = {
    candidates.foreach { candidate =>
      val animations = AirportAnimationSource.loadAirportAnimationByAirportId(candidate.id)
      if (!animations.isEmpty) {
        return animations(random.nextInt(animations.size))
      }
    }

    //cannot find an exact match, try to find sample
    candidates.foreach { candidate =>
      sampleAnimationsByAirportSize.get(candidate.size).foreach { sampleAnimations =>
        return sampleAnimations(random.nextInt(sampleAnimations.size))
      }
    }

    return defaultAnimation
  }
}

