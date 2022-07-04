package com.patson.model.airplane

import com.patson.util.AirplaneOwnershipCache

object AirplaneMaintenanceUtil {
  val BASE_MAINTENANCE_FACTOR = 0.4
  val PER_FAMILY_MAINTENANCE_FACTOR = 0.1
  val PER_MODEL_MAINTENANCE_FACTOR = 0.02
  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  val simpleCache: LoadingCache[Int, Double] = CacheBuilder.newBuilder.maximumSize(10000).build(new SimpleLoader())

  def getMaintenanceFactor(airlineId : Int) : Double = {
    testFactor.getOrElse(simpleCache.get(airlineId))
  }

  var testFactor : Option[Double] = None //for testing...but kinda yike
  def setTestFactor(factor : Option[Double]): Unit = {
    testFactor = factor
  }


  class SimpleLoader() extends CacheLoader[Int, Double] {

    def computeMaintenanceFactor(airplanes : List[Airplane]) = {
      val ownedModels = airplanes.map(_.model).toSet
      val ownedFamilies = ownedModels.map(_.family)
      BASE_MAINTENANCE_FACTOR + ownedFamilies.size * PER_FAMILY_MAINTENANCE_FACTOR + ownedModels.size * PER_MODEL_MAINTENANCE_FACTOR
    }

    override def load(airlineId: Int) = {
      computeMaintenanceFactor(AirplaneOwnershipCache.getOwnership(airlineId))
    }
  }

}


