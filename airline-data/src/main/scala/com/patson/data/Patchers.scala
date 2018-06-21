package com.patson.data

import com.patson.model.Computation

object Patchers {
  def patchHomeCountry() {
    AirlineSource.loadAllAirlines(true).foreach { airline =>
      airline.bases.find( _.headquarter ).foreach { headquarter =>
        airline.setCountryCode(headquarter.countryCode)
        AirlineSource.saveAirlineInfo(airline)
      }
    }
  }
  
//  ALTER TABLE `airline`.`link` 
//ADD COLUMN `flight_type` INT(2) NULL AFTER `frequency`;

  def patchFlightType() {
    val updatingLinks = LinkSource.loadAllLinks(LinkSource.FULL_LOAD).map { link =>
      val flightType = Computation.getFlightType(link.from, link.to, link.distance)
      println(flightType.id)
      link.copy(flightType = flightType)
      //LinkSource.updateLink(link)
    }
    
    LinkSource.updateLinks(updatingLinks)
  }
}