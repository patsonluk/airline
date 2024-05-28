package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline
import com.patson.model.airplane.Model.Category
import com.patson.util.AirplaneModelCache

case class Model(name : String, family : String = "", capacity : Int, quality : Double, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, manufacturer: Manufacturer, runwayRequirement : Int, imageUrl : String = "", var id : Int = 0) extends IdObject {
  import Model.Type._

  val countryCode = manufacturer.countryCode
  val SUPERSONIC_SPEED_THRESHOLD = 1236
  val airplaneType : Type = {
    if (speed > SUPERSONIC_SPEED_THRESHOLD) {
      SUPERSONIC
    } else {
      capacity match {
        case x if (x <= 15) => LIGHT
        case x if (x <= 70) => SMALL
        case x if (x <= 150) => REGIONAL
        case x if (x <= 250) => MEDIUM
        case x if (x <= 350) => LARGE
        case x if (x <= 500) => X_LARGE
        case _ => JUMBO
      }
    }
  }
  val category = Category.fromType(airplaneType)

  private[this]val BASE_TURNAROUND_TIME = 40
  val turnaroundTime : Int = {
    BASE_TURNAROUND_TIME +
      (airplaneType match {
        case LIGHT => capacity / 3 //45 - old value
        case SMALL =>  capacity / 3 //70
        case REGIONAL => capacity / 3 //100
        case MEDIUM =>  capacity / 2.5 //140
        case LARGE => capacity / 2.5 //180
        case X_LARGE => capacity / 2.5 //200
        case JUMBO => capacity / 2.5 //220
        case SUPERSONIC => capacity / 2.5
      }).toInt
  }

  val airplaneTypeLabel : String = label(airplaneType)

  //weekly fixed cost
  val baseMaintenanceCost : Int = {
    (capacity * 150).toInt //for now
  }

  def applyDiscount(discounts : List[ModelDiscount]) = {
    var discountedModel = this
    discounts.groupBy(_.discountType).foreach {
      case (discountType, discounts) => discountType match {
        case DiscountType.PRICE =>
          val totalDiscount = discounts.map(_.discount).sum
          discountedModel = discountedModel.copy(price = (price * (1 - totalDiscount)).toInt)
        case DiscountType.CONSTRUCTION_TIME =>
          var totalDiscount = discounts.map(_.discount).sum
          totalDiscount = Math.min(1, totalDiscount)
          discountedModel = discountedModel.copy(constructionTime = (constructionTime * (1 - totalDiscount)).toInt)
      }
    }
    discountedModel
  }

  val purchasableWithRelationship = (relationship : Int) => {
    relationship >= Model.BUY_RELATIONSHIP_THRESHOLD
  }
}

object Model {
  val BUY_RELATIONSHIP_THRESHOLD = 0

  def fromId(id : Int) = {
    val modelWithJustId = Model("Unknown", "Unknown", 0, 0, 0, 0, 0, 0, 0, 0, Manufacturer("Unknown", countryCode = ""), runwayRequirement = 0)
    modelWithJustId.id = id
    modelWithJustId
  }

  object Type extends Enumeration {
    type Type = Value
    val LIGHT, SMALL, REGIONAL, MEDIUM, LARGE, X_LARGE, JUMBO, SUPERSONIC = Value

    val label = (airplaneType : Type) => { airplaneType match {
        case LIGHT => "Light"
        case SMALL => "Small"
        case REGIONAL => "Regional"
        case MEDIUM => "Medium"
        case LARGE => "Large"
        case X_LARGE => "Extra large"
        case JUMBO => "Jumbo"
        case SUPERSONIC => "Supersonic"
      }
    }
  }

  object Category extends Enumeration {
    type Category = Value
    val LIGHT, REGIONAL, MEDIUM, LARGE, SUPERSONIC = Value
    val grouping = Map(
      LIGHT -> List(Type.LIGHT, Type.SMALL),
      REGIONAL -> List(Type.REGIONAL),
      MEDIUM -> List(Type.MEDIUM),
      LARGE -> List(Type.LARGE, Type.X_LARGE, Type.JUMBO),
      SUPERSONIC -> List(Type.SUPERSONIC)
    )

    val fromType = (airplaneType : Type.Value) => {
      grouping.find(_._2.contains(airplaneType)).get._1
    }

    val capacityRange : Map[Category.Value, (Int, Int)]= {
      AirplaneModelCache.allModels.map(_._2).groupBy(_.category).view.mapValues { models =>
        val sortedByCapacity = models.toList.sortBy(_.capacity)
        (sortedByCapacity.head.capacity, sortedByCapacity.last.capacity)
      }.toMap
    }

    def getCapacityRange(category: Category.Value) = {
      capacityRange.get(category).getOrElse((0, 0))
    }

  }

