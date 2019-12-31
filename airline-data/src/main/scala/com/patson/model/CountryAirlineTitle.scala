package com.patson.model

case class CountryAirlineTitle(countryCode : String, airline : Airline, title : Title.Value)

object Title extends Enumeration {
  type Title = Value
  val NATIONAL_AIRLINE, PARTNERED_AIRLINE = Value
}

object CountryAirlineTitle {
  val getLoyaltyBonus : (Title.Value => Int) = {
    case Title.NATIONAL_AIRLINE => 15
    case Title.PARTNERED_AIRLINE => 5
  }

  val getBonusType : (Title.Value => BonusType.Value) = {
    case Title.NATIONAL_AIRLINE => BonusType.NATIONAL_AIRLINE
    case Title.PARTNERED_AIRLINE => BonusType.PARTNERED_AIRLINE
  }
}
