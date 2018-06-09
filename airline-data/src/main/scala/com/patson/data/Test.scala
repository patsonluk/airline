package com.patson.data

import com.patson.init.IsolatedAirportPatcher
import com.patson.init.WikiUtil
import com.patson.init.AirportProfilePicturePatcher

object Test extends App {
     println(WikiUtil.queryProfilePicture("Vancouver City, Canada"))
     println(WikiUtil.queryProfilePicture("Vancouver, CA"))
     println(WikiUtil.queryProfilePicture("Vancouver"))
     
 //       AirportProfilePicturePatcher.patchProfilePictures
    
}