  val models = List(
    Model("Airbus A220-100", "Airbus A220", 133, 7, 425, 828, 5741, 104300000, 1820, 42, Manufacturer("Airbus", countryCode = "CA"), 1463, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs100-blank-illustration-templates/"),
    Model("Airbus A220-300", "Airbus A220", 160, 7, 544, 828, 6112, 127200000, 1820, 60, Manufacturer("Airbus", countryCode = "CA"), 1890, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs300-blank-illustration-templates/"),
    Model("Airbus A220-500", "Airbus A220", 190, 7, 830, 828, 6000, 134700000, 1820, 60, Manufacturer("Airbus", countryCode = "NL"), 2000, imageUrl = ""),
    Model("Airbus A300-600", "Airbus A300/A310", 266, 5, 1223, 833, 7500, 130200000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2400, imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
    Model("Airbus A300B4", "Airbus A300/A310", 345, 5, 1660, 847, 5375, 211200000, 1820, 96, Manufacturer("Airbus", countryCode = "NL"), 1950, imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
    Model("Airbus A310-200", "Airbus A300/A310", 240, 6, 1086, 850, 6800, 152000000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 1860, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
    Model("Airbus A310-300", "Airbus A300/A310", 240, 6, 1032, 850, 8050, 157000000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2000, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
    Model("Airbus A318", "Airbus A320", 132, 6, 435, 829, 7800, 104900000, 1820, 24, Manufacturer("Airbus", countryCode = "NL"), 1780, imageUrl = "https://www.norebbo.com/airbus-a318-blank-illustration-templates-with-pratt-whitney-and-cfm56-engines/"),
    Model("Airbus A319", "Airbus A320", 156, 6, 530, 830, 6940, 112600000, 1820, 48, Manufacturer("Airbus", countryCode = "NL"), 1850, imageUrl = "https://www.norebbo.com/2014/05/airbus-a319-blank-illustration-templates/"),
    Model("Airbus A319neo", "Airbus A320", 160, 7, 560, 828, 6850, 129200000, 1820, 60, Manufacturer("Airbus", countryCode = "NL"), 2164, imageUrl = "https://www.norebbo.com/2017/09/airbus-a319-neo-blank-illustration-templates/"),
    Model("Airbus A320", "Airbus A320", 180, 6, 684, 828, 6150, 120300000, 1820, 72, Manufacturer("Airbus", countryCode = "NL"), 2100, imageUrl = "https://www.norebbo.com/2013/08/airbus-a320-blank-illustration-templates/"),
    Model("Airbus A320neo", "Airbus A320", 195, 7, 780, 833, 6500, 145600000, 1820, 72, Manufacturer("Airbus", countryCode = "NL"), 2100, imageUrl = "https://www.norebbo.com/2017/08/airbus-a320-neo-blank-illustration-templates/"),
    Model("Airbus A321", "Airbus A320", 236, 6, 1014, 830, 5930, 146600000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2210, imageUrl = "https://www.norebbo.com/2014/03/airbus-a321-blank-illustration-templates/"),
    Model("Airbus A321neo", "Airbus A320", 244, 7, 1000, 828, 6850, 174500000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 1988, imageUrl = "https://www.norebbo.com/2017/09/airbus-a321-neo-blank-illustration-templates/"),
    Model("Airbus A321neoLR", "Airbus A320", 240, 7, 996, 828, 7400, 175800000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2300, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
    Model("Airbus A321neoXLR", "Airbus A320", 236, 7, 1038, 828, 8700, 176100000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2400, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
    Model("Airbus A330-200", "Airbus A330", 380, 6, 1786, 871, 11300, 262800000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2770, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-200-blank-illustration-templates-with-pratt-whitney-engines/"),
    Model("Airbus A330-300", "Airbus A330", 404, 6, 1898, 870, 13400, 280500000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2770, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-300-blank-illustration-templates-with-all-three-engine-options/"),
    Model("Airbus A330-800neo", "Airbus A330", 406, 6, 1827, 912, 13900, 305700000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2770, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-800-neo-blank-illustration-templates/"),
    Model("Airbus A330-900neo", "Airbus A330", 440, 6, 1980, 912, 12130, 339500000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2770, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-900-neo-blank-illustration-templates/"),
    Model("Airbus A340-200", "Airbus A340", 315, 5, 1480, 880, 14800, 223900000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2900, imageUrl = "https://www.norebbo.com/2019/01/airbus-a340-200-side-view/"),
    Model("Airbus A340-300", "Airbus A340", 350, 5, 1645, 880, 13350, 256600000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 3000, imageUrl = "https://www.norebbo.com/2016/04/airbus-340-300-and-a340-300x-blank-illustration-templates/"),
    Model("Airbus A340-500", "Airbus A340", 375, 5, 1762, 871, 17000, 298500000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 3350, imageUrl = "https://www.norebbo.com/2016/08/airbus-a340-500-blank-illustration-templates/"),
    Model("Airbus A340-600", "Airbus A340", 440, 5, 2288, 905, 13900, 323400000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 3400, imageUrl = "https://www.norebbo.com/2016/11/airbus-a340-600-blank-illustration-templates/"),
    Model("Airbus A350-1000", "Airbus A350", 475, 7, 2205, 910, 15050, 446700000, 1820, 120, Manufacturer("Airbus", countryCode = "NL"), 2600, imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
    Model("Airbus A350-1000 Sunrise", "Airbus A350", 380, 9, 2126, 910, 18000, 579100000, 1820, 120, Manufacturer("Airbus", countryCode = "NL"), 2700, imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
    Model("Airbus A350-900", "Airbus A350", 440, 7, 2196, 903, 15000, 360300000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2600, imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
    Model("Airbus A350-900ULR", "Airbus A350", 440, 7, 2288, 910, 17960, 405300000, 1820, 108, Manufacturer("Airbus", countryCode = "NL"), 2600, imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
    Model("Airbus A380-800", "Airbus A380", 853, 7, 5318, 925, 15700, 605600000, 1820, 162, Manufacturer("Airbus", countryCode = "NL"), 3000, imageUrl = "https://www.norebbo.com/2013/06/airbus-a380-800-blank-illustration-templates/"),
    Model("Airbus ZeroE Turbofan", "Airbus ZE", 175, 8, 150, 795, 2000, 273200000, 1040, 60, Manufacturer("Airbus", countryCode = "NL"), 2200, imageUrl = ""),
    Model("Airbus ZeroE Turboprop", "Airbus ZE", 85, 8, 70, 680, 1000, 111300000, 1040, 72, Manufacturer("Airbus", countryCode = "NL"), 2000, imageUrl = ""),
    Model("Airlander 10 Cruise", "HAV Airlander", 84, 10, 15, 102, 3700, 39200000, 780, 60, Manufacturer("HAV Airlander", countryCode = "GB"), 0, imageUrl = ""),
    Model("Airlander 10 Ferry", "HAV Airlander", 110, 9, 21, 102, 1600, 36100000, 780, 60, Manufacturer("HAV Airlander", countryCode = "GB"), 0, imageUrl = ""),
    Model("Airlander 50", "HAV Airlander", 200, 9, 44, 102, 2200, 70700000, 780, 60, Manufacturer("HAV Airlander", countryCode = "GB"), 0, imageUrl = ""),
    Model("Antonov An-10A", "Antonov An", 132, 3, 482, 680, 4075, 53000000, 1040, 24, Manufacturer("Antonov", countryCode = "UA"), 1200, imageUrl = ""),
    Model("Antonov An-148", "Antonov An", 75, 4, 283, 835, 5300, 32300000, 1040, 12, Manufacturer("Antonov", countryCode = "UA"), 1600, imageUrl = ""),
    Model("Antonov An-158", "Antonov An", 102, 4, 302, 835, 3500, 31700000, 1040, 24, Manufacturer("Antonov", countryCode = "UA"), 1900, imageUrl = ""),
    Model("Antonov An-74", "Antonov An", 68, 4, 198, 750, 4325, 21300000, 1040, 0, Manufacturer("Antonov", countryCode = "UA"), 700, imageUrl = ""),
    Model("ATR 42-600", "ATR-Regional", 48, 4, 76, 556, 1326, 18500000, 1040, 0, Manufacturer("ATR", countryCode = "FR"), 1050, imageUrl = ""),
    Model("ATR 42-600S", "ATR-Regional", 48, 5, 76, 556, 1308, 21100000, 1040, 0, Manufacturer("ATR", countryCode = "FR"), 750, imageUrl = ""),
    Model("ATR 72-600", "ATR-Regional", 78, 5, 187, 556, 1528, 29400000, 1040, 12, Manufacturer("ATR", countryCode = "FR"), 1367, imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
    Model("Aurora D8", "Aurora D", 180, 9, 351, 937, 5600, 218000000, 1820, 84, Manufacturer("Aurora Flight Sciences", countryCode = "US"), 2300, imageUrl = ""),
    Model("BAe 146-100", "BAe 146", 82, 7, 303, 789, 3650, 48300000, 1560, 24, Manufacturer("BAe", countryCode = "GB"), 1195, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("BAe 146-200", "BAe 146", 98, 7, 313, 789, 3650, 51800000, 1560, 24, Manufacturer("BAe", countryCode = "GB"), 1390, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("BAe 146-300", "BAe 146", 112, 7, 323, 789, 3650, 56500000, 1560, 24, Manufacturer("BAe", countryCode = "GB"), 1535, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
    Model("BAe Jetstream 31", "BAe Jetstream", 20, 4, 28, 430, 1260, 7100000, 1820, 0, Manufacturer("BAe", countryCode = "GB"), 950, imageUrl = ""),
    Model("BAe Jetstream 41", "BAe Jetstream", 29, 4, 45, 482, 1433, 10600000, 1820, 0, Manufacturer("BAe", countryCode = "GB"), 1524, imageUrl = ""),
    Model("BAe Jetstream 61", "BAe Jetstream", 56, 4, 110, 496, 1825, 26500000, 1820, 12, Manufacturer("BAe", countryCode = "GB"), 1200, imageUrl = ""),
    Model("Boeing 2707", "Boeing 2707", 300, 10, 6400, 3300, 5900, 605300000, 1820, 126, Manufacturer("Boeing", countryCode = "US"), 3800, imageUrl = ""),
    Model("Boeing 307 Stratoliner", "Post-War Props", 60, 1, 226, 357, 3850, 6400000, 1456, 0, Manufacturer("Boeing", countryCode = "US"), 620, imageUrl = ""),
    Model("Boeing 377 Stratocruiser", "Post-War Props", 117, 2, 448, 480, 6760, 39000000, 1456, 12, Manufacturer("Boeing", countryCode = "US"), 1000, imageUrl = ""),
    Model("Boeing 707", "Boeing 707", 194, 3, 1015, 1003, 6700, 70200000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 2700, imageUrl = "https://www.norebbo.com/boeing-707-320c-blank-illustration-templates/"),
    Model("Boeing 717-200", "DC-9", 134, 5, 506, 811, 2645, 60700000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 2100, imageUrl = "https://www.norebbo.com/2017/06/boeing-717-200-blank-illustration-templates/"),
    Model("Boeing 727-100", "Boeing 727", 131, 4, 512, 960, 4170, 57100000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 1750, imageUrl = ""),
    Model("Boeing 727-200", "Boeing 727", 189, 4, 831, 811, 4020, 97700000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 1800, imageUrl = "https://www.norebbo.com/2018/03/boeing-727-200-blank-illustration-templates/"),
    Model("Boeing 737 MAX 10", "Boeing 737", 230, 6, 867, 830, 6500, 157900000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2700, imageUrl = "https://www.norebbo.com/2019/01/737-10-max-side-view/"),
    Model("Boeing 737 MAX 7", "Boeing 737", 172, 6, 619, 830, 6500, 124400000, 1820, 60, Manufacturer("Boeing", countryCode = "US"), 2100, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-7-blank-illustration-templates/"),
    Model("Boeing 737 MAX 8", "Boeing 737", 189, 6, 717, 830, 6500, 131300000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 2500, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
    Model("Boeing 737 MAX 8-200", "Boeing 737", 200, 6, 804, 839, 6570, 132500000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 2500, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
    Model("Boeing 737 MAX 9", "Boeing 737", 220, 6, 838, 839, 6570, 148800000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2600, imageUrl = "https://www.norebbo.com/2018/05/boeing-737-9-max-blank-illustration-templates/"),
    Model("Boeing 737-100", "Boeing 737", 124, 3, 496, 780, 3440, 43400000, 1820, 24, Manufacturer("Boeing", countryCode = "US"), 1800, imageUrl = "https://www.norebbo.com/2018/10/boeing-737-100-blank-illustration-templates/"),
    Model("Boeing 737-200", "Boeing 737", 136, 4, 584, 780, 4200, 66400000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 1859, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-200-blank-illustration-templates/"),
    Model("Boeing 737-300", "Boeing 737", 149, 4, 610, 800, 4400, 83100000, 1820, 48, Manufacturer("Boeing", countryCode = "US"), 1940, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-300-blank-illustration-templates/"),
    Model("Boeing 737-400", "Boeing 737", 188, 5, 770, 800, 5000, 104300000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 2540, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-400-blank-illustration-templates/"),
    Model("Boeing 737-500", "Boeing 737", 140, 5, 574, 800, 5200, 90600000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 1830, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-500-blank-illustration-templates-with-and-without-blended-winglets/"),
    Model("Boeing 737-600", "Boeing 737", 149, 5, 581, 830, 7200, 93800000, 1820, 48, Manufacturer("Boeing", countryCode = "US"), 1878, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-600-blank-illustration-templates/"),
    Model("Boeing 737-700", "Boeing 737", 149, 5, 566, 830, 7630, 96300000, 1820, 48, Manufacturer("Boeing", countryCode = "US"), 2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
    Model("Boeing 737-700C", "Boeing 737", 140, 5, 462, 825, 6083, 95600000, 1820, 36, Manufacturer("Boeing", countryCode = "US"), 2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
    Model("Boeing 737-700ER", "Boeing 737", 149, 5, 625, 830, 10200, 101300000, 1820, 48, Manufacturer("Boeing", countryCode = "US"), 2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
    Model("Boeing 737-800", "Boeing 737", 184, 5, 699, 825, 5436, 114000000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 2316, imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
    Model("Boeing 737-900", "Boeing 737", 189, 5, 756, 830, 6660, 119400000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 3000, imageUrl = "https://www.norebbo.com/2014/08/boeing-737-900-blank-illustration-templates/"),
    Model("Boeing 737-900ER", "Boeing 737", 220, 6, 880, 830, 6500, 134800000, 1820, 72, Manufacturer("Boeing", countryCode = "US"), 3000, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-900er-with-split-scimitar-winglets-blank-illustration-templates/"),
    Model("Boeing 747-100", "Boeing 747", 550, 5, 3325, 907, 8560, 281800000, 1820, 132, Manufacturer("Boeing", countryCode = "US"), 3250, imageUrl = "https://www.norebbo.com/2019/07/boeing-747-100-side-view/"),
    Model("Boeing 747-200", "Boeing 747", 550, 5, 3245, 907, 12150, 301800000, 1820, 132, Manufacturer("Boeing", countryCode = "US"), 3300, imageUrl = "https://www.norebbo.com/2019/08/boeing-747-200-side-view/"),
    Model("Boeing 747-300", "Boeing 747", 660, 6, 3962, 907, 11720, 364300000, 1820, 144, Manufacturer("Boeing", countryCode = "US"), 3300, imageUrl = "https://www.norebbo.com/boeing-747-300-side-view/"),
    Model("Boeing 747-400", "Boeing 747", 660, 6, 3825, 933, 13492, 424300000, 1820, 144, Manufacturer("Boeing", countryCode = "US"), 3300, imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
    Model("Boeing 747-8i", "Boeing 747", 660, 7, 3494, 933, 14320, 570400000, 1820, 144, Manufacturer("Boeing", countryCode = "US"), 3100, imageUrl = "https://www.norebbo.com/2015/12/boeing-747-8i-blank-illustration-templates/"),
    Model("Boeing 747SP", "Boeing 747", 400, 6, 2180, 980, 10800, 280000000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2820, imageUrl = "https://www.norebbo.com/2019/08/boeing-747sp-side-view/"),
    Model("Boeing 757-100", "Boeing 757", 150, 5, 610, 854, 8250, 131400000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 1910, imageUrl = ""),
    Model("Boeing 757-200", "Boeing 757", 239, 5, 1003, 854, 7250, 143200000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2070, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
    Model("Boeing 757-200ER", "Boeing 757", 239, 5, 1027, 850, 9170, 153200000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2070, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
    Model("Boeing 757-300", "Boeing 757", 295, 5, 1386, 850, 6421, 152400000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2377, imageUrl = "https://www.norebbo.com/2017/03/boeing-757-300-blank-illustration-templates/"),
    Model("Boeing 767-200", "Boeing 767", 255, 5, 1045, 851, 7200, 144400000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 1900, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 767-200ER", "Boeing 767", 245, 5, 1005, 903, 12200, 193600000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2480, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 767-300", "Boeing 767", 350, 5, 1575, 851, 7200, 206600000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2800, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 767-300ER", "Boeing 767", 290, 6, 1257, 903, 11093, 222700000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2650, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
    Model("Boeing 767-400ER", "Boeing 767", 375, 6, 1745, 851, 10415, 247200000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 3290, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-400-blank-illustration-templates/"),
    Model("Boeing 777-200", "Boeing 777", 440, 6, 1980, 896, 9700, 329500000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2440, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 777-200ER", "Boeing 777", 440, 6, 2023, 896, 13080, 334500000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 3380, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 777-200LR", "Boeing 777", 440, 6, 2098, 896, 15843, 339500000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2800, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
    Model("Boeing 777-300", "Boeing 777", 550, 6, 3105, 945, 11121, 401900000, 1820, 132, Manufacturer("Boeing", countryCode = "US"), 3230, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
    Model("Boeing 777-300ER", "Boeing 777", 550, 7, 3115, 896, 13649, 445300000, 1820, 132, Manufacturer("Boeing", countryCode = "US"), 3050, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
    Model("Boeing 777-8", "Boeing 777", 440, 8, 1966, 896, 16090, 441900000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 3050, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-8-side-view/"),
    Model("Boeing 777-9", "Boeing 777", 550, 8, 2705, 896, 13940, 577400000, 1820, 132, Manufacturer("Boeing", countryCode = "US"), 3050, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-9-side-view/"),
    Model("Boeing 787-10 Dreamliner", "Boeing 787", 440, 7, 2003, 903, 11750, 355300000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2800, imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
    Model("Boeing 787-10ER", "Boeing 787", 440, 7, 1927, 903, 13750, 385300000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2900, imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
    Model("Boeing 787-8 Dreamliner", "Boeing 787", 360, 7, 1584, 907, 13621, 265700000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2600, imageUrl = "https://www.norebbo.com/2013/02/boeing-787-8-blank-illustration-templates/"),
    Model("Boeing 787-9 Dreamliner", "Boeing 787", 420, 7, 1913, 903, 14010, 321600000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2800, imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
    Model("Boeing 787-9ER", "Boeing 787", 406, 7, 1852, 903, 15400, 329100000, 1820, 108, Manufacturer("Boeing", countryCode = "US"), 2900, imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
    Model("Boeing 797-6", "Boeing 797", 225, 8, 825, 890, 9260, 192100000, 1820, 120, Manufacturer("Boeing", countryCode = "US"), 2600, imageUrl = "https://www.norebbo.com/boeing-797-side-view/"),
    Model("Boeing 797-7", "Boeing 797", 275, 8, 1200, 890, 8330, 208700000, 1820, 120, Manufacturer("Boeing", countryCode = "US"), 2600, imageUrl = "https://www.norebbo.com/boeing-797-side-view/"),
    Model("Bombardier CRJ100", "Bombardier CRJ", 50, 7, 95, 830, 2250, 37100000, 1820, 0, Manufacturer("Bombardier", countryCode = "CA"), 1920, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
    Model("Bombardier CRJ1000", "Bombardier CRJ", 104, 7, 312, 870, 3004, 69000000, 1820, 24, Manufacturer("Bombardier", countryCode = "CA"), 2120, imageUrl = "https://www.norebbo.com/2019/06/bombardier-crj-1000-side-view/"),
    Model("Bombardier CRJ200", "Bombardier CRJ", 50, 7, 95, 830, 3150, 39100000, 1820, 0, Manufacturer("Bombardier", countryCode = "CA"), 1920, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
    Model("Bombardier CRJ700", "Bombardier CRJ", 78, 7, 179, 828, 3045, 56200000, 1820, 12, Manufacturer("Bombardier", countryCode = "CA"), 1605, imageUrl = "https://www.norebbo.com/2015/05/bombardier-canadair-regional-jet-700-blank-illustration-templates/"),
    Model("Bombardier CRJ900", "Bombardier CRJ", 90, 7, 261, 870, 2876, 61400000, 1820, 24, Manufacturer("Bombardier", countryCode = "CA"), 1939, imageUrl = "https://www.norebbo.com/2016/07/bombardier-canadair-regional-jet-900-blank-illustration-templates/"),
    Model("Bombardier Global 5000", "Bombardier Global", 20, 10, 26, 934, 9630, 19400000, 1820, 24, Manufacturer("Bombardier", countryCode = "CA"), 1689, imageUrl = "https://www.norebbo.com/bombardier-global-5000-blank-illustration-templates/"),
    Model("Bombardier Global 7500", "Bombardier Global", 30, 10, 42, 1080, 14260, 40500000, 1820, 24, Manufacturer("Bombardier", countryCode = "CA"), 1768, imageUrl = "https://www.norebbo.com/bombardier-global-7500-side-view/"),
    Model("Boom Overture", "Boom Overture", 88, 10, 1056, 1800, 7870, 249700000, 1664, 300, Manufacturer("Boom Technology", countryCode = "US"), 3048, imageUrl = ""),
    Model("CASA C-212 Aviocar", "CASA", 26, 2, 44, 354, 2680, 7000000, 1560, 0, Manufacturer("CASA", countryCode = "ES"), 600, imageUrl = ""),
    Model("CASA CN-235", "CASA", 40, 2, 76, 460, 3658, 15000000, 1560, 0, Manufacturer("CASA", countryCode = "ES"), 1204, imageUrl = ""),
    Model("Cessna Caravan", "Cessna", 15, 4, 14, 355, 2400, 4300000, 1560, 0, Manufacturer("Cessna", countryCode = "US"), 762, imageUrl = "https://www.norebbo.com/2017/06/cessna-208-grand-caravan-blank-illustration-templates/"),
    Model("Cessna Citation X", "Cessna", 25, 10, 34, 900, 6050, 24400000, 1820, 24, Manufacturer("Cessna", countryCode = "US"), 1600, imageUrl = "https://www.norebbo.com/cessna-citation-x-template/"),
    Model("Comac ARJ21", "Comac ARJ21", 90, 4, 315, 828, 2200, 48500000, 1300, 24, Manufacturer("COMAC", countryCode = "CN"), 1700, imageUrl = ""),
    Model("Comac C919", "Comac C919", 168, 5, 634, 834, 4075, 79100000, 1300, 60, Manufacturer("Comac", countryCode = "CN"), 2000, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
    Model("Comac C929-500", "Comac C929", 348, 6, 1668, 908, 14000, 238000000, 1300, 72, Manufacturer("Comac", countryCode = "CN"), 2700, imageUrl = ""),
    Model("Comac C929-600", "Comac C929", 405, 6, 1950, 908, 12000, 277100000, 1560, 72, Manufacturer("Comac", countryCode = "CN"), 2800, imageUrl = ""),
    Model("Comac C929-700", "Comac C929", 440, 6, 2170, 908, 10000, 277500000, 1560, 72, Manufacturer("Comac", countryCode = "CN"), 2900, imageUrl = ""),
    Model("Comac C939", "Comac C939", 460, 6, 2288, 908, 14000, 279400000, 1560, 90, Manufacturer("Comac", countryCode = "CN"), 2800, imageUrl = ""),
    Model("Concorde", "Concorde", 144, 10, 2016, 2158, 7223, 296200000, 1560, 180, Manufacturer("BAe", countryCode = "GB"), 3600, imageUrl = "https://www.norebbo.com/aerospatiale-bac-concorde-blank-illustration-templates/"),
    Model("Dassault Falcon 50", "Dassault", 12, 9, 50, 903, 5660, 7100000, 1456, 24, Manufacturer("Dassault", countryCode = "FR"), 1524, imageUrl = "https://www.norebbo.com/dassault-falcon-50/"),
    Model("Dassault Mercure", "Dassault", 150, 3, 510, 926, 2084, 77600000, 1820, 36, Manufacturer("Dassault Aviation", countryCode = "FR"), 2100, imageUrl = ""),
    Model("De Havilland Canada DHC-6-400", "De Havilland Canada DHC-8", 19, 4, 22, 337, 1480, 6100000, 1560, 12, Manufacturer("De Havilland Canada", countryCode = "CA"), 366, imageUrl = ""),
    Model("De Havilland Canada DHC-7-100", "De Havilland Canada DHC", 50, 5, 79, 428, 1300, 23600000, 1820, 12, Manufacturer("De Havilland Canada", countryCode = "CA"), 620, imageUrl = ""),
    Model("De Havilland Canada DHC-8-100", "De Havilland Canada DHC-8", 40, 5, 64, 448, 1889, 19000000, 1820, 12, Manufacturer("De Havilland Canada", countryCode = "CA"), 950, imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
    Model("De Havilland Canada DHC-8-200", "De Havilland Canada DHC-8", 40, 5, 52, 448, 1713, 21000000, 1820, 12, Manufacturer("De Havilland Canada", countryCode = "CA"), 1000, imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
    Model("De Havilland Canada DHC-8-300", "De Havilland Canada DHC-8", 56, 5, 95, 450, 1711, 26300000, 1820, 12, Manufacturer("De Havilland Canada", countryCode = "CA"), 1085, imageUrl = "https://www.norebbo.com/2018/05/de-havilland-dhc-8-300-blank-illustration-templates/"),
    Model("De Havilland Canada DHC-8-400", "De Havilland Canada DHC-8", 86, 5, 215, 667, 2063, 37500000, 1820, 18, Manufacturer("De Havilland Canada", countryCode = "CA"), 1220, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
    Model("De Havilland Canada Q400", "De Havilland Canada DHC-8", 90, 5, 225, 556, 2040, 38900000, 1560, 24, Manufacturer("De Havilland Canada", countryCode = "CA"), 1885, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
    Model("Dornier 1128", "Dornier 728", 140, 8, 411, 923, 3800, 88200000, 1300, 36, Manufacturer("Dornier", countryCode = "DE"), 1550, imageUrl = ""),
    Model("Dornier 328-110", "Dornier 328", 33, 8, 59, 620, 1310, 24600000, 1820, 18, Manufacturer("Dornier", countryCode = "DE"), 1088, imageUrl = "https://www.norebbo.com/2019/01/dornier-328-110-blank-illustration-templates/"),
    Model("Dornier 328JET", "Dornier 328", 44, 8, 83, 740, 1665, 35200000, 1820, 18, Manufacturer("Dornier", countryCode = "DE"), 1367, imageUrl = "https://www.norebbo.com/2019/01/fairchild-dornier-328jet-illustrations/"),
    Model("Dornier 528", "Dornier 728", 65, 8, 198, 1000, 3000, 41600000, 1820, 18, Manufacturer("Dornier", countryCode = "DE"), 1363, imageUrl = ""),
    Model("Dornier 728", "Dornier 728", 80, 8, 233, 1000, 3300, 48500000, 1820, 18, Manufacturer("Dornier", countryCode = "DE"), 1463, imageUrl = ""),
    Model("Dornier 928", "Dornier 728", 120, 8, 366, 951, 3600, 65900000, 1300, 30, Manufacturer("Dornier", countryCode = "DE"), 1513, imageUrl = ""),
    Model("Douglas DC-3", "Post-War Props", 32, 0, 111, 333, 2400, 2400000, 2480, 0, Manufacturer("Douglas Aircraft Company", countryCode = "US"), 1312, imageUrl = ""),
    Model("Douglas DC-8-10", "DC-8", 177, 2, 1080, 895, 6960, 70000000, 1820, 48, Manufacturer("Douglas Aircraft Company", countryCode = "US"), 2680, imageUrl = "https://www.norebbo.com/douglas-dc-8-61-blank-illustration-templates/"),
    Model("Douglas DC-8-72", "DC-8", 189, 3, 940, 895, 9800, 77400000, 1560, 60, Manufacturer("Douglas Aircraft Company", countryCode = "US"), 2680, imageUrl = "https://www.norebbo.com/douglas-dc-8-73-and-dc-8-73cf-blank-illustration-templates/"),
    Model("Douglas DC-8-73", "DC-8", 259, 3, 1360, 895, 8300, 92000000, 1040, 60, Manufacturer("Douglas Aircraft Company", countryCode = "US"), 2680, imageUrl = "https://www.norebbo.com/douglas-dc-8-73-and-dc-8-73cf-blank-illustration-templates/"),
    Model("Embraer E175-E2", "Embraer E-Jet E2", 88, 5, 264, 870, 3735, 40700000, 1560, 18, Manufacturer("Embraer", countryCode = "BR"), 1800, imageUrl = "https://www.norebbo.com/2019/03/e175-e2-side-view/"),
    Model("Embraer E190-E2", "Embraer E-Jet E2", 106, 6, 371, 870, 5278, 55200000, 1560, 24, Manufacturer("Embraer", countryCode = "BR"), 1450, imageUrl = "https://www.norebbo.com/2019/03/e190-e2-blank-side-view/"),
    Model("Embraer E195-E2", "Embraer E-Jet E2", 146, 6, 540, 833, 4800, 74500000, 1560, 36, Manufacturer("Embraer", countryCode = "BR"), 1970, imageUrl = "https://www.norebbo.com/2019/03/embraer-e195-e2-side-view/"),
    Model("Embraer EMB120 Brasilia", "EMB 120", 30, 4, 42, 552, 1750, 9400000, 1560, 0, Manufacturer("Embraer", countryCode = "BR"), 1220, imageUrl = "https://www.norebbo.com/2015/02/embraer-120-brasilia-blank-illustration-templates/"),
    Model("Embraer ERJ135", "Embraer ERJ", 37, 5, 81, 850, 3241, 19900000, 1560, 12, Manufacturer("Embraer", countryCode = "BR"), 1580, imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-135-blank-illustration-templates/"),
    Model("Embraer ERJ145", "Embraer ERJ", 50, 6, 130, 850, 2800, 25300000, 1560, 12, Manufacturer("Embraer", countryCode = "BR"), 1410, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145-blank-illustration-templates/"),
    Model("Embraer ERJ145XR", "Embraer ERJ", 50, 6, 135, 850, 3700, 26800000, 1560, 12, Manufacturer("Embraer", countryCode = "BR"), 1720, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145xr-blank-illustration-templates/"),
    Model("Embraer ERJ170", "Embraer ERJ", 72, 6, 216, 870, 3982, 38400000, 1560, 12, Manufacturer("Embraer", countryCode = "BR"), 1644, imageUrl = "https://www.norebbo.com/embraer-erj-175-templates-with-the-new-style-winglets/"),
    Model("Embraer ERJ175", "Embraer ERJ", 78, 6, 249, 870, 4074, 42500000, 1560, 12, Manufacturer("Embraer", countryCode = "BR"), 2244, imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
    Model("Embraer ERJ190", "Embraer ERJ", 100, 5, 330, 823, 4537, 46500000, 1560, 24, Manufacturer("Embraer", countryCode = "BR"), 2100, imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
    Model("F27-100", "Fokker", 44, 4, 90, 460, 1468, 18000000, 1560, 12, Manufacturer("Fokker", countryCode = "NL"), 850, imageUrl = ""),
    Model("Fokker 100", "Fokker", 109, 5, 425, 845, 3170, 46100000, 1560, 24, Manufacturer("Fokker", countryCode = "NL"), 1621, imageUrl = "https://www.norebbo.com/2018/07/fokker-100-f-28-0100-blank-illustration-templates/"),
    Model("Fokker 50", "Fokker", 60, 4, 132, 471, 2400, 26800000, 1560, 12, Manufacturer("Fokker", countryCode = "NL"), 1350, imageUrl = ""),
    Model("Fokker 70", "Fokker", 79, 4, 268, 845, 2010, 36200000, 1560, 12, Manufacturer("Fokker", countryCode = "NL"), 1300, imageUrl = ""),
    Model("Fokker 70ER", "Fokker", 79, 4, 276, 845, 3410, 37200000, 1560, 12, Manufacturer("Fokker", countryCode = "NL"), 1300, imageUrl = ""),
    Model("Gulfstream G650ER", "Gulfstream", 30, 10, 34, 904, 13890, 50900000, 2080, 24, Manufacturer("Gulfstream", countryCode = "US"), 1920, imageUrl = "https://www.norebbo.com/gulfstream-g650er-template/"),
    Model("Ilyushin Il-18", "Ilyushin Il", 120, 1, 468, 625, 6500, 38300000, 1560, 24, Manufacturer("Ilyushin", countryCode = "RU"), 1350, imageUrl = ""),
    Model("Ilyushin Il-62", "Ilyushin Il", 186, 2, 983, 900, 10000, 78500000, 1560, 72, Manufacturer("Ilyushin", countryCode = "RU"), 2300, imageUrl = ""),
    Model("Ilyushin Il-86", "Ilyushin Il-96", 320, 3, 1680, 950, 5000, 104900000, 1456, 96, Manufacturer("Ilyushin", countryCode = "RU"), 2800, imageUrl = ""),
    Model("Ilyushin Il-96-300", "Ilyushin Il-96", 300, 4, 1560, 900, 13500, 111600000, 1300, 108, Manufacturer("Ilyushin", countryCode = "RU"), 3200, imageUrl = ""),
    Model("Ilyushin Il-96-400", "Ilyushin Il-96", 436, 4, 2398, 900, 14500, 166800000, 1300, 108, Manufacturer("Ilyushin", countryCode = "RU"), 2600, imageUrl = ""),
    Model("Lockheed Constellation L-749", "Post-War Props", 81, 1, 355, 555, 8039, 14800000, 1820, 12, Manufacturer("Lockheed", countryCode = "US"), 1050, imageUrl = ""),
    Model("Lockheed JetStar", "Lockheed JetStar", 15, 8, 45, 920, 4820, 6700000, 1820, 12, Manufacturer("Lockheed", countryCode = "US"), 1100, imageUrl = ""),
    Model("Lockheed L-1011-200", "Lockheed TriStar", 400, 4, 1990, 954, 6667, 158500000, 1560, 108, Manufacturer("Lockheed", countryCode = "US"), 2560, imageUrl = ""),
    Model("Lockheed L-1011-500", "Lockheed TriStar", 330, 4, 1722, 972, 9899, 145300000, 1560, 108, Manufacturer("Lockheed", countryCode = "US"), 2865, imageUrl = "https://www.norebbo.com/2015/03/lockheed-l-1011-500-blank-illustration-templates/"),
    Model("McDonnell Douglas DC-9-10", "DC-9", 92, 3, 216, 965, 2367, 30400000, 1040, 12, Manufacturer("McDonnell Douglas", countryCode = "US"), 1816, imageUrl = ""),
    Model("McDonnell Douglas DC-9-30", "DC-9", 115, 4, 302, 804, 2778, 42100000, 1040, 12, Manufacturer("McDonnell Douglas", countryCode = "US"), 1900, imageUrl = "https://www.norebbo.com/mcdonnell-douglas-dc-9-30-templates/"),
    Model("McDonnell Douglas DC-9-50", "DC-9", 139, 4, 417, 804, 3030, 57300000, 1040, 12, Manufacturer("McDonnell Douglas", countryCode = "US"), 2200, imageUrl = "https://www.norebbo.com/dc-9-50-side-view/"),
    Model("McDonnell Douglas MD-11", "MD-11", 293, 4, 1435, 876, 12670, 138600000, 1560, 108, Manufacturer("McDonnell Douglas", countryCode = "US"), 2964, imageUrl = "https://www.norebbo.com/2018/05/mcdonnell-douglas-md-11-blank-illustration-templates-with-ge-engines/"),
    Model("McDonnell Douglas MD-11 ER", "MD-11", 323, 4, 1650, 886, 13408, 180000000, 1560, 84, Manufacturer("McDonnell Douglas", countryCode = "US"), 3292, imageUrl = "https://www.norebbo.com/2018/05/mcdonnell-douglas-md-11-blank-illustration-templates-with-ge-engines/"),
    Model("McDonnell Douglas MD-11 ST", "MD-11", 410, 4, 2100, 940, 12392, 234000000, 1560, 96, Manufacturer("McDonnell Douglas", countryCode = "US"), 3292, imageUrl = ""),
    Model("McDonnell Douglas MD-12", "MD-11", 511, 6, 3150, 1050, 14825, 280300000, 1560, 120, Manufacturer("McDonnell Douglas", countryCode = "US"), 3050, imageUrl = ""),
    Model("McDonnell Douglas MD-220", "MD-220", 38, 8, 75, 1020, 4100, 18600000, 1820, 24, Manufacturer("McDonnell Douglas", countryCode = "US"), 1200, imageUrl = ""),
    Model("McDonnell Douglas MD-81", "DC-9", 150, 4, 525, 811, 4635, 81900000, 1560, 48, Manufacturer("McDonnell Douglas", countryCode = "US"), 2200, imageUrl = "https://www.norebbo.com/2015/02/mcdonnell-douglas-md-80-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90", "DC-9", 160, 5, 568, 811, 3787, 90400000, 1560, 60, Manufacturer("McDonnell Douglas", countryCode = "US"), 2134, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("McDonnell Douglas MD-90ER", "DC-9", 160, 5, 568, 811, 4143, 91400000, 1560, 60, Manufacturer("McDonnell Douglas", countryCode = "US"), 2134, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
    Model("Mitsubishi MRJ-100", "Mitsubishi SpaceJet", 88, 7, 195, 900, 3540, 86300000, 2080, 18, Manufacturer("Mitsubishi", countryCode = "JP"), 1760, imageUrl = ""),
    Model("Mitsubishi MRJ-90", "Mitsubishi SpaceJet", 92, 7, 204, 900, 3770, 94200000, 2080, 24, Manufacturer("Mitsubishi", countryCode = "JP"), 1740, imageUrl = ""),
    Model("Pilatus PC-12", "Pilatus", 9, 5, 10, 528, 3417, 2700000, 1456, 0, Manufacturer("Pilatus", countryCode = "CH"), 758, imageUrl = ""),
    Model("Saab 2000", "Saab Regional", 58, 5, 105, 608, 2868, 28900000, 1820, 12, Manufacturer("Saab", countryCode = "SE"), 1252, imageUrl = ""),
    Model("Saab 90 Scandia", "Saab Regional", 32, 4, 50, 340, 2650, 10700000, 1820, 0, Manufacturer("Saab", countryCode = "SE"), 850, imageUrl = ""),
    Model("Shaanxi Y-10", "Shaanxi Y", 178, 4, 885, 917, 5560, 50500000, 1040, 36, Manufacturer("Shaanxi Aircraft Corporation", countryCode = "CN"), 2700, imageUrl = ""),
    Model("Sukhoi KR860", "Sukhoi", 860, 6, 5550, 1000, 15000, 439200000, 1300, 174, Manufacturer("JSC Sukhoi", countryCode = "RU"), 3400, imageUrl = ""),
    Model("Sukhoi Su-80", "Sukhoi", 32, 4, 49, 520, 1600, 12200000, 1300, 24, Manufacturer("JSC Sukhoi", countryCode = "RU"), 650, imageUrl = ""),
    Model("Sukhoi Superjet 100", "Sukhoi", 108, 6, 442, 828, 4578, 48400000, 1560, 48, Manufacturer("JSC Sukhoi", countryCode = "RU"), 1731, imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
    Model("Sukhoi Superjet 130NG", "Sukhoi", 130, 6, 520, 871, 4008, 58100000, 1560, 48, Manufacturer("JSC Sukhoi", countryCode = "RU"), 1731, imageUrl = ""),
    Model("Tupolev Tu-124", "Tupolev Tu", 56, 1, 253, 970, 2300, 12900000, 1300, 24, Manufacturer("Tupolev", countryCode = "RU"), 1550, imageUrl = ""),
    Model("Tupolev Tu-204", "Tupolev Tu", 210, 4, 965, 810, 4300, 58100000, 1300, 72, Manufacturer("Tupolev", countryCode = "RU"), 1870, imageUrl = "https://www.norebbo.com/tupolev-tu-204-100-blank-illustration-templates/"),
    Model("Tupolev Tu-334-100", "Tupolev Tu", 102, 4, 258, 820, 4100, 44400000, 1300, 24, Manufacturer("Tupolev", countryCode = "RU"), 1980, imageUrl = ""),
    Model("Tupolev Tu-334-200", "Tupolev Tu", 126, 4, 315, 820, 3150, 44900000, 1300, 12, Manufacturer("Tupolev", countryCode = "RU"), 1820, imageUrl = ""),
    Model("Vickers VC10", "Vickers", 150, 3, 645, 930, 9410, 75500000, 1560, 36, Manufacturer("Vickers-Armstrongs", countryCode = "GB"), 2520, imageUrl = ""),
    Model("Xi'an MA600", "Xi'an Turboprops", 60, 4, 99, 514, 1600, 27300000, 1300, 18, Manufacturer("Xi'an Aircraft Industrial Corporation", countryCode = "CN"), 750, imageUrl = ""),
    Model("Xi'an MA700", "Xi'an Turboprops", 86, 4, 195, 637, 1500, 34300000, 1300, 30, Manufacturer("Xi'an Aircraft Industrial Corporation", countryCode = "CN"), 630, imageUrl = ""),
    Model("Yakovlev MC-21-100", "Yakovlev MC-21", 132, 6, 465, 870, 6140, 76600000, 1300, 60, Manufacturer("Irkut", countryCode = "RU"), 1322, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Yakovlev MC-21-200", "Yakovlev MC-21", 165, 6, 590, 870, 6400, 91300000, 1300, 72, Manufacturer("Irkut", countryCode = "RU"), 1350, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Yakovlev MC-21-300", "Yakovlev MC-21", 211, 6, 813, 870, 6000, 103000000, 1300, 90, Manufacturer("Irkut", countryCode = "RU"), 1644, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
    Model("Yakovlev MC-21-400", "Yakovlev MC-21", 230, 6, 997, 870, 5500, 113500000, 1300, 108, Manufacturer("Irkut", countryCode = "RU"), 1500, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
  )
  val modelByName = models.map { model => (model.name, model) }.toMap
}