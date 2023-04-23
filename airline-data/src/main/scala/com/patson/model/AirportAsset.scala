package com.patson.model

import com.patson.data.{AirportAssetSource, AirportSource, CycleSource}
import com.patson.model.AirportAssetType.{PassengerCostAssetModifier, TransitWaitTimeModifier}
import com.patson.model.Country.CountryCode

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

case class AirportAssetBlueprint(airport : Airport, assetType : AirportAssetType.Value, var id : Int = 0) extends IdObject

object AirportAssetType extends Enumeration {
    abstract class AirportAssetType() extends super.Val {
        val constructionDuration : Int
        val label : String
        val descriptions : List[String] = List(s"Description 1 : blah blah blah", s"Description 2 : blah blah blah")
        val baseBoosts : List[AirportBoost]
        val baseCost : Long
        val baseRequirement : Int //base level req to build such asset
        val maxRoi : Double //annual
        val initRoi : Double
        def minRoi = initRoi / 2
        def upgradeCooldown = constructionDuration  //same for now
        val publicPropertyKeys = List[String]()
        val privatePropertyKeys = List[String]()
    }

    abstract class HotelAssetType() extends AirportAssetType {
        override val publicPropertyKeys = List[String]("capacity", "rate")
        override val privatePropertyKeys = List[String]("occupancy")
    }

    abstract class AdmissionAssetType() extends AirportAssetType {
        override val publicPropertyKeys = List[String]("capacity", "rate")
        override val privatePropertyKeys = List[String]("visitors")
    }

    abstract class RentalAssetType() extends AirportAssetType {
        override val publicPropertyKeys = List[String]("totalSpace", "rate100Point")
        override val privatePropertyKeys = List[String]("leasedSpace")
    }


    import AirportBoostType._

    case class SkiResortAssetType() extends HotelAssetType {
        override val label = "Ski Resort"
        override val constructionDuration : Int = 3 * 52
        //override val descriptions = List(s"Ski resort attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 4))
        override val baseCost : Long = 500_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 25
        override val initRoi : Double = 1.0 / 70
    }

    case class BeachResortAssetType() extends HotelAssetType {
        override val label = "Beach Resort"
        override val constructionDuration : Int = 2 * 52
        //override val descriptions = List(s"Beach resort attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 2))
        override val baseCost : Long = 200_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 20
        override val initRoi : Double = 1.0 / 50
    }

    case class ConventionCenterAssetType() extends AirportAssetType {
        override val label = "Convention Center"
        override val constructionDuration : Int = 6 * 52
        //override val descriptions = List(s"Convention center to boost business")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(FINANCIAL_HUB, 8))
        override val baseCost : Long = 2_000_000_000
        override val baseRequirement : Int = 11

        override val maxRoi : Double = 1.0 / 50
        override val initRoi : Double = 1.0 / 100
    }

    case class MuseumAssetType() extends AdmissionAssetType {
        override val label = "Museum"
        override val constructionDuration : Int = 4 * 52
        //override val descriptions = List(s"Museum attracts tourists")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INTERNATIONAL_HUB, 2))
        override val baseCost : Long = 800_000_000
        override val baseRequirement : Int = 9

        override val maxRoi : Double = 1.0 / 50
        override val initRoi : Double = 1.0 / 100
    }

    case class ResidentialComplexAssetType() extends RentalAssetType {
        override val label = "Residential Complex"
        override val constructionDuration : Int = 2 * 52
        //override val descriptions = List(s"Residential Complex increases airport population")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 25000))
        override val baseCost : Long = 200_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 25
        override val initRoi : Double = 1.0 / 50
    }

    case class SportArenaAssetType() extends AdmissionAssetType {
        override val label = "Sport Arena"
        override val constructionDuration : Int = 2 * 52
        //override val descriptions = List(s"Sport Arena")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.1))
        override val baseCost : Long = 200_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 35
        override val initRoi : Double = 1.0 / 100
    }

