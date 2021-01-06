package com.patson

import com.patson.data._
import com.patson.model._

import scala.collection.mutable.ListBuffer


object CountrySimulation {
  val MAX_NATIONAL_AIRLINE_COUNT = 2 //What US will have
  val MAX_PARTNERED_AIRLINE_COUNT = 4 //What US will have

  def computeNationalAirlineCount(country: Country) : Int = {
    val ratioToModelPower = country.airportPopulation * country.income.toDouble / Computation.MODEL_COUNTRY_POWER

    val ratio: Double = math.log10(ratioToModelPower * 100) / 2

    val result = Math.round(MAX_NATIONAL_AIRLINE_COUNT * ratio).toInt
    if (result <= 0) 1 else result
  }

  def computePartneredAirlineCount(country: Country) : Int = {
    val ratioToModelPower = country.airportPopulation * country.income.toDouble / Computation.MODEL_COUNTRY_POWER

    val ratio: Double = math.log10(ratioToModelPower * 100) / 2

    val result = Math.round(MAX_PARTNERED_AIRLINE_COUNT * ratio).toInt
    if (result <= 0) 1 else result
  }

  def simulate(cycle: Int) = {
    println("starting country simulation")
    val marketSharesByCountryCode: Map[String, Map[Int, Long]] = CountrySource.loadMarketSharesByCriteria(List.empty).map(marketShare => (marketShare.countryCode, marketShare.airlineShares)).toMap
    val countriesByCode = CountrySource.loadAllCountries().map(entry => (entry.countryCode, entry)).toMap
    val airlinesById = AirlineSource.loadAllAirlines(fullLoad = false).map(airline => (airline.id, airline)).toMap
    val countryAirlineTitles = ListBuffer[CountryAirlineTitle]()
    marketSharesByCountryCode.foreach {
      case (countryCode, marketSharesByAirline) =>

        val orderedMarketShares: List[(Int, Long)] = marketSharesByAirline.toList.sortBy(_._2).reverse
        var nationalAirlineQuota = computeNationalAirlineCount(countriesByCode(countryCode))
        var partneredAirlineQuota = computePartneredAirlineCount(countriesByCode(countryCode))

        orderedMarketShares.foreach {
          case (airlineId, _) =>
            val airline = airlinesById(airlineId)
            if (nationalAirlineQuota > 0 && airline.getCountryCode() == Some(countryCode)) {
              countryAirlineTitles.append(CountryAirlineTitle(Country.fromCode(countryCode), airline, Title.NATIONAL_AIRLINE))
              nationalAirlineQuota -= 1
            } else if (partneredAirlineQuota > 0) {
              countryAirlineTitles.append(CountryAirlineTitle(Country.fromCode(countryCode), airline, Title.PARTNERED_AIRLINE))
              partneredAirlineQuota -= 1
            } //shortcut out?
        }
    }
    CountrySource.saveCountryAirlineTitles(countryAirlineTitles.toList)
  }
}