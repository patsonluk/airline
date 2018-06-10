package com.patson.data

import com.patson.init.IsolatedAirportPatcher
import com.patson.init.WikiUtil
import com.patson.init.AirportProfilePicturePatcher

object Test extends App {
     
//       println(WikiUtil.queryProfilePicture("Charles de Gaulle Airport", List.empty))
//       println(WikiUtil.queryOtherPicture("Charles de Gaulle Airport", AirportProfilePicturePatcher.airportPreferredWords))
       
       //println(WikiUtil.queryProfilePicture("Mexico City", AirportProfilePicturePatcher.cityPreferredWords))
//       println(WikiUtil.queryOtherPicture("Mexico City", AirportProfilePicturePatcher.cityPreferredWords))
//       println(WikiUtil.queryOtherPicture("Chek Lap Kok International Airport", AirportProfilePicturePatcher.airportPreferredWords))
//     println(WikiUtil.queryOtherPicture("Vancouver"))
     
        AirportProfilePicturePatcher.patchProfilePictures
    
}




