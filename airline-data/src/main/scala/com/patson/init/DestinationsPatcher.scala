package com.patson.init

import com.patson.Util
import com.patson.data.{AirportSource, DestinationSource, CountrySource}
import com.patson.model._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object DestinationsPatcher extends App {

  import actorSystem.dispatcher

  mainFlow
  
  def mainFlow() : Unit = {
    loadDestinations()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def loadDestinations() : Unit = {
    val destinations = AdditionalLoader.loadDestinations()

    println("Loaded " + destinations.length + " destinations")
    //destinations.foreach(println)

    try {
      DestinationSource.deleteAllDestinations()
      DestinationSource.saveAllDestinations(destinations)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}