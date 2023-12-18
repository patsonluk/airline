package com.patson.init

import com.patson.data.Patchers

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirplaneModelPatcher extends App {
  mainFlow
  
  def mainFlow() = {
    Patchers.airplaneModelPatcher()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }
}