package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline

case class Model(name : String, family : String = "", capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, countryCode : String, imageUrl : String = "", var id : Int = 0) extends IdObject {
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

  def applyDiscount(discounts : List[ModelDiscount]) = {
    var discountedModel = this
    discounts.groupBy(_.discountType).foreach {
      case(discountType, discounts) => discountType match {
        case DiscountType.PRICE =>
          var totalDiscount = discounts.map(_.discount).sum
          totalDiscount = Math.min(Model.MAX_PRICE_DISCOUNT, totalDiscount)
          discountedModel = discountedModel.copy(price = (price * (1 - totalDiscount)).toInt)
        case DiscountType.CONSTRUCTION_TIME =>
          var totalDiscount = discounts.map(_.discount).sum
          totalDiscount = Math.min(1, totalDiscount)
          discountedModel = discountedModel.copy(constructionTime = (constructionTime * (1 - totalDiscount)).toInt)
      }
    }
    discountedModel
  }
}

object Model {
  def fromId(id : Int) = {
    val modelWithJustId = Model("Unknown", "Unknown", 0, 0, 0, 0, 0, 0, 0, countryCode = "")
    modelWithJustId.id = id
    modelWithJustId
  }
  object Type extends Enumeration {
    type Type = Value
    val LIGHT, REGIONAL, SMALL, MEDIUM, LARGE, X_LARGE, JUMBO = Value
  }
  //https://en.wikipedia.org/wiki/List_of_jet_airliners
  val models = List(Model("Cessna 421", "Cessna", capacity = 7, fuelBurn = (7 * 1).toInt, speed = 300, range = 1555, price = 550000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US"),
                    Model("Pilatus PC-12", "Pilatus", capacity = 9, fuelBurn = (9 * 1.2).toInt, speed = 528, range = 3417, price = 1800000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CH"),
                    Model("Britten-Norman BN-2 Islander", "Britten-Norman", capacity = 9, fuelBurn = (9 * 1).toInt, speed = 273, range = 1400, price = 2000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "GB", imageUrl = ""),
                    Model("Cessna Caravan", "Cessna", capacity = 14, fuelBurn = (14 * 1).toInt, speed = 344, range = 2400, price = 2500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "US", imageUrl = "https://www.norebbo.com/2017/06/cessna-208-grand-caravan-blank-illustration-templates/"),
                    Model("Britten-Norman MKIII Trislander", "Britten-Norman", capacity = 17, fuelBurn = (17 * 1.3).toInt, speed = 350, range = 1600, price = 4000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "GB", imageUrl = ""),
                    Model("Bombardier DHC-6-400", "Bombardier DHC-8", capacity = 19, fuelBurn = (19 * 1.2).toInt, speed = 337, range = 1480, price = 5200000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = ""),
                    Model("Let L-410NG", "Let L-410", capacity = 19, fuelBurn = (19 * 2.2).toInt, speed = 417, range = 2570, price = 5000000, lifespan = 25 * 52, constructionTime = 0, countryCode = "RU", imageUrl = ""),
                    Model("Let L-410UVP-E20", "Let L-410", capacity = 19, fuelBurn = (19 * 1.9).toInt, speed = 417, range = 1356, price = 4200000, lifespan = 25 * 52, constructionTime = 0, countryCode = "RU", imageUrl = ""),
                      Model("Embraer EMB120 Brasilia", "Embraer ERJ", capacity = 30, fuelBurn = (30 * 1.9).toInt, speed = 552, range = 1750, price = 8000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/02/embraer-120-brasilia-blank-illustration-templates/"),
                      Model("Short 330-200", "Short", capacity = 32, fuelBurn = (32 * 2.5).toInt, speed = 351, range = 875, price = 11000000, lifespan = 30 * 52, constructionTime = 0, countryCode = "GB", imageUrl = ""),
                      Model("Dornier 328-110", "Dornier 328", capacity = 33, fuelBurn = (33 * 1.8).toInt, speed = 620, range = 1310, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "DE", imageUrl = "https://www.norebbo.com/2019/01/dornier-328-110-blank-illustration-templates/"),
                      Model("Saab 340", "Saab Regional", capacity = 34, fuelBurn = (34 * 2).toInt, speed = 467, range = 1732, price = 12000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "SE", imageUrl = "https://www.norebbo.com/2018/12/saab-340b-blank-illustration-templates/"),
                      Model("Short 360-200", "Short", capacity = 36, fuelBurn = (36 * 2.4).toInt, speed = 393, range = 806, price = 12000000, lifespan = 30 * 52, constructionTime = 0, countryCode = "GB", imageUrl = ""),
                      Model("Embraer ERJ135", "Embraer ERJ", capacity = 37, fuelBurn = (37 * 2.2).toInt, speed = 850, range = 3241, price = 17500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-135-blank-illustration-templates/"),
                      Model("Bombardier DHC-8-100", "Bombardier DHC-8", capacity = 40, fuelBurn = (40 * 2.2).toInt, speed = 448, range = 1889, price = 20000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
                      Model("Bombardier DHC-8-200", "Bombardier DHC-8", capacity = 40, fuelBurn = (40 * 1.9).toInt, speed = 448, range = 1713, price = 22000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
                      Model("CASA CN-235", "CASA CN-235", capacity = 40, fuelBurn = (40 * 2.2).toInt, speed = 460, range = 3658, price = 25000000, lifespan = 30 * 52, constructionTime = 0, countryCode = "ES", imageUrl = ""),
                      Model("Embraer ERJ140", "Embraer ERJ", capacity = 44, fuelBurn = (44 * 2.5).toInt, speed = 828, range = 2315, price = 15000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-140-blank-illustration-templates/"),
                      Model("Dornier 328JET", "Dornier 328", capacity = 44, fuelBurn = (44 * 1.7).toInt, speed = 740, range = 1665, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "DE", imageUrl = "https://www.norebbo.com/2019/01/fairchild-dornier-328jet-illustrations/"),
                      Model("ATR 42-600", "ATR-Regional", capacity = 48, fuelBurn = (48 * 1.5).toInt, speed = 556, range = 1326, price = 20000000, lifespan = 20 * 52, constructionTime = 0, countryCode = "FR", imageUrl = "https://www.norebbo.com/2018/06/atr-42-blank-illustration-templates/"),
                      Model("Bombardier CRJ100", "Bombardier CRJ", capacity = 50, fuelBurn = (50 * 1.6).toInt, speed = 830, range = 1850, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
                      Model("Bombardier CRJ200", "Bombardier CRJ", capacity = 50, fuelBurn = (50 * 1.6).toInt, speed = 830, range = 3150, price = 30000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
                      Model("Embraer ERJ145", "Embraer ERJ", capacity = 50, fuelBurn = (50 * 2.6).toInt, speed = 850, range = 2800, price = 22500000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145-blank-illustration-templates/"),
                      Model("Embraer ERJ145XR", "Embraer ERJ", capacity = 50, fuelBurn = (50 * 2.7).toInt, speed = 850, range = 3700, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "BR", imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145xr-blank-illustration-templates/"),
                      Model("Bombardier DHC-8-300", "Bombardier DHC-8", capacity = 56, fuelBurn = (56 * 2.2).toInt, speed = 450, range = 1711, price = 25000000, lifespan = 35 * 52, constructionTime = 0, countryCode = "CA", imageUrl = "https://www.norebbo.com/2018/05/de-havilland-dhc-8-300-blank-illustration-templates/"),
                      Model("Saab 2000", "Saab Regional", capacity = 58, fuelBurn = (58 * 2.5).toInt, speed = 594, range = 2868, price = 22500000, lifespan = 30 * 52, constructionTime = 4, countryCode = "SE", imageUrl = ""),
                      Model("Fokker 50", "Fokker", capacity = 60, fuelBurn = (60 * 2.6).toInt, speed = 471, range = 2400, price = 55000000, lifespan = 30 * 52, constructionTime = 4, countryCode = "NL", imageUrl = ""),
                      Model("Embraer ERJ170", "Embraer ERJ", capacity = 72, fuelBurn = (72 * 3).toInt, speed = 870, range = 3982, price = 27500000, lifespan = 35 * 52, constructionTime = 4, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
                      Model("Bombardier CRJ700", "Bombardier CRJ", capacity = 78, fuelBurn = (78 * 2.3).toInt, speed = 828, range = 3045, price = 42000000, lifespan = 35 * 52, constructionTime = 4, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/05/bombardier-canadair-regional-jet-700-blank-illustration-templates/"),
                      Model("ATR 72-600", "ATR-Regional", capacity = 78, fuelBurn = (78 * 2.8).toInt, speed = 556, range = 1528, price = 26000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "FR", imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
                      Model("Embraer ERJ175", "Embraer ERJ", capacity = 78, fuelBurn = (78 * 3.2).toInt, speed = 870, range = 4074, price = 35000000, lifespan = 35 * 52, constructionTime = 4, countryCode = "BR"),
                      Model("Fokker 70", "Fokker", capacity = 79, fuelBurn = (79 * 3.4).toInt, speed = 845, range = 2010, price = 37500000, lifespan = 30 * 52, constructionTime = 4, countryCode = "NL", imageUrl = ""),
                      Model("Fokker 70ER", "Fokker", capacity = 79, fuelBurn = (79 * 3.5).toInt, speed = 845, range = 3410, price = 40000000, lifespan = 30 * 52, constructionTime = 4, countryCode = "NL", imageUrl = ""),
                      Model("Antonov An148", "Antonov-A", capacity = 85, fuelBurn = (85 * 3.8).toInt, speed = 835, range = 3500, price = 30000000, lifespan = 20 * 52, constructionTime = 4, countryCode = "UA"),
                      Model("Bombardier DHC-8-400", "Bombardier DHC-8", capacity = 86, fuelBurn = (86 * 2.7).toInt, speed = 667, range = 2063, price = 40000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
                      Model("Bombardier Q400", "Bombardier DHC-8", capacity = 86, fuelBurn = (86 * 2.7).toInt, speed = 556, range = 2040, price = 35000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
                      Model("Embraer EMB170-200", "Embraer ERJ", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 871, range = 2200, price = 50000000, lifespan = 30 * 52, constructionTime = 6, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
                      Model("Embraer E175-E2", "Embraer E-Jet E2", capacity = 88, fuelBurn = (88 * 3).toInt, speed = 870, range = 3735, price = 40000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "BR", imageUrl = "https://www.norebbo.com/2019/03/e175-e2-side-view/"),
                      Model("Mitsubishi MRJ-100", "Mitsubishi SpaceJet", capacity = 88, fuelBurn = (88 * 2.5).toInt, speed = 417, range = 3540, price = 60000000, lifespan = 35 * 52, constructionTime = 6, countryCode = "JP", imageUrl = ""),
                      Model("Comac ARJ21", "Comac ARJ", capacity = 90, fuelBurn = (90 * 3.5).toInt, speed = 828, range = 2200, price = 45000000, lifespan = 25 * 52, constructionTime = 8, countryCode = "CN"),
                      Model("Bombardier Q400", "Bombardier DHC-8", capacity = 90, fuelBurn = (90 * 2.8).toInt, speed = 556, range = 2040, price = 35000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "CA", imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
                      Model("Bombardier CRJ900", "Bombardier CRJ", capacity = 90, fuelBurn = (90 * 2.9).toInt, speed = 885, range = 2876, price = 45000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA", imageUrl = "https://www.norebbo.com/2016/07/bombardier-canadair-regional-jet-900-blank-illustration-templates/"),
                      Model("Mitsubishi MRJ-90", "Mitsubishi SpaceJet", capacity = 92, fuelBurn = (92 * 2.5).toInt, speed = 417, range = 3770, price = 75000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "JP", imageUrl = ""),
                      Model("Embraer EMB190", "Embraer ERJ", capacity = 100, fuelBurn = (100 * 3.3).toInt, speed = 823, range = 4537, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "BR", imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
                      Model("Bombardier CRJ1000", "Bombardier CRJ", capacity = 104, fuelBurn = (104 * 3).toInt, speed = 870, range = 3004, price = 50000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CZ", imageUrl = "https://www.norebbo.com/2019/06/bombardier-crj-1000-side-view/"),
                      Model("Embraer E190-E2", "Embraer E-Jet E2", capacity = 106, fuelBurn = (106 * 3.5).toInt, speed = 870, range = 5278, price = 45000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "BR", imageUrl = "https://www.norebbo.com/2019/03/e190-e2-blank-side-view/"),
                      Model("Sukhoi Superjet 100", "Sukhoi Superjet", capacity = 108, fuelBurn = (108 * 4.1).toInt, speed = 828, range = 4578, price = 40000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "RU", imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
                      Model("Fokker 100", "Fokker", capacity = 109, fuelBurn = (109 * 3.9).toInt, speed = 845, range = 3170, price = 55000000, lifespan = 30 * 52, constructionTime = 8, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/07/fokker-100-f-28-0100-blank-illustration-templates/"),
                      Model("Boeing 737-100", "Boeing 737 Original", capacity = 124, fuelBurn = (124 * 4).toInt, speed = 780, range = 3440, price = 40000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/10/boeing-737-100-blank-illustration-templates/"),
                      Model("Airbus A318", "Airbus A320", capacity = 132, fuelBurn = (132 * 3).toInt, speed = 829, range = 7800, price = 90000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/01/airbus-a318-blank-illustration-templates-with-pratt-whitney-and-cfm56-engines/"),
                      Model("Bombardier CS100", "Bombardier CS", capacity = 133, fuelBurn = (133 * 3.3).toInt, speed = 828, range = 5741, price = 80000000, lifespan = 35 * 52, constructionTime = 8, countryCode = "CA", imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs100-blank-illustration-templates/"),
                      Model("Boeing 717-200", "Boeing 717", capacity = 134, fuelBurn = (134 * 4.3).toInt, speed = 811, range = 2645, price = 52500000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", imageUrl = "https://www.norebbo.com/2017/06/boeing-717-200-blank-illustration-templates/"),
                      Model("Boeing 737-200", "Boeing 737 Original", capacity = 136, fuelBurn = (136 * 4.3).toInt, speed = 780, range = 4200, price = 59000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/09/boeing-737-200-blank-illustration-templates/"),
                      Model("Boeing 737-500", "Boeing 737 Classic", capacity = 140, fuelBurn = (140 * 4.1).toInt, speed = 800, range = 5200, price = 80000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/09/boeing-737-500-blank-illustration-templates-with-and-without-blended-winglets/"),
                      Model("Boeing 737-700C", "Boeing 737 Next Generation", capacity = 140, fuelBurn = (140 * 3.3).toInt, speed = 825, range = 6083, price = 85000000, lifespan = 35 * 52, constructionTime = 12, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
                      Model("Embraer E195-E2", "Embraer E-Jet E2", capacity = 146, fuelBurn = (146 * 3.7).toInt, speed = 833, range = 4800, price = 60400000, lifespan = 30 * 52, constructionTime = 12, countryCode = "BR", imageUrl = "https://www.norebbo.com/2019/03/embraer-e195-e2-side-view/"),
                      Model("Boeing 737-300", "Boeing 737 Classic", capacity = 149, fuelBurn = (149 * 4.1).toInt, speed = 800, range = 4400, price = 75000000, lifespan = 35 * 52, constructionTime = 16, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/09/boeing-737-300-blank-illustration-templates/"),
                      Model("Boeing 737-600", "Boeing 737 Next Generation", capacity = 149, fuelBurn = (149 * 3.9).toInt, speed = 830, range = 7200, price = 82500000, lifespan = 35 * 52, constructionTime = 16, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/09/boeing-737-600-blank-illustration-templates/"),
                      Model("Boeing 737-700", "Boeing 737 Next Generation", capacity = 149, fuelBurn = (149 * 3.8).toInt, speed = 830, range = 7630, price = 85000000, lifespan = 35 * 52, constructionTime = 16, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
                      Model("Boeing 737-700ER", "Boeing 737 Next Generation", capacity = 149, fuelBurn = (149 * 4.2).toInt, speed = 830, range = 10200, price = 90000000, lifespan = 35 * 52, constructionTime = 16, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
                      Model("McDonnel Douglas MD-81", "McDonnel Douglas", capacity = 150, fuelBurn = (150 * 3.5).toInt, speed = 811, range = 3787, price = 75000000, lifespan = 30 * 52, constructionTime = 16, countryCode = "US",imageUrl = "https://www.norebbo.com/2015/02/mcdonnell-douglas-md-80-blank-illustration-templates/"),
                      Model("Airbus A319", "Airbus A320", capacity = 156, fuelBurn = (156 * 3.4).toInt, speed = 830, range = 6940, price = 95000000, lifespan = 35 * 52, constructionTime = 16, countryCode = "NL", imageUrl = "https://www.norebbo.com/2014/05/airbus-a319-blank-illustration-templates/"),
                      Model("McDonnel Douglas MD-90", "McDonnel Douglas", capacity = 160, fuelBurn = (160 * 3.55).toInt, speed = 811, range = 3787, price = 80000000, lifespan = 30 * 52, constructionTime = 20, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
                      Model("Airbus A319neo", "Airbus A320", capacity = 160, fuelBurn = (160 * 3).toInt, speed = 828, range = 6850, price = 100000000, lifespan = 35 * 52, constructionTime = 20, countryCode = "NL", imageUrl = "https://www.norebbo.com/2017/09/airbus-a319-neo-blank-illustration-templates/"),
                      Model("Bombardier CS300", "Bombardier CS", capacity = 160, fuelBurn = (160 * 3.3).toInt, speed = 828, range = 6112, price = 87500000, lifespan = 35 * 52, constructionTime = 20, countryCode = "CA", imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs300-blank-illustration-templates/"),
                      Model("Boeing 737 MAX 7", "Boeing 737 MAX", capacity = 172, fuelBurn = (172 * 3.3).toInt, speed = 830, range = 6500, price = 95000000, lifespan = 35 * 52, constructionTime = 20, countryCode = "US", imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-7-blank-illustration-templates/"),
                      Model("Tupolev Tu-154", "Tupolev Tu", capacity = 180, fuelBurn = (180 * 4.8).toInt, speed = 913, range = 5280, price = 45000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU", imageUrl = "https://www.norebbo.com/2019/05/tupolev-tu-154-side-view/"),
                      Model("Airbus A320", "Airbus A320", capacity = 180, fuelBurn = (180 * 3.6).toInt, speed = 828, range = 6150, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/08/airbus-a320-blank-illustration-templates/"),
                      Model("Boeing 737-800", "Boeing 737 Next Generation", capacity = 184, fuelBurn = (184 * 3.8).toInt, speed = 825, range = 5436, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
                      Model("Boeing 737-400", "Boeing 737 Classic", capacity = 188, fuelBurn = (188 * 4.1).toInt, speed = 800, range = 5000, price = 90000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/09/boeing-737-400-blank-illustration-templates/"),
                      Model("Boeing 727-200", "Boeing 727", capacity = 189, fuelBurn = (189 * 4.4).toInt, speed = 811, range = 4020, price = 87500000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/03/boeing-727-200-blank-illustration-templates/"),
                      Model("Boeing 737-800", "Boeing 737 Next Generation", capacity = 189, fuelBurn = (189 * 3.7).toInt, speed = 830, range = 6650, price = 100000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
                      Model("Boeing 737-900", "Boeing 737 Next Generation", capacity = 189, fuelBurn = (189 * 3.6).toInt, speed = 830, range = 6660, price = 105000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/08/boeing-737-900-blank-illustration-templates/"),
                      Model("Boeing 737 MAX 8", "Boeing 737 MAX", capacity = 189, fuelBurn = (189 * 3.4).toInt, speed = 830, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
                      Model("Airbus A320neo", "Airbus A320", capacity = 195, fuelBurn = (195 * 4).toInt, speed = 833, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "NL", imageUrl = "https://www.norebbo.com/2017/08/airbus-a320-neo-blank-illustration-templates/"),
                      Model("Boeing 757-200", "Boeing 757", capacity = 200, fuelBurn = (200 * 4.25).toInt, speed = 854, range = 7250, price = 95000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
                      Model("Tupolev Tu-204", "Tupolev Tu", capacity = 210, fuelBurn = (210 * 4.5).toInt, speed = 810, range = 4300, price = 50000000, lifespan = 25 * 52, constructionTime = 24, countryCode = "RU"),
                      Model("Boeing 737-900ER", "Boeing 737 Next Generation", capacity = 220, fuelBurn = (220 * 4).toInt, speed = 830, range = 6500, price = 110000000, lifespan = 35 * 52, constructionTime = 24, countryCode = "US", imageUrl = "https://www.norebbo.com/2016/07/boeing-737-900er-with-split-scimitar-winglets-blank-illustration-templates/"),
                      Model("Boeing 737 MAX 9", "Boeing 737 MAX", capacity = 230, fuelBurn = (230 * 3.5).toInt, speed = 839, range = 6570, price = 134000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/05/boeing-737-9-max-blank-illustration-templates/"),
                      Model("Boeing 737 MAX 10", "Boeing 737 MAX", capacity = 230, fuelBurn = (230 * 3.6).toInt, speed = 830, range = 6500, price = 144000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/01/737-10-max-side-view/"),
                      Model("Airbus A321", "Airbus A320", capacity = 236, fuelBurn = (236 * 4.3).toInt, speed = 830, range = 5930, price = 120000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2014/03/airbus-a321-blank-illustration-templates/"),
                      Model("Airbus A321neoXLR", "Airbus A320", capacity = 236, fuelBurn = (236 * 4.4).toInt, speed = 828, range = 8700, price = 130000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
                      Model("Boeing 757-200ER", "Boeing 757", capacity = 239, fuelBurn = (239 * 4.3).toInt, speed = 850, range = 9170, price = 135000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
                      Model("Airbus A310-200", "Airbus A300/A310", capacity = 240, fuelBurn = (240 * 4.4).toInt, speed = 850, range = 6800, price = 125000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
                      Model("Airbus A310-300", "Airbus A300/A310", capacity = 240, fuelBurn = (240 * 4.3).toInt, speed = 850, range = 8050, price = 130000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
                      Model("Airbus A321neoLR", "Airbus A320", capacity = 240, fuelBurn = (240 * 3.8).toInt, speed = 828, range = 7400, price = 132000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
                      Model("Airbus A321neo", "Airbus A320", capacity = 244, fuelBurn = (244 * 3.7).toInt, speed = 828, range = 6850, price = 130000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2017/09/airbus-a321-neo-blank-illustration-templates/"),
                      Model("Boeing 767-200", "Boeing 767", capacity = 245, fuelBurn = (245 * 4.1).toInt, speed = 851, range = 7200, price = 140000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
                      Model("Boeing 767-200ER", "Boeing 767", capacity = 245, fuelBurn = (245 * 4.3).toInt, speed = 851, range = 12200, price = 150000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
                      Model("Boeing 787-8 Dreamliner", "Boeing 787", capacity = 250, fuelBurn = (250 * 4.5).toInt, speed = 907, range = 13621, price = 125000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2013/02/boeing-787-8-blank-illustration-templates/"),
                      Model("Airbus A300-600", "Airbus A300/A310", capacity = 266, fuelBurn = (266 * 4.4).toInt, speed = 833, range = 7500, price = 110000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
                      Model("Boeing 767-300", "Boeing 767", capacity = 290, fuelBurn = (295 * 4.5).toInt, speed = 851, range = 7200, price = 137500000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
                      Model("McDonnel Douglas MD-11", "McDonnel Douglas MD", capacity = 293, fuelBurn = (293 * 4.9).toInt, speed = 876, range = 12670, price = 105000000, lifespan = 30 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2018/05/mcdonnell-douglas-md-11-blank-illustration-templates-with-ge-engines/"),
                      Model("Boeing 757-300", "Boeing 757", capacity = 295, fuelBurn = (295 * 4.7).toInt, speed = 850, range = 6421, price = 130000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2017/03/boeing-757-300-blank-illustration-templates/"),
                      Model("Ilyushin Il-96-300", "Ilyushin Il-96", capacity = 300, fuelBurn = (300 * 5).toInt, speed = 850, range = 11500, price = 60000000, lifespan = 25 * 52, constructionTime = 36, countryCode = "RU"),
                      Model("Boeing 787-9 Dreamliner", "Boeing 787", capacity = 300, fuelBurn = (300 * 4.5).toInt, speed = 903, range = 13950, price = 135000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
                      Model("Airbus A340-200", "Airbus A340", capacity = 315, fuelBurn = (315 * 4.7).toInt, speed = 880, range = 14800, price = 200000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2019/01/airbus-a340-200-side-view/"),
                      Model("Airbus A340-300", "Airbus A340", capacity = 350, fuelBurn = (350 * 4.7).toInt, speed = 880, range = 13350, price = 230000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/04/airbus-340-300-and-a340-300x-blank-illustration-templates/"),
                      Model("Boeing 767-300ER", "Boeing 767", capacity = 350, fuelBurn = (350 * 4.5).toInt, speed = 913, range = 11093, price = 181000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
                      Model("Boeing 787-10 Dreamliner", "Boeing 787", capacity = 360, fuelBurn = (360 * 4.4).toInt, speed = 903, range = 11750, price = 215000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
                      Model("Airbus A340-500", "Airbus A340", capacity = 375, fuelBurn = (375 * 4.7).toInt, speed = 871, range = 17000, price = 300000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/08/airbus-a340-500-blank-illustration-templates/"),
                      Model("Airbus A330-300", "Airbus A330", capacity = 380, fuelBurn = (380 * 4.7).toInt, speed = 871, range = 11300, price = 220000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-300-blank-illustration-templates-with-all-three-engine-options/"),
                      Model("Airbus A350-800", "Airbus A350", capacity = 390, fuelBurn = (390 * 3.8).toInt, speed = 910, range = 15860, price = 280000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2015/06/airbus-a350-800-blank-illustration-templates/"),
                      Model("Boeing 747SP", "Boeing 747", capacity = 400, fuelBurn = (400 * 5).toInt, speed = 1000, range = 15400, price = 180000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/08/boeing-747sp-side-view/"),
                      Model("Airbus A330-200", "Airbus A330", capacity = 404, fuelBurn = (404 * 4.7).toInt, speed = 870, range = 13400, price = 235000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-200-blank-illustration-templates-with-pratt-whitney-engines/"),
                      Model("Airbus A330-800neo", "Airbus A330", capacity = 406, fuelBurn = (406 * 4.5).toInt, speed = 912, range = 13900, price = 260000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-800-neo-blank-illustration-templates/"),
                      Model("Boeing 767-400ER", "Boeing 767", capacity = 409, fuelBurn = (409 * 4.7).toInt, speed = 851, range = 10415, price = 225000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/07/boeing-767-400-blank-illustration-templates/"),
                      Model("Ilyushin 96-400", "Ilyushin Il-96", capacity = 436, fuelBurn = (436 * 6.4).toInt, speed = 870, range = 10000, price = 90000000, lifespan = 25 * 52, constructionTime = 36, countryCode = "RU"),
                      Model("Airbus A350-900", "Airbus A350", capacity = 440, fuelBurn = (440 * 4.9).toInt, speed = 903, range = 15000, price = 280000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
                      Model("Airbus A330-900neo", "Airbus A330", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 912, range = 12130, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-900-neo-blank-illustration-templates/"),
                      Model("Airbus A340-600", "Airbus A340", capacity = 440, fuelBurn = (440 * 5.2).toInt, speed = 905, range = 13900, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2016/11/airbus-a340-600-blank-illustration-templates/"),
                      Model("Airbus A350-900ULR", "Airbus A350", capacity = 440, fuelBurn = (440 * 5.2).toInt, speed = 910, range = 17960, price = 325000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
                      Model("Boeing 777-200", "Boeing 777", capacity = 440, fuelBurn = (440 * 4.5).toInt, speed = 896, range = 9700, price = 280000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
                      Model("Boeing 777-200ER", "Boeing 777", capacity = 440, fuelBurn = (440 * 4.6).toInt, speed = 896, range = 13080, price = 285000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
                      Model("Boeing 777-200LR", "Boeing 777", capacity = 440, fuelBurn = (440 * 4.7).toInt, speed = 896, range = 15843, price = 290000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
                      Model("Boeing 777-8", "Boeing 777", capacity = 440, fuelBurn = (440 * 4.4).toInt, speed = 896, range = 16090, price = 340000000, lifespan = 35 * 52, constructionTime = 36, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/12/boeing-777-8-side-view/"),
                      Model("Airbus A350-1000", "Airbus A350", capacity = 475, fuelBurn = (475 * 4.5).toInt, speed = 910, range = 15550, price = 360000000, lifespan = 35 * 52, constructionTime = 40, countryCode = "NL", imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
                      Model("Boeing 747-100", "Boeing 747", capacity = 550, fuelBurn = (550 * 5.7).toInt, speed = 895, range = 9800, price = 250000000, lifespan = 35 * 52, constructionTime = 44, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/07/boeing-747-100-side-view/"),
                      Model("Boeing 747-200", "Boeing 747", capacity = 550, fuelBurn = (550 * 5.7).toInt, speed = 895, range = 12700, price = 260000000, lifespan = 35 * 52, constructionTime = 44, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/08/boeing-747-200-side-view/"),
                      Model("Boeing 777-300", "Boeing 777", capacity = 550, fuelBurn = (550 * 5.6).toInt, speed = 945, range = 11121, price = 300000000, lifespan = 35 * 52, constructionTime = 44, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
                      Model("Boeing 777-300ER", "Boeing 777", capacity = 550, fuelBurn = (550 * 5.7).toInt, speed = 896, range = 13649, price = 340000000, lifespan = 35 * 52, constructionTime = 44, countryCode = "US", imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
                      Model("Boeing 777-9", "Boeing 777", capacity = 550, fuelBurn = (550 * 4.9).toInt, speed = 896, range = 13940, price = 450000000, lifespan = 35 * 52, constructionTime = 44, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/12/boeing-777-9-side-view/"),
                      Model("Boeing 747-8i", "Boeing 747", capacity = 605, fuelBurn = (605 * 5.2).toInt, speed = 920, range = 14815, price = 420000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2015/12/boeing-747-8i-blank-illustration-templates/"),
                      Model("Boeing 747-300", "Boeing 747", capacity = 660, fuelBurn = (660 * 6.2).toInt, speed = 910, range = 12400, price = 290000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2019/10/boeing-747-300-side-view/"),
                      Model("Boeing 747-400", "Boeing 747", capacity = 660, fuelBurn = (660 * 6.1).toInt, speed = 945, range = 13446, price = 350000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
                      Model("Boeing 747-400ER", "Boeing 747", capacity = 660, fuelBurn = (660 * 6.2).toInt, speed = 913, range = 14200, price = 350000000, lifespan = 35 * 52, constructionTime = 48, countryCode = "US", imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
                      Model("Airbus A380-800", "Airbus A380", capacity = 853, fuelBurn = (853 * 6).toInt, speed = 945, range = 15700, price = 450000000, lifespan = 35 * 52, constructionTime = 54, countryCode = "NL", imageUrl = "https://www.norebbo.com/2013/06/airbus-a380-800-blank-illustration-templates/"))

  val modelByName = models.map { model => (model.name, model) }.toMap

  val MAX_PRICE_DISCOUNT = 0.2 //at most 20% off
}


