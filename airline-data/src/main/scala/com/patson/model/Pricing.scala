package com.patson.model

/**
 * Cost base model
 */
object Pricing {
  //200 km = 300
  //1000 km = 300 + 200 = 500 
  //2000 km = 500 + 200 = 700
  //10000 km = 700 + 800 = 1500
  val modifierBrackets = List((200, 1.5),(800, 0.25),(1000, 0.2),(Int.MaxValue, 0.1))
  
  
  def computeStandardPrice(distance : Int) : Int = {
    var remainDistance = distance
    var price = 0.0
    for (priceBracket <- modifierBrackets) {
      if (priceBracket._1 >= remainDistance) {
        return (price + remainDistance * priceBracket._2).toInt
      } else {
        price += priceBracket._1 * priceBracket._2
      }
    }
    price.toInt
  }
  
  // if price is zero, adjustmentRatio = 0 
  // if price is at standard price, adjustmentRatio = 1
  // if price is at double the standard price, adjustmentRatio = 2 . Fair enough!
  def standardCostAdjustmentRatioFromPrice(distance: Int, price: Int): Double = {
    val standardPrice = computeStandardPrice(distance)
    ((price - standardPrice).toDouble / standardPrice) + 1
  }
  
 
}