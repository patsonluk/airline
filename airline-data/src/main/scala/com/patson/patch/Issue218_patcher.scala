package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.CountrySimulation
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirplaneSource, LinkSource, Meta}
import com.patson.init.actorSystem
import com.patson.model._
import com.patson.model.airplane._

import scala.collection.{View, mutable}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v1.1
  */
object Issue218_patcher extends App {
  mainFlow

  def mainFlow() {
    patchAirplaneConfiguration()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def patchAirplaneConfiguration() = {
    var count = 0
    val links =LinkSource.loadAllFlightLinks(LinkSource.FULL_LOAD)
    links.foreach { link =>
      if (link.getAssignedAirplanes().size > 0) {
        val updatedAssignments: Map[Airplane, LinkAssignment] = link.getAssignedAirplanes().view.map {
          case (airplane, existingAssignment) =>
            val flightMinutes = existingAssignment.frequency * Computation.calculateFlightMinutesRequired(airplane.model, link.distance)
            (airplane, LinkAssignment(existingAssignment.frequency, flightMinutes))
        }.toMap
        LinkSource.updateAssignedPlanes(link.id, updatedAssignments)
      }
      count += 1
      if (count % 1000 == 0) {
        println(s"$count out of ${links.size}")
      }
    }
  }


}