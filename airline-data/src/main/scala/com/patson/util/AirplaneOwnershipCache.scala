package com.patson.util

import com.patson.data.AirplaneSource
import com.patson.model.airplane.Airplane

object AirplaneOwnershipCache {
  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  val simpleCache: LoadingCache[Int, AirplaneOwnershipInfo] = CacheBuilder.newBuilder.maximumSize(10000).build(new SimpleLoader())

  def getOwnershipInfo(airlineId : Int) : AirplaneOwnershipInfo = {
    simpleCache.get(airlineId)
  }

  def getOwnership(airlineId : Int) : List[Airplane] = {
    simpleCache.get(airlineId).airplanes
  }

  def invalidateAll() = {
    simpleCache.invalidateAll()
  }

  def invalidate(airlineId : Int) = {
    simpleCache.invalidate(airlineId)
  }


  class SimpleLoader() extends CacheLoader[Int, AirplaneOwnershipInfo] {
    override def load(airlineId: Int) = {
      AirplaneOwnershipInfo(AirplaneSource.loadAirplanesByOwner(airlineId))
    }
  }

}

case class AirplaneOwnershipInfo(airplanes : List[Airplane]) {
  lazy val models = airplanes.map(_.model).toSet
  lazy val families = models.map(_.family)
}


