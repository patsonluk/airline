package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.CountrySimulation
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirplaneSource, LinkSource, Meta}
import com.patson.init.actorSystem
import com.patson.model._
import com.patson.model.airplane._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v1.1
  */
object Issue227Patcher extends App {
  mainFlow

  def mainFlow() {
    patchMissingConfigurations()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }



  def patchMissingConfigurations() = {
    println("Patching Unassigned airplanes (flight minutes, configuration)")
    val missingConfigAirplanes = AirplaneSource.loadAllAirplanes().filter(airplane => airplane.configuration.id == 0)

    println("Found " + missingConfigAirplanes.length + " airplanes without config")
    missingConfigAirplanes.foreach(_.assignDefaultConfiguration())

    AirplaneSource.updateAirplanes(missingConfigAirplanes)
  }
}