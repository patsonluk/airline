package com.patson.model

import com.patson.data.{AirportSource, CountrySource, LinkStatisticsSource}
import com.patson.model.AirportFeatureType.{FINANCIAL_HUB, GATEWAY_AIRPORT, INTERNATIONAL_HUB, VACATION_HUB}
import com.patson.util.{AirportCache, CountryCache}

case class AirportRating(economicPowerRating : Int, competitionRating : Int, countryPowerRating : Int, features : List[AirportFeature]) {
  val ECONOMIC_POWER_WEIGHT = 0.8
  val COUNTRY_POWER_WEIGHT = 0.2
  val COMPETITION_WEIGHT = 0.5
  val featureRating = features.map { feature =>
    feature.featureType match {
      case INTERNATIONAL_HUB => feature.strength.toDouble / 3
      case FINANCIAL_HUB => feature.strength.toDouble / 5
      case VACATION_HUB => feature.strength.toDouble  / 10
      case GATEWAY_AIRPORT => 3
      case _ => 0
    }
  }.sum

  val overallDifficulty = Math.max(0, Math.min(100, (100 - economicPowerRating) * ECONOMIC_POWER_WEIGHT + (100 - countryPowerRating) * COUNTRY_POWER_WEIGHT + competitionRating * COMPETITION_WEIGHT - featureRating).toInt)
  val overallRating = economicPowerRating * ECONOMIC_POWER_WEIGHT + countryPowerRating * COUNTRY_POWER_WEIGHT + featureRating
}

object AirportRating {
  val modelAirportPower : Long = AirportSource.loadAllAirports().map(_.power).sorted.last
  val modelCountryPower : Long = CountrySource.loadAllCountries().map(country => country.airportPopulation.toLong * country.income).sorted.last

  val MAX_COMPETITION_RATIO = 0.0001 //ratio of departingPassenger / airport power. If the ratio reaches this ratio, competition rating is considered 100

  def rateAirport(airport : Airport) : AirportRating = {
    val detailedAirport =
      if (!airport.isFeaturesLoaded) {
        AirportCache.getAirport(airport.id, true).get
      } else {
        airport
      }
    val flightsFromThisAirport = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airport.id, LinkStatisticsSource.SIMPLE_LOAD)
    val departurePassenger = flightsFromThisAirport.map(_.passengers).sum
    val country = CountryCache.getCountry(airport.countryCode).get
    val ratioToModelAirportPower = airport.power.toDouble / modelAirportPower
    val economicPowerRating = Math.max(0, (math.log10(ratioToModelAirportPower * 100) / 2 * 100).toInt)
    val ratioToModelCountryPower = country.airportPopulation * country.income.toDouble / modelCountryPower
    val countryPowerRating = Math.max(0,(math.log10(ratioToModelCountryPower * 100) / 2 * 100).toInt)
    val competitionRating = Math.min(100, (Math.min(MAX_COMPETITION_RATIO, departurePassenger.toDouble / airport.power) / MAX_COMPETITION_RATIO * 10000).toInt)


    AirportRating(economicPowerRating, competitionRating, countryPowerRating, detailedAirport.getFeatures())
  }
}