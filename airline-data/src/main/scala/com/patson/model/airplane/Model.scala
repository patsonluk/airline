package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, var id : Int = 0) extends IdObject {
  import Model.Type._
  val airplaneType : Type = {
    capacity match {
      case x if (x <= 15) => LIGHT
      case x if (x <= 60) => REGIONAL
      case x if (x <= 150) => SMALL 
      case x if (x <= 250) => MEDIUM
      case x if (x <= 350) => LARGE
      case _ => JUMBO
    }
  }
  val turnoverTime : Int = {
    airplaneType match {
      case LIGHT => 45
      case REGIONAL => 70
      case SMALL => 100 
      case MEDIUM => 140
      case LARGE => 180
      case JUMBO => 220 
    }
  }
  

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
  
  val models = List(Model("Cessna 421", capacity = 7, fuelBurn = (7 * 1.2).toInt, speed = 300, range = 1555, price = 550000),
                      Model("Cessna Caravan", capacity = 14, fuelBurn = (14 * 1.2).toInt, speed = 344, range = 2400, price = 1600000),
                      Model("Embraer EMB 120 Brasilia", capacity = 30, fuelBurn = (30 * 1.9).toInt, speed = 552, range = 1750, price = 3500000),
                      Model("Embraer ERJ 140", capacity = 44, fuelBurn = (44 * 2.20).toInt, speed = 828, range = 2315, price = 7000000),
                      Model("Bombardier CRJ700", capacity = 78, fuelBurn = (78 * 2.3).toInt, speed = 828, range = 3045, price = 14400000),
                      Model("Bombardier CS100", capacity = 133, fuelBurn = (133 * 2.45).toInt, speed = 828, range = 5741, price = 38000000),
                      Model("Boeing 737-700C", capacity = 140, fuelBurn = (140 * 2.4).toInt, speed = 825, range = 6083, price = 73000000),
                      Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = (250 * 2.55).toInt, speed = 907, range = 13621, price = 125000000),
                      Model("Boeing 767-300ER", capacity = 350, fuelBurn = (350 * 2.55).toInt, speed = 913, range = 11093, price = 181000000),
                      Model("Boeing 777-300", capacity = 550, fuelBurn = (550 * 2.55).toInt, speed = 945, range = 11121, price = 300000000),
                      Model("Boeing 747-400", capacity = 524, fuelBurn = (524 * 2.55).toInt, speed = 907, range = 13446, price = 280000000))
                      
  val modelByName = models.map { model => (model.name, model) }.toMap 
}



