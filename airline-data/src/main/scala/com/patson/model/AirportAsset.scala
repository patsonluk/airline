package com.patson.model

case class AirportAssetBlueprint(airport : Airport, assetType : AirportAssetType.Value, var id : Int = 0) extends IdObject

abstract class AirportAsset() extends IdObject{
    val blueprint : AirportAssetBlueprint
    val airline : Option[Airline]
    val name : String

    val level : Int
    val completionCycle : Option[Int]
    val revenue : Long
    val expense : Long
    var boosts : List[AirportBoost]
    val properties : Map[String, Long]
    val id = blueprint.id
    val assetType = blueprint.assetType

    val status : AirportAssetStatus.Value


}

case class SkiResortAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class BeachResortAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class ConventionCenterAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class MuseumAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class ResidentialComplexAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class SportArenaAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class ShoppingMallAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class GrandHotelTouristAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class GrandHotelBusinessAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class AmusementParkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class SubwayAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class StadiumAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class ScienceParkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class LandmarkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset

case class SolarPowerPlantAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class TravelAgencyAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class GameArcadeAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class CinemaAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class InnAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class LuxuriousHotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class GolfCourseAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class OfficeBuilding1Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class HotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class OfficeBuilding2Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class RestaurantAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class OfficeBuilding3Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class OfficeBuilding4Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class AirportHotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset
case class CityTransitAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override val revenue : Long, override val expense : Long, override val properties : Map[String, Long]) extends AirportAsset


object AirportAsset {
    def getAirportAsset(id : Int, airport : Airport, assetType : AirportAssetType.Value, airline : Option[Airline], name : String, level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, properties : Map[String, Long], currentCycle : Int) : AirportAsset = {
        val blueprint = AirportAssetBlueprint(airport, assetType, id)
        getAirportAsset(blueprint, airline, name, level, completionCycle, boosts, revenue, expense, properties, currentCycle)
    }

    def getAirportAsset(blueprint : AirportAssetBlueprint, airline : Option[Airline], name : String, level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, properties : Map[String, Long], currentCycle : Int) : AirportAsset = {
        import AirportAssetType._
        blueprint.assetType match {
            case SKI_RESORT => SkiResortAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case RESIDENTIAL_COMPLEX => ResidentialComplexAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case CITY_TRANSIT => CityTransitAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case AIRPORT_HOTEL => AirportHotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case AMUSEMENT_PARK => AmusementParkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case SUBWAY => SubwayAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case STADIUM => StadiumAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case GRAND_HOTEL_TOURIST => GrandHotelTouristAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case GRAND_HOTEL_BUSINESS => GrandHotelBusinessAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case CONVENTION_CENTER => ConventionCenterAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case MUSEUM => MuseumAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case LANDMARK => LandmarkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case SCIENCE_PARK => ScienceParkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case SOLAR_POWER_PLANT => SolarPowerPlantAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case BEACH_RESORT => BeachResortAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case TRAVEL_AGENCY => TravelAgencyAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case SPORT_ARENA => SportArenaAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case GAME_ARCADE => GameArcadeAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case CINEMA => CinemaAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case INN => InnAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case GOLF_COURSE => GolfCourseAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case OFFICE_BUILDING_1 => OfficeBuilding1Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case HOTEL => HotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case OFFICE_BUILDING_2 => OfficeBuilding2Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case RESTAURANT => RestaurantAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case OFFICE_BUILDING_3 => OfficeBuilding3Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case SHOPPING_MALL => ShoppingMallAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case LUXURIOUS_HOTEL => LuxuriousHotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case OFFICE_BUILDING_4 => OfficeBuilding4Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
            case RESIDENTIAL_COMPLEX =>ResidentialComplexAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, properties)
        }


    }

    val status = (completionCycle : Option[Int], currentCycle : Int) =>
      completionCycle match {
          case Some(completionCycle) => if (completionCycle >= currentCycle) AirportAssetStatus.COMPLETED else AirportAssetStatus.UNDER_CONSTRUCTION
          case None => AirportAssetStatus.BLUEPRINT
      }
}


case class AirportBoost(boostType : AirportBoostType.Value, value : Double) //the value is of 1/100 for some attributes


object AirportAssetType extends Enumeration {

    abstract class AirportAssetType() extends super.Val {
        val constructionDuration : Int
        val label : String
        //val descriptions : List[String]
        val baseBoosts : List[AirportBoost]
        val baseCost : Long
        val baseRequirement : Int //base level req to build such asset
        //        def apply(airline : Airline, airport : Airport) {}
        //
        //        def unapply(airline : Airline, airport : Airport) {}
    }

    import AirportBoostType._

