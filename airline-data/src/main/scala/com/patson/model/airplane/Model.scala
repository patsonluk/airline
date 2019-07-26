package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, countryCode : String, imageUrl : String = "", var id : Int = 0) extends IdObject {
  import Model.Type._
  val airplaneType : Type = {
    capacity match {
      case x if (x <= 15) => LIGHT
      case x if (x <= 60) => REGIONAL
      case x if (x <= 150) => SMALL 
      case x if (x <= 250) => MEDIUM
      case x if (x <= 350) => LARGE
      case x if (x <= 500) => X_LARGE
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
      case X_LARGE => 200
      case JUMBO => 220 
    }
  }
  
  val minAirportSize : Int = {
    airplaneType match {
      case LIGHT => 1   
      case REGIONAL => 1
      case SMALL => 2
      case MEDIUM => 3
      case LARGE => 4
      case X_LARGE => 5
      case JUMBO => 5
    }
  }
  
  val airplaneTypeLabel : String = {
    airplaneType match {
      case LIGHT => "Light"
      case REGIONAL => "Regional"
      case SMALL => "Small" 
      case MEDIUM => "Medium"
      case LARGE => "Large"
      case X_LARGE => "Extra large"
      case JUMBO => "Jumbo"
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
    val LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, X_LARGE, JUMBO = Value
  }
  //https://en.wikipedia.org/wiki/List_of_jet_airliners
  val models = List(Model("Cessna 421", capacity = 7, fuelBurn = (7 * 1).toInt, speed = 300, range = 1555, price = 550000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US"),
                    Model("Pilatus PC-12", capacity = 9, fuelBurn = (9 * 1.2).toInt, speed = 528, range = 3417, price = 4000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CH"),
                      Model("Cessna Caravan", capacity = 14, fuelBurn = (14 * 1).toInt, speed = 344, range = 2400, price = 2500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US", imageUrl = "https://www.norebbo.com/2017/06/cessna-208-grand-caravan-blank-illustration-templates/"),
                      Model("Embraer EMB120 Brasilia", capacity = 30, fuelBurn = (30 * 1.9).toInt, speed = 552, range = 1750, price = 8000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/02/embraer-120-brasilia-blank-illustration-templates/"),
                      Model("Saab 340", capacity = 34, fuelBurn = (34 * 2).toInt, speed = 467, range = 1732, price = 12000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "SE", imageUrl = "https://www.norebbo.com/2018/12/saab-340b-blank-illustration-templates/"),
                      Model("Embraer ERJ140", capacity = 44, fuelBurn = (44 * 2.5).toInt, speed = 828, range = 2315, price = 15000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-140-blank-illustration-templates/"),
                      Model("ATR 42-600", capacity = 48, fuelBurn = (48 * 1.5).toInt, speed = 556, range = 1326, price = 20000000, lifespan = 20 * 52, constructionTime = 0, countryCode = "FR", imageUrl = "https://www.norebbo.com/2018/06/atr-42-blank-illustration-templates/"),
                      Model("Bombardier CRJ200", capacity = 50, fuelBurn = (50 * 1.6).toInt, speed = 830, range = 3150, price = 30000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
                      Model("Bombardier CRJ700", capacity = 78, fuelBurn = (78 * 3).toInt, speed = 828, range = 3045, price = 42000000, lifespan = 35 * 52, constructionTime = 4, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/05/bombardier-canadair-regional-jet-700-blank-illustration-templates/"),
                      Model("ATR 72-600", capacity = 78, fuelBurn = (78 * 2.8).toInt, speed = 556, range = 1528, price = 26000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "FR", imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
                      Model("Antonov An148", capacity = 85, fuelBurn = (85 * 3.8).toInt, speed = 835, range = 3500, price = 30000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "UA"),
                      Model("Embraer EMB170-200", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 871, range = 2200, price = 50000000, lifespan = 30 * 52, constructionTime = 4, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
                      Model("Comac ARJ21", capacity = 90, fuelBurn = (90 * 3.5).toInt, speed = 828, range = 2200, price = 45000000, lifespan = 25 * 52, constructionTime = 8, countryCode = "CN"),
                      Model("Bombardier Q400", capacity = 90, fuelBurn = (90 * 3.5).toInt, speed = 556, range = 2040, price = 35000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
                      Model("Embraer EMB190", capacity = 100, fuelBurn = (100 * 3.3).toInt, speed = 823, range = 4537, price = 60000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
                      Model("Sukhoi Superjet 100", capacity = 108, fuelBurn = (108 * 4.1).toInt, speed = 828, range = 4578, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "RU", imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
                      Model("Fokker 100", capacity = 109, fuelBurn = (109 * 3.6).toInt, speed = 845, range = 3170, price = 55000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/07/fokker-100-f-28-0100-blank-illustration-templates/"),
                      Model("Airbus A318", capacity = 132, fuelBurn = (132 * 3).toInt, speed = 829, range = 7800, price = 90000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/01/airbus-a318-blank-illustration-templates-with-pratt-whitney-and-cfm56-engines/"),
                      Model("Bombardier CS100", capacity = 133, fuelBurn = (133 * 3.3).toInt, speed = 828, range = 5741, price = 80000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA", imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs100-blank-illustration-templates/"),
                      Model("Boeing 737-700C", capacity = 140, fuelBurn = (140 * 3.3).toInt, speed = 825, range = 6083, price = 85000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
                      Model("McDonnel Douglas MD-90", capacity = 160, fuelBurn = (160 * 3.55).toInt, speed = 811, range = 3787, price = 80000000, lifespan = 30 * 52, constructionTime = 18, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
                      Model("Boeing 737-800", capacity = 184, fuelBurn = (184 * 3.8).toInt, speed = 825, range = 5436, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
                      Model("Airbus A320neo", capacity = 195, fuelBurn = (195 * 4).toInt, speed = 833, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "NL", imageUrl = "https://www.norebbo.com/2017/08/airbus-a320-neo-blank-illustration-templates/"),
                      Model("Boeing 757-200", capacity = 200, fuelBurn = (200 * 4.25).toInt, speed = 854, range = 7250, price = 95000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
                      Model("Tupolev Tu-204", capacity = 210, fuelBurn = (210 * 4.5).toInt, speed = 810, range = 4300, price = 50000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU"),
                      Model("Boeing 737 MAX 9", capacity = 230, fuelBurn = (230 * 4.2).toInt, speed = 839, range = 6570, price = 124000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/05/boeing-737-9-max-blank-illustration-templates/"),
                      Model("Boeing 787-8 Dreamliner", capacity = 250, fuelBurn = (250 * 4.5).toInt, speed = 907, range = 13621, price = 125000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2013/02/boeing-787-8-blank-illustration-templates/"),
                      Model("Airbus A300-600", capacity = 266, fuelBurn = (266 * 4.7).toInt, speed = 833, range = 7500, price = 110000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
                      Model("McDonnel Douglas MD-11", capacity = 293, fuelBurn = (293 * 4.9).toInt, speed = 876, range = 12670, price = 105000000, lifespan = 30 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/05/mcdonnell-douglas-md-11-blank-illustration-templates-with-ge-engines/"),
                      Model("Ilyushin Il-96-300", capacity = 300, fuelBurn = (300 * 5).toInt, speed = 850, range = 11500, price = 60000000, lifespan = 25 * 52, constructionTime = 36, countryCode = "RU"),
                      Model("Boeing 767-300ER", capacity = 350, fuelBurn = (350 * 4.5).toInt, speed = 913, range = 11093, price = 181000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
                      Model("Airbus A340-500", capacity = 375, fuelBurn = (375 * 4.7).toInt, speed = 871, range = 17000, price = 300000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/08/airbus-a340-500-blank-illustration-templates/"),
                      Model("Airbus A330-300", capacity = 380, fuelBurn = (380 * 4.7).toInt, speed = 871, range = 11300, price = 220000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-300-blank-illustration-templates-with-all-three-engine-options/"),
                      Model("Ilyushin 96-400", capacity = 436, fuelBurn = (436 * 6.4).toInt, speed = 870, range = 10000, price = 50000000, lifespan = 30 * 52, constructionTime = 48, countryCode = "RU"),
                      Model("Airbus A350-900", capacity = 440, fuelBurn = (440 * 5).toInt, speed = 903, range = 15000, price = 280000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
                      Model("Boeing 777-300", capacity = 550, fuelBurn = (550 * 5.5).toInt, speed = 945, range = 11121, price = 300000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
                      Model("Boeing 747-400", capacity = 660, fuelBurn = (660 * 5.7).toInt, speed = 945, range = 13446, price = 350000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
                      Model("Airbus A380-800", capacity = 853, fuelBurn = (853 * 6).toInt, speed = 945, range = 15700, price = 450000000, lifespan = 35 * 52, constructionTime = 54, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/06/airbus-a380-800-blank-illustration-templates/"),
                      Model("Airbus A319", capacity = 156, fuelBurn = (156 * 3.6).toInt, speed = 830, range = 3357, price = 95000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "DE", imageUrl = "https://www.norebbo.com/2014/05/airbus-a319-blank-illustration-templates/"))
  
  val modelByName = models.map { model => (model.name, model) }.toMap 
}
