package com.patson.model

import com.patson.util.AirlineCache

case class AirportAssetBlueprint(airport : Airport, assetType : AirportAssetType.Value, var id : Int = 0) extends IdObject

abstract class AirportAsset() extends IdObject{
    val blueprint : AirportAssetBlueprint
    val airline : Option[Airline]

    val level : Int
    val completionCycle : Option[Int]
    val revenue : Long
    val expense : Long
    val boosts : List[AirportBoost]
    val properties : Map[String, Long]
    val id = blueprint.id
    val assetType = blueprint.assetType

    val status : AirportAssetStatus.Value
}

case class SkiResortAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override val boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class ResidentialComplexAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override val boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset

object AirportAsset {
    def getAirportAsset(id : Int, airport : Airport, assetType : AirportAssetType.Value, airline : Option[Airline], level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, properties : Map[String, Long], currentCycle : Int) : AirportAsset = {
        val blueprint = AirportAssetBlueprint(airport, assetType, id)
        getAirportAsset(blueprint, airline, level, completionCycle, boosts, revenue, expense, properties, currentCycle)
    }

    def getAirportAsset(blueprint : AirportAssetBlueprint, airline : Option[Airline], level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, properties : Map[String, Long], currentCycle : Int) : AirportAsset = {
        import AirportAssetType._
        blueprint.assetType match {
            case SKI_RESORT => SkiResortAsset(blueprint, airline, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case RESIDENTIAL_COMPLEX => ResidentialComplexAsset(blueprint, airline, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
        }
    }

    val status = (completionCycle : Option[Int], currentCycle : Int) =>
      completionCycle match {
          case Some(completionCycle) => if (completionCycle >= currentCycle) AirportAssetStatus.COMPLETED else AirportAssetStatus.UNDER_CONSTRUCTION
          case None => AirportAssetStatus.BLUEPRINT
      }
}


case class AirportBoost(boostType : AirportBoostType.Value, value : Long)


object AirportAssetType extends Enumeration {
    abstract class AirportAssetType() extends super.Val {
        val constructionDuration : Int
        val label : String
        val descriptions : List[String]
//        def apply(airline : Airline, airport : Airport) {}
//
//        def unapply(airline : Airline, airport : Airport) {}
    }

    case class SkiResortAssetType() extends AirportAssetType {
        override val label = "Ski Resort"
        override val constructionDuration : Int = 5 * 52
        override val descriptions = List(s"Ski resort attracts tourists")
    }

    case class ResidentialComplexAssetType() extends AirportAssetType {
        override val label = "Residential Complex"
        override val constructionDuration : Int = 2 * 52
        override val descriptions = List(s"Residential Complex increases airport population")
    }

    implicit def valueToSpecialization(x: Value) = x.asInstanceOf[AirportAssetType]

    val SKI_RESORT = SkiResortAssetType()
    val RESIDENTIAL_COMPLEX = ResidentialComplexAssetType()
}





object AirportBoostType extends Enumeration {
    type AirportBoostType = Value
    val POPULATION, INCOME, INTERNATIONAL_HUB, VACATION_HUB, FINANCIAL_HUB = Value

}

object AirportAssetStatus extends Enumeration {
    type ProjectStatus = Value
    val BLUEPRINT, UNDER_CONSTRUCTION, COMPLETED = Value
}
