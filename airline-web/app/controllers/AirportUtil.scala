package controllers

import com.patson.data.{AirportSource, CountrySource, LinkStatisticsSource}
import com.patson.model._
import com.patson.util.{AirportChampionInfo, ChampionUtil, CountryCache}
import models.AirportWithChampion

object AirportUtil {
  val modelAirportPower : Long = AirportSource.loadAllAirports().map(_.power).sorted.last
  val modelCountryPower : Long = CountrySource.loadAllCountries().map(country => country.airportPopulation.toLong * country.income).sorted.last
  val MAX_COMPETITION_RATIO = 0.0001 //ratio of departingPassenger / airport power. If the ratio reaches this ratio, competition rating is considered 100
  val ECONOMIC_POWER_WEIGHT = 0.8
  val COUNTRY_POWER_WEIGHT = 0.2
  val COMPETITION_WEIGHT = 0.5

  def rateAirport(airport : Airport) : AirportRating = {
    val flightsFromThisAirport = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airport.id, LinkStatisticsSource.SIMPLE_LOAD)
    val departurePassenger = flightsFromThisAirport.map(_.passengers).sum
    val country = CountryCache.getCountry(airport.countryCode).get
    val ratioToModelAirportPower = airport.power.toDouble / modelAirportPower
    val economicPowerRating = Math.max(0, (math.log10(ratioToModelAirportPower * 100) / 2 * 100).toInt)
    val ratioToModelCountryPower = country.airportPopulation * country.income.toDouble / modelCountryPower
    val countryPowerRating = Math.max(0,(math.log10(ratioToModelCountryPower * 100) / 2 * 100).toInt)
    val competitionRating = Math.min(100, (Math.min(MAX_COMPETITION_RATIO, departurePassenger.toDouble / airport.power) / MAX_COMPETITION_RATIO * 10000).toInt)
    import AirportFeatureType._
    val featureAdjustment = airport.getFeatures().map { feature =>
      feature.featureType match {
        case INTERNATIONAL_HUB => feature.strength / -5
        case FINANCIAL_HUB => feature.strength / -10
        case VACATION_HUB => feature.strength / -10
        case _ => 0
      }
    }.sum
    val overallDifficulty = Math.max(0, Math.min(100, (100 - economicPowerRating) * ECONOMIC_POWER_WEIGHT + (100 - countryPowerRating) * COUNTRY_POWER_WEIGHT + competitionRating * COMPETITION_WEIGHT + featureAdjustment).toInt)


    AirportRating(economicPowerRating, competitionRating, countryPowerRating, airport.getFeatures(), overallDifficulty)
  }

  var cachedAirportChampions : List[AirportWithChampion] = getAirportChampions()

  def getAirportChampions() : List[AirportWithChampion] = {
    val loyalistByAirportId : Map[Int, List[AirportChampionInfo]] = ChampionUtil.loadAirportChampionInfo().groupBy(_.loyalist.airport.id)
    cachedAirportsByPower.map { airport =>
      val championAirline : Option[Airline] = loyalistByAirportId.get(airport.id).map { loyalists =>
        loyalists.sortBy(_.ranking).map(_.loyalist.airline).head
      }
      AirportWithChampion(airport, championAirline)
    }
  }

  def refreshAirports() = {
    cachedAirportChampions = getAirportChampions()
    visibleAirports = getVisibleAirports(airportByPowerCount)
  }

  private val airportByPowerCount = 4000
  var visibleAirports = getVisibleAirports(airportByPowerCount)

  private[this] def getVisibleAirports(airportByPowerCount : Int) : List[AirportWithChampion] = {
    val cachedAirportChampions = AirportUtil.cachedAirportChampions
    val powerfulAirports : Map[Int, AirportWithChampion] = cachedAirportChampions.takeRight(airportByPowerCount).map(entry => (entry.airport.id, entry)).toMap
    val mostPowerfulAirportsPerCountry : List[AirportWithChampion] = cachedAirportChampions.groupBy(_.airport.countryCode).values.flatMap { airportsOfACountry =>
      if (airportsOfACountry.length > 0) {
        List(airportsOfACountry.reverse.apply(0))
      } else {
        List()
      }
    }.toList
    val result = (powerfulAirports.values ++ mostPowerfulAirportsPerCountry.filter { mostPowerfulAirportOfACountry =>
      val alreadyInList = powerfulAirports.contains(mostPowerfulAirportOfACountry.airport.id)
      //println(s"$alreadyInList ? $mostPowerfulAirportOfACountry")
      !alreadyInList
    }).toList
    result
  }
}

//from 0 to 100
case class AirportRating(economicPowerRating : Int, competitionRating : Int, countryPowerRating : Int, features : List[AirportFeature], overallDifficulty : Int)
