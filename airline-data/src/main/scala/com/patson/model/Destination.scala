package com.patson.model

case class Destination(id : Int, airport : Airport, name : String, destinationType : DestinationType.Value, strength : Int, description : String, latitude : Double, longitude : Double, countryCode : String ) extends IdObject

object DestinationType extends Enumeration {
  type DestinationType = Value
  val LUXURY_HOTEL, ELITE_RESIDENTIAL, ELITE_DESTINATION, SKI_RESORT, UNIVERSITY, ADVENTURE_TOURISM, BUSINESS_TOP_100 = Value

  val label = (destinationType: DestinationType.Value) => destinationType match {
    case ADVENTURE_TOURISM => "Adventure tourism"
    case BUSINESS_TOP_100 => "Business Top 100"
    case ELITE_RESIDENTIAL => "Elite residential"
    case ELITE_DESTINATION => "Elite destination"
    case LUXURY_HOTEL => "Luxury hotel"
    case SKI_RESORT => "Ski resort"
    case UNIVERSITY => "University"
  }

  def withNameSafe(name: String): Option[DestinationType.Value] =
    values.find(_.toString.equalsIgnoreCase(name))

}
