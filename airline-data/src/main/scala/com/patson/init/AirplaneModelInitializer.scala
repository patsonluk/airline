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
    val models = List(Model("Cessna Citation X", 9, 10, 720, 2408, 50000),
                      Model("Embrader ERJ 140ER", 50, 50, 828 , 2317 , 300000),
                      Model("Bombardier CS100", 120 , 100, 870 , 5741 , 700000),
                      Model("Boeing 737-700", 140, 120, 877, 6230 , 900000),
                      Model("Boeing 767-300", 218, 180, 913, 7890 , 1400000),
                      Model("Boeing 777-300", 386, 300, 950, 11100 , 2000000),
                      Model("Boeing 787-8", 242, 200, 954 , 14100 , 2000000),
                      Model("Boeing 747-400ER", 412, 330, 988 , 14200 , 2600000)
        )
    
    ModelSource.saveModels(models)
  }
}