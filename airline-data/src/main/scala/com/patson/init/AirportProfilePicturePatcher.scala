package com.patson.init

import com.patson.data.AirportSource

object AirportProfilePicturePatcher {
  
  def patchProfilePictures() = {
    AirportSource.loadAllAirports().sortBy(_.power).reverse.foreach { airport =>
      var cityUrl : Option[String] = None
      if (!"".equals(airport.city)) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + airport.countryCode)
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city + " city," + airport.countryCode)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + airport.countryCode)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city + "," + airport.countryCode)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city)
        }
        
        //no preferred pic, just get profile one
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + airport.countryCode, matchPreferredWords = false)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + airport.countryCode, matchPreferredWords = false)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city, matchPreferredWords = false)
        }
        
        println(airport.city + " => " + cityUrl)
      }
      
      var airportUrl : Option[String] = None
      airportUrl = WikiUtil.queryProfilePicture(airport.name, matchPreferredWords = false)
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
}