package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, countryCode : String, series : Series, var id : Int = 0) extends IdObject {
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
  val turnaroundTime : Int = {
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
val models = List(Model("Airbus A300-600", capacity = 266, fuelBurn = (266 * 4.5).toInt, speed = 833, range = 7500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "EU", series = Series("Airbus A300/310-Family")),		
				  Model("Airbus A310-200", capacity = 240, fuelBurn = (240 * 4.1).toInt, speed = 850, range = 6800, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "EU", series = Series("Airbus A300/310-Family")),		
				  Model("Airbus A310-300", capacity = 240, fuelBurn = (240 * 4.1).toInt, speed = 850, range = 8050, price = 107500000, lifespan = 35 * 52, constructionTime = 24, countryCode = "EU", series = Series("Airbus A300/310-Family")),				  
				  Model("Airbus A318", capacity = 132, fuelBurn = (132 * 3.2).toInt, speed = 828, range = 6000, price = 80000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A319", capacity = 156, fuelBurn = (156 * 3.6).toInt, speed = 828, range = 6850, price = 90000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A319-NEO", capacity = 160, fuelBurn = (160 * 3).toInt, speed = 828, range = 6850, price = 100000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A320", capacity = 180, fuelBurn = (180 * 3.6).toInt, speed = 828, range = 6150, price = 100000000, lifespan = 35 * 52, constructionTime = 15, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A320-NEO", capacity = 189, fuelBurn = (189 * 3).toInt, speed = 828, range = 6300, price = 110000000, lifespan = 35 * 52, constructionTime = 15, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A321", capacity = 236, fuelBurn = (236 * 3.6).toInt, speed = 828, range = 5950, price = 120000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A321-NEO", capacity = 244, fuelBurn = (244 * 3).toInt, speed = 828, range = 6850, price = 130000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A321-NEO-LR", capacity = 240, fuelBurn = (240 * 3).toInt, speed = 828, range = 7400, price = 130000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "EU", series = Series("Airbus A320-Family")),	
				  Model("Airbus A321-NEO-XLR", capacity = 236, fuelBurn = (236 * 3).toInt, speed = 828, range = 8700, price = 130000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "EU", series = Series("Airbus A320-Family")),	
			´	  Model("Airbus A330-200", capacity = 404, fuelBurn = (404 * 4.5).toInt, speed = 870, range = 13400, price = 235000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A330-Family")),	
				  Model("Airbus A330-300", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 870, range = 11300, price = 260000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A330-Family")),	
				  Model("Airbus A330-800-NEO", capacity = 406, fuelBurn = (406 * 3.9).toInt, speed = 912, range = 13900, price = 260000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A330-Family")),	
				  Model("Airbus A330-900-NEO", capacity = 440, fuelBurn = (440 * 3.9).toInt, speed = 912, range = 12130, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A330-Family")),			  
				  Model("Airbus A340-200", capacity = 315, fuelBurn = (315 * 4.7).toInt, speed = 880, range = 14800, price = 200000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A340-Family")),		
				  Model("Airbus A340-300", capacity = 350, fuelBurn = (350 * 4.7).toInt, speed = 880, range = 13350, price = 230000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A340-Family")),		
				  Model("Airbus A340-500", capacity = 375, fuelBurn = (375 * 4.3).toInt, speed = 905, range = 16700, price = 260000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A340-Family")),		
				  Model("Airbus A340-600", capacity = 440, fuelBurn = (440 * 4.3).toInt, speed = 905, range = 13900, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A340-Family")),						  
				  Model("Airbus A350-800", capacity = 390, fuelBurn = (390 * 3.8).toInt, speed = 910, range = 15860, price = 280000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A350-Family")),		
				  Model("Airbus A350-900", capacity = 440, fuelBurn = (440 * 3.8).toInt, speed = 910, range = 15000, price = 320000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A350-Family")),		
				  Model("Airbus A350-900ULR", capacity = 440, fuelBurn = (440 * 3.8).toInt, speed = 910, range = 17960, price = 325000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A350-Family")),		
				  Model("Airbus A350-1000", capacity = 475, fuelBurn = (475 * 3.8).toInt, speed = 910, range = 15550, price = 360000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A350-Family")),				  
				  Model("Airbus-A380-800", capacity = 853, fuelBurn = (853 * 6).toInt, speed = 945, range = 15700, price = 450000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "EU", series = Series("Airbus A380-Family")),
				  Model("Antonov An148", capacity = 85, fuelBurn = (85 * 3.8).toInt, speed = 835, range = 3500, price = 30000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "UA" series = Series("Antonov A-Family")),				  
				  Model("ATR 42-600", capacity = 48, fuelBurn = (48 * 1.5).toInt, speed = 556, range = 1326, price = 20000000, lifespan = 20 * 52, constructionTime = 0, countryCode = "FR", series = Series("ATR-Regional-Family")),
				  Model("ATR 72-600", capacity = 78, fuelBurn = (78 * 2.8).toInt, speed = 556, range = 1528, price = 26000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "FR", series = Series("ATR-Regional-Family")),
				  Model("Boeing 717-200", capacity = 134, fuelBurn = (134 * 4.3).toInt, speed = 811, range = 2645, price = 52500000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", series = Series("Boeing 717-Family")),	
				  Model("Boeing 727-200", capacity = 189, fuelBurn = (189 * 4.4).toInt, speed = 811, range = 4020, price = 87500000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 727-Family")),				  
				  Model("Boeing 737-100", capacity = 124, fuelBurn = (124 * 4.3).toInt, speed = 780, range = 3440, price = 50000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-200", capacity = 136, fuelBurn = (136 * 4.3).toInt, speed = 780, range = 4200, price = 55000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-300", capacity = 149, fuelBurn = (149 * 4.1).toInt, speed = 800, range = 4400, price = 7500000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-400", capacity = 188, fuelBurn = (188 * 4.1).toInt, speed = 800, range = 5000, price = 90000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-500", capacity = 140, fuelBurn = (140 * 4.1).toInt, speed = 800, range = 5200, price = 80000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-600", capacity = 149, fuelBurn = (149 * 4).toInt, speed = 830, range = 7200, price = 82500000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-700", capacity = 149, fuelBurn = (149 * 3.9).toInt, speed = 830, range = 7630, price = 85000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-700ER", capacity = 149, fuelBurn = (149 * 4.2).toInt, speed = 830, range = 10200, price = 90000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-800", capacity = 189, fuelBurn = (189 * 4).toInt, speed = 830, range = 6650, price = 100000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-900", capacity = 189, fuelBurn = (189 * 4).toInt, speed = 830, range = 6660, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-900ER", capacity = 220, fuelBurn = (220 * 3.8).toInt, speed = 830, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-MAX-7", capacity = 172, fuelBurn = (172 * 3.3).toInt, speed = 830, range = 6500, price = 95000000, lifespan = 35 * 52, constructionTime = 18, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-MAX-8", capacity = 189, fuelBurn = (189 * 3.4).toInt, speed = 830, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-MAX-9", capacity = 220, fuelBurn = (220 * 3.5).toInt, speed = 830, range = 6500, price = 125000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 737-Family")),	
				  Model("Boeing 737-MAX-10", capacity = 230, fuelBurn = (230 * 3.6).toInt, speed = 830, range = 6500, price = 130000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 737-Family")),			  
				  Model("Boeing-747 100", capacity = 550, fuelBurn = (550 * 5.2).toInt, speed = 895, range = 9800, price = 150000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 200", capacity = 550, fuelBurn = (550 * 5.2).toInt, speed = 895, range = 12700, price = 160000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 SP", capacity = 400, fuelBurn = (400 * 5).toInt, speed = 1000, range = 15400, price = 130000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 300", capacity = 660, fuelBurn = (660 * 5).toInt, speed = 910, range = 12400, price = 245000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 400", capacity = 660, fuelBurn = (660 * 5).toInt, speed = 913, range = 13450, price = 250000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 400ER", capacity = 660, fuelBurn = (660 * 5).toInt, speed = 913, range = 14200, price = 230000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),
				  Model("Boeing-747 8i", capacity = 605, fuelBurn = (605 * 3.8).toInt, speed = 920, range = 14815, price = 350000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 747-Family")),							  
				  Model("Boeing-757 200", capacity = 239, fuelBurn = (239 * 4.3).toInt, speed = 850, range = 7870, price = 95000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 757-Family")),			
				  Model("Boeing-757 200ER", capacity = 239, fuelBurn = (239 * 4.3).toInt, speed = 850, range = 9170, price = 95000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 757-Family")),			
				  Model("Boeing-757 300", capacity = 295, fuelBurn = (295 * 4.7).toInt, speed = 850, range = 6421, price = 130000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 757-Family")),							  
				  Model("Boeing-767 200", capacity = 245, fuelBurn = (245 * 4.1).toInt, speed = 851, range = 7200, price = 140000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 767-Family")),			
				  Model("Boeing-767 200ER", capacity = 245, fuelBurn = (245 * 4.3).toInt, speed = 851, range = 12200, price = 150000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 767-Family")),			
				  Model("Boeing-767 300", capacity = 290, fuelBurn = (295 * 4.5).toInt, speed = 851, range = 7200, price = 17500000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 767-Family")),			
				  Model("Boeing-767 300ER", capacity = 290, fuelBurn = (295 * 4.5).toInt, speed = 851, range = 11070, price = 180000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 767-Family")),			
				  Model("Boeing-767 400ER", capacity = 409, fuelBurn = (295 * 4.7).toInt, speed = 851, range = 10415, price = 225000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 767-Family")),					  
				  Model("Boeing-777 200", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 896, range = 9700, price = 280000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 200ER", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 896, range = 13080, price = 285000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 200LR", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 896, range = 15843, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 300", capacity = 550, fuelBurn = (550 * 5).toInt, speed = 896, range = 11165, price = 335000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 300ER", capacity = 550, fuelBurn = (550 * 5).toInt, speed = 896, range = 13649, price = 340000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 8", capacity = 440, fuelBurn = (440 * 3.8).toInt, speed = 896, range = 16090, price = 340000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),
				  Model("Boeing-777 9", capacity = 550, fuelBurn = (550 * 3.8).toInt, speed = 896, range = 13940, price = 400000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 777-Family")),				  
				  Model("Boeing-787 8", capacity = 250, fuelBurn = (250 * 4.5).toInt, speed = 903, range = 13530, price = 125000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 787-Family")),
				  Model("Boeing-787 9", capacity = 300, fuelBurn = (300 * 4.5).toInt, speed = 903, range = 13950, price = 135000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 787-Family")),
				  Model("Boeing-787 10", capacity = 350, fuelBurn = (350 * 4.5).toInt, speed = 903, range = 11750, price = 145000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", series = Series("Boeing 787-Family")),
				  Model("Bombardier DHC-8-100", capacity = 40, fuelBurn = (40 * 2.2).toInt, speed = 448, range = 1889, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),
				  Model("Bombardier DHC-8-200", capacity = 40, fuelBurn = (40 * 2.2).toInt, speed = 448, range = 1713, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),
				  Model("Bombardier DHC-8-300", capacity = 56, fuelBurn = (56 * 2.5).toInt, speed = 450, range = 1711, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),
				  Model("Bombardier DHC-8-400", capacity = 86, fuelBurn = (86 * 3.2).toInt, speed = 667, range = 2063, price = 40000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),
				  Model("Bombardier Q400", capacity = 86, fuelBurn = (90 * 3.5).toInt, speed = 556, range = 2040, price = 35000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),	  
				  Model("Bombardier CRJ-100", capacity = 50, fuelBurn = (50 * 1.25).toInt, speed = 830, range = 1850, price = 30000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier CRJ-Family")),
				  Model("Bombardier CRJ-200", capacity = 50, fuelBurn = (50 * 1.3).toInt, speed = 830, range = 3148, price = 30000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier CRJ-Family")),
				  Model("Bombardier CRJ-700", capacity = 70, fuelBurn = (70 * 1.5).toInt, speed = 870, range = 3045, price = 35000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "CA", series = Series("Bombardier CRJ-Family")),
				  Model("Bombardier CRJ-900", capacity = 90, fuelBurn = (90 * 1.7).toInt, speed = 885, range = 2876, price = 45000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "CA", series = Series("Bombardier CRJ-Family")),
				  Model("Bombardier CRJ-1000", capacity = 104, fuelBurn = (104 * 1.9).toInt, speed = 870, range = 3004, price = 50000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "CZ", series = Series("Bombardier CRJ-Family")),
				  Model("Bombardier CS100", capacity = 133, fuelBurn = (133 * 3.3).toInt, speed = 828, range = 5741, price = 80000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA", series = Series("Bombardier C-Series")),
				  Model("Bombardier CS300", capacity = 160, fuelBurn = (160 * 3.3).toInt, speed = 828, range = 6112, price = 87500000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA", series = Series("Bombardier C-Series")),
				  Model("Britten-Norman BN-2 Islander", capacity = 9, fuelBurn = (9 * 1.2).toInt, speed = 273, range = 1400, price = 4000000, lifespan = 25 * 52, constructionTime = 0, countryCode = "UK", series = Series("Britten-Norman-Family")),
				  Model("Britten-Norman MKIII Trislander", capacity = 17, fuelBurn = (17 * 1.2).toInt, speed = 350, range = 1600, price = 6000000, lifespan = 25 * 52, constructionTime = 0, countryCode = "UK", series = Series("Britten-Norman-Family")),
				  Model("CASA CN-235", capacity = 40, fuelBurn = (40 * 2.1).toInt, speed = 460, range = 3658, price = 25000000, lifespan = 30 * 52, constructionTime = 0, countryCode = "ES", series = Series("CASA CN-235")),
				  Model("Cessna 421", capacity = 7, fuelBurn = (7 * 1).toInt, speed = 300, range = 1555, price = 550000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US", series = Series("Cessna Family")),
				  Model("Cessna Caravan", capacity = 14, fuelBurn = (14 * 1).toInt, speed = 344, range = 2400, price = 2500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US", series = Series("Cessna Family")),
				  Model("Comac ARJ21", capacity = 90, fuelBurn = (90 * 3.5).toInt, speed = 828, range = 2200, price = 45000000, lifespan = 25 * 52, constructionTime = 8, countryCode = "CN" series = Series("Comac ARJ-Family")),
				  Model("Bombardier DHC-6-400", capacity = 19, fuelBurn = (19 * 1.2).toInt, speed = 337, range = 1480, price = 6500000, lifespan = 30 * 52, constructionTime = 0, countryCode = "CA", series = Series("Bombardier DHC-8-Family")),
			    	  Model("Dornier 328-110", capacity = 33, fuelBurn = (33 * 2).toInt, speed = 620, range = 1310, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "DE", series = Series("Dornier 328-Family")),
				  Model("Dornier 328JET", capacity = 44, fuelBurn = (44 * 1.7).toInt, speed = 740, range = 1665, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "DE", series = Series("Dornier 328-Family")),
				  Model("Embraer EMB120 Brasilia", capacity = 30, fuelBurn = (30 * 1.9).toInt, speed = 552, range = 1750, price = 8000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ135", capacity = 37, fuelBurn = (37 * 2.2).toInt, speed = 850, range = 3241, price = 17500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ140", capacity = 44, fuelBurn = (44 * 2.4).toInt, speed = 850, range = 3056, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ145", capacity = 50, fuelBurn = (50 * 2.6).toInt, speed = 850, range = 2800, price = 22500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ145XR", capacity = 50, fuelBurn = (50 * 2.8).toInt, speed = 850, range = 3700, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ170", capacity = 72, fuelBurn = (72 * 3).toInt, speed = 870, range = 3982, price = 27500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ175", capacity = 78, fuelBurn = (78 * 3.2).toInt, speed = 870, range = 4074, price = 35000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ190", capacity = 100, fuelBurn = (100 * 3.4).toInt, speed = 870, range = 4537, price = 42500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
				  Model("Embraer ERJ195", capacity = 116, fuelBurn = (116 * 3.9).toInt, speed = 870, range = 4260, price = 50000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E-Jet-Family")),
			   	  Model("Embraer E175-E2", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 870, range = 3735, price = 40000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E2-Jet-Family")),
				  Model("Embraer E190-E2", capacity = 106, fuelBurn = (106 * 3.2).toInt, speed = 870, range = 5278, price = 45000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E2-Jet-Family")),
				  Model("Embraer E195-E2", capacity = 132, fuelBurn = (132 * 3.7).toInt, speed = 870, range = 4917, price = 60400000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", series = Series("Embraer E2-Jet-Family")),
				  Model("Fokker 50", capacity = 60, fuelBurn = (60 * 2.6).toInt, speed = 471, range = 2400, price = 55000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", series = Series("Fokker F-Family")),
				  Model("Fokker 70", capacity = 79, fuelBurn = (79 * 2.4).toInt, speed = 845, range = 2010, price = 37500000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", series = Series("Fokker F-Family")),
				  Model("Fokker 70ER", capacity = 79, fuelBurn = (79 * 2.4).toInt, speed = 845, range = 3410, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", series = Series("Fokker F-Family")),
				  Model("Fokker 100", capacity = 109, fuelBurn = (109 * 3.6).toInt, speed = 845, range = 3170, price = 55000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", series = Series("Fokker F-Family")),
				  Model("Ilyushin Il-96-300", capacity = 300, fuelBurn = (300 * 5).toInt, speed = 850, range = 11500, price = 60000000, lifespan = 25 * 52, constructionTime = 36, countryCode = "RU", series = Series("Illyushin 96-Family")),
				  Model("Ilyushin 96-400", capacity = 436, fuelBurn = (436 * 6.4).toInt, speed = 870, range = 10000, price = 50000000, lifespan = 30 * 52, constructionTime = 36, countryCode = "RU", series = Series("Illyushin 96-Family")),
				  Model("Let L-410NG", capacity = 19, fuelBurn = (19 * 1.3).toInt, speed = 417, range = 2570, price = 6000000, lifespan = 25 * 52, constructionTime = 0, countryCode = "RU", series = Series("Let L-410-Family")),
				  Model("Let L-410UVP-E20", capacity = 19, fuelBurn = (19 * 1.2).toInt, speed = 417, range = 1356, price = 5000000, lifespan = 25 * 52, constructionTime = 0, countryCode = "RU", series = Series("Let L-410-Family")),
				  Model("Mitsubishi MRJ-90", capacity = 92, fuelBurn = (92 * 3).toInt, speed = 417, range = 3770, price = 42500000, lifespan = 35 * 52, constructionTime = 6, countryCode = "JP", series = Series("Mitsubishi SpaceJet-Family")),
				  Model("Mitsubishi MRJ-100", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 417, range = 3540, price = 40000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "JP", series = Series("Mitsubishi SpaceJet-Family")),
				  Model("Pilatus PC-12", capacity = 9, fuelBurn = (9 * 1.2).toInt, speed = 528, range = 3417, price = 4000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CH", series = Series("Pilatus-PC-Family")),
				  Model("Saab 340", capacity = 34, fuelBurn = (34 * 2).toInt, speed = 467, range = 1732, price = 12000000, lifespan = 30 * 52, constructionTime = 0, countryCode = "SE", series = Series("Saab Regional-Family")),
				  Model("Saab 2000", capacity = 58, fuelBurn = (58 * 2.5).toInt, speed = 594, range = 2868, price = 22500000, lifespan = 30 * 52, constructionTime = 6, countryCode = "SE", series = Series("Saab Regional-Family")),
				  Model("Short 330-200", capacity = 32, fuelBurn = (32 * 2.5).toInt, speed = 351, range = 875, price = 11000000, lifespan = 30 * 52, constructionTime = 6, countryCode = "SE", series = Series("Saab Regional-Family")),
				  Model("Short 360-200", capacity = 36, fuelBurn = (36 * 2.4).toInt, speed = 393, range = 806, price = 12000000, lifespan = 30 * 52, constructionTime = 6, countryCode = "SE", series = Series("Saab Regional-Family")),
				  Model("Sukhoi Superjet 100", capacity = 108, fuelBurn = (108 * 4.1).toInt, speed = 828, range = 4578, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "RU", series = Series("Sukhoi Superjet-Family")),
				  Model("Tupolev Tu-154", capacity = 180, fuelBurn = (180 * 4.8).toInt, speed = 913, range = 5280, price = 45000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU", series = Series("Tupolev TU-Family")),
				  Model("Tupolev Tu-204", capacity = 210, fuelBurn = (210 * 4.5).toInt, speed = 810, range = 4300, price = 50000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU", series = Series("Tupolev TU-Family")))
  val modelByName = models.map { model => (model.name, model) }.toMap 
}
