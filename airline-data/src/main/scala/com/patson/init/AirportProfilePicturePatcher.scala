package com.patson.init

import com.patson.data.AirportSource

object AirportProfilePicturePatcher {
  
  def patchProfilePictures() = {
    AirportSource.loadAllAirports().sortBy(_.power).reverse.foreach { airport =>
      var cityUrl : Option[String] = None
      if (!"".equals(airport.city)) {
        cityUrl = getMontageUrl(airport.city + " city," + airport.countryCode)
        if (cityUrl.isEmpty) {
          cityUrl = getMontageUrl(airport.city + "," + airport.countryCode)
        }
        if (cityUrl.isEmpty) {
          cityUrl = getMontageUrl(airport.city)
        }
        
        println(airport.city + " => " + cityUrl)
      }
      
      var airportUrl : Option[String] = None
      airportUrl = getMontageUrl(airport.name)
      println(airport.city + " (airport) => " + airportUrl)
      
      if (cityUrl.isDefined || airportUrl.isDefined) {
        if (cityUrl.isDefined) {
          airport.setCityImageUrl(cityUrl.get)
        }
        if (airportUrl.isDefined) {
          airport.setAirportImageUrl(airportUrl.get)
        }
        AirportSource.updateAirportImages(List(airport))              
      }
    }
  }
  
  def getMontageUrl(queryValue : String) : Option[String] = {
//    WikiUtil.queryProfilePicture(queryValue) match {
//      case Some(profileUrl) => if (profileUrl.toLowerCase.contains("montage")) Some(profileUrl) else None
//      case None => None
//    }
    WikiUtil.queryProfilePicture(queryValue)
  }
}