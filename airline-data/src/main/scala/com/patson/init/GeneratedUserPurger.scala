package com.patson.init

import com.patson.data.AirlineSource
import com.patson.data.UserSource

object GeneratedUserPurger extends App {
  UserSource.deleteGeneratedUsers(3)
  AirlineSource.deleteGeneratedAirlines(3)
}