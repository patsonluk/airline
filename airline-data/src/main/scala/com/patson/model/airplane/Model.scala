package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, var id : Int = 0) extends IdObject {
  import Model.Type._
  val airplaneType : Type = {
    capacity match {
      case x if (x <= 15) => LIGHT
      case x if (x <= 40) => REGIONAL
      case x if (x <= 150) => SMALL 
      case x if (x <= 250) => MEDIUM
      case x if (x <= 350) => LARGE
      case _ => JUMBO
    }
  }
  val turnoverTime : Int = {
    airplaneType match {
      case LIGHT => 30
      case REGIONAL => 50
      case SMALL => 80 
      case MEDIUM => 120
      case LARGE => 160
      case JUMBO => 200 
    }
  }
  
//  val models = List(Model("Cessna 421", capacity = 7, fuelBurn = 10, speed = 300, range = 1555, price = 550000),
//                      Model("Cessna Caravan", capacity = 14, fuelBurn = 15, speed = 344, range = 2400, price = 1600000),
//                      Model("Embraer EMB 120 Brasilia", capacity = 30, fuelBurn = 54, speed = 552, range = 1750, price = 8500000),
//                      Model("Embraer ERJ 140", capacity = 44, fuelBurn = 81, speed = 828, range = 2315, price = 17000000),
//                      Model("Bombardier CRJ700", capacity = 78, fuelBurn = 143, speed = 828, range = 3045, price = 24400000),
//                      Model("Bombardier CS100", capacity = 133, fuelBurn = 267, speed = 828, range = 5741, price = 71800000),
//                      Model("Boeing 737-700C", capacity = 140, fuelBurn = 125, speed = 825, range = 6083, price = 63000000),
//                      Model("Boeing 767-300ER", capacity = 350, fuelBurn = 240, speed = 913, range = 11093, price = 141000000),
//                      Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = 274, speed = 907, range = 13621, price = 225000000),
//                      Model("Boeing 777-300", capacity = 550, fuelBurn = 451, speed = 945, range = 11121, price = 250000000),
//                      Model("Boeing 747-400", capacity = 524, fuelBurn = 473, speed = 907, range = 13446, price = 260000000)
  
  //weekly fixed cost
  val maintenanceCost : Int = { 
    (capacity * 100).toInt //for now
  }
}

object Model {
  def fromId(id : Int) = {
    val modelWithJustId = Model("", 0, 0, 0, 0, 0)
    modelWithJustId.id = id
    modelWithJustId
  }
  object Type extends Enumeration {
    type Type = Value
    val LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, JUMBO = Value
  }
}