    case class ShoppingMallAssetType() extends RentalAssetType {
        override val label = "Shopping Mall"
        override val constructionDuration : Int = 3 * 52
        //override val descriptions = List(s"Shopping Mall")
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.2))
        override val baseCost : Long = 800_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 25
        override val initRoi : Double = 1.0 / 80
    }

    case class GrandHotelTouristAssetType() extends HotelAssetType {
        override val label = "Grand Tourist Hotel"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 2))
        override val baseCost : Long = 300_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 35
        override val initRoi : Double = 1.0 / 60
    }
    case class GrandHotelBusinessAssetType() extends HotelAssetType {
        override val label = "Grand Business Hotel"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(FINANCIAL_HUB, 2))
        override val baseCost : Long = 300_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 35
        override val initRoi : Double = 1.0 / 60
    }
    case class AmusementParkAssetType() extends AdmissionAssetType {
        override val label = "Amusement Park"
        override val constructionDuration : Int = 4 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 5))
        override val baseCost : Long = 800_000_000
        override val baseRequirement : Int = 9

        override val maxRoi : Double = 1.0 / 20
        override val initRoi : Double = 1.0 / 100
    }
    case class SubwayAssetType() extends AirportAssetType {
        override val label = "Subway"
        override val constructionDuration : Int = 6 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 100000)) //base
        override val baseCost : Long = 4_000_000_000L
        override val baseRequirement : Int = 11

        override val maxRoi : Double = 1.0 / 80
        override val initRoi : Double = 1.0 / 200
    }
    case class StadiumAssetType() extends AdmissionAssetType {
        override val label = "Stadium"
        override val constructionDuration : Int = 4 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(VACATION_HUB, 2), AirportBoost(FINANCIAL_HUB, 1))
        override val baseCost : Long = 600_000_000
        override val baseRequirement : Int = 10

        override val maxRoi : Double = 1.0 / 50
        override val initRoi : Double = 1.0 / 200
    }
    case class ScienceParkAssetType() extends RentalAssetType {
        override val label = "Science Park"
        override val constructionDuration : Int = 6 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.5), AirportBoost(FINANCIAL_HUB, 5), AirportBoost(POPULATION, 50000))
        override val baseCost : Long = 5_000_000_000L
        override val baseRequirement : Int = 12

        override val maxRoi : Double = 1.0 / 100
        override val initRoi : Double = 1.0 / 500
    }
    case class LandmarkAssetType() extends AdmissionAssetType {
        override val label = "Landmark"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INTERNATIONAL_HUB, 3))
        override val baseCost : Long = 1_000_000_000
        override val baseRequirement : Int = 11

        override val maxRoi : Double = 1.0 / 50
        override val initRoi : Double = 1.0 / 200
    }

    case class SolarPowerPlantAssetType() extends AirportAssetType {
        override val label = "Solar Power Plant"
        override val constructionDuration : Int = 4 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 20000), AirportBoost(INCOME, 0.3))
        override val baseCost : Long = 500000000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 30
        override val initRoi : Double = 1.0 / 40
    }
    case class TravelAgencyAssetType() extends AirportAssetType {
        override val label = "Travel Agency"
        override val constructionDuration : Int = 1 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.05))
        override val baseCost : Long = 100_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 10
        override val initRoi : Double = 1.0 / 100
    }
    case class GameArcadeAssetType() extends AirportAssetType {
        override val label = "Game Arcade"
        override val constructionDuration : Int = 1 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.02))
        override val baseCost : Long = 30_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 15
        override val initRoi : Double = 1.0 / 50
    }
    case class CinemaAssetType() extends AdmissionAssetType {
        override val label = "Cinema"
        override val constructionDuration : Int = 2 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.05))
        override val baseCost : Long = 50_000_000
        override val baseRequirement : Int = 3

        override val maxRoi : Double = 1.0 / 30
        override val initRoi : Double = 1.0 / 50
    }
    case class InnAssetType() extends HotelAssetType {
        override val label = "Inn"
        override val constructionDuration : Int = 1 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.02))
        override val baseCost : Long = 10_000_000
        override val baseRequirement : Int = 1

        override val maxRoi : Double = 1.0 / 8
        override val initRoi : Double = 1.0 / 30
    }
    case class GolfCourseAssetType() extends AdmissionAssetType {
        override val label = "Golf Course"
        override val constructionDuration : Int = 4 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.1))
        override val baseCost : Long = 400_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 25
        override val initRoi : Double = 1.0 / 80
    }
    case class OfficeBuilding1AssetType() extends RentalAssetType {
        override val label = "Office Building I"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.1))
        override val baseCost : Long = 300_000_000
        override val baseRequirement : Int = 7

        override val maxRoi : Double = 1.0 / 35
        override val initRoi : Double = 1.0 / 70
    }
    case class AverageHotelAssetType() extends HotelAssetType {
        override val label = "Hotel"
        override val constructionDuration : Int = 2 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.05))
        override val baseCost : Long = 100_000_000
        override val baseRequirement : Int = 5

        override val maxRoi : Double = 1.0 / 25
        override val initRoi : Double = 1.0 / 60
    }
    case class OfficeBuilding2AssetType() extends RentalAssetType {
        override val label = "Office Building II"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.12))
        override val baseCost : Long = 500_000_000
        override val baseRequirement : Int = 9

        override val maxRoi : Double = 1.0 / 45
        override val initRoi : Double = 1.0 / 90
    }
    case class RestaurantAssetType() extends AirportAssetType {
        override val label = "Restaurant"
        override val constructionDuration : Int = 1 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.02))
        override val baseCost : Long = 10_000_000
        override val baseRequirement : Int = 1

        override val maxRoi : Double = 1.0 / 10
        override val initRoi : Double = 1.0 / 50
    }
    case class OfficeBuilding3AssetType() extends RentalAssetType {
        override val label = "Office Building III"
        override val constructionDuration : Int = 4 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.2))
        override val baseCost : Long = 1_000_000_000
        override val baseRequirement : Int = 11

        override val maxRoi : Double = 1.0 / 55
        override val initRoi : Double = 1.0 / 100
    }
    case class LuxuriousHotelAssetType() extends HotelAssetType {
        override val label = "Luxurious Hotel"
        override val constructionDuration : Int = 3 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.1))
        override val baseCost : Long = 500_000_000
        override val baseRequirement : Int = 7

        override val maxRoi : Double = 1.0 / 30
        override val initRoi : Double = 1.0 / 100
    }
    case class OfficeBuilding4AssetType() extends RentalAssetType {
        override val label = "Office Building IV"
        override val constructionDuration : Int = 5 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(INCOME, 0.25))
        override val baseCost : Long = 1_500_000_000
        override val baseRequirement : Int = 12

        override val maxRoi : Double = 1.0 / 65
        override val initRoi : Double = 1.0 / 120
    }
    case class CityTransitAssetType() extends AirportAssetType {
        override val label = "City Transit"
        override val constructionDuration : Int = 6 * 52
        override val baseBoosts : List[AirportBoost] = List(AirportBoost(POPULATION, 100000))
        override val baseCost : Long = 2_000_000_000
        override val baseRequirement : Int = 10

        override val maxRoi : Double = 1.0 / 80
        override val initRoi : Double = 1.0 / 150
    }
    case class AirportHotelAssetType() extends HotelAssetType {
        override val label = "Airport Hotel"
        override val constructionDuration : Int = 2 * 52
        override val baseBoosts : List[AirportBoost] = List()
        override val baseCost : Long = 200_000_000
        override val baseRequirement : Int = 7

        override val maxRoi : Double = 1.0 / 20
        override val initRoi : Double = 1.0 / 60

    }

    trait TransitModifier {
        def computeTransitDiscount(fromLinkConsideration : LinkConsideration, toLinkConsideration : LinkConsideration, paxGroup : PassengerGroup): Double


    }
    trait PassengerCostModifier {
        def computeDiscount(linkConsideration : LinkConsideration, paxGroup : PassengerGroup): Option[Double]
    }

    trait TransitWaitTimeModifier extends TransitModifier {
        override def computeTransitDiscount(fromLinkConsideration : LinkConsideration, toLinkConsideration : LinkConsideration, paxGroup : PassengerGroup) : Double = {
            val fromLinkFreq = fromLinkConsideration.link.frequencyByClass(fromLinkConsideration.linkClass)
            val toLinkFreq = toLinkConsideration.link.frequencyByClass(fromLinkConsideration.linkClass)
            if (isOperational()) {
                computeTransitFreqDiscount(fromLinkFreq, toLinkFreq, paxGroup)
            } else {
                0
            }
        }
        def computeTransitFreqDiscount(arrivalLinkFreq : Int, departureLinkFreq : Int, paxGroup : PassengerGroup) : Double
        def isOperational() : Boolean
    }


    trait PassengerCostAssetModifier extends PassengerCostModifier {
        override def computeDiscount(linkConsideration : LinkConsideration, paxGroup : PassengerGroup) : Option[Double] = {
            if (paxGroup.preference.getPreferenceType == FlightPreferenceType.SPEED || !isOperational()) {
                None
            } else if (ThreadLocalRandom.current().nextInt(100) < Math.max(1, probability * level / AirportAsset.MAX_LEVEL)){
                Some(computeAssetDiscount(paxGroup))
            } else {
                None
            }

        }

        def computeAssetDiscount(paxGroup : PassengerGroup) : Double = {
            if (paxGroup.passengerType == PassengerType.BUSINESS) {
                businessDiscount * (0.5 + 0.5 * level / AirportAsset.MAX_LEVEL)
            } else {
                touristDiscount * (0.5 + 0.5 * level / AirportAsset.MAX_LEVEL)
            }
        }

        def probability : Int //probability in percentage that this asset will be picked for stop over consideration
        def touristDiscount : Double //max discount to fromCost at max level for Passenger Type NON BUSINESS (mostly tourist, sometimes olympic pax too), for example 0.1 means this asset can impost 10% discount to the from cost
        def businessDiscount : Double //max discount to fromCost at max level for Passenger Type BUSINESS

        def level : Int
        def isOperational() : Boolean
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
    val HOTEL = AverageHotelAssetType()
    val OFFICE_BUILDING_2 = OfficeBuilding2AssetType()
    val RESTAURANT = RestaurantAssetType()
    val OFFICE_BUILDING_3 = OfficeBuilding3AssetType()
    val SHOPPING_MALL = ShoppingMallAssetType()
    val LUXURIOUS_HOTEL = LuxuriousHotelAssetType()
    val OFFICE_BUILDING_4 = OfficeBuilding4AssetType()
    val RESIDENTIAL_COMPLEX = ResidentialComplexAssetType()
}


