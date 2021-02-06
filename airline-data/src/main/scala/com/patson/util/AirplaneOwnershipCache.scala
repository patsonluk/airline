package com.patson.util

import com.patson.data.AirplaneSource
import com.patson.model.airplane.Airplane


object AirplaneOwnershipCache {
  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  val simpleCache: LoadingCache[Int, List[Airplane]] = CacheBuilder.newBuilder.maximumSize(10000).build(new SimpleLoader())

  def getOwnership(airlineId : Int) : List[Airplane] = {
    simpleCache.get(airlineId)
  }

  def invalidateAll() = {
    simpleCache.invalidateAll()
  }

  def invalidate(airlineId : Int) = {
    simpleCache.invalidate(airlineId)
  }


  class SimpleLoader() extends CacheLoader[Int, List[Airplane]] {
    override def load(airlineId: Int) = {
      AirplaneSource.loadAirplanesByOwner(airlineId)
    }
  }

}



