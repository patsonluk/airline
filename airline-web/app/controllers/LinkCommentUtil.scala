package controllers

import com.patson.DemandGenerator
import com.patson.model.airplane.Airplane
import com.patson.model.{FlightPreferenceType, _}
import com.patson.util.AirportCache

import scala.collection.mutable.ListBuffer
import scala.collection.{MapView, immutable, mutable}
import scala.util.Random

object LinkCommentUtil {
  val SIMULATE_AIRPORT_COUNT = 5

  def simulateComments(consumptionEntries : List[LinkConsumptionHistory], airline : Airline, link : Link) : Map[(LinkClass, FlightPreferenceType.Value), LinkCommentSummary] = {
    val random = new Random(airline.id + link.id) //need a steady generator

    val topConsumptionEntriesByHomeAirport : List[(Int, List[LinkConsumptionHistory])] = consumptionEntries.groupBy(_.homeAirport.id).toList.sortBy(_._2.map(_.passengerCount).sum).takeRight(SIMULATE_AIRPORT_COUNT)
    val result = mutable.Map[(LinkClass, FlightPreferenceType.Value), ListBuffer[LinkComment]]()
    topConsumptionEntriesByHomeAirport.foreach {
      case (airportId, consumptions) => AirportCache.getAirport(airportId, true).foreach { homeAirport =>
        val pool : immutable.Map[(LinkClass, FlightPreferenceType.Value), List[FlightPreference]] = DemandGenerator.getFlightPreferencePoolOnAirport(homeAirport).pool.toList.map {
          case ((linkClass, preferences)) => preferences.groupBy(_.getPreferenceType).map {
            case ((preferenceType, preferences)) => ((linkClass, preferenceType), preferences)
          }
        }.flatten.toMap
        consumptions.foreach { consumption =>
          pool.get((consumption.preferredLinkClass, consumption.preferenceType)).foreach { preferences =>
            val pickedPreference = preferences(Random.nextInt(preferences.length))
            result.getOrElseUpdate((consumption.linkClass, consumption.preferenceType), ListBuffer()).appendAll(generateCommentsPerConsumption(pickedPreference, consumption, homeAirport, airline, link, random))
            //println(s"Picked $pickedPreference for $consumption")
          }
        }

      }
    }

    val sampleSizeGrouping : MapView[(LinkClass, FlightPreferenceType.Value), Int] = topConsumptionEntriesByHomeAirport.flatMap(_._2).groupBy(entry => (entry.linkClass, entry.preferenceType)).view.mapValues(_.map(_.passengerCount).sum)
    result.map {
      case(key, comments) => (key, LinkCommentSummary(comments.toList, sampleSizeGrouping(key)))
    }.toMap

  }



  def generateCommentsPerConsumption(preference : FlightPreference, consumption : LinkConsumptionHistory, homeAirport : Airport, airline : Airline, link : Link, random: Random) = {
    implicit val randomImplicit : Random = random
    //pricing
    val linkClass = consumption.linkClass
    val flightType = link.flightType
    val preferredLinkClass = consumption.preferredLinkClass
    val standardPrice = link.standardPrice(preferredLinkClass)
    val passengerCount = consumption.passengerCount
    val priceDeltaRatio = (preference.priceAdjustedByLinkDiff(link, linkClass) - standardPrice).toDouble / standardPrice
    val allComments = ListBuffer[LinkComment]()
    allComments.appendAll(generateCommentsForPrice(priceDeltaRatio, passengerCount, preference.priceSensitivity))
    allComments.appendAll(generateCommentsForLoyalty(homeAirport.getAirlineLoyalty(airline.id), passengerCount, preference.loyaltySensitivity))
    val expectedQuality = homeAirport.expectedQuality(link.flightType, linkClass)
    allComments.appendAll(generateCommentsForRawQuality(link.rawQuality, airline.getCurrentServiceQuality(), expectedQuality, flightType, passengerCount, preference.qualitySensitivity))
    allComments.appendAll(generateCommentsForServiceQuality(airline.getCurrentServiceQuality(), expectedQuality, flightType, passengerCount, preference.qualitySensitivity))
    allComments.appendAll(generateCommentsForAirplaneCondition(link.getAssignedAirplanes().keys.toList, passengerCount))
    val expectedFrequency = 7 * 24 * 60 / preference.waitDurationThreshold
    allComments.appendAll(generateCommentsForFrequency(link.frequency, expectedFrequency, passengerCount, preference.waitDurationSensitivity))
    allComments.appendAll(generateCommentsForFlightDuration(link.duration, Computation.computeStandardFlightDuration(link.distance), passengerCount, preference.speedSensitivity))

    val commentCount = allComments.groupBy(comment => comment).view.mapValues(_.length).toMap
    //println(s"${consumption.passengerCount} of [[Preference $preference ]]has comments $commentCount")

    allComments
  }



