package com.patson.model.christmas

import com.patson.data.AirportSource
import com.patson.model.{Airline, Airport}

case class SantaClausInfo(airport : Airport, airline : Airline, attemptsLeft : Int, found : Boolean, pickedAward : Option[SantaClausAwardType.Value], var id : Int = 0)