    case class SkiResortAssetType() extends AirportAssetType {
        override val label = "Ski Resort"
        override val constructionDuration : Int = 5 * 52
        //override val descriptions = List(s"Ski resort attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 4))
        override val baseCost : Long = 500000000
        override val baseRequirement : Int = 5
    }

    case class BeachResortAssetType() extends AirportAssetType {
        override val label = "Beach Resort"
        override val constructionDuration : Int = 3 * 52
        //override val descriptions = List(s"Beach resort attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 2))
        override val baseCost : Long = 200000000
        override val baseRequirement : Int = 3
    }

    case class ConventionCenterAssetType() extends AirportAssetType {
        override val label = "Convention Center"
        override val constructionDuration : Int = 10 * 52
        //override val descriptions = List(s"Convention center to boost business")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(FINANCIAL_HUB, 8))
        override val baseCost : Long = 2000000000
        override val baseRequirement : Int = 11
    }

    case class MuseumAssetType() extends AirportAssetType {
        override val label = "Museum"
        override val constructionDuration : Int = 8 * 52
        //override val descriptions = List(s"Museum attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INTERNATIONAL_HUB, 2))
        override val baseCost : Long = 800000000
        override val baseRequirement : Int = 9
    }

    case class ResidentialComplexAssetType() extends AirportAssetType {
        override val label = "Residential Complex"
        override val constructionDuration : Int = 2 * 52
        //override val descriptions = List(s"Residential Complex increases airport population")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 10000))
        override val baseCost : Long = 100000000
        override val baseRequirement : Int = 3
    }

    case class SportArenaAssetType() extends AirportAssetType {
        override val label = "Sport Arena"
        override val constructionDuration : Int = 3 * 52
        //override val descriptions = List(s"Sport Arena")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.50))
        override val baseCost : Long = 200000000
        override val baseRequirement : Int = 3
    }

    case class ShoppingMallAssetType() extends AirportAssetType {
        override val label = "Shopping Mall"
        override val constructionDuration : Int = 5 * 52
        //override val descriptions = List(s"Shopping Mall")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 1))
        override val baseCost : Long = 800000000
        override val baseRequirement : Int = 5
    }