abstract class AirportAsset() extends IdObject{
    val blueprint : AirportAssetBlueprint
    val airline : Option[Airline]
    val name : String

    def baseBoosts = blueprint.assetType.baseBoosts.find(_.boostType == AirportBoostType.INCOME) match {
        case Some(_) => //need to create a new list
            blueprint.assetType.baseBoosts.map { baseBoost =>
                baseBoost.boostType match {
                    case AirportBoostType.INCOME =>
                        baseBoost.copy(value = Computation.computeIncomeLevelBoostFromLevel(airport.baseIncome, baseBoost.value)) //income level boost need adjustments for high income country
                    case _ => baseBoost
                }
            }
        case None => blueprint.assetType.baseBoosts //can just use the list directly
    }

    val level : Int
    val completionCycle : Option[Int]
    var revenue : Long
    var expense : Long
    var roi : Double //max profit, changes during upgrades. Hidden from player
    var boosts : List[AirportBoost] //this should contain the "current boosts" (under construction - upgrade. this should have previous level bonus still)
    var properties : Map[String, Long]
    val id = blueprint.id
    val assetType = blueprint.assetType
    val airport = blueprint.airport
    var upgradeApplied : Boolean = false

    val costModifierConst = {
        val incomeModifier = 0.2 + (airport.baseIncome.toDouble / Computation.MAX_INCOME) * 0.8 //0.2 to 1

        val populationModifier = (1 + 0.1 * Math.max(-10, (Math.log(airport.basePopulation.toDouble / Computation.MAX_POPULATION)))) //Each e away, 10% less.

        val featureRatio = AirportSource.loadAirportFeatures(airport.id).map { feature => //have to reload features, since the airport in blueprint is not full load, doing so might have cyclic dependencies issue
          import com.patson.model.AirportFeatureType._
            feature.featureType match {
                case INTERNATIONAL_HUB =>
                    feature.strength.toDouble / feature.MAX_STRENGTH * 0.7
                case VACATION_HUB =>
                    feature.strength.toDouble / feature.MAX_STRENGTH * 0.3
                case FINANCIAL_HUB =>
                    feature.strength.toDouble / feature.MAX_STRENGTH * 0.3
                case GATEWAY_AIRPORT =>
                    feature.strength.toDouble / feature.MAX_STRENGTH * 0.1
                case _ => 0
            }
        }.sum


        val randomRatio = 0.8 + new Random(id).nextDouble() * 0.4   //20% fluctuation ie 0.8 - 1.2
        Math.max(0.2, (4 + featureRatio * 6) * incomeModifier * populationModifier * randomRatio)
    }

