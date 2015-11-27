package com.patson.data

import com.patson.init.GeoDataGenerator
import com.patson.Authentication
import java.util.Calendar
import com.patson.model._


object Test extends App {
  UserSource.saveUser(User("patson", "test", Calendar.getInstance, UserStatus.ACTIVE))
  Authentication.createUserSecret("patson", "1234")
  
//  println(Authentication.authenticate("patson", "23433"))
}
