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
            result.getOrElseUpdate((consumption.linkClass, consumption.preferenceType), ListBuffer()).appendAll(generateCommentsPerConsumption(preferences, consumption, homeAirport, airline, link, random))
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

  val MAX_SAMPLE_SIZE_PER_CONSUMPTION = 20

  case class CommentWeight(commentGroup : LinkCommentGroup.Value, weight : Int, adjustRatio : Double)
  case class CommentWeightedPool(weights : List[CommentWeight]) {
    val totalWeights = weights.map(_.weight).sum
    var weightMarkerWalker = 0
    val weightMarkers = weights.map { weight =>
      weightMarkerWalker = weightMarkerWalker + weight.weight
      (weight, weightMarkerWalker)
    }
    def drawCommentWeight(random : Random) = {

      val target = random.nextInt(totalWeights)
      weightMarkers.find( weightMarker => weightMarker._2 >= target).map(_._1)
    }
  }

  def generateCommentsPerConsumption(preferences : List[FlightPreference], consumption : LinkConsumptionHistory, homeAirport : Airport, airline : Airline, link : Link, random: Random) = {
    implicit val randomImplicit : Random = random
    //pricing
    val linkClass = consumption.linkClass
    val flightType = link.flightType
    val preferredLinkClass = consumption.preferredLinkClass
    val sampleSize = Math.min(MAX_SAMPLE_SIZE_PER_CONSUMPTION, consumption.passengerCount)
    val allComments = ListBuffer[LinkComment]()
    val standardDuration = Computation.computeStandardFlightDuration(link.distance)

    import LinkCommentGroup._
    val poolByPreference : Map[FlightPreference, CommentWeightedPool] = preferences.map { preference =>
      val adjustRatioByGroup : Map[controllers.LinkCommentGroup.Value, Double] = Map(
         PRICE -> preference.priceAdjustRatio(link, linkClass),
         LOYALTY -> preference.loyaltyAdjustRatio(link),
         QUALITY -> preference.qualityAdjustRatio(homeAirport, link, linkClass),
         DURATION -> preference.tripDurationAdjustRatio(link)
      )

      val pool = CommentWeightedPool(adjustRatioByGroup.map {
        case((group, ratio)) =>  CommentWeight(group, Math.abs(((1 - ratio) * 100).toInt), ratio)
      }.toList)

      (preference, pool)
    }.toMap

    for (i <- 0 until sampleSize) {
      val preference = preferences(random.nextInt(preferences.length))
      val commentWeight = poolByPreference(preference).drawCommentWeight(random)
      println(s"${consumption.preferenceType} : $commentWeight")
      commentWeight.foreach { weight =>
        val comments = weight.commentGroup match {
          case PRICE => generateCommentsForPrice(weight.adjustRatio)
          case LOYALTY => generateCommentsForLoyalty(weight.adjustRatio)
          case QUALITY => generateCommentsForQuality(link.rawQuality, airline.getCurrentServiceQuality(), link.getAssignedAirplanes().keys.toList, homeAirport.expectedQuality(flightType, linkClass), flightType)
          case DURATION => generateCommentsForFlightDuration(link.frequency, preference.frequencyThreshold, link.duration, standardDuration)
          case _ => List.empty
        }

        allComments.appendAll(comments)
      }


    }
    allComments



//      allComments.appendAll(generateCommentsForPrice(priceDeltaRatio, sampleSize, preference.priceSensitivity))
//      allComments.appendAll(generateCommentsForLoyalty(homeAirport.getAirlineLoyalty(airline.id), sampleSize, preference.loyaltySensitivity))
//      val expectedQuality = homeAirport.expectedQuality(link.flightType, linkClass)
//      allComments.appendAll(generateCommentsForRawQuality(link.rawQuality, airline.getCurrentServiceQuality(), expectedQuality, flightType, sampleSize, preference.qualitySensitivity))
//      allComments.appendAll(generateCommentsForServiceQuality(airline.getCurrentServiceQuality(), expectedQuality, flightType, sampleSize, preference.qualitySensitivity))
//      allComments.appendAll(generateCommentsForAirplaneCondition(link.getAssignedAirplanes().keys.toList, sampleSize))
//      val expectedFrequency = 7 * 24 * 60 / preference.waitDurationThreshold
//      allComments.appendAll(generateCommentsForFrequency(link.frequency, expectedFrequency, sampleSize, preference.waitDurationSensitivity))
//      allComments.appendAll(generateCommentsForFlightDuration(link.duration, Computation.computeStandardFlightDuration(link.distance), sampleSize, preference.speedSensitivity))

//      val commentCount = allComments.groupBy(comment => comment).view.mapValues(_.length).toMap
      //println(s"${consumption.passengerCount} of [[Preference $preference ]]has comments $commentCount")
//    }

  }

  def generateCommentsForPrice(ratio : Double)(implicit random : Random) = {
    val expectedRatio = com.patson.Util.getBellRandom(1, 0.4, Some(random.nextInt()))
    List(LinkComment.priceComment(ratio, expectedRatio)).flatten
  }

  def generateCommentsForLoyalty(ratio : Double)(implicit random : Random) = {
    val expectedRatio = com.patson.Util.getBellRandom(1, 0.4, Some(random.nextInt()))
    List(LinkComment.loyaltyComment(ratio, expectedRatio)).flatten
  }

  def generateCommentsForQuality(rawQuality : Int, serviceQuality : Double, airplanes : List[Airplane], expectedQuality : Int, flightType : FlightType.Value)(implicit random : Random) = {
    List(
      generateCommentForRawQuality(rawQuality, serviceQuality, expectedQuality, flightType),
      generateCommentForServiceQuality(serviceQuality, expectedQuality, flightType),
      generateCommentForAirplaneCondition(airplanes)).flatten

  }

  def generateCommentForRawQuality(rawQuality : Int, serviceQuality : Double, expectedQuality : Int, flightType : FlightType.Value)(implicit random : Random) = {
    val adjustedExpectation = expectedQuality + com.patson.Util.getBellRandom(0, 60, Some(random.nextInt()))
    List(LinkComment.rawQualityComment(rawQuality, serviceQuality, adjustedExpectation, flightType)).flatten
  }

  def generateCommentForServiceQuality(serviceQuality : Double, expectedQuality : Double, flightType : FlightType.Value)(implicit random : Random) = {
    val adjustedExpectation = expectedQuality + com.patson.Util.getBellRandom(0, 60, Some(random.nextInt()))
    List(LinkComment.serviceQualityComment(serviceQuality, adjustedExpectation, flightType)).flatten
  }

  def generateCommentForAirplaneCondition(airplanes : List[Airplane])(implicit random : Random) = {
    val expectation = com.patson.Util.getBellRandom(50, 60, Some(random.nextInt()))
    val pickedAirplane = airplanes(Random.nextInt(airplanes.length))
    List(LinkComment.airplaneConditionComment(pickedAirplane.condition, expectation)).flatten
  }