    def costModifier = costModifierConst

    val status : AirportAssetStatus.Value
    val cost = (blueprint.assetType.baseCost * costModifier).toLong / 1000 * 1000 //zero last 3 digits
    val value = cost * (status match {
        case AirportAssetStatus.BLUEPRINT => 0
        case AirportAssetStatus.UNDER_CONSTRUCTION => level - 1
        case AirportAssetStatus.COMPLETED => level
    })
    val sellValue = (value * 0.5).toLong
    def isOperational() = { level > 1 || status == AirportAssetStatus.COMPLETED }


    def levelUp(name : String) = {
        val currentCycle = CycleSource.loadCycle()
        val completionCycle = currentCycle + assetType.constructionDuration

        //do not generate new boosts here, should let only do it when upgrade is completed
        AirportAsset.getAirportAsset(blueprint, airline, name, level + 1, Some(completionCycle), boosts, revenue, expense, roi, false, properties, currentCycle)
    }

    lazy val boostHistory : List[AirportAssetBoostHistory] = {
        AirportAssetSource.loadAirportBoostHistoryByAssetId(id)
    }
    lazy val propertiesHistory : List[AirportAssetPropertiesHistory] = {
        AirportAssetSource.loadAirportPropertyHistoryByAssetId(id)
    }

    lazy val publicProperties : Map[String, Long] = {
        properties.filter {
            case(key, _) => assetType.publicPropertyKeys.contains(key)
        }
    }

