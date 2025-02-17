package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirlineSource, AirportSource, Meta}
import com.patson.init.{AirplaneModelPatcher, AirportGeoPatcher, AssetBlueprintGenerator, actorSystem}
import com.patson.model.AirlineBaseSpecialization.HomeBaseLoyaltySpecialization

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v2.1
  */
object Version2_2Patcher extends App {
  mainFlow

  def mainFlow() {
    patchHomeBaseLoyalty()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }



  def patchHomeBaseLoyalty(): Unit = {
    println("Patching home base loyalty spec")
    AirlineSource.loadAllAirlines(true).foreach { airline =>
      airline.getHeadQuarter().foreach { hq =>
        hq.specializations.filter { spec =>
          spec match {
            case HomeBaseLoyaltySpecialization() => true
            case _ => false
          }
        }.foreach { homeBaseSpec =>
          homeBaseSpec.unapply(airline, hq.airport)
          homeBaseSpec.apply(airline, hq.airport)
        }
      }
    }
  }
}