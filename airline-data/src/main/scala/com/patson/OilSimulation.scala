package com.patson

import com.patson.data._
import com.patson.model.oil.OilPrice

object OilSimulation {
  def simulate(cycle: Int) : Unit = {
    val fromCycle = cycle - 10
    val prices = OilSource.loadOilPricesFromCycle(fromCycle).sortBy(_.cycle).map(_.price)
    val nextPrice = getNextPrice(prices)
    OilSource.saveOilPrice(OilPrice(nextPrice, cycle))
    //purge 200 turns ago
    OilSource.deleteOilPricesUpToCycle(cycle - 200)
    
    
    //expire contracts
    val expiringContracts = OilSource.loadAllOilContracts().filter(contract => contract.startCycle + contract.contractDuration <= cycle)
    expiringContracts.foreach(OilSource.deleteOilContract(_))
    
  }
  
  def getPriceVelocity(price1 : Double, price2 : Double) = {
    price2 - price1      
  }
  
  def getPriceAccelaration(price1 : Double, price2 : Double, price3 : Double) = {
    getPriceVelocity(price2, price3) - getPriceVelocity(price1, price2)
  }
  
  
  
  def getNextPrice(prices : List[Double]) = {
    if (prices.length == 0) {
      OilPrice.DEFAULT_PRICE
    } else if (prices.length == 1) {
      //computeNextPrice(prices(0), 0, 0)  
      computeNextPrice(prices(0), 0)
//    } else if (prices.length == 2) {
//      computeNextPrice(prices(1), getPriceVelocity(prices(0), prices(1)), 0)
    } else {
      val lastIndex = prices.length - 1
      //computeNextPrice(prices(lastIndex), getPriceVelocity(prices(lastIndex - 1), prices(lastIndex)), getPriceAccelaration(prices(lastIndex - 2), prices(lastIndex - 1), prices(lastIndex)))
      computeNextPrice(prices(lastIndex), getPriceVelocity(prices(lastIndex - 1), prices(lastIndex)))
    }
  }
  
//  val MAX_ACCELERATION = 3
//  val MAX_ACCELERATION_DELTA = 1
  val MAX_VELOCITY = 10
  val MAX_VELOCITY_DELTA_FACTOR = 3.25
  val MIN_PRICE = 10
  val MAX_PRICE = OilPrice.DEFAULT_PRICE * 2 - MIN_PRICE
  val BOUNDARY_ZONE_FACTOR = 0.35
  val BOUNDARY_ZONE_VELOCITY_ADJUSTMENT = 3
  val HIGH_PRICE_THRESHOLD = MAX_PRICE - (MAX_PRICE - MIN_PRICE) * BOUNDARY_ZONE_FACTOR
  val LOW_PRICE_THRESHOLD = MIN_PRICE + (MAX_PRICE - MIN_PRICE) * BOUNDARY_ZONE_FACTOR 
  
//  def computeNextPrice(previousPrice : Double, previousVelocity : Double , previousAcceleration : Double) : Double = {
//     val accelerationDelta = (Math.random() - 0.5) * 2 * MAX_ACCELERATION_DELTA
//     var newAcceleration = previousAcceleration + accelerationDelta
//     newAcceleration = Math.min(MAX_ACCELERATION, newAcceleration)
//     newAcceleration = Math.max(MAX_ACCELERATION * -1, newAcceleration)
//     
//     var newVelocity = previousVelocity + newAcceleration
//     
//     //now adjust the acceleration if it's very close to boundary zone
//     if (previousPrice <= LOW_PRICE_THRESHOLD && newVelocity < 0) { //still dropping
//       newVelocity += BOUNDARY_ZONE_VELOCITY_ADJUSTMENT   
//     } else if (previousPrice >= HIGH_PRICE_THRESHOLD && newVelocity > 0) { //still rising
//       newVelocity -= BOUNDARY_ZONE_VELOCITY_ADJUSTMENT
//     }
//     
//     newVelocity = Math.min(MAX_VELOCITY, newVelocity)
//     newVelocity = Math.max(MAX_VELOCITY * -1, newVelocity)
//     
//     var newPrice = previousPrice + newVelocity
//     newPrice = Math.min(MAX_PRICE, newPrice)
//     newPrice = Math.max(MIN_PRICE, newPrice)
//     
////     println("pa " + previousAcceleration + " pv " +  previousVelocity + " pp " + previousPrice + " a " + newAcceleration + " v " + newVelocity + " p " + newPrice)
//     
//     newPrice
//  }
  
  def computeNextPrice(previousPrice : Double, previousVelocity : Double) : Double = {
    var bellRandom = 0.0
    for (i <- 0 to 2) {
      bellRandom += Util.getBellRandom(0)
    }
    bellRandom /= 2
    
    
    var acceleration =  bellRandom * MAX_VELOCITY_DELTA_FACTOR * (3.5 + Math.abs(previousVelocity))
    
     
     var newVelocity = previousVelocity / 5 + acceleration
     
     //now adjust the acceleration if it's very close to boundary zone
     if (previousPrice <= LOW_PRICE_THRESHOLD && newVelocity < 0) { //still dropping
       newVelocity += BOUNDARY_ZONE_VELOCITY_ADJUSTMENT   
     } else if (previousPrice >= HIGH_PRICE_THRESHOLD && newVelocity > 0) { //still rising
       newVelocity -= BOUNDARY_ZONE_VELOCITY_ADJUSTMENT
     }
     
     newVelocity = Math.min(MAX_VELOCITY, newVelocity)
     newVelocity = Math.max(MAX_VELOCITY * -1, newVelocity)
     
     var newPrice = previousPrice + newVelocity
     newPrice = Math.min(MAX_PRICE, newPrice)
     newPrice = Math.max(MIN_PRICE, newPrice)
     
     println(" pv " +  previousVelocity + " pp " + previousPrice  + " v " + newVelocity + " p " + newPrice)
     
     BigDecimal(newPrice).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}