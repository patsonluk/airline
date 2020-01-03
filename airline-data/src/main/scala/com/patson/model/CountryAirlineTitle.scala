package com.patson.model

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
    })).toInt
  }
}

object Title extends Enumeration {
  type Title = Value
  val NATIONAL_AIRLINE, PARTNERED_AIRLINE = Value
}

object CountryAirlineTitle {
  val MAX_LOYALTY_BONUS = 25
  val MIN_LOYALTY_BONUS = 5

  val getBonusType : (Title.Value => BonusType.Value) = {
    case Title.NATIONAL_AIRLINE => BonusType.NATIONAL_AIRLINE
    case Title.PARTNERED_AIRLINE => BonusType.PARTNERED_AIRLINE
  }

  val getLinkLimitBonus : (Title.Value => Int) = {
    case Title.NATIONAL_AIRLINE => 20
    case Title.PARTNERED_AIRLINE => 10
  }
}
