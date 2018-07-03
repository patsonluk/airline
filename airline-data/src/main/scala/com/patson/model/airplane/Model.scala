package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, countryCode : String, var id : Int = 0) extends IdObject {
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
    val modelWithJustId = Model("", 0, 0, 0, 0, 0, 0, 0, countryCode = "")
    modelWithJustId.id = id
    modelWithJustId
  }
  object Type extends Enumeration {
    type Type = Value
    val LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, JUMBO = Value
  }
  //https://en.wikipedia.org/wiki/List_of_jet_airliners
  val models = List(Model("Cessna 421", capacity = 7, fuelBurn = (7 * 1).toInt, speed = 300, range = 1555, price = 550000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US"),
                      Model("Cessna Caravan", capacity = 14, fuelBurn = (14 * 1).toInt, speed = 344, range = 2400, price = 1600000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US"),
                      Model("Embraer EMB 120 Brasilia", capacity = 30, fuelBurn = (30 * 1.9).toInt, speed = 552, range = 1750, price = 3500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR"),
                      Model("Embraer ERJ 140", capacity = 44, fuelBurn = (44 * 2.5).toInt, speed = 828, range = 2315, price = 7000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR"),
                      Model("Bombardier CRJ 200", capacity = 50, fuelBurn = (50 * 1.6).toInt, speed = 830, range = 3150, price = 24000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA"),
                      Model("Bombardier CRJ700", capacity = 78, fuelBurn = (78 * 3).toInt, speed = 828, range = 3045, price = 14400000, lifespan = 35 * 52, constructionTime = 4, countryCode = "CA"),
                      Model("Antonov An148", capacity = 85, fuelBurn = (85 * 3.8).toInt, speed = 835, range = 3500, price = 30000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "UA"),
                      Model("Embrarer EMB 170-200", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 871, range = 2200, price = 50000000, lifespan = 30 * 52, constructionTime = 4, countryCode = "BR"),
                      Model("Comac ARJ21", capacity = 90, fuelBurn = (90 * 3.5).toInt, speed = 828, range = 2200, price = 45000000, lifespan = 25 * 52, constructionTime = 8, countryCode = "CN"),
                      Model("Sukhoi Superjet 100", capacity = 108, fuelBurn = (108 * 4.1).toInt, speed = 828, range = 4578, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "RU"),
                      Model("Airbus A318", capacity = 132, fuelBurn = (132 * 3).toInt, speed = 829, range = 7800, price = 90000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "NL"),
                      Model("Bombardier CS100", capacity = 133, fuelBurn = (133 * 3.5).toInt, speed = 828, range = 5741, price = 80000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA"),
                      Model("Boeing 737-700C", capacity = 140, fuelBurn = (140 * 3.5).toInt, speed = 825, range = 6083, price = 85000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US"),
                      Model("Tupolev Tu-204", capacity = 210, fuelBurn = (210 * 4.5).toInt, speed = 810, range = 4300, price = 50000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU"),
                      Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = (250 * 4.5).toInt, speed = 907, range = 13621, price = 125000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US"),
                      Model("Ilyushin Il-96-300", capacity = 300, fuelBurn = (300 * 5).toInt, speed = 850, range = 11500, price = 60000000, lifespan = 25 * 52, constructionTime = 36, countryCode = "RU"),
                      Model("Boeing 767-300ER", capacity = 350, fuelBurn = (350 * 4.5).toInt, speed = 913, range = 11093, price = 181000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US"),
                      Model("Ilyushin 96-400", capacity = 436, fuelBurn = (436 * 6.4).toInt, speed = 870, range = 10000, price = 50000000, lifespan = 30 * 52, constructionTime = 36, countryCode = "RU"),
                      Model("Airbus A330-300", capacity = 440, fuelBurn = (440 * 5).toInt, speed = 871, range = 11300, price = 250000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "NL"),
                      Model("Airbus A350-900", capacity = 440, fuelBurn = (440 * 5).toInt, speed = 903, range = 15000, price = 280000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "NL"),
                      Model("Boeing 777-300", capacity = 550, fuelBurn = (550 * 5.5).toInt, speed = 945, range = 11121, price = 300000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US"),
                      Model("Boeing 747-400", capacity = 524, fuelBurn = (524 * 5.5).toInt, speed = 907, range = 13446, price = 280000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US"),
                      Model("Airbus A380-900", capacity = 853, fuelBurn = (853 * 6).toInt, speed = 945, range = 15700, price = 420000000, lifespan = 35 * 52, constructionTime = 54, countryCode = "NL"))
                      
  val modelByName = models.map { model => (model.name, model) }.toMap 
}