    lazy val privateProperties : Map[String, Long] = {
        properties.filter {
            case(key, _) => assetType.privatePropertyKeys.contains(key)
        }
    }

    lazy val propertyHistoryLastCycle =  {
        AirportAssetSource.loadAirportPropertyHistoryByAssetIdAndCycle(id, CycleSource.loadCycle() - 1)
    }

    lazy val paxByCountryCodeLastCycle : List[(String, Long)] = {
        propertyHistoryLastCycle match {
            case Some(entry) => AirportAsset.fromPropertiesToCountryStats(entry.properties).sortBy(_._2).reverse
            case None => List.empty
        }
    }

    lazy val transitPaxLastCycle : Long = propertyHistoryLastCycle match {
        case Some(entry) => entry.properties.get("transit_pax_count").map(_.toLong).getOrElse(0)
        case None => 0
    }

    lazy val destinationPaxLastCycle : Long = propertyHistoryLastCycle match {
        case Some(entry) => entry.properties.get("total_pax_count").map(_.toLong).getOrElse(0L) - transitPaxLastCycle
        case None => 0
    }

    def performance = {
        properties.get("performance") match {
            case Some(performance) => performance
            case None => 0
        }
    }

}

abstract class HotelAsset extends AirportAsset {
    def capacity = initialCapacity * (status match {
        case AirportAssetStatus.BLUEPRINT => 0
        case AirportAssetStatus.UNDER_CONSTRUCTION => level - 1
        case AirportAssetStatus.COMPLETED => level
    })
    val initialCapacity : Int
}

abstract class AdmissionAsset extends AirportAsset {
    def capacity = initialCapacity * (status match {
        case AirportAssetStatus.BLUEPRINT => 0
        case AirportAssetStatus.UNDER_CONSTRUCTION => level - 1
        case AirportAssetStatus.COMPLETED => level
    })
    val initialCapacity : Int
}

