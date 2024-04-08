package com.patson.model.airplane

import com.patson.model.IdObject
import com.patson.model.Airline
import com.patson.model.airplane.Model.Category
import com.patson.util.AirplaneModelCache

case class Model(name : String, family : String = "", capacity : Int, fuelBurn : Int, speed : Int, range : Int, price : Int, lifespan : Int, constructionTime : Int, manufacturer: Manufacturer, runwayRequirement : Int, imageUrl : String = "", var id : Int = 0) extends IdObject {
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
    val modelWithJustId = Model("Unknown", "Unknown", 0, 0, 0, 0, 0, 0, 0, Manufacturer("Unknown", countryCode = ""), runwayRequirement = 0)
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

  //https://en.wikipedia.org/wiki/List_of_jet_airliners
  val models = List(
Model("Airbus A220-100","Airbus A220",133,425,828,5741,80000000,1820,8,Manufacturer("Airbus",countryCode = "CA"),1463, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs100-blank-illustration-templates/"),
Model("Airbus A220-300","Airbus A220",160,544,828,6112,98000000,1820,20,Manufacturer("Airbus",countryCode = "CA"),1890, imageUrl = "https://www.norebbo.com/2016/02/bombardier-cs300-blank-illustration-templates/"),
Model("Airbus A220-500","Airbus A220",190,730,828,6000,100000000,1820,20,Manufacturer("Airbus",countryCode = "NL"),2000),
Model("Airbus A300-600","Airbus A300/A310",266,1223,833,7500,110000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2400, imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
Model("Airbus A300B4","Airbus A300/A310",345,1630,847,5375,185000000,1820,32,Manufacturer("Airbus",countryCode = "NL"),1950, imageUrl = "https://www.norebbo.com/2018/11/airbus-a300b4-600r-blank-illustration-templates-with-general-electric-engines/"),
Model("Airbus A310-200","Airbus A300/A310",240,1056,850,6800,125000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),1860, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
Model("Airbus A310-300","Airbus A300/A310",240,1032,850,8050,130000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2000, imageUrl = "https://www.norebbo.com/2015/07/airbus-a310-300-blank-illustration-templates/"),
Model("Airbus A318","Airbus A320",132,435,829,7800,90000000,1820,8,Manufacturer("Airbus",countryCode = "NL"),1780, imageUrl = "https://www.norebbo.com/airbus-a318-blank-illustration-templates-with-pratt-whitney-and-cfm56-engines/"),
Model("Airbus A319","Airbus A320",156,530,830,6940,95000000,1820,16,Manufacturer("Airbus",countryCode = "NL"),1850, imageUrl = "https://www.norebbo.com/2014/05/airbus-a319-blank-illustration-templates/"),
Model("Airbus A319neo","Airbus A320",160,560,828,6850,100000000,1820,20,Manufacturer("Airbus",countryCode = "NL"),2164, imageUrl = "https://www.norebbo.com/2017/09/airbus-a319-neo-blank-illustration-templates/"),
Model("Airbus A320","Airbus A320",180,684,828,6150,100000000,1820,24,Manufacturer("Airbus",countryCode = "NL"),2100, imageUrl = "https://www.norebbo.com/2013/08/airbus-a320-blank-illustration-templates/"),
Model("Airbus A320neo","Airbus A320",195,780,833,6500,110000000,1820,24,Manufacturer("Airbus",countryCode = "NL"),2100, imageUrl = "https://www.norebbo.com/2017/08/airbus-a320-neo-blank-illustration-templates/"),
Model("Airbus A321","Airbus A320",236,1014,830,5930,120000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2210, imageUrl = "https://www.norebbo.com/2014/03/airbus-a321-blank-illustration-templates/"),
Model("Airbus A321neo","Airbus A320",244,1000,828,6850,130000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),1988, imageUrl = "https://www.norebbo.com/2017/09/airbus-a321-neo-blank-illustration-templates/"),
Model("Airbus A321neoLR","Airbus A320",240,996,828,7400,132000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2300, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
Model("Airbus A321neoXLR","Airbus A320",236,1038,828,8700,133000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2400, imageUrl = "https://www.norebbo.com/2018/10/airbus-a321neo-lr-long-range-blank-illustration-templates/"),
Model("Airbus A330-200","Airbus A330",380,1786,871,11300,220000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2770, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-200-blank-illustration-templates-with-pratt-whitney-engines/"),
Model("Airbus A330-300","Airbus A330",404,1898,870,13400,235000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2770, imageUrl = "https://www.norebbo.com/2016/02/airbus-a330-300-blank-illustration-templates-with-all-three-engine-options/"),
Model("Airbus A330-800neo","Airbus A330",406,1827,912,13900,260000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2770, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-800-neo-blank-illustration-templates/"),
Model("Airbus A330-900neo","Airbus A330",440,1980,912,12130,290000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2770, imageUrl = "https://www.norebbo.com/2018/06/airbus-a330-900-neo-blank-illustration-templates/"),
Model("Airbus A340-200","Airbus A340",315,1480,880,14800,200000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2900, imageUrl = "https://www.norebbo.com/2019/01/airbus-a340-200-side-view/"),
Model("Airbus A340-300","Airbus A340",350,1645,880,13350,230000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),3000, imageUrl = "https://www.norebbo.com/2016/04/airbus-340-300-and-a340-300x-blank-illustration-templates/"),
Model("Airbus A340-500","Airbus A340",375,1762,871,17000,270000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),3350, imageUrl = "https://www.norebbo.com/2016/08/airbus-a340-500-blank-illustration-templates/"),
Model("Airbus A340-600","Airbus A340",440,2288,905,13900,290000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),3400, imageUrl = "https://www.norebbo.com/2016/11/airbus-a340-600-blank-illustration-templates/"),
Model("Airbus A350-1000","Airbus A350",475,2235,910,15050,360000000,1820,40,Manufacturer("Airbus",countryCode = "NL"),2600, imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
Model("Airbus A350-1000 Sunrise","Airbus A350",475,2276,910,18000,415000000,1820,40,Manufacturer("Airbus",countryCode = "NL"),2700, imageUrl = "https://www.norebbo.com/2015/11/airbus-a350-1000-blank-illustration-templates/"),
Model("Airbus A350-900","Airbus A350",440,2156,903,15000,280000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2600, imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
Model("Airbus A350-900ULR","Airbus A350",440,2288,910,17960,325000000,1820,36,Manufacturer("Airbus",countryCode = "NL"),2600, imageUrl = "https://www.norebbo.com/2013/07/airbus-a350-900-blank-illustration-templates/"),
Model("Airbus A380-800","Airbus A380",853,5318,925,15700,450000000,1820,54,Manufacturer("Airbus",countryCode = "NL"),3000, imageUrl = "https://www.norebbo.com/2013/06/airbus-a380-800-blank-illustration-templates/"),
Model("Airbus ZeroE Turbofan","Airbus ZE",175,160,795,3200,250000000,1040,12,Manufacturer("Airbus",countryCode = "NL"),2200),
Model("Airbus ZeroE Turboprop","Airbus ZE",85,75,680,1600,100000000,780,8,Manufacturer("Airbus",countryCode = "NL"),2000),
Model("Antonov An-10","Antonov-A",132,482,680,4075,51000000,1040,8,Manufacturer("Antonov",countryCode = "UA"),1200),
Model("Antonov An-158","Antonov-A",102,302,835,3500,28500000,1040,8,Manufacturer("Antonov",countryCode = "UA"),1900),
Model("Antonov An-74","Antonov-A",52,195,705,4325,19200000,1040,0,Manufacturer("Antonov",countryCode = "UA"),700),
Model("Antonov An-148","Antonov-A",75,283,835,5300,30000000,1040,4,Manufacturer("Antonov",countryCode = "UA"),1600),
Model("ATR 42-600","ATR-Regional",48,76,556,1326,17000000,1040,0,Manufacturer("ATR",countryCode = "FR"),1050, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
Model("ATR 42-600S","ATR-Regional",48,76,556,1308,19000000,1040,0,Manufacturer("ATR",countryCode = "FR"),750, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
Model("ATR 72-600","ATR-Regional",78,187,556,1528,26000000,1040,4,Manufacturer("ATR",countryCode = "FR"),1367, imageUrl = "https://www.norebbo.com/2017/04/atr-72-blank-illustration-templates/"),
Model("Aurora D8","Aurora D",180,351,937,5600,145000000,1820,28,Manufacturer("Aurora Flight Sciences",countryCode = "US"),2300),
Model("BAe 146-100","BAe 146",82,303,789,3650,35500000,1560,8,Manufacturer("BAe",countryCode = "GB"),1195),
Model("BAe 146-200","BAe 146",98,313,789,3650,36500000,1560,8,Manufacturer("BAe",countryCode = "GB"),1390, imageUrl = "https://www.norebbo.com/2018/11/british-aerospace-bae-146-200-avro-rj85-blank-illustration-templates/"),
Model("BAe 146-300","BAe 146",112,323,789,3650,39000000,1560,8,Manufacturer("BAe",countryCode = "GB"),1535),
Model("BAe Jetstream 31","BAe Jetstream",19,28,430,1260,6000000,1820,0,Manufacturer("BAe",countryCode = "GB"),950),
Model("BAe Jetstream 41","BAe Jetstream",29,45,482,1433,9000000,1820,0,Manufacturer("BAe",countryCode = "GB"),1524),
Model("BAe Jetstream 61","BAe Jetstream",56,110,496,1825,23500000,1820,4,Manufacturer("BAe",countryCode = "GB"),1200),
Model("Boeing 2707","Boeing 2707",300,9600,3300,5900,420000000,1820,42,Manufacturer("Boeing",countryCode = "US"),3800),
Model("Boeing 307 Stratoliner","Boeing 307",33,152,357,3850,11000000,1820,0,Manufacturer("Boeing",countryCode = "US"),620),
Model("Boeing 377 Stratocruiser","Boeing 307",117,538,547,6760,41000000,1820,0,Manufacturer("Boeing",countryCode = "US"),1000),
Model("Boeing 707","Boeing 707",194,925,1003,6700,65000000,30,12,Manufacturer("Boeing",countryCode = "US"),2700, imageUrl = "https://www.norebbo.com/boeing-707-320c-blank-illustration-templates/"),
Model("Boeing 717-200","Boeing 717",134,576,811,2645,52500000,1820,12,Manufacturer("Boeing",countryCode = "US"),2100, imageUrl = "https://www.norebbo.com/2017/06/boeing-717-200-blank-illustration-templates/"),
Model("Boeing 727-100","Boeing 727",131,512,960,4170,50000000,1820,12,Manufacturer("Boeing",countryCode = "US"),1850),
Model("Boeing 727-200","Boeing 727",189,831,811,4020,87500000,1820,24,Manufacturer("Boeing",countryCode = "US"),1981, imageUrl = "https://www.norebbo.com/2018/03/boeing-727-200-blank-illustration-templates/"),
Model("Boeing 737 MAX 10","Boeing 737",230,897,830,6500,132000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2700, imageUrl = "https://www.norebbo.com/2019/01/737-10-max-side-view/"),
Model("Boeing 737 MAX 7","Boeing 737",172,619,830,6500,105000000,1820,20,Manufacturer("Boeing",countryCode = "US"),2100, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-7-blank-illustration-templates/"),
Model("Boeing 737 MAX 8","Boeing 737",189,737,830,6500,110000000,1820,24,Manufacturer("Boeing",countryCode = "US"),2500, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
Model("Boeing 737 MAX 8-200","Boeing 737",200,804,839,6570,110000000,1820,24,Manufacturer("Boeing",countryCode = "US"),2500, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-max-8-blank-illustration-templates/"),
Model("Boeing 737 MAX 9","Boeing 737",220,858,839,6570,124000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2600, imageUrl = "https://www.norebbo.com/2018/05/boeing-737-9-max-blank-illustration-templates/"),
Model("Boeing 737-100","Boeing 737",124,496,780,3440,40000000,1820,8,Manufacturer("Boeing",countryCode = "US"),1800, imageUrl = "https://www.norebbo.com/2018/10/boeing-737-100-blank-illustration-templates/"),
Model("Boeing 737-200","Boeing 737",136,584,780,4200,59000000,1820,12,Manufacturer("Boeing",countryCode = "US"),1859, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-200-blank-illustration-templates/"),
Model("Boeing 737-300","Boeing 737",149,610,800,4400,75000000,1820,16,Manufacturer("Boeing",countryCode = "US"),1940, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-300-blank-illustration-templates/"),
Model("Boeing 737-400","Boeing 737",188,770,800,5000,90000000,1820,24,Manufacturer("Boeing",countryCode = "US"),2540, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-400-blank-illustration-templates/"),
Model("Boeing 737-500","Boeing 737",140,574,800,5200,80000000,1820,12,Manufacturer("Boeing",countryCode = "US"),1830, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-500-blank-illustration-templates-with-and-without-blended-winglets/"),
Model("Boeing 737-600","Boeing 737",149,581,830,7200,82500000,1820,16,Manufacturer("Boeing",countryCode = "US"),1878, imageUrl = "https://www.norebbo.com/2018/09/boeing-737-600-blank-illustration-templates/"),
Model("Boeing 737-700","Boeing 737",149,566,830,7630,85000000,1820,16,Manufacturer("Boeing",countryCode = "US"),2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
Model("Boeing 737-700C","Boeing 737",140,462,825,6083,85000000,1820,12,Manufacturer("Boeing",countryCode = "US"),2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
Model("Boeing 737-700ER","Boeing 737",149,625,830,10200,90000000,1820,16,Manufacturer("Boeing",countryCode = "US"),2042, imageUrl = "https://www.norebbo.com/2014/04/boeing-737-700-blank-illustration-templates/"),
Model("Boeing 737-800","Boeing 737",184,699,825,5436,100000000,1820,24,Manufacturer("Boeing",countryCode = "US"),2316, imageUrl = "https://www.norebbo.com/2012/11/boeing-737-800-blank-illustration-templates/"),
Model("Boeing 737-900","Boeing 737",189,756,830,6660,105000000,1820,24,Manufacturer("Boeing",countryCode = "US"),3000, imageUrl = "https://www.norebbo.com/2014/08/boeing-737-900-blank-illustration-templates/"),
Model("Boeing 737-900ER","Boeing 737",220,880,830,6500,110000000,1820,24,Manufacturer("Boeing",countryCode = "US"),3000, imageUrl = "https://www.norebbo.com/2016/07/boeing-737-900er-with-split-scimitar-winglets-blank-illustration-templates/"),
Model("Boeing 747-100","Boeing 747",550,3245,895,9800,250000000,1820,44,Manufacturer("Boeing",countryCode = "US"),3250, imageUrl = "https://www.norebbo.com/2019/07/boeing-747-100-side-view/"),
Model("Boeing 747-200","Boeing 747",550,3245,895,12700,260000000,1820,44,Manufacturer("Boeing",countryCode = "US"),3300, imageUrl = "https://www.norebbo.com/2019/08/boeing-747-200-side-view/"),
Model("Boeing 747-300","Boeing 747",660,4092,910,12400,290000000,1820,48,Manufacturer("Boeing",countryCode = "US"),3300, imageUrl = "https://www.norebbo.com/boeing-747-300-side-view/"),
Model("Boeing 747-400","Boeing 747",660,4025,945,13446,350000000,1820,48,Manufacturer("Boeing",countryCode = "US"),2955, imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
Model("Boeing 747-400ER","Boeing 747",660,4092,913,14200,355000000,1820,48,Manufacturer("Boeing",countryCode = "US"),3260, imageUrl = "https://www.norebbo.com/2013/09/boeing-747-400-blank-illustration-templates/"),
Model("Boeing 747-8i","Boeing 747",605,3146,920,14815,420000000,1820,48,Manufacturer("Boeing",countryCode = "US"),3100, imageUrl = "https://www.norebbo.com/2015/12/boeing-747-8i-blank-illustration-templates/"),
Model("Boeing 747SP","Boeing 747",400,2120,980,14950,235000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2950, imageUrl = "https://www.norebbo.com/2019/08/boeing-747sp-side-view/"),
Model("Boeing 757-100","Boeing 757",150,1103,854,8250,120000000,1820,36,Manufacturer("Boeing",countryCode = "US"),1910),
Model("Boeing 757-200","Boeing 757",239,1003,854,7250,125000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2070, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
Model("Boeing 757-200ER","Boeing 757",239,1027,850,9170,135000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2070, imageUrl = "https://www.norebbo.com/2015/01/boeing-757-200-blank-illustration-templates/"),
Model("Boeing 757-300","Boeing 757",295,1386,850,6421,130000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2377, imageUrl = "https://www.norebbo.com/2017/03/boeing-757-300-blank-illustration-templates/"),
Model("Boeing 767-200","Boeing 767",245,1004,851,7200,140000000,1820,36,Manufacturer("Boeing",countryCode = "US"),1900, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
Model("Boeing 767-200ER","Boeing 767",245,1053,851,12200,150000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2480, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-200-blank-illustration-templates/"),
Model("Boeing 767-300","Boeing 767",290,1327,851,7200,137500000,1820,36,Manufacturer("Boeing",countryCode = "US"),2800, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
Model("Boeing 767-300ER","Boeing 767",350,1575,913,11093,220000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2650, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-300-blank-illustration-templates/"),
Model("Boeing 767-400ER","Boeing 767",409,1922,851,10415,225000000,1820,36,Manufacturer("Boeing",countryCode = "US"),3290, imageUrl = "https://www.norebbo.com/2014/07/boeing-767-400-blank-illustration-templates/"),
Model("Boeing 777-200","Boeing 777",440,1980,896,9700,280000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2440, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
Model("Boeing 777-200ER","Boeing 777",440,2023,896,13080,285000000,1820,36,Manufacturer("Boeing",countryCode = "US"),3380, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
Model("Boeing 777-200LR","Boeing 777",440,2068,896,15843,290000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2800, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
Model("Boeing 777-300","Boeing 777",550,3135,945,11121,340000000,1820,44,Manufacturer("Boeing",countryCode = "US"),3230, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
Model("Boeing 777-300ER","Boeing 777",550,3135,896,13649,345000000,1820,44,Manufacturer("Boeing",countryCode = "US"),3050, imageUrl = "https://www.norebbo.com/2014/03/boeing-777-300-blank-illustration-templates/"),
Model("Boeing 777-8","Boeing 777",440,1936,896,16090,340000000,1820,36,Manufacturer("Boeing",countryCode = "US"),3050, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-8-side-view/"),
Model("Boeing 777-9","Boeing 777",550,2695,896,13940,450000000,1820,44,Manufacturer("Boeing",countryCode = "US"),3050, imageUrl = "https://www.norebbo.com/2019/12/boeing-777-9-side-view/"),
Model("Boeing 787-10 Dreamliner","Boeing 787",360,1584,903,11750,225000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2800, imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
Model("Boeing 787-10ER","Boeing 787",360,1656,903,13750,235000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2900, imageUrl = "https://www.norebbo.com/2017/06/boeing-787-10-blank-illustration-templates/"),
Model("Boeing 787-8 Dreamliner","Boeing 787",250,1125,907,13621,125000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2600, imageUrl = "https://www.norebbo.com/2013/02/boeing-787-8-blank-illustration-templates/"),
Model("Boeing 787-9 Dreamliner","Boeing 787",300,1350,903,13950,175000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2800, imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
Model("Boeing 787-9ER","Boeing 787",300,1410,903,15400,183000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2900, imageUrl = "https://www.norebbo.com/2014/04/boeing-787-9-blank-illustration-templates/"),
Model("Boeing 797-6","Boeing 797",225,825,890,9260,140000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2600, imageUrl = "https://www.norebbo.com/boeing-797-side-view/"),
Model("Boeing 797-7","Boeing 797",275,1200,890,8330,145000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2600, imageUrl = "https://www.norebbo.com/boeing-797-side-view/"),
Model("Bombardier CRJ100","Bombardier CRJ",50,95,830,2250,28000000,1820,0,Manufacturer("Bombardier",countryCode = "CA"),1920, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
Model("Bombardier CRJ1000","Bombardier CRJ",104,312,870,3004,50000000,1820,8,Manufacturer("Bombardier",countryCode = "CA"),2120, imageUrl = "https://www.norebbo.com/2019/06/bombardier-crj-1000-side-view/"),
Model("Bombardier CRJ200","Bombardier CRJ",50,95,830,3150,30000000,1820,0,Manufacturer("Bombardier",countryCode = "CA"),1920, imageUrl = "https://www.norebbo.com/2015/04/bombardier-canadair-regional-jet-200-blank-illustration-templates/"),
Model("Bombardier CRJ700","Bombardier CRJ",78,179,828,3045,42000000,1820,4,Manufacturer("Bombardier",countryCode = "CA"),1605, imageUrl = "https://www.norebbo.com/2015/05/bombardier-canadair-regional-jet-700-blank-illustration-templates/"),
Model("Bombardier CRJ900","Bombardier CRJ",90,261,870,2876,45000000,1820,8,Manufacturer("Bombardier",countryCode = "CA"),1939, imageUrl = "https://www.norebbo.com/2016/07/bombardier-canadair-regional-jet-900-blank-illustration-templates/"),
Model("De Havilland Canada DHC-6-400","De Havilland Canada DHC-8",19,22,337,1480,5200000,1820,0,Manufacturer("De Havilland Canada",countryCode = "CA"),366),
Model("De Havilland Canada DHC-8-100","De Havilland Canada DHC-8",40,64,448,1889,16000000,1820,0,Manufacturer("De Havilland Canada",countryCode = "CA"),950, imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
Model("De Havilland Canada DHC-8-200","De Havilland Canada DHC-8",40,52,448,1713,18000000,1820,0,Manufacturer("De Havilland Canada",countryCode = "CA"),1000, imageUrl = "https://www.norebbo.com/2018/01/de-havilland-dhc-8-200-dash-8-blank-illustration-templates/"),
Model("De Havilland Canada DHC-8-300","De Havilland Canada DHC-8",56,95,450,1711,22000000,1820,0,Manufacturer("De Havilland Canada",countryCode = "CA"),1085, imageUrl = "https://www.norebbo.com/2018/05/de-havilland-dhc-8-300-blank-illustration-templates/"),
Model("De Havilland Canada DHC-8-400","De Havilland Canada DHC-8",86,215,667,2063,31000000,1820,6,Manufacturer("De Havilland Canada",countryCode = "CA"),1220, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
Model("Bombardier Global 5000","Bombardier Global",13,19,902,9630,7000000,1820,0,Manufacturer("Bombardier",countryCode = "CA"),1689, imageUrl = "https://www.norebbo.com/bombardier-global-5000-blank-illustration-templates/"),
Model("Bombardier Global 7500","Bombardier Global",19,30,902,14260,13000000,1820,0,Manufacturer("Bombardier",countryCode = "CA"),1768, imageUrl = "https://www.norebbo.com/bombardier-global-7500-side-view/"),
Model("De Havilland Canada Q400","De Havilland Canada DHC-8",90,225,556,2040,33000000,1560,8,Manufacturer("Bombardier",countryCode = "CA"),1885, imageUrl = "https://www.norebbo.com/2015/08/bombardier-dhc-8-402-q400-blank-illustration-templates/"),
Model("Boom Overture","Boom Overture",88,1056,1800,7870,200000000,1664,100,Manufacturer("Boom Technology",countryCode = "US"),3048),
Model("CASA C-212 Aviocar","CASA",26,44,354,2680,7000000,1560,0,Manufacturer("CASA",countryCode = "ES"),600),
Model("CASA CN-235","CASA",40,76,460,3658,15000000,1560,0,Manufacturer("CASA",countryCode = "ES"),1204),
Model("Cessna Caravan","Cessna",15,14,355,2400,2500000,1820,0,Manufacturer("Cessna",countryCode = "US"),762, imageUrl = "https://www.norebbo.com/2017/06/cessna-208-grand-caravan-blank-illustration-templates/"),
Model("Cessna Citation X","Cessna",25,34,900,6050,9000000,1820,0,Manufacturer("Cessna",countryCode = "US"),1600, imageUrl = "https://www.norebbo.com/cessna-citation-x-template/"),
Model("Comac ARJ21","Comac C",90,315,828,2200,45000000,1300,8,Manufacturer("Comac",countryCode = "CN"),1700),
Model("Comac C919","Comac C",168,604,834,4075,70000000,1300,20,Manufacturer("Comac",countryCode = "CN"),2000, imageUrl = "https://www.norebbo.com/comac-c919-side-view/"),
Model("Comac C929-500","Comac C",250,1225,908,14000,95000000,1300,24,Manufacturer("Comac",countryCode = "CN"),2700),
Model("Comac C929-600","Comac C",280,1350,908,12000,115000000,1300,24,Manufacturer("Comac",countryCode = "CN"),2800),
Model("Comac C929-700","Comac C",320,1650,908,10000,120000000,1300,24,Manufacturer("Comac",countryCode = "CN"),2900),
Model("Comac C939","Comac C",390,1950,908,12000,200000000,1300,24,Manufacturer("Comac",countryCode = "CN"),2800),
Model("Comac C949","Comac C",490,2500,908,14000,220000000,1300,32,Manufacturer("Comac",countryCode = "CN"),3100),
Model("Concorde","Concorde",144,2016,2158,7223,220000000,1560,8,Manufacturer("BAe",countryCode = "GB"),3600, imageUrl = "https://www.norebbo.com/aerospatiale-bac-concorde-blank-illustration-templates/"),
Model("Dassault Falcon 50","Dassault",9,14,800,5660,2800000,1820,0,Manufacturer("Dassault",countryCode = "FR"),1524, imageUrl = "https://www.norebbo.com/dassault-falcon-50/"),
Model("Dassault Mercure","Dassault",150,510,926,2084,73500000,1820,12,Manufacturer("Dassault",countryCode = "FR"),2100),
Model("De Havilland Canada DHC-7-100","De Havilland Canada DHC",50,79,428,1300,19800000,1820,0,Manufacturer("De Havilland Canada",countryCode = "CA"),620),
Model("Dornier 1128","Dornier 728",140,411,923,3800,65000000,1300,10,Manufacturer("Dornier",countryCode = "DE"),1550),
Model("Dornier 328-110","Dornier 328",33,59,620,1310,17000000,1820,0,Manufacturer("Dornier",countryCode = "DE"),1088, imageUrl = "https://www.norebbo.com/2019/01/dornier-328-110-blank-illustration-templates/"),
Model("Dornier 328JET","Dornier 328",44,83,740,1665,25000000,1820,0,Manufacturer("Dornier",countryCode = "DE"),1367, imageUrl = "https://www.norebbo.com/2019/01/fairchild-dornier-328jet-illustrations/"),
Model("Dornier 528","Dornier 728",65,198,1000,3000,26500000,1820,4,Manufacturer("Dornier",countryCode = "DE"),1363),
Model("Dornier 728","Dornier 728",80,233,1000,3300,30000000,1820,4,Manufacturer("Dornier",countryCode = "DE"),1463),
Model("Dornier 928","Dornier 728",120,366,951,3600,46000000,1300,10,Manufacturer("Dornier",countryCode = "DE"),1513),
Model("Embraer E175-E2","Embraer E-Jet E2",88,264,870,3735,35000000,1560,6,Manufacturer("Embraer",countryCode = "BR"),1800, imageUrl = "https://www.norebbo.com/2019/03/e175-e2-side-view/"),
Model("Embraer E190-E2","Embraer E-Jet E2",106,371,870,5278,45000000,1560,8,Manufacturer("Embraer",countryCode = "BR"),1450, imageUrl = "https://www.norebbo.com/2019/03/e190-e2-blank-side-view/"),
Model("Embraer E195-E2","Embraer E-Jet E2",146,540,833,4800,60400000,1560,12,Manufacturer("Embraer",countryCode = "BR"),1970, imageUrl = "https://www.norebbo.com/2019/03/embraer-e195-e2-side-view/"),
Model("Embraer EMB120 Brasilia","Embraer ERJ",30,57,552,1750,8000000,1560,0,Manufacturer("Embraer",countryCode = "BR"),1420, imageUrl = "https://www.norebbo.com/2015/02/embraer-120-brasilia-blank-illustration-templates/"),
Model("Embraer EMB190","Embraer ERJ",100,330,823,4537,40000000,1560,8,Manufacturer("Embraer",countryCode = "BR"),2100, imageUrl = "https://www.norebbo.com/2015/06/embraer-190-blank-illustration-templates/"),
Model("Embraer ERJ135","Embraer ERJ",37,81,850,3241,17500000,1560,0,Manufacturer("Embraer",countryCode = "BR"),1580, imageUrl = "https://www.norebbo.com/2018/05/embraer-erj-135-blank-illustration-templates/"),
Model("Embraer ERJ145","Embraer ERJ",50,130,850,2800,20500000,1560,0,Manufacturer("Embraer",countryCode = "BR"),1410, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145-blank-illustration-templates/"),
Model("Embraer ERJ145XR","Embraer ERJ",50,135,850,3700,22000000,1560,0,Manufacturer("Embraer",countryCode = "BR"),1720, imageUrl = "https://www.norebbo.com/2018/04/embraer-erj-145xr-blank-illustration-templates/"),
Model("Embraer ERJ170","Embraer ERJ",72,216,870,3982,31500000,1560,4,Manufacturer("Embraer",countryCode = "BR"),1644, imageUrl = "https://www.norebbo.com/embraer-erj-175-templates-with-the-new-style-winglets/"),
Model("Embraer ERJ175","Embraer ERJ",78,249,870,4074,35000000,1560,4,Manufacturer("Embraer",countryCode = "BR"),2244, imageUrl = "https://www.norebbo.com/2015/10/embraer-erj-175-templates-with-the-new-style-winglets/"),
Model("F27-100","Fokker",44,90,460,1468,16000000,1560,0,Manufacturer("Fokker",countryCode = "NL"),850),
Model("Fokker 100","Fokker",109,425,845,3170,39000000,1560,8,Manufacturer("Fokker",countryCode = "NL"),1621, imageUrl = "https://www.norebbo.com/2018/07/fokker-100-f-28-0100-blank-illustration-templates/"),
Model("Fokker 50","Fokker",60,132,471,2400,24000000,1560,4,Manufacturer("Fokker",countryCode = "NL"),1350),
Model("Fokker 70","Fokker",79,268,845,2010,32500000,1560,4,Manufacturer("Fokker",countryCode = "NL"),1300),
Model("Fokker 70ER","Fokker",79,276,845,3410,33500000,1560,4,Manufacturer("Fokker",countryCode = "NL"),1300),
Model("Gulfstream G650ER","Gulfstream",30,34,904,13890,30000000,2080,0,Manufacturer("Gulfstream",countryCode = "US"),1920, imageUrl = "https://www.norebbo.com/gulfstream-g650er-template/"),
Model("Ilyushin Il-18","Ilyushin Il",120,468,625,6500,41000000,1560,8,Manufacturer("Ilyushin",countryCode = "RU"),1350),
Model("Ilyushin Il-62","Ilyushin Il",186,963,900,10000,78500000,1300,32,Manufacturer("Ilyushin",countryCode = "RU"),2300),
Model("Ilyushin Il-86","Ilyushin Il-96",320,1650,950,5000,98000000,1300,32,Manufacturer("Ilyushin",countryCode = "RU"),2800),
Model("Ilyushin Il-96-300","Ilyushin Il-96",300,1560,900,13500,100000000,1300,36,Manufacturer("Ilyushin",countryCode = "RU"),3200),
Model("Ilyushin Il-96-400","Ilyushin Il-96",436,2398,900,14500,150000000,1300,36,Manufacturer("Ilyushin",countryCode = "RU"),2600),
Model("Irkut MC-21-100","Irkut MC-21",132,465,870,6140,66000000,1300,10,Manufacturer("Irkut",countryCode = "RU"),1322),
Model("Irkut MC-21-200","Irkut MC-21",165,590,870,6400,78000000,1300,12,Manufacturer("Irkut",countryCode = "RU"),1350),
Model("Irkut MC-21-300","Irkut MC-21",211,813,870,6000,86000000,1300,15,Manufacturer("Irkut",countryCode = "RU"),1644, imageUrl = "https://www.norebbo.com/irkut-mc-21-300/"),
Model("Irkut MC-21-400","Irkut MC-21",230,997,870,5500,95000000,1300,18,Manufacturer("Irkut",countryCode = "RU"),1500),
Model("Lockheed Constellation L-749","Lockheed",81,445,555,8039,17000000,1820,4,Manufacturer("Lockheed",countryCode = "US"),1050),
Model("Lockheed JetStar","Lockheed",12,16,920,4820,3000000,1820,0,Manufacturer("Lockheed",countryCode = "US"),1100),
Model("Lockheed L-1011-200","Lockheed",400,1960,954,6667,140000000,1560,36,Manufacturer("Lockheed",countryCode = "US"),2560),
Model("Lockheed L-1011-500","Lockheed",330,1682,972,9899,130000000,1560,36,Manufacturer("Lockheed",countryCode = "US"),2865, imageUrl = "https://www.norebbo.com/2015/03/lockheed-l-1011-500-blank-illustration-templates/"),
Model("McDonnell Douglas DC-3","McDonnell Douglas DC",32,192,333,2400,3800000,1040,0,Manufacturer("McDonnell Douglas",countryCode = "US"),1312),
Model("McDonnell Douglas DC-8-10","McDonnell Douglas DC",269,1398,946,4800,34000000,1040,8,Manufacturer("McDonnell Douglas",countryCode = "US"),2680, imageUrl = "https://www.norebbo.com/douglas-dc-8-61-blank-illustration-templates/"),
Model("McDonnell Douglas DC-8-72","McDonnell Douglas DC",189,945,895,10850,42000000,1040,8,Manufacturer("McDonnell Douglas",countryCode = "US"),2680, imageUrl = "https://www.norebbo.com/douglas-dc-8-73-and-dc-8-73cf-blank-illustration-templates/"),
Model("McDonnell Douglas DC-9-10","McDonnell Douglas DC",92,216,965,2367,29000000,1040,4,Manufacturer("McDonnell Douglas",countryCode = "US"),1816),
Model("McDonnell Douglas DC-9-30","McDonnell Douglas DC",115,302,804,2778,38500000,1040,4,Manufacturer("McDonnell Douglas",countryCode = "US"),1900, imageUrl = "https://www.norebbo.com/mcdonnell-douglas-dc-9-30-templates/"),
Model("McDonnell Douglas DC-9-50","McDonnell Douglas DC",139,417,804,3030,53000000,1040,4,Manufacturer("McDonnell Douglas",countryCode = "US"),2200, imageUrl = "https://www.norebbo.com/dc-9-50-side-view/"),
Model("McDonnell Douglas MD-11","McDonnell Douglas MD",293,1435,876,12670,125000000,1560,36,Manufacturer("McDonnell Douglas",countryCode = "US"),2964, imageUrl = "https://www.norebbo.com/2018/05/mcdonnell-douglas-md-11-blank-illustration-templates-with-ge-engines/"),
Model("McDonnell Douglas MD-220","McDonnell Douglas MD",35,45,1020,4100,10000000,1820,0,Manufacturer("McDonnell Douglas",countryCode = "US"),1200),
Model("McDonnell Douglas MD-81","McDonnell Douglas MD",150,525,811,4635,75000000,1560,16,Manufacturer("McDonnell Douglas",countryCode = "US"),2200, imageUrl = "https://www.norebbo.com/2015/02/mcdonnell-douglas-md-80-blank-illustration-templates/"),
Model("McDonnell Douglas MD-90","McDonnell Douglas MD",160,568,811,3787,80000000,1560,20,Manufacturer("McDonnell Douglas",countryCode = "US"),2134, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
Model("McDonnell Douglas MD-90ER","McDonnell Douglas MD",160,568,811,4143,81000000,1560,20,Manufacturer("McDonnell Douglas",countryCode = "US"),2134, imageUrl = "https://www.norebbo.com/2018/02/mcdonnell-douglas-md-90-blank-illustration-templates/"),
Model("McDonnell Douglas MD-11 ER","McDonnell Douglas MD",323,1650,886,13408,165000000,1560,28,Manufacturer("McDonnell Douglas",countryCode = "US"),3292),
Model("McDonnell Douglas MD-11 ST","McDonnell Douglas MD",410,2100,940,12392,215000000,1560,32,Manufacturer("McDonnell Douglas",countryCode = "US"),3292),
Model("McDonnell Douglas MD-12","McDonnell Douglas MD",511,3150,1050,14825,235000000,1560,36,Manufacturer("McDonnell Douglas",countryCode = "US"),3050),
Model("Mitsubishi MRJ-100","Mitsubishi SpaceJet",88,202,900,3540,68000000,2080,6,Manufacturer("Mitsubishi",countryCode = "JP"),1760),
Model("Mitsubishi MRJ-90","Mitsubishi SpaceJet",92,211,900,3770,75000000,2080,8,Manufacturer("Mitsubishi",countryCode = "JP"),1740),
Model("Pilatus PC-12","Pilatus",9,10,528,3417,1800000,1820,0,Manufacturer("Pilatus",countryCode = "CH"),758),
Model("Saab 2000","Saab Regional",58,120,608,2868,24500000,1820,4,Manufacturer("Saab",countryCode = "SE"),1252),
Model("Saab 90 Scandia","Saab Regional",32,63,340,2650,9000000,1820,0,Manufacturer("Saab",countryCode = "SE"),850),
Model("Shaanxi Y-10","Shaanxi Y",178,885,917,5560,45000000,1040,12,Manufacturer("Shaanxi Aircraft Corporation",countryCode = "CN"),2700),
Model("Sukhoi KR860","Sukhoi",860,5150,1000,15000,370000000,1300,32,Manufacturer("JSC Sukho",countryCode = "RU"),3400),
Model("Sukhoi Su-80","Sukhoi",32,49,520,1600,11000000,1300,0,Manufacturer("JSC Sukhoi",countryCode = "RU"),650),
Model("Sukhoi Superjet 100","Sukhoi",108,442,828,4578,38000000,1560,8,Manufacturer("JSC Sukhoi",countryCode = "RU"),1731, imageUrl = "https://www.norebbo.com/2016/02/sukhoi-ssj-100-blank-illustration-templates/"),
Model("Sukhoi Superjet 130NG","Sukhoi",130,520,871,4008,45600000,1560,0,Manufacturer("JSC Sukhoi",countryCode = "RU"),1731),
Model("Tupolev Tu-124","Tupolev Tu",56,253,970,2300,14000000,1300,8,Manufacturer("Tupolevi",countryCode = "RU"),1550),
Model("Tupolev Tu-204","Tupolev Tu",210,945,810,4300,50000000,1300,24,Manufacturer("Tupolev",countryCode = "RU"),1870, imageUrl = "https://www.norebbo.com/tupolev-tu-204-100-blank-illustration-templates/"),
Model("Tupolev Tu-334-100","Tupolev Tu",102,258,820,4100,40500000,1300,8,Manufacturer("Tupolev",countryCode = "RU"),1980),
Model("Tupolev Tu-334-200","Tupolev Tu",126,315,820,3150,40000000,1300,4,Manufacturer("Tupolev",countryCode = "RU"),1820),
Model("Vickers VC10","Vickers",150,645,930,9410,72000000,1560,12,Manufacturer("Vickers-Armstrongs",countryCode = "GB"),2520),
Model("Xi'an MA600","Xi'an Turboprops",60,99,514,1600,25000000,1300,6,Manufacturer("Xi'an Aircraft Industrial Corporation",countryCode = "CN"),750),
Model("Xi'an MA700","Xi'an Turboprops",86,195,637,1500,31000000,1300,10,Manufacturer("Xi'an Aircraft Industrial Corporation",countryCode = "CN"),630)
  )
  val modelByName = models.map { model => (model.name, model) }.toMap
}