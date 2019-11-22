package com.patson.model.christmas

import com.patson.model.{Airline, Airport}

case class SantaClausInfo(airport : Airport, airline : Airline, attemptsLeft : Int, guesses : List[SantaClausGuess], found : Boolean, pickedAward : Option[SantaClausAwardType.Value], var id : Int = 0)

case class SantaClausGuess(airport: Airport, airline : Airline)

object SantaClausInfo {
  val AIRPORT_SIZE_THRESHOLD = 6 //airport size should be >= than this to be a candidate
  val MAX_ATTEMPTS = 10
}