//  def generateCommentsForFrequency(frequency: Int, expectedFrequency : Int, passengerCount : Int, frequencySensitivity: Double)(implicit random : Random) = {
//    (0 until passengerCount).map { i =>
//      //waitDurationSensitivity/frequencySensitivity from 0.02 to 0.2
//      if (random.nextDouble() * 0.4 <= frequencySensitivity) {
//        val adjustedExpectation = (expectedFrequency + com.patson.Util.getBellRandom(0, 40, Some(random.nextInt()))).toInt
//        LinkComment.frequencyComment(frequency, adjustedExpectation)
//      } else {
//        List.empty
//      }
//    }.flatten
//  }

  def generateCommentsForFlightDuration(frequency: Int, expectedFrequency : Int, flightDuration : Int, expectedDuration : Int)(implicit random : Random) = {
    val adjustedExpectedDuration = (expectedDuration * com.patson.Util.getBellRandom(1, 0.7, Some(random.nextInt()))).toInt
    val adjustedExceptedFrequency = (expectedFrequency * com.patson.Util.getBellRandom(1, 0.7, Some(random.nextInt()))).toInt

    List(
      LinkComment.frequencyComment(frequency, adjustedExceptedFrequency),
      LinkComment.flightDurationComment(flightDuration, adjustedExpectedDuration)).flatten
   }
}

case class LinkCommentSummary(comments : List[LinkComment], sampleSize : Int)
case class LinkComment(description : String, category : LinkCommentType.Value, positive : Boolean)

