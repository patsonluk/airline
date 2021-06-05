package com.patson.model.animation
import com.patson.model.{Airport, IdObject}

case class AirportAnimation(airport : Airport, animationType : AirportAnimationType.Value, url : String, var id : Int = 0) extends IdObject

object AirportAnimationType extends Enumeration {
  type AirportAnimationType = Value
  val AIRPORT, CITY, SCENERY = Value
}