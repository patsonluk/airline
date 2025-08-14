package com.patson.util

import com.patson.data.AllianceSource
import com.patson.model._

import java.util.concurrent.TimeUnit

/**
 * Just a holder for plain demand for now
 */
object DemandCache {

  private[this] var plainDemand : Map[Int, Long] = Map.empty

  def getPlainDemand(airport: Airport) : Option[Long] = {
    plainDemand.get(airport.id)
  }

  def update(plainDemand : Map[Int, Long]) : Unit = {
    this.plainDemand = plainDemand
  }


}



