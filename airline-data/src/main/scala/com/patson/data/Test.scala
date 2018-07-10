package com.patson.data

import com.patson.init.IsolatedAirportPatcher
import com.patson.init.WikiUtil
import com.patson.init.AirportProfilePicturePatcher
import com.patson.model.Bank
import com.patson.model.Airport
import com.patson.init.AirportFeaturePatcher
import com.patson.Util
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model
import com.patson.model.CountryMarketShare
import com.patson.model.Country
import com.patson.model.Computation
import java.awt.Color
import com.patson.util.LogoGenerator

object Test extends App {
     
//       println(WikiUtil.queryProfilePicture("Charles de Gaulle Airport", List.empty))
//       println(WikiUtil.queryOtherPicture("Charles de Gaulle Airport", AirportProfilePicturePatcher.airportPreferredWords))
       
//       println(WikiUtil.queryProfilePicture("Barrow City, United States Of America", List.empty))
//       println(WikiUtil.queryProfilePicture("Duncan city, Canada", List.empty))
//       println(WikiUtil.queryProfilePicture("City of Las Vegas, nm, United States Of America", AirportProfilePicturePatcher.cityPreferredWords))
//       println(WikiUtil.queryProfilePicture("City of Las Vegas, nm, United States Of America", List.empty))
       
//       println(WikiUtil.queryOtherPicture("Mexico City", AirportProfilePicturePatcher.cityPreferredWords))
//       println(WikiUtil.queryOtherPicture("Chek Lap Kok International Airport", AirportProfilePicturePatcher.airportPreferredWords))
//     println(WikiUtil.queryOtherPicture("Vancouver"))
     
//        println(AirportProfilePicturePatcher.getCityProfilePictureUrl(Airport.fromId(0).copy(city="Barrow", countryCode="US")))
//       AirportFeaturePatcher.patchFeatures()
    
//    Patchers.patchAirlineCode()
//    Patchers.patchFlightNumber()
//  Patchers.airplaneModelPatcher()
  Patchers.patchAirlineLogos()
  
  
  
}






