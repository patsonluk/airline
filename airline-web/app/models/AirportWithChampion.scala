package models

import com.patson.model.Airline
import com.patson.model.Airport

case class AirportWithChampion(airport : Airport, champion : Option[Airline], contested : Option[Airline])
