package com.patson.init

import com.patson.model.airplane.Model
import com.patson.data.airplane.ModelSource

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AirplaneModelInitializer extends App {
  mainFlow
  
  def mainFlow() = {
    val airlines = populateAirplaneModels()
    Await.result(actorSystem.terminate(), Duration.Inf)
  }
  
  def populateAirplaneModels() = {
    ModelSource.deleteAllModels()
  
    //case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int) extends IdObject
    ModelSource.saveModels(Model.models)
  }
}