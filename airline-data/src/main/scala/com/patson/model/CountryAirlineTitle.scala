package com.patson.model

import com.patson.data.{CountrySource, LinkSource}
import com.patson.util.CountryCache

case class CountryAirlineTitle(country : Country, airline : Airline, title : Title.Value) {

  lazy val loyaltyBonus : Int = {
    val modelPower = 97499995L * 54629L //US
    val ratioToModelPower = country.airportPopulation * country.income.toDouble / modelPower

    val ratio: Double = Math.max(0, math.log10(ratioToModelPower * 100) / 2)

    import CountryAirlineTitle._
    val loyaltyBonus = MIN_LOYALTY_BONUS + (MAX_LOYALTY_BONUS - MIN_LOYALTY_BONUS) * (1 - ratio)

    Math.round(loyaltyBonus / (title match {
      case Title.NATIONAL_AIRLINE => 1
      case Title.PARTNERED_AIRLINE => 3
      case _ => 0
    })).toInt
  }

  lazy val description = Title.description(title)
}


object Title extends Enumeration {
  type Title = Value
  val NATIONAL_AIRLINE, PARTNERED_AIRLINE, PRIVILEGED_AIRLINE, ESTABLISHED_AIRLINE, APPROVED_AIRLINE = Value
  val description = (title : Title.Value) => title match {
    case Title.NATIONAL_AIRLINE => "National Airline"
    case Title.PARTNERED_AIRLINE => "Partnered Airline"
    case Title.PRIVILEGED_AIRLINE => "Privileged Airline"
    case Title.ESTABLISHED_AIRLINE => "Established Airline"
    case Title.APPROVED_AIRLINE => "Approved Airline"
  }

}

object CountryAirlineTitle {
  val MAX_LOYALTY_BONUS = 25
  val MIN_LOYALTY_BONUS = 5

  val getBonusType : (Title.Value => BonusType.Value) = {
    case Title.NATIONAL_AIRLINE => BonusType.NATIONAL_AIRLINE
    case Title.PARTNERED_AIRLINE => BonusType.PARTNERED_AIRLINE
    case _ => BonusType.NO_BONUS

  }

//  val getLinkLimitBonus : (Title.Value => Int) = {
//    case Title.NATIONAL_AIRLINE => 10
//    case Title.PARTNERED_AIRLINE => 5
//    case _ => 0
//  }

  val getTitle : (String, Airline) => Option[CountryAirlineTitle] = (countryCode : String, airline : Airline) => {
    getTopTitle(countryCode, airline).orElse {
      //no top country title, check lower ones
      val title : Option[Title.Value] = {
        val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline).relationship
        if (relationship < 5) {
          None
        } else if (relationship < 10) {
          Some(Title.APPROVED_AIRLINE)
        } else {
          val links = LinkSource.loadLinksByCriteria(List(("airline", airline.id), ("from_country", countryCode)))
          if (links.isEmpty) {
            Some(Title.APPROVED_AIRLINE)
          } else {
            if (relationship < 30) {
              Some(Title.ESTABLISHED_AIRLINE)
            } else {
              Some(Title.PRIVILEGED_AIRLINE)
            }
          }
        }
      }
      title.map(title => CountryAirlineTitle(CountryCache.getCountry(countryCode).get, airline, title))
    }


  }
  /**
    * Top titles are those that are driven my market share, cannot be computed right the way
    */
  val getTopTitle : (String, Airline) => Option[CountryAirlineTitle] = (countryCode : String, airline : Airline) => {
    CountrySource.loadCountryAirlineTitlesByAirlineAndCountry(airline.id, countryCode)
  }

  val getTopTitlesByCountry : (String) => List[CountryAirlineTitle] = (countryCode : String) => {
    CountrySource.loadCountryAirlineTitlesByCountryCode(countryCode)
  }
  val getTopTitlesByAirline : (Int) => List[CountryAirlineTitle] = (airlineId : Int) => {
    CountrySource.loadCountryAirlineTitlesByCriteria(List(("airline", airlineId)))
  }
}
