package com.patson.model

import com.patson.CountrySimulation
import com.patson.data.{CountrySource, LinkSource}
import com.patson.util.CountryCache

case class CountryAirlineTitle(country : Country, airline : Airline, title : Title.Value) {

  lazy val loyaltyBonus : Int = {
    val ratioToModelPower = country.airportPopulation * country.income.toDouble / Computation.MODEL_COUNTRY_POWER

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
  val NATIONAL_AIRLINE, PARTNERED_AIRLINE, PRIVILEGED_AIRLINE, ESTABLISHED_AIRLINE, APPROVED_AIRLINE, NONE = Value
  val description = (title : Title.Value) => title match {
    case Title.NATIONAL_AIRLINE => "National Airline"
    case Title.PARTNERED_AIRLINE => "Partnered Airline"
    case Title.PRIVILEGED_AIRLINE => "Privileged Airline"
    case Title.ESTABLISHED_AIRLINE => "Established Airline"
    case Title.APPROVED_AIRLINE => "Approved Airline"
    case Title.NONE => "None"
  }

  val relationshipBonus = (title : Title.Value) => title match {
    case Title.NATIONAL_AIRLINE => 30
    case Title.PARTNERED_AIRLINE => 15
    case _ => 0
  }

}

object CountryAirlineTitle {
  val MAX_LOYALTY_BONUS = 30
  val MIN_LOYALTY_BONUS = 10

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

  val PRIVILEGED_AIRLINE_RELATIONSHIP_THRESHOLD = 40
  val ESTABLISHED_AIRLINE_RELATIONSHIP_THRESHOLD = 20
  val APPROVED_AIRLINE_RELATIONSHIP_THRESHOLD = 5

  val getTitle : (String, Airline) => CountryAirlineTitle = (countryCode : String, airline : Airline) => {
    getTopTitle(countryCode, airline).getOrElse {
      //no top country title, check lower ones
      val title : Title.Value = {
        val relationship = AirlineCountryRelationship.getAirlineCountryRelationship(countryCode, airline).relationship
        if (relationship < APPROVED_AIRLINE_RELATIONSHIP_THRESHOLD) {
          Title.NONE
        } else if (relationship < ESTABLISHED_AIRLINE_RELATIONSHIP_THRESHOLD) {
          Title.APPROVED_AIRLINE
        } else {
          val links = LinkSource.loadLinksByCriteria(List(("airline", airline.id), ("to_country", countryCode))) ++
            LinkSource.loadLinksByCriteria(List(("airline", airline.id), ("from_country", countryCode)))
          if (links.isEmpty) {
            Title.APPROVED_AIRLINE
          } else {
            if (relationship < PRIVILEGED_AIRLINE_RELATIONSHIP_THRESHOLD) {
              Title.ESTABLISHED_AIRLINE
            } else {
              Title.PRIVILEGED_AIRLINE
            }
          }
        }
      }
      CountryAirlineTitle(CountryCache.getCountry(countryCode).get, airline, title)
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

  import Title._
  val getTitleRequirements = (title : Title.Value, country : Country) => title match  {
    case NATIONAL_AIRLINE =>
      List(s"Airline market share reach top ${CountrySimulation.computeNationalAirlineCount(country)} of all airlines HQ in ${country.name}")
    case PARTNERED_AIRLINE =>
      List(s"Airline market share reach top ${CountrySimulation.computePartneredAirlineCount(country)} of all airlines in ${country.name} excluding the national airline")
    case PRIVILEGED_AIRLINE =>
      List(s"Airline reaches relationship $PRIVILEGED_AIRLINE_RELATIONSHIP_THRESHOLD with ${country.name}", s"Flight route established with ${country.name}")
    case ESTABLISHED_AIRLINE =>
      List(s"Airline reaches relationship $ESTABLISHED_AIRLINE_RELATIONSHIP_THRESHOLD with ${country.name}", s"Flight route established with ${country.name}")
    case APPROVED_AIRLINE =>
      List(s"Airline reaches relationship $APPROVED_AIRLINE_RELATIONSHIP_THRESHOLD with ${country.name}")
    case NONE =>
      List(s"Airline relationship with ${country.name} below $APPROVED_AIRLINE_RELATIONSHIP_THRESHOLD")
  }

  val getTitleBonus = (title : Title.Value, country : Country) => title match {
    case NATIONAL_AIRLINE =>
      List(s"Loyalty +${CountryAirlineTitle(country, Airline.fromId(0), NATIONAL_AIRLINE).loyaltyBonus} on all airports in ${country.name}",
        s"Relationship +${Title.relationshipBonus(NATIONAL_AIRLINE)} with ${country.name}",
        s"Allow building airport bases in any airports in ${country.name}"
      )
    case PARTNERED_AIRLINE =>
      List(s"Loyalty +${CountryAirlineTitle(country, Airline.fromId(0), PARTNERED_AIRLINE).loyaltyBonus} on all airports in ${country.name}",
        s"Relationship +${Title.relationshipBonus(PARTNERED_AIRLINE)} with ${country.name}",
        s"Allow building airport bases in any airports in ${country.name}"
      )
    case PRIVILEGED_AIRLINE =>
      List(s"Allow building airport bases in any airports in ${country.name}"
      )
    case ESTABLISHED_AIRLINE =>
      List(s"Allow building airport bases in only Gateway airports in ${country.name}"
      )
    case APPROVED_AIRLINE =>
      List()
    case NONE =>
      List()
  }
}
