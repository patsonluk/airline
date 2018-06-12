package com.patson.init

import com.patson.data.AirportSource

object AirportProfilePicturePatcher {
  val cityPreferredWords = List("montage", "montaje", "downtown", "skyline")  
  val airportPreferredWords = List("concourse", "terminal")
  
  def patchProfilePictures() = {
    val airportParList = AirportSource.loadAllAirports().par
    
    println("parallelism level: " + airportParList.tasksupport.parallelismLevel)
    
    airportParList.foreach { airport =>
      var cityUrl : Option[String] = None
      if (!"".equals(airport.city)) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + airport.countryCode, cityPreferredWords)
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city + " city," + airport.countryCode, cityPreferredWords)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + airport.countryCode, cityPreferredWords)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city + "," + airport.countryCode, cityPreferredWords)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city, cityPreferredWords)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryOtherPicture(airport.city, cityPreferredWords)
        }
        
        //no preferred pic, just get profile one
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + airport.countryCode, List.empty)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + airport.countryCode, List.empty)
        }
        if (cityUrl.isEmpty) {
          cityUrl = WikiUtil.queryProfilePicture(airport.city, List.empty)
        }
        
        println(airport.city + " => " + cityUrl)
      }
      
      var airportUrl : Option[String] = None
      airportUrl = WikiUtil.queryProfilePicture(airport.name, List.empty)
      if (airportUrl.isEmpty) {
        airportUrl = WikiUtil.queryOtherPicture(airport.name, airportPreferredWords)
      }
//      if (airportUrl.isEmpty) {
//        airportUrl = WikiUtil.queryProfilePicture(airport.name, List.empty)
//      }
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