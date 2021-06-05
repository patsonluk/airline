package com.patson.model.airplane

import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane.Model.Category
import com.patson.model.airplane.Model.Type.{JUMBO, LARGE, LIGHT, MEDIUM, REGIONAL, SMALL, SUPERSONIC, X_LARGE}
import com.patson.util.{AirplaneModelCache, AirplaneOwnershipCache}

import scala.collection.MapView
import scala.collection.mutable.ListBuffer

case class ModelDiscount(modelId : Int, discount : Double, discountType : DiscountType.Value, discountReason : DiscountReason.Value, expirationCycle : Option[Int]) {
  val description = discountReason match {
    case DiscountReason.FAVORITE => s"${(discount * 100).toInt}% off ${DiscountType.description(discountType)} for being the favorite model"
    case DiscountReason.PREFERRED_SUPPLIER => s"${(discount * 100).toInt}% off ${DiscountType.description(discountType)} for being preferred supplier"
    case DiscountReason.LOW_DEMAND => s"${(discount * 100).toInt}% off ${DiscountType.description(discountType)} due to low demand"
  }
}

object ModelDiscount {
  val MAKE_FAVORITE_PERCENTAGE_THRESHOLD = 5 //5%
  val MAKE_FAVORITE_RESET_THRESHOLD = 52 //1 year at least

  val getFavoriteDiscounts: Model => List[ModelDiscount] = (model : Model) => {
    val constructionTimeDiscount = ModelDiscount(model.id, 0.25, DiscountType.CONSTRUCTION_TIME, DiscountReason.FAVORITE, None)
    val priceDiscount = model.airplaneType match {
      case LIGHT => 0.20
      case REGIONAL => 0.15
      case SMALL => 0.10
      case MEDIUM => 0.06
      case LARGE => 0.04
      case X_LARGE => 0.03
      case JUMBO => 0.02
      case SUPERSONIC => 0.05
    }
    List(ModelDiscount(model.id, priceDiscount, DiscountType.PRICE, DiscountReason.FAVORITE, None), constructionTimeDiscount)
  }

  def getAllCombinedDiscountsByAirlineId(airlineId : Int) : Map[Int, List[ModelDiscount]] = {
    val airlineDiscountByModelId : Map[Int, List[ModelDiscount]] = ModelSource.loadAirlineDiscountsByAirlineId(airlineId).groupBy(_.modelId)
    val supplierDiscountInfoByCategory : Map[Model.Category.Value, PreferredSupplierDiscountInfo] = getPreferredSupplierDiscounts(airlineId)
    val blanketModelDiscountByModelId : Map[Int, List[ModelDiscount]] = ModelSource.loadAllModelDiscounts().groupBy(_.modelId)
    AirplaneModelCache.allModels.values.map { model =>
      val discounts = ListBuffer[ModelDiscount]()
      airlineDiscountByModelId.get(model.id).foreach(discounts.appendAll(_))
      getPreferredSupplierDiscountByModelId(supplierDiscountInfoByCategory, model.id).foreach(discounts.append(_))
      blanketModelDiscountByModelId.get(model.id).foreach(discounts.appendAll(_))
      (model.id, discounts.toList)
    }.toMap
  }

  /**
    * Get discounts including both specific to airline and those blanket to model
    * @param airlineId
    * @param modelId
    * @return
    */
  def getCombinedDiscountsByModelId(airlineId : Int, modelId : Int) : List[ModelDiscount] = {
    val discounts = ListBuffer[ModelDiscount]()
    //get airline specific discounts
    discounts.appendAll(ModelSource.loadAirlineDiscountsByAirlineIdAndModelId(airlineId, modelId))
    //get preferred supplier discounts
    getPreferredSupplierDiscountByModelId(airlineId, modelId).foreach {
      discounts.append(_)
    }
    //get blanket model discounts
    discounts.appendAll(getBlanketModelDiscounts(modelId))
    discounts.toList
  }

  /**
    * Get discounts that is blanket to the model
    * @param modelId
    * @return
    */
  def getBlanketModelDiscounts(modelId : Int)  : List[ModelDiscount] = {
    val discounts = ListBuffer[ModelDiscount]()
    //get blanket model discounts
    discounts.appendAll(ModelSource.loadModelDiscountsByModelId(modelId))
    discounts.toList
  }


