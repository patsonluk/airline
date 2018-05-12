package com.patson.init

import com.patson.model.airplane.Model
import com.patson.data.airplane.ModelSource

object AirplaneModelInitializer extends App {
  mainFlow
  
  def mainFlow() = {
    val airlines = populateAirplaneModels()
    actorSystem.shutdown()
  }
  
  def populateAirplaneModels() = {
    ModelSource.deleteAllModels()
  
    //case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int) extends IdObject
    ModelSource.saveModels(Model.models)
  }
}