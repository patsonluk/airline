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
Model("Boeing 737 MAX 10","Boeing 737",230,5,897,830,6500,132000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2700, imageUrl = "https://www.norebbo.com/2019/01/737-10-max-side-view/"),
Model("Boeing 777-200","Boeing 777",440,7,1980,896,9700,280000000,1820,36,Manufacturer("Boeing",countryCode = "US"),2440, imageUrl = "https://www.norebbo.com/2012/12/boeing-777-200-blank-illustration-templates/"),
  )
  val modelByName = models.map { model => (model.name, model) }.toMap
}