  def generateCommentsForPrice(priceDeltaRatio : Double, passengerCount : Int, priceSensitivity : Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>  //sensitivity 0.7 - 1.2
      val randomAdjust = com.patson.Util.getBellRandom(0, 0.4, Some(random.nextInt()))
      LinkComment.priceComment((priceDeltaRatio + randomAdjust) * priceSensitivity)
    }.flatten
  }

  def generateCommentsForLoyalty(loyalty : Double, passengerCount : Int, loyaltySensitivity : Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      // loyaltySensitivity from 0 to 2
      if (random.nextDouble() * 4 <= loyaltySensitivity) {
        val adjustedLoyalty = loyalty + com.patson.Util.getBellRandom(0, 40, Some(random.nextInt()))
        LinkComment.loyaltyComment(adjustedLoyalty)
      } else {
        List.empty
      }
    }.flatten
  }

  def generateCommentsForRawQuality(rawQuality : Int, serviceQuality : Double, expectedRawQuality : Int, flightType : FlightType.Value, passengerCount : Int, qualitySensitivity : Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      //qualitySensitivity from 1/3 to 1
      if (random.nextDouble() * 2 <= qualitySensitivity) {
        val adjustedExpectation = expectedRawQuality + com.patson.Util.getBellRandom(0, 60, Some(random.nextInt()))
        LinkComment.rawQualityComment(rawQuality, serviceQuality, expectedRawQuality, flightType)
      } else {
        List.empty
      }
    }.flatten
  }

  def generateCommentsForServiceQuality(serviceQuality : Double, expectedRawQuality : Double, flightType : FlightType.Value, passengerCount : Int, qualitySensitivity : Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      if (random.nextDouble() * 2 <= qualitySensitivity) {
        val adjustedExpectation = expectedRawQuality + com.patson.Util.getBellRandom(0, 60, Some(random.nextInt()))
        LinkComment.serviceQualityComment(serviceQuality, expectedRawQuality, flightType)
      } else {
        List.empty
      }
    }.flatten
  }

  def generateCommentsForAirplaneCondition(airplanes : List[Airplane], passengerCount : Int)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      val expectation = com.patson.Util.getBellRandom(50, 70, Some(random.nextInt()))
      val pickedAirplane = airplanes(Random.nextInt(airplanes.length))
      LinkComment.airplaneConditionComment(pickedAirplane.condition, expectation)
    }.flatten
  }

  def generateCommentsForFrequency(frequency: Int, expectedFrequency : Int, passengerCount : Int, frequencySensitivity: Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      //waitDurationSensitivity/frequencySensitivity from 0.02 to 0.2
      if (random.nextDouble() * 0.4 <= frequencySensitivity) {
        val adjustedExpectation = (expectedFrequency + com.patson.Util.getBellRandom(0, 20, Some(random.nextInt()))).toInt
        LinkComment.frequencyComment(frequency, adjustedExpectation)
      } else {
        List.empty
      }
    }.flatten
  }

  def generateCommentsForFlightDuration(duration: Int, expectedDuration : Int, passengerCount : Int, durationSensitivity : Double)(implicit random : Random) = {
    (0 until passengerCount).map { i =>
      //durationSensitivity/speedSensitivity from to 0 to 0.5
      if (random.nextDouble() * 1 <= durationSensitivity) {
        val adjustedExpectation = (expectedDuration * com.patson.Util.getBellRandom(1, 0.7, Some(random.nextInt()))).toInt
        LinkComment.flightDurationComment(duration, adjustedExpectation)
      } else {
        List.empty
      }
    }.flatten
  }
}