object LinkComment {
  val priceComment = (priceRatio : Double, expectationRatio : Double) => {
    val priceDeltaRatio = priceRatio - expectationRatio
    val comment =
      if (priceDeltaRatio < -0.7) {
        Some("Wow! This ticket is a steal!")
      } else if (priceDeltaRatio < -0.5) {
        Some("Such a money saver!")
      } else if (priceDeltaRatio < -0.3) {
        Some("The ticket price is very reasonable.")
      } else if (priceDeltaRatio < 0) {
        Some("The ticket price is quite reasonable.")
      } else if (priceDeltaRatio < 0.2) {
        Some("This ticket is not cheap.")
      } else if (priceDeltaRatio < 0.4) {
        Some("The ticket is expensive.")
      } else if (priceDeltaRatio < 0.6) {
        Some("The ticket is very expensive!")
      } else {
        Some("Insane! This is highway robbery!")
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentType.PRICE, priceDeltaRatio < 0)
    }
  }

  val loyaltyComment = (ratio : Double, expectedRatio : Double) => {
    val ratioDelta = ratio - expectedRatio

    val comment =
      if (ratioDelta < -0.4) {
        Some("I would never travel with any airline other than yours!")
      } else if (ratioDelta < -0.2) {
        Some("I am a fan of your airline!")
      } else if (ratioDelta < 0) {
        Some("Your airline has pretty good reputation.")
      } else if (ratioDelta < 0.2) {
        Some("I am not a fan of your airline.")
      } else  {
        Some("I would rather travel with other airlines!")
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentType.LOYALTY, ratioDelta < 0)
    }
  }

  val rawQualityComment = (rawQuality : Int, serviceQuality: Double, expectedQuality : Double, flightType : FlightType.Value) => { //top comment requires good research ie serviceQuality
    val qualityDelta = rawQuality - expectedQuality
    val random = Random.nextInt(3)
    import FlightType._
    val comment = flightType match {
      case SHORT_HAUL_DOMESTIC | SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL =>
        if (rawQuality <= 20 && qualityDelta < -10) {
          if (random == 0) {
            Some("Not even a cup of water was provided!")
          } else if (random == 1) {
            Some("This service is beyond thrifty!")
          } else {
            Some("Absolutely zero service!")
          }
        } else if (rawQuality >= 40 && qualityDelta > 10) {
          if (random == 0) {
            Some("I was pleasantly surprised with the snack given the short flight duration!")
          } else if (random == 1) {
            Some("This drink is a nice touch even for such a short flight!")
          } else {
            Some("I am liking this welcome cookie!")
          }
        } else {
          None
        }
      case _ =>
        if (rawQuality <= 40 && qualityDelta < -10) {
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
        } else if (rawQuality == 100 && serviceQuality >= 60 && qualityDelta > 20) {
          if (random == 0) {
            Some("Wow! I was served a full course meal and the dessert was delicious!")
          } else if (random == 1) {
            Some("The signature special drink was so good!")
          } else {
            Some("Michelin star quality meal!")
          }
        } else if (rawQuality >= 60 && qualityDelta > 10) {
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
      LinkComment(comment, LinkCommentType.RAW_QUALITY, qualityDelta > 0)
    }
  }


  val serviceQualityComment = (serviceQuality : Double, expectedQuality : Double, flightType : FlightType.Value) => {
    val qualityDelta = serviceQuality - expectedQuality
    val random = Random.nextInt(3)
    import FlightType._
    val comment = flightType match {
      case SHORT_HAUL_DOMESTIC | SHORT_HAUL_INTERNATIONAL | SHORT_HAUL_INTERCONTINENTAL =>
        if (serviceQuality <= 20 && qualityDelta < 0) {
          if (random == 0) {
            Some("The flight attendants were unprofessional!")
          } else if (random == 1) {
            Some("This seat is so uncomfortable!")
          } else {
            Some("There is no IFE at all!")
          }
        } else if (serviceQuality >= 40 && qualityDelta > 10) {
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
        if (serviceQuality <= 40 && qualityDelta < -10) {
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
            Some("This seat is so-so, I would rather have a better seat for long flight like this.")
          } else {
            Some("The entertainment options are uninspiring!")
          }
        } else if (serviceQuality >= 70 && qualityDelta > 20) {
          if (random == 0) {
            Some("Service was impeccable from start to finish!")
          } else if (random == 1) {
            Some("This seat is superb! I don't even mind the long flight!")
          } else {
            Some("State-of-the-art IFE!")
          }
        } else if (serviceQuality >= 50 && qualityDelta > 10) {
          if (random == 0) {
            Some("The flight attendants are polite an cheerful!")
          } else if (random == 1) {
            Some("I found the seat pretty comfy!")
          } else {
            Some("The entertainment options are decent!")
          }
        } else {
          None
        }
    }
    comment.map { comment =>
      LinkComment(comment, LinkCommentType.SERVICE_QUALITY, qualityDelta > 0)
    }

  }

  val airplaneConditionComment = (airplaneCondition : Double, conditionExpectation : Double) => {
    val delta = airplaneCondition - conditionExpectation
    val comment =
      if (airplaneCondition >= 80 && delta > 20) {
        Some("I like this fresh smell of new airplane!")
      } else if (airplaneCondition < 20 && delta < -20) {
        Some("Looks like this airplane is going to fall apart at any time!")
      } else if (airplaneCondition < 40 && delta < -10) {
        Some("Is it safe to fly with this old airplane?")
      } else if (airplaneCondition < 60 && delta < 0) {
        Some("This airplane has shown signs of age.")
      } else {
        None
      }

    comment.map { comment =>
      LinkComment(comment, LinkCommentType.AIRPLANE_CONDITION, delta > 0)
    }
  }

  val frequencyComment = (frequency : Double, expectedFrequency : Double) => {
    val delta = frequency - expectedFrequency
    val comment =
      if (delta < -5) {
        Some("The flight is not frequent enough!")
      } else if (delta > 5) {
        Some("This flight suits my schedule well")
      } else {
        None
      }
    comment.map { comment =>
      LinkComment(comment, LinkCommentType.FREQUENCY, delta > 0)
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
      LinkComment(comment, LinkCommentType.FLIGHT_DURATION, deltaRatio < 0)
    }
  }
}

object LinkCommentGroup extends Enumeration {
  type LinkCommentGroup = Value
  val PRICE, LOYALTY, QUALITY, DURATION, OTHER = Value
}

object LinkCommentType extends Enumeration {
  type LinkCommentType = Value
  val PRICE, LOYALTY, RAW_QUALITY, SERVICE_QUALITY, AIRPLANE_CONDITION, FREQUENCY, FLIGHT_DURATION = Value
}

