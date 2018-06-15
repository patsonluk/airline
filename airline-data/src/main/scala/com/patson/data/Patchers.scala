package com.patson.data

object Patchers {
  def patchHomeCountry() {
    AirlineSource.loadAllAirlines(true).foreach { airline =>
      airline.bases.find( _.headquarter ).foreach { headquarter =>
        airline.setCountryCode(headquarter.countryCode)
        AirlineSource.saveAirlineInfo(airline)
      }
    }
  }
}