case class LinkCommentSummary(comments : List[LinkComment], sampleSize : Int)
case class LinkComment(description : String, category : LinkCommentCategory.Value, positive : Boolean)

object LinkComment {
  val priceComment = (priceDeltaRatio : Double) => {
    val comment =
      if (priceDeltaRatio < -0.8) {
        Some("Wow! This ticket is a steal!")
      } else if (priceDeltaRatio < -0.6) {
        Some("Such a money saver!")
      } else if (priceDeltaRatio < -0.3) {
        Some("The ticket price is very reasonable.")
      } else if (priceDeltaRatio < 0.2) {
        None
      } else if (priceDeltaRatio < 0.4) {
        Some("The ticket is quite expensive.")
      } else if (priceDeltaRatio < 0.7) {
        Some("The ticket is very expensive!")
      } else {
        Some("Insane! This is highway robbery!")
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.PRICE, priceDeltaRatio < 0)
    }
  }

  val loyaltyComment = (loyalty : Double) => {
    val comment =
      if (loyalty <= 0) {
        Some("I am not a fan of your airline")
      } else if (loyalty < 10) {
        None
      } else if (loyalty < 50) {
        Some("Your airline has pretty good reputation")
      } else if (loyalty < 80) {
        Some("I am a fan of your airline")
      } else {
        Some("I would never travel with any airline other than yours!")
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.LOYALTY, loyalty > 0)
    }
  }

  val rawQualityComment = (rawQuality : Int, serviceQuality: Double, expectedQuality : Double, flightType : FlightType.Value) => { //top comment requires good research ie serviceQuality
    val qualityDelta = rawQuality - expectedQuality
    val random = Random.nextInt(3)
    import FlightType._
    val comment = flightType match {
      case SHORT_HAUL_DOMESTIC | SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL =>
        if (rawQuality <= 20 && qualityDelta < -20) {
          if (random == 0) {
            Some("Not even a cup of water was provided!")
          } else if (random == 1) {
            Some("This service is beyond thrifty!")
          } else {
            Some("Absolutely zero service!")
          }
        } else if (rawQuality >= 40 && qualityDelta > 40) {
          if (random == 0) {
            Some("I was pleasantly surprised with the snack given the short flight duration!")
          } else if (random == 1) {
            Some("This drink is a nice touch even for such a short flight")
          } else {
            Some("I am liking this welcome cookie!")
          }
        } else {
          None
        }
      case _ =>
        if (rawQuality <= 40 && qualityDelta < -20) {
          if (random == 0) {
            Some("Either I missed the food service or there was none provided at all!")
          } else if (random == 1) {
            Some("This service is beyond thrifty!")
          } else {
            Some("Absolutely zero service!")
          }
        } else if (rawQuality <= 60 && qualityDelta < 0) {
          if (random == 0) {
            Some("The food was terrible!")
          } else if (random == 1) {
            Some("The drink selection was bad!")
          } else {
            Some("This snack cracker tastes cheap!")
          }
        } else if (rawQuality == 100 && serviceQuality >= 60 && qualityDelta > 30) {
          if (random == 0) {
            Some("Wow! I was served a full course meal and the dessert was delicious!")
          } else if (random == 1) {
            Some("The signature special drink was so good!")
          } else {
            Some("Michelin star quality meal!")
          }
        } else if (rawQuality >= 60 && qualityDelta > 30) {
          if (random == 0) {
            Some("The in-flight meal served was quite tasty!")
          } else if (random == 1) {
            Some("There is a good selection of meal options!")
          } else {
            Some("This wine is quite decent.")
          }
        } else {
          None
        }
    }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.RAW_QUALITY, qualityDelta > 0)
    }
  }


  val serviceQualityComment = (serviceQuality : Double, expectedQuality : Double, flightType : FlightType.Value) => {
    val qualityDelta = serviceQuality - expectedQuality
    val random = Random.nextInt(3)
    import FlightType._
    val comment = flightType match {
      case SHORT_HAUL_DOMESTIC | SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL =>
        if (serviceQuality <= 20 && qualityDelta < -20) {
          if (random == 0) {
            Some("The flight attendants were unprofessional!")
          } else if (random == 1) {
            Some("This seat is so uncomfortable!")
          } else {
            Some("There is no IFE at all!")
          }
        } else if (serviceQuality >= 40 && qualityDelta > 40) {
          if (random == 0) {
            Some("The flight attendants are helpful!")
          } else if (random == 1) {
            Some("This seat is pretty comfy for a short fight like this!")
          } else {
            Some("The entertainment options are decent!")
          }
        } else {
          None
        }
      case _ =>
        if (serviceQuality <= 40 && qualityDelta < -20) {
          if (random == 0) {
            Some("The flight attendants were unprofessional!")
          } else if (random == 1) {
            Some("This seat is so uncomfortable!")
          } else {
            Some("The entertainment options are poor!")
          }
        } else if (serviceQuality <= 40 && qualityDelta < 0) {
          if (random == 0) {
            Some("The flight attendants were not very helpful.")
          } else if (random == 1) {
            Some("This seat is soso, I would rather have a better seat for long flight like this.")
          } else {
            Some("The entertainment options are uninspiring!")
          }
        } else if (serviceQuality >= 70 && qualityDelta > 30) {
          if (random == 0) {
            Some("Service was impeccable from start to finish")
          } else if (random == 1) {
            Some("This seat is superb! I don't even mind the long flight")
          } else {
            Some("State-of-the-art IFE!")
          }
        } else if (serviceQuality >= 50 && qualityDelta > 30) {
          if (random == 0) {
            Some("The flight attendants are polite an cheerful!")
          } else if (random == 1) {
            Some("This seat is quite comfy!")
          } else {
            Some("The entertainment options are decent!")
          }
        } else {
          None
        }
    }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.SERVICE_QUALITY, qualityDelta > 0)
    }

  }

  val airplaneConditionComment = (airplaneCondition : Double, conditionExpectation : Double) => {
    val delta = airplaneCondition - conditionExpectation
    val comment =
      if (airplaneCondition >= 80 && delta > 50) {
        Some("Minty airplane!")
      } else if (airplaneCondition < 20 && delta < -50) {
        Some("Looks like this airplane is going to fall apart at any time!")
      } else if (airplaneCondition < 40 && delta < -30) {
        Some("Is it safe to fly with this old airplane?")
      } else if (airplaneCondition < 60 && delta < -20) {
        Some("This airplane has shown signs of age.")
      } else {
        None
      }

    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.AIRPLANE_CONDITION, delta > 0)
    }
  }

  val frequencyComment = (frequency : Double, expectedFrequency : Double) => {
    val delta = frequency - expectedFrequency
    val comment =
      if (delta < -5) {
        Some("The flight is not frequent enough!")
      } else if (delta > 15) {
        Some("This flight suits my schedule well")
      } else {
        None
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.FREQUENCY, delta > 0)
    }
  }

  val flightDurationComment = (duration : Int, expectedDuration : Int) => {
    val deltaRatio = (duration - expectedDuration).toDouble / expectedDuration
    val comment =
      if (deltaRatio < -0.5) {
        Some("This flight is speedy!")
      } else {
        None
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentCategory.FLIGHT_DURATION, deltaRatio < 0)
    }
  }
}

object LinkCommentCategory extends Enumeration {
  type LinkCommentCategory = Value
  val PRICE, LOYALTY, RAW_QUALITY, SERVICE_QUALITY, AIRPLANE_CONDITION, FREQUENCY, FLIGHT_DURATION = Value
}