abstract class RentalAsset extends AirportAsset {
    def space = spacePerLease * maxLeaseCount
    val spacePerLease : Int
    val leasePerLevel : Int
    final def maxLeaseCount : Int = leasePerLevel * (status match {
        case AirportAssetStatus.BLUEPRINT => 0
        case AirportAssetStatus.UNDER_CONSTRUCTION => level - 1
        case AirportAssetStatus.COMPLETED => level
    })
    final def initialSpace : Int = spacePerLease * leasePerLevel
}

case class SkiResortAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset with PassengerCostAssetModifier {
    override val initialCapacity = 200
    override val probability = 30
    override val touristDiscount = 0.25
    override val businessDiscount = 0.08
}
case class BeachResortAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset with PassengerCostAssetModifier {
    override val initialCapacity = 200
    override val probability = 40
    override val touristDiscount = 0.1
    override val businessDiscount = 0.05
}
case class ConventionCenterAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset with PassengerCostAssetModifier {
    override val probability = 40
    override val touristDiscount = 0.0
    override val businessDiscount = 0.3
}
case class MuseumAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset with PassengerCostAssetModifier {
    override val initialCapacity = 5000

    override val probability = 30
    override val touristDiscount = 0.12
    override val businessDiscount = 0.05
}
case class ResidentialComplexAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends RentalAsset {
    override val spacePerLease = 1000
    override val leasePerLevel = 400
}
case class SportArenaAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset {
    override val initialCapacity = 2000
}
case class ShoppingMallAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends RentalAsset {
    override val spacePerLease = 2000
    override val leasePerLevel = 20
}
case class GrandHotelTouristAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset {
    override val initialCapacity = 300
}
case class GrandHotelBusinessAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset {
    override val initialCapacity = 300
}
case class AmusementParkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset with PassengerCostAssetModifier {
    override val initialCapacity = 5000

    override val probability = 50
    override val touristDiscount = 0.1
    override val businessDiscount = 0.0
}
case class SubwayAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset {
    override def costModifier : Double = super.costModifier +
      Math.max(0, Math.log(airport.basePopulation.toDouble / 5000000) / Math.log(2))


    override def baseBoosts : List[AirportBoost] = super.baseBoosts.map { baseBoost =>
        baseBoost.boostType match {
            case AirportBoostType.POPULATION => AirportBoost(AirportBoostType.POPULATION, (baseBoost.value + airport.basePopulation * 0.03).toLong / 1000 * 1000)
            case _ => baseBoost
        }
    }
}
case class StadiumAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset with PassengerCostAssetModifier {
    override val initialCapacity = 2000
    override val probability = 15
    override val touristDiscount = 0.1
    override val businessDiscount = 0.05
}
case class ScienceParkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long])   extends RentalAsset {
    override val spacePerLease = 30000
    override val leasePerLevel = 50
}
case class LandmarkAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset with PassengerCostAssetModifier {
    override val initialCapacity = 5000
    override val probability = 70
    override val touristDiscount = 0.08
    override val businessDiscount = 0.03
}

case class SolarPowerPlantAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset
case class TravelAgencyAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset
case class GameArcadeAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset
case class CinemaAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset {
    override val initialCapacity = 4000
}

