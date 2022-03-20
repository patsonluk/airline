package com.patson.patch

import com.patson.data.{AirlineSource, AirportSource}
import com.patson.init.actorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Issue498_spec_patcher extends App {
  mainFlow

  def mainFlow() {
    patchInvalidSpecs()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def patchInvalidSpecs() = {
    AirlineSource.loadAirlineBasesByCriteria(List.empty).sortBy(_.airline.id).foreach { base =>
      val specs = AirportSource.loadAirportBaseSpecializations(base.airport.id, base.airline.id)
      val (updatingSpecs, purgingSpecs) = specs.partition(_.scaleRequirement <= base.scale) //remove spec that no longer able to support

      if (!purgingSpecs.isEmpty) {
        AirportSource.updateAirportBaseSpecializations(base.airport.id, base.airline.id, updatingSpecs)

        purgingSpecs.foreach(_.unapply(base.airline, base.airport))

        println(s"${base.airline} (${base.airport.displayText}) - purged specs $purgingSpecs")
      }
    }
  }
}