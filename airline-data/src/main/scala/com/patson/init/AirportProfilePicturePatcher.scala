package com.patson.init

import com.patson.data.AirportSource
import com.patson.model.Airport
import com.patson.data.CountrySource
import scala.collection.parallel.CollectionConverters._

/**
  * This is not used anymore. No longer getting from wiki
  */
object AirportProfilePicturePatcher {
  val cityPreferredWords = List("montage", "montaje", "downtown", "skyline")  
  val airportPreferredWords = List("concourse", "terminal")
  val countriesByCode = CountrySource.loadAllCountries().map { country => (country.countryCode, country) }.toMap
  
//  def patchProfilePictures() = {
//    val airportParList = AirportSource.loadAllAirports().par
//
//    println("parallelism level: " + airportParList.tasksupport.parallelismLevel)
//
//    airportParList.foreach { airport =>
//      var cityUrl : Option[String] = getCityProfilePictureUrl(airport)
//
//      println(airport.city + " => " + cityUrl)
//
//      var airportUrl : Option[String] = None
//      airportUrl = WikiUtil.queryProfilePicture(airport.name, List.empty)
//      if (airportUrl.isEmpty) {
//        airportUrl = WikiUtil.queryOtherPicture(airport.name, airportPreferredWords)
//      }
////      if (airportUrl.isEmpty) {
////        airportUrl = WikiUtil.queryProfilePicture(airport.name, List.empty)
////      }
//      println(airport.city + " (airport) => " + airportUrl)
//
//      if (cityUrl.isDefined || airportUrl.isDefined) {
//        if (cityUrl.isDefined) {
//          airport.setCityImageUrl(cityUrl.get)
//        }
//        if (airportUrl.isDefined) {
//          airport.setAirportImageUrl(airportUrl.get)
//        }
//        AirportSource.updateAirportImages(List(airport))
//      }
//    }
//  }
  
  def getCityProfilePictureUrl(airport : Airport) : Option[String] = {
    var cityUrl : Option[String] = None
    if (!"".equals(airport.city)) {
      val countryName = countriesByCode(airport.countryCode).name
      cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + countryName, cityPreferredWords)
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryOtherPicture(airport.city + " city," + countryName, cityPreferredWords)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + countryName, cityPreferredWords)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryOtherPicture(airport.city + "," + countryName, cityPreferredWords)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city, cityPreferredWords)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryOtherPicture(airport.city, cityPreferredWords)
      }
      
      //no preferred pic, just get profile one
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city + " city," + countryName, List.empty)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city + "," + countryName, List.empty)
      }
      if (cityUrl.isEmpty) {
        cityUrl = WikiUtil.queryProfilePicture(airport.city, List.empty)
      }
    }
    
    cityUrl
  }
  
}