case class InnAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset {
    override val initialCapacity = 70
}
case class LuxuriousHotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset {
    override val initialCapacity = 200
}
case class GolfCourseAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AdmissionAsset with PassengerCostAssetModifier {
    override val initialCapacity = 400

    override val probability = 10
    override val touristDiscount = 0.15
    override val businessDiscount = 0.25
}
case class OfficeBuilding1Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends RentalAsset {
    override val spacePerLease = 10000
    override val leasePerLevel = 25
}
case class AverageHotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset {
    override val initialCapacity = 100
}
case class OfficeBuilding2Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long])  extends RentalAsset {
    override val spacePerLease = 10000
    override val leasePerLevel = 30
}
case class RestaurantAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset
case class OfficeBuilding3Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends RentalAsset {
    override val spacePerLease = 10000
    override val leasePerLevel = 45
}
case class OfficeBuilding4Asset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends RentalAsset {
    override val spacePerLease = 10000
    override val leasePerLevel = 50
}
case class AirportHotelAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends HotelAsset with TransitWaitTimeModifier {
    override val initialCapacity = 500

    override def computeTransitFreqDiscount(arrivalLinkFreq : Int, departureLinkFreq : Int, paxGroup : PassengerGroup) : Double = {
        val minFrequency = Math.min(arrivalLinkFreq,departureLinkFreq)
        var discountPercentage = {
          if (minFrequency <= 7) { //very helpful
            0.3 + (level * 1.0 / AirportAsset.MAX_LEVEL) * 0.2 //30% - 50% off
          } else if (minFrequency <= 14) {
            0.2 + (level * 1.0 / AirportAsset.MAX_LEVEL) * 0.1 //20% - 30% off
          } else {
            level * 1.0 / AirportAsset.MAX_LEVEL * 0.1 //<10% off
          }
        }
        if (paxGroup.preference.preferredLinkClass.level >= BUSINESS.level) { //extra max 20% off
            discountPercentage +=  (level * 1.0 / AirportAsset.MAX_LEVEL) * 0.2
        }
        discountPercentage
    }
}
case class CityTransitAsset(override val blueprint : AirportAssetBlueprint, override val airline : Option[Airline], override val name : String, override val level : Int, override val completionCycle : Option[Int], override val status : AirportAssetStatus.Value, override var boosts : List[AirportBoost], override var revenue : Long, override var expense : Long, override var roi : Double, override var properties : Map[String, Long]) extends AirportAsset {
    override def costModifier : Double = super.costModifier +
      Math.max(0, Math.log(airport.basePopulation.toDouble / 1000000) / Math.log(2))

    override def baseBoosts : List[AirportBoost] = super.baseBoosts.map { baseBoost =>
        baseBoost.boostType match {
            case AirportBoostType.POPULATION => AirportBoost(AirportBoostType.POPULATION, (baseBoost.value + airport.basePopulation * 0.025).toLong / 1000 * 1000)
            case _ => baseBoost
        }
    }
}


object AirportAsset {
    val MAX_LEVEL = 10

    def getAirportAsset(id : Int, airport : Airport, assetType : AirportAssetType.Value, airline : Option[Airline], name : String, level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, roi : Double, upgradeApplied : Boolean ,properties : Map[String, Long], currentCycle : Int) : AirportAsset = {
        val blueprint = AirportAssetBlueprint(airport, assetType, id)
        getAirportAsset(blueprint, airline, name, level, completionCycle, boosts, revenue, expense, roi, upgradeApplied, properties, currentCycle)
    }

    def getAirportAsset(blueprint : AirportAssetBlueprint, airline : Option[Airline], name : String, level : Int, completionCycle : Option[Int], boosts : List[AirportBoost], revenue : Long, expense : Long, roi : Double,  upgradeApplied : Boolean, properties : Map[String, Long], currentCycle : Int = 0) : AirportAsset = {
        import AirportAssetType._
        val result = blueprint.assetType match {
            case SKI_RESORT => SkiResortAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case CITY_TRANSIT => CityTransitAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case AIRPORT_HOTEL => AirportHotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case AMUSEMENT_PARK => AmusementParkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case SUBWAY => SubwayAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case STADIUM => StadiumAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case GRAND_HOTEL_TOURIST => GrandHotelTouristAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case GRAND_HOTEL_BUSINESS => GrandHotelBusinessAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case CONVENTION_CENTER => ConventionCenterAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case MUSEUM => MuseumAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case LANDMARK => LandmarkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case SCIENCE_PARK => ScienceParkAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case SOLAR_POWER_PLANT => SolarPowerPlantAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case BEACH_RESORT => BeachResortAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case TRAVEL_AGENCY => TravelAgencyAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case SPORT_ARENA => SportArenaAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case GAME_ARCADE => GameArcadeAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case CINEMA => CinemaAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case INN => InnAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case GOLF_COURSE => GolfCourseAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case OFFICE_BUILDING_1 => OfficeBuilding1Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case HOTEL => AverageHotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case OFFICE_BUILDING_2 => OfficeBuilding2Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case RESTAURANT => RestaurantAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case OFFICE_BUILDING_3 => OfficeBuilding3Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case SHOPPING_MALL => ShoppingMallAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case LUXURIOUS_HOTEL => LuxuriousHotelAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case OFFICE_BUILDING_4 => OfficeBuilding4Asset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
            case RESIDENTIAL_COMPLEX =>ResidentialComplexAsset(blueprint, airline, name, level, completionCycle, status(completionCycle, currentCycle), boosts = boosts, revenue, expense, roi, properties)
        }
        result.upgradeApplied = upgradeApplied
        result

    }