  def getPreferredSupplierDiscounts(airlineId: Int) : Map[Model.Category.Value, PreferredSupplierDiscountInfo] = {
    val currentSuppliersByCategory : MapView[Model.Category.Value, List[Manufacturer]] = AirplaneOwnershipCache.getOwnership(airlineId).groupBy(_.model.category).view.mapValues(_.map(_.model.manufacturer).distinct)
    Category.values.toList.map { category =>
      val info = currentSuppliersByCategory.get(category) match {
        case None => PreferredSupplierDiscountInfo(None, category, None, "No preferred Supplier")
        case Some(currentSuppliers) =>
          if (currentSuppliers.length == 1) {
            val discount = category match {
              case Category.LIGHT => 0.1
              case Category.REGIONAL => 0.07
              case Category.MEDIUM => 0.05
              case Category.LARGE => 0.02
              case Category.SUPERSONIC => 0
            }
            if (discount > 0) {
              PreferredSupplierDiscountInfo(Some(discount), category, Some(currentSuppliers(0)), s"${(discount * 100).toInt}% off price for being preferred supplier")
            } else {
              PreferredSupplierDiscountInfo(None, category, Some(currentSuppliers(0)), s"${category.toString} offers no discount")
            }
          } else {
            PreferredSupplierDiscountInfo(None, category, None, "No discount as there are more than one supplier")
          }
      }
      (category, info)
    }.toMap
  }
  case class PreferredSupplierDiscountInfo(discount : Option[Double], category: Model.Category.Value, soleSupplier : Option[Manufacturer], description : String)



  def getPreferredSupplierDiscountByModelId(airlineId : Int, modelId : Int) : Option[ModelDiscount] = {
//    val model = AirplaneModelCache.getModel(modelId).get
//    val currentSuppliersByCategory : MapView[Model.Category.Value, List[Manufacturer]] = AirplaneOwnershipCache.getOwnership(airlineId).groupBy(_.model.category).view.mapValues(_.map(_.model.manufacturer).distinct)
//    currentSuppliersByCategory.get(model.category) match {
//      case None => None
//      case Some(currentSuppliers) =>
//        if (currentSuppliers.length == 1 && currentSuppliers(0) == model.manufacturer) {
//          val discount = model.category match {
//            case Category.LIGHT => 0.1
//            case Category.MEDIUM => 0.05
//            case Category.LARGE => 0.02
//            case Category.SUPERSONIC => 0
//          }
//          if (discount > 0) {
//            Some(ModelDiscount(model.id, discount, DiscountType.PRICE, DiscountReason.PREFERRED_SUPPLIER, None))
//          } else {
//            None
//          }
//        } else {
//          None
//        }
//    }
    getPreferredSupplierDiscountByModelId(getPreferredSupplierDiscounts(airlineId), modelId)
  }

  def getPreferredSupplierDiscountByModelId(lookup : Map[Category.Value, PreferredSupplierDiscountInfo], modelId : Int) : Option[ModelDiscount] = {
    val model = AirplaneModelCache.getModel(modelId).get
    val supplierDiscountInfo = lookup(model.category)

    supplierDiscountInfo.soleSupplier match {
      case Some(soleSupplier) =>
        if (soleSupplier == model.manufacturer) {
          supplierDiscountInfo.discount match {
            case Some(discount) => Some(ModelDiscount(modelId, discount, DiscountType.PRICE, DiscountReason.PREFERRED_SUPPLIER, None))
            case None => None
          }
        } else {
          None
        }
      case None => None
    }
  }
}



object DiscountReason extends Enumeration {
  type Type = Value
  val FAVORITE, LOW_DEMAND, PREFERRED_SUPPLIER = Value
}

object DiscountType extends Enumeration {
  type Type = Value
  val PRICE, CONSTRUCTION_TIME = Value

  val description = (discountType : DiscountType.Value) => { discountType match {
      case PRICE => "Price"
      case CONSTRUCTION_TIME => "Construction Time"
      case _ => "Unknown"
    }
  }
}

