package com.patson.model

case class Airline(name: String) extends IdObject

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("")
    airlineWithJustId.id = id
    airlineWithJustId
  }
}