    def buildNewAsset(airline : Airline, blueprint : AirportAssetBlueprint, name : String) : AirportAsset = {
        val currentCycle = CycleSource.loadCycle()
        val completionCycle = currentCycle + blueprint.assetType.constructionDuration

        getAirportAsset(blueprint, Some(airline), name, 1, Some(completionCycle), List.empty, 0, 0, blueprint.assetType.initRoi, false, Map.empty, currentCycle)
    }

    val status = (completionCycle : Option[Int], currentCycle : Int) =>
      completionCycle match {
          case Some(completionCycle) => if (completionCycle <= currentCycle) AirportAssetStatus.COMPLETED else AirportAssetStatus.UNDER_CONSTRUCTION
          case None => AirportAssetStatus.BLUEPRINT
      }

    def getPropertiesFromCountryStats(countryStats : Map[String, Int]) : Map[String, String] = {
        val result = mutable.HashMap[String, String]()
        var counter = 0
        countryStats.foreach {
            case (countryCode, paxCount) =>
                result.put(s"country_pax_${countryCode}", paxCount.toString)
        }
        result.toMap
    }

    def getPropertiesFromPaxTypeStats(transitCount : Long, totalCount : Long) = {
        val result = mutable.HashMap[String, String]()
        result.put("transit_pax_count", transitCount.toString)
        result.put("total_pax_count", totalCount.toString)
        result.toMap
    }

    def fromPropertiesToCountryStats(properties : Map[String, String]) : List[(CountryCode, Long)] = {
        val countryProperties = properties.filter(_._1.startsWith("country_pax_"))
        val countryStats = ListBuffer[(CountryCode, Long)]()
        countryProperties.foreach {
            case(key, paxCount) =>
            val countryCode = key.substring("country_pax_".length)
            countryStats.append((countryCode, paxCount.toLong))
        }
        countryStats.toList
    }
}


case class AirportBoost(boostType : AirportBoostType.Value, value : Double) //the value is of 1/100 for some attributes


object AirportBoostType extends Enumeration {
    type AirportBoostType = Value
    val POPULATION, INCOME, INTERNATIONAL_HUB, VACATION_HUB, FINANCIAL_HUB = Value
    val getLabel = (boostType : AirportBoostType.Value) => boostType match {
        case POPULATION => "Airport Population"
        case INCOME => "Airport Income Level"
        case INTERNATIONAL_HUB => "International Hub Strength"
        case VACATION_HUB => "Vacation Hub Strength"
        case FINANCIAL_HUB => "Financial Hub Strength"
    }

    val getValueType = (boostType : AirportBoostType.Value) => boostType match {
        case POPULATION => classOf[Long]
        case INCOME => classOf[Double]
        case INTERNATIONAL_HUB => classOf[Double]
        case VACATION_HUB =>  classOf[Double]
        case FINANCIAL_HUB => classOf[Double]
    }
}

object AirportAssetStatus extends Enumeration {
    type ProjectStatus = Value
    val BLUEPRINT, UNDER_CONSTRUCTION, COMPLETED = Value
}

//history entries are designed to be loosely coupled with asset itself, make no direct reference back nor asset has reference to it
case class AirportAssetBoostHistory(assetId : Int, level : Int, boostType: AirportBoostType.Value, value : Double, gain : Double, upgradeFactor : Double, cycle : Int)
case class AirportAssetPropertiesHistory(assetId : Int, properties : Map[String, String], cycle : Int)


