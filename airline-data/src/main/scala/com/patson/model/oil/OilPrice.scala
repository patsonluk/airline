package com.patson.model.oil

case class OilPrice(price : Double, cycle : Int)

object OilPrice {
  val DEFAULT_PRICE : Double = 70
    //the price used for actual simulation calculation
}