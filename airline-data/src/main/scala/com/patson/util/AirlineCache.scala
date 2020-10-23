package com.patson.util

import java.util.concurrent.TimeUnit

import com.patson.data.AirlineSource
import com.patson.model._


object AirlineCache {

  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  val detailedCache: LoadingCache[Int, Option[Airline]] = CacheBuilder.newBuilder.maximumSize(5000).expireAfterAccess(10, TimeUnit.MINUTES).build(new DetailedLoader())
  val simpleCache: LoadingCache[Int, Option[Airline]] = CacheBuilder.newBuilder.maximumSize(5000).expireAfterAccess(10, TimeUnit.MINUTES).build(new SimpleLoader())

  def getAirline(airlineId : Int, fullLoad : Boolean = false) : Option[Airline] = {
    if (fullLoad) {
      detailedCache.get(airlineId)
    } else {
      simpleCache.get(airlineId)
    }
  }

  def invalidateAirline(airlineId : Int) = {
    detailedCache.invalidate(airlineId)
    simpleCache.invalidate(airlineId)
  }

  def invalidateAll() = {
    detailedCache.invalidateAll()
    simpleCache.invalidateAll()
  }

  class DetailedLoader extends CacheLoader[Int, Option[Airline]] {
    override def load(airlineId: Int) = {
      AirlineSource.loadAirlineById(airlineId, true)
    }
  }

  class SimpleLoader extends CacheLoader[Int, Option[Airline]] {
    override def load(airlineId: Int) = {
      AirlineSource.loadAirlineById(airlineId, false)
    }
  }


}



