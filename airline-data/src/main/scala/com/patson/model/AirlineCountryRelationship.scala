package com.patson.model

import com.patson.data.CountrySource

import scala.collection.mutable

case class AirlineCountryRelationship(airline : Airline, country : Country, factors : Map[RelationshipFactor, Int]) {
  val relationship = factors.values.sum
}

abstract class RelationshipFactor {
  val getDescription : String
}

object AirlineCountryRelationship {
  val countryRelationships = CountrySource.getCountryMutualRelationships()
  val countryMap = CountrySource.loadAllCountries().map(country => (country.countryCode, country)).toMap
  val HOME_COUNTRY = (homeCountry : Country, targetCountry : Country, relationship : Int) => new RelationshipFactor {
    override val getDescription: String = {
      val relationshipString = relationship match {
        case x if x >= 5 => "Home Country"
        case 4 => "Alliance"
        case 3 => "Close"
        case 2 => "Friendly"
        case 1 => "Warm"
        case 0 => "Neutral"
        case -1 => "Cold"
        case -2 => "Hostile"
        case -3 => "In Conflict"
        case _ => "War"
      }
      if (homeCountry.countryCode == targetCountry.countryCode) {
        s"Your home country ${homeCountry.name}"
      } else {
        s"Relationship between your home country ${homeCountry.name} and ${targetCountry.name} : ${relationshipString}"
      }
    }
  }


  val MARKET_SHARE = (percentage : BigDecimal) => new RelationshipFactor {
    override val getDescription: String = {
      s"${percentage}% of market share"
    }
  }

  val TITLE = (title : CountryAirlineTitle) => new RelationshipFactor {
    override val getDescription: String = {
      title.description
    }
  }

  val DELEGATE = (delegateCount : Int) => new RelationshipFactor {
    override val getDescription: String = {
      s"${delegateCount} assigned delegate(s)"
    }
  }

  val HOME_COUNTRY_RELATIONSHIP_MULTIPLIER = 5
  def getAirlineCountryRelationship(countryCode : String, airline : Airline) : AirlineCountryRelationship = {
    val factors = mutable.HashMap[RelationshipFactor, Int]()
    val targetCountry = countryMap(countryCode)
    airline.getCountryCode() match {
      case Some(homeCountryCode) =>
        val relationship = countryRelationships.getOrElse((homeCountryCode, countryCode), 0)
        factors.put(HOME_COUNTRY(countryMap(homeCountryCode), targetCountry, relationship), relationship * HOME_COUNTRY_RELATIONSHIP_MULTIPLIER)
      case None =>
    }

    CountrySource.loadCountryAirlineTitlesByAirlineAndCountry(airline.id, countryCode).foreach {
      title => {
        val relationshipBonus = title.title match {
          case Title.NATIONAL_AIRLINE => 50
          case Title.PARTNERED_AIRLINE => 20
        }
        factors.put(TITLE(title), relationshipBonus)
      }
    }

    CountrySource.loadMarketSharesByCountryCode(countryCode).foreach {
      marketShares => {
        marketShares.airlineShares.get(airline.id).foreach {
          marketShareOfThisAirline => {
            var percentage = BigDecimal(marketShareOfThisAirline.toDouble / marketShares.airlineShares.values.sum * 100)
            percentage = percentage.setScale(2, BigDecimal.RoundingMode.HALF_UP)
            val relationshipBonus : Int = percentage match {
              case x if x >= 50 => 40
              case x if x >= 25 => 30
              case x if x >= 10 => 25
              case x if x >= 5 => 20
              case x if x >= 2 => 15
              case x if x >= 1 => 10
              case x if x >= 0.5 => 8
              case x if x >= 0.1 => 6
              case x if x >= 0.02 => (x * 50).toInt
              case _ => 1
            }
            factors.put(MARKET_SHARE(percentage), relationshipBonus)
          }
        }
      }
    }

    //TODO delegates

    AirlineCountryRelationship(airline, targetCountry, factors.toMap)
  }
}