    case class GrandHotelTouristAssetType() extends AirportAssetType {
        override val label = "Grand Tourist Hotel"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 2))
        override val baseCost : Long = 300000000
        override val baseRequirement : Int = 5
    }
    case class GrandHotelBusinessAssetType() extends AirportAssetType {
        override val label = "Grand Business Hotel"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(FINANCIAL_HUB, 2))
        override val baseCost : Long = 300000000
        override val baseRequirement : Int = 5
    }
    case class AmusementParkAssetType() extends AirportAssetType {
        override val label = "Amusement Park"
        override val constructionDuration : Int = 8 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 5))
        override val baseCost : Long = 800000000
        override val baseRequirement : Int = 9
    }
    case class SubwayAssetType() extends AirportAssetType {
        override val label = "Subway"
        override val constructionDuration : Int = 12 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 100000))
        override val baseCost : Long = 4000000000L
        override val baseRequirement : Int = 11
    }
    case class StadiumAssetType() extends AirportAssetType {
        override val label = "Stadium"
        override val constructionDuration : Int = 8 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INTERNATIONAL_HUB, 3), AirportBoost(FINANCIAL_HUB, 2))
        override val baseCost : Long = 800000000
        override val baseRequirement : Int = 10
    }
    case class ScienceParkAssetType() extends AirportAssetType {
        override val label = "Science Park"
        override val constructionDuration : Int = 12 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 2), AirportBoost(FINANCIAL_HUB, 5), AirportBoost(POPULATION, 50000))
        override val baseCost : Long = 5000000000L
        override val baseRequirement : Int = 12
    }
    case class LandmarkAssetType() extends AirportAssetType {
        override val label = "Landmark"
        override val constructionDuration : Int = 10 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INTERNATIONAL_HUB, 4))
        override val baseCost : Long = 2000000000
        override val baseRequirement : Int = 11
    }

    case class SolarPowerPlantAssetType() extends AirportAssetType {
        override val label = "Solr Power Plant"
        override val constructionDuration : Int = 8 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 20000), AirportBoost(INCOME, 2))
        override val baseCost : Long = 500000000
        override val baseRequirement : Int = 5
    }
    case class TravelAgencyAssetType() extends AirportAssetType {
        override val label = "Travel Agency"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.30))
        override val baseCost : Long = 100000000
        override val baseRequirement : Int = 3
    }
    case class GameArcadeAssetType() extends AirportAssetType {
        override val label = "Sport Arena"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.20))
        override val baseCost : Long = 30000000
        override val baseRequirement : Int = 3
    }
    case class CinemaAssetType() extends AirportAssetType {
        override val label = "Cinema"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.50))
        override val baseCost : Long = 200000000
        override val baseRequirement : Int = 3
    }
    case class InnAssetType() extends AirportAssetType {
        override val label = "Inn"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.30))
        override val baseCost : Long = 10000000
        override val baseRequirement : Int = 1
    }
    case class GolfCourseAssetType() extends AirportAssetType {
        override val label = "Golf Course"
        override val constructionDuration : Int = 8 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 1))
        override val baseCost : Long = 2000000000
        override val baseRequirement : Int = 5
    }
    case class OfficeBuilding1AssetType() extends AirportAssetType {
        override val label = "Office Building I"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.70))
        override val baseCost : Long = 300000000
        override val baseRequirement : Int = 7
    }
    case class HotelAssetType() extends AirportAssetType {
        override val label = "Hotel"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.50))
        override val baseCost : Long = 200000000
        override val baseRequirement : Int = 5
    }
    case class OfficeBuilding2AssetType() extends AirportAssetType {
        override val label = "Office Building II"
        override val constructionDuration : Int = 7 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 1))
        override val baseCost : Long = 500000000
        override val baseRequirement : Int = 9
    }
    case class RestaurantAssetType() extends AirportAssetType {
        override val label = "Restaurant"
        override val constructionDuration : Int = 2 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.30))
        override val baseCost : Long = 10000000
        override val baseRequirement : Int = 1
    }
    case class OfficeBuilding3AssetType() extends AirportAssetType {
        override val label = "Office Building III"
        override val constructionDuration : Int = 9 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 1.20))
        override val baseCost : Long = 1000000000
        override val baseRequirement : Int = 11
    }
    case class LuxuriousHotelAssetType() extends AirportAssetType {
        override val label = "Luxurious Hotel"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.80))
        override val baseCost : Long = 500000000
        override val baseRequirement : Int = 7
    }
    case class OfficeBuilding4AssetType() extends AirportAssetType {
        override val label = "Office Building IV"
        override val constructionDuration : Int = 12 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 1.50))
        override val baseCost : Long = 1500000000
        override val baseRequirement : Int = 12
    }
    case class CityTransitAssetType() extends AirportAssetType {
        override val label = "City Transit"
        override val constructionDuration : Int = 12 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 100000))
        override val baseCost : Long = 2000000000
        override val baseRequirement : Int = 10
    }
    case class AirportHotelAssetType() extends AirportAssetType {
        override val label = "Airport Hotel"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List()
        override val baseCost : Long = 200000000
        override val baseRequirement : Int = 7
    }


    implicit def valueToAirportAssetType(x : Value) = x.asInstanceOf[AirportAssetType]

    val CITY_TRANSIT = CityTransitAssetType()
    val AIRPORT_HOTEL = AirportHotelAssetType()
    val AMUSEMENT_PARK = AmusementParkAssetType()
    val SUBWAY = SubwayAssetType()
    val STADIUM = StadiumAssetType()
    val GRAND_HOTEL_TOURIST = GrandHotelTouristAssetType()
    val GRAND_HOTEL_BUSINESS = GrandHotelBusinessAssetType()
    val CONVENTION_CENTER = ConventionCenterAssetType()
    val MUSEUM = MuseumAssetType()
    val LANDMARK = LandmarkAssetType()
    val SCIENCE_PARK = ScienceParkAssetType()


    val SOLAR_POWER_PLANT = SolarPowerPlantAssetType()
    val BEACH_RESORT = BeachResortAssetType()
    val SKI_RESORT = SkiResortAssetType()


    val TRAVEL_AGENCY = TravelAgencyAssetType()
    val SPORT_ARENA = SportArenaAssetType()
    val GAME_ARCADE = GameArcadeAssetType()
    val CINEMA = CinemaAssetType()
    val INN = InnAssetType()
    val GOLF_COURSE = GolfCourseAssetType()
    val OFFICE_BUILDING_1 = OfficeBuilding1AssetType()
    val HOTEL = HotelAssetType()
    val OFFICE_BUILDING_2 = OfficeBuilding2AssetType()
    val RESTAURANT = RestaurantAssetType()
    val OFFICE_BUILDING_3 = OfficeBuilding3AssetType()
    val SHOPPING_MALL = ShoppingMallAssetType()
    val LUXURIOUS_HOTEL = LuxuriousHotelAssetType()
    val OFFICE_BUILDING_4 = OfficeBuilding4AssetType()
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


