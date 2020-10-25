package com.patson.util

import java.util.concurrent.TimeUnit

import com.patson.data.CountrySource
import com.patson.model._


object CountryCache {

  import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

  val simpleCache: LoadingCache[String, Option[Country]] = CacheBuilder.newBuilder.maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build(new SimpleLoader())

  def getCountry(countryCode : String) : Option[Country] = {
    simpleCache.get(countryCode)
  }

  def invalidateCountry(countryCode : String) = {
    simpleCache.invalidate(countryCode)
  }

  def invalidateAll() = {
    simpleCache.invalidateAll()
  }

  class SimpleLoader extends CacheLoader[String, Option[Country]] {
    override def load(countryCode: String) = {
      CountrySource.loadCountryByCode(countryCode)
    }
  }


}



