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
    val models = List(Model("Cessna 421", capacity = 7, fuelBurn = 7, speed = 300, range = 1555, price = 550000),
                      Model("Cessna Caravan", capacity = 14, fuelBurn = 15, speed = 344, range = 2400, price = 1600000),
                      Model("Embraer EMB 120 Brasilia", capacity = 30, fuelBurn = 54, speed = 552, range = 1750, price = 8500000),
                      Model("Embraer ERJ 140", capacity = 44, fuelBurn = 81, speed = 828, range = 2315, price = 17000000),
                      Model("Bombardier CRJ700", capacity = 78, fuelBurn = 143, speed = 828, range = 3045, price = 24400000),
                      Model("Bombardier CS100", capacity = 133, fuelBurn = 267, speed = 828, range = 5741, price = 71800000),
                      Model("Boeing 737-700C", capacity = 140, fuelBurn = 125, speed = 825, range = 6083, price = 63000000),
                      Model("Boeing 767-300ER", capacity = 350, fuelBurn = 240, speed = 913, range = 11093, price = 141000000),
                      Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = 274, speed = 907, range = 13621, price = 225000000),
                      Model("Boeing 777-300", capacity = 550, fuelBurn = 451, speed = 945, range = 11121, price = 250000000),
                      Model("Boeing 747-400", capacity = 524, fuelBurn = 473, speed = 907, range = 13446, price = 260000000)
        )
    
    ModelSource.saveModels(models)
  }
}