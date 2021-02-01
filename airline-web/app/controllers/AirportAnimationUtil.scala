package controllers

import com.patson.data.AirportAnimationSource
import com.patson.model.Airport
import com.patson.model.animation._

import scala.util.Random

object AirportAnimationUtil {
  val random = new Random()
  val sampleAnimationsByAirportSize = AirportAnimationSource.loadAirportAnimationByType(AirportAnimationType.AIRPORT).groupBy(_.airport.size)
  val nearbyAirports : Map[(String, String), List[Airport]] = findNearbyAirports()

  val defaultAnimation = sampleAnimationsByAirportSize.toList.sortBy(_._1).headOption match {
    case Some((size, animationsOfSmallestAirport)) => animationsOfSmallestAirport.head
    case None => AirportAnimation(Airport.fromId(0), AirportAnimationType.AIRPORT, "") //dummy
  } //smallest airport

  def getAnimation(candidates : Airport*) : AirportAnimation = {
    candidates.foreach { candidate =>
      val animations = AirportAnimationSource.loadAirportAnimationByAirportId(candidate.id) ++ nearByAnimations(candidate)
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

  def nearByAnimations(airport : Airport) = {
    nearbyAirports.get((airport.city, airport.countryCode)) match {
      case Some(candidates) => candidates.filter(candidate => candidate.id != airport.id).flatMap { nearbyAirport =>
        AirportAnimationSource.loadAirportAnimationByAirportId(nearbyAirport.id).filter(_.animationType != AirportAnimationType.AIRPORT) //only get non airport animations
      }
      case None => List.empty
    }
  }

  /**
   * find Airports that serve the same city
   */
  def findNearbyAirports() = {
    val airportsByCity = cachedAirportsByPower.filterNot(_.city.isBlank).groupBy(airport => (airport.city, airport.countryCode)) //might need to split since there are cities with same name within the country. o well...
    airportsByCity.filter(_._2.size > 1) //filter out loners
  }
}

