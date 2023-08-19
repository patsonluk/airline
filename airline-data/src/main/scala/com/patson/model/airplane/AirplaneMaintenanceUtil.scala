package com.patson.model.airplane

import com.patson.util.{AirplaneOwnershipCache, AirplaneOwnershipInfo}

object AirplaneMaintenanceUtil {
  val BASE_MAINTENANCE_FACTOR = 0.4
  val PER_FAMILY_MAINTENANCE_FACTOR = 0.1
  val PER_MODEL_MAINTENANCE_FACTOR = 0.02

  def getMaintenanceFactor(airlineId : Int) : Double = {
    testFactor.getOrElse(computeMaintenanceFactor(AirplaneOwnershipCache.getOwnershipInfo(airlineId)))
  }

  var testFactor : Option[Double] = None //for testing...but kinda yike
  def setTestFactor(factor : Option[Double]): Unit = {
    testFactor = factor
  }

  def computeMaintenanceFactor(ownershipInfo : AirplaneOwnershipInfo) = {
    BASE_MAINTENANCE_FACTOR + ownershipInfo.families.size * PER_FAMILY_MAINTENANCE_FACTOR + ownershipInfo.models.size * PER_MODEL_MAINTENANCE_FACTOR
  }
}


