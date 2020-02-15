package com.patson.patch

import com.patson.data.AirplaneSource
import com.patson.init.actorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Issue271Patcher extends App {
  mainFlow

  def mainFlow() {
    com.patson.data.Patchers.airplaneModelPatcher()
  }
}