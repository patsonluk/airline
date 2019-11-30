package com.patson.model

import com.patson.model.airplane.Model
import scala.collection.mutable.ListBuffer
import com.patson.data.AirlineSource
import com.patson.data.CountrySource
import scala.jdk.CollectionConverters._


case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, zone : String, var size : Int, var power : Long, var population : Long, var slots : Int, var id : Int = 0) extends IdObject {
  val citiesServed = scala.collection.mutable.ListBuffer[(City, Double)]()
  private[this] val airlineAppeals = new java.util.HashMap[Int, AirlineAppeal]()//scala.collection.mutable.Map[Int, AirlineAppeal]()
  private[this] var airlineAppealsLoaded = false
//  private[this] val slotAssignments = scala.collection.mutable.Map[Int, Int]()
//  private[this] var slotAssignmentsLoaded = false
  private[this] val airlineBases = scala.collection.mutable.Map[Int, AirlineBase]()
  private[this] var airlineBasesLoaded = false
  private[this] val features = ListBuffer[AirportFeature]()
  private[this] var featuresLoaded = false
  private[this] val loungesByAirline = scala.collection.mutable.Map[Int, Lounge]()
  private[this] val loungesByAlliance = scala.collection.mutable.Map[Int, Lounge]()
  //private[this] var loungesLoaded = false
  
  private[this] var airportImageUrl : Option[String] = None
  private[this] var cityImageUrl : Option[String] = None
  
  private[model] var country : Option[Country] = None
  
  val income = if (population > 0) (power / population).toInt  else 0
  lazy val incomeLevel = Computation.getIncomeLevel(income)
  
//  def availableSlots : Int = {
//    if (slotAssignmentsLoaded) {
//      slots - slotAssignments.foldLeft(0)(_ + _._2)
//    } else {
//      throw new IllegalStateException("airline slot assignment is not properly initialized! If loaded from DB, please use fullload")
//    }
//  }
  
  def addCityServed(city : City, share : Double) {
    citiesServed += Tuple2(city, share)
  }
  
  def getAirlineAppeals() : Map[Int, AirlineAppeal] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAppeals.asScala.toMap
  }
  
  def setAirlineLoyalty(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrDefault(airlineId, AirlineAppeal(0, 0))
    airlineAppeals.put(airlineId, AirlineAppeal(value, oldAppeal.awareness))
  }
  def setAirlineAwareness(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrDefault(airlineId, AirlineAppeal(0, 0))
    airlineAppeals.put(airlineId, AirlineAppeal(oldAppeal.loyalty, value))
  }
  def getAirlineLoyalty(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val appeal = airlineAppeals.get(airlineId)
    if (appeal != null) {
      appeal.loyalty
    } else {
      0
    }
  }
  def getAirlineAwareness(airlineId : Int) : Double = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val appeal = airlineAppeals.get(airlineId)
    if (appeal != null) {
      appeal.awareness
    } else {
      0
    }
  }
  
  def getAirportImageUrl() : Option[String] = {
    airportImageUrl
  }
  
  def getCityImageUrl() : Option[String] = {
    cityImageUrl
  }
  
  def setAirportImageUrl(imageUrl : String) = {
    airportImageUrl = Some(imageUrl)
  }
  
  def setCityImageUrl(imageUrl : String) = {
    cityImageUrl = Some(imageUrl)
  }
  
  
  def isAirlineAppealsInitialized = airlineAppealsLoaded
  
  def getAirlineBase(airlineId : Int) : Option[AirlineBase] = {
    if (!airlineBasesLoaded) {
      throw new IllegalStateException("airport base is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineBases.get(airlineId)
  }
  
  def getAirlineBases() : Map[Int, AirlineBase] = {
    airlineBases.toMap
  }
  
  def getLoungeByAirline(airlineId : Int, activeOnly : Boolean = false) : Option[Lounge] = {
    loungesByAirline.get(airlineId).filter(!activeOnly || _.status == LoungeStatus.ACTIVE)
  }
  
  def getLounges() : List[Lounge] = {
    loungesByAirline.values.toList
  }
  
  def getLoungeByAlliance(alliance : Int, activeOnly : Boolean = false) : Option[Lounge] = {
    loungesByAlliance.get(alliance).filter(!activeOnly || _.status == LoungeStatus.ACTIVE)
  }
  
  def getLounge(airlineId : Int, allianceIdOption : Option[Int], activeOnly : Boolean = false) : Option[Lounge] = {
     getLoungeByAirline(airlineId, activeOnly) match {
       case Some(lounge) => Some(lounge)
       case None => allianceIdOption match {
         case Some(allianceId) => getLoungeByAlliance(allianceId, activeOnly)
         case None => None
       }
     }
     
  }
  
  def isFeaturesLoaded = featuresLoaded
  
  def getFeatures() : List[AirportFeature] = {
    features.toList
  }
  
  def initAirlineAppeals(airlineAppeals : Map[Int, AirlineAppeal]) = {
    this.airlineAppeals.clear()
    this.airlineAppeals.asScala ++= airlineAppeals
    airlineAppealsLoaded = true
  }
  def initAirlineBases(airlineBases : List[AirlineBase]) = {
    this.airlineBases.clear()
    airlineBases.foreach { airlineBase =>
      this.airlineBases.put(airlineBase.airline.id, airlineBase)
    }
    airlineBasesLoaded = true
  }
  def initFeatures(features : List[AirportFeature]) = {
    this.features.clear()
    this.features ++= features
    featuresLoaded = true
  }
  
  def initLounges(lounges : List[Lounge]) = {
    this.loungesByAirline.clear()
    lounges.foreach { lounge =>
      this.loungesByAirline.put(lounge.airline.id, lounge)
      lounge.allianceId.foreach {
         allianceId => this.loungesByAlliance.put(allianceId, lounge)  
      }
    }
  }
  
  def slotFee(airplaneModel : Model, airline : Airline) : Int = { 
    val baseSlotFee = size match {
      case 1 => 50 //small
      case 2 => 50 //medium
      case 3 => 80 //large
      case 4 => 150  //international class
      case 5 => 250
      case 6 => 350 
      case _ => 500 //mega airports - not suitable for tiny jets
    }
    
    import Model.Type._
    val multipler = airplaneModel.airplaneType match {
      case LIGHT => 1
      case REGIONAL => 1
      case SMALL => 3
      case MEDIUM => 8
      case LARGE => 12
      case X_LARGE => 15
      case JUMBO => 18
    }
    
    //apply discount if it's a base
    val discount = getAirlineBase(airline.id) match {
      case Some(airlineBase) =>
        if (airlineBase.headquarter) 0.5 else 0.8 //headquarter 50% off, base 20% off
      case None =>
        1 //no discount
    }
    
    (baseSlotFee * multipler * discount).toInt    
  }
  
  def landingFee(airplaneModel : Model) : Int = {
    val perSeat = 
      if (size <= 3) {
        3
      } else {
        size
      }
    
    airplaneModel.capacity * perSeat
  }
  
  def allowsModel(airplaneModel : Model) : Boolean = {
    size >= airplaneModel.minAirportSize
  }
  
  val expectedQuality = (flightType : FlightType.Value, linkClass : LinkClass) => {
    Math.max(0, Math.min(incomeLevel, 50) + Airport.qualityExpectationFlightTypeAdjust(flightType)(linkClass)) //50% on income level, 50% on flight adjust
  }
  
  private[this] def getCountry() : Country = {
    if (country.isEmpty) {
      country = CountrySource.loadCountryByCode(countryCode)
    }
    country.get
  }
  
  lazy val airportRadius : Int = {
    size match {
      case 1 => 100
      case 2 => 150
      case n if (n >= 3) => 250
      case _ => 0
    }
  }
  
  val displayText = city + "(" + iata + ")"
}

case class AirlineAppeal(loyalty : Double, awareness : Double)
object AirlineAppeal {
  val MAX_LOYALTY = 100
  val MAX_AWARENESS = 100
}

object Airport {
  def fromId(id : Int) = {
    val airportWithJustId = Airport("", "", "", 0, 0, "", "", "", 0, 0, 0, 0, 0)
    airportWithJustId.id = id
    airportWithJustId
  }
  
  val MAJOR_AIRPORT_LOWER_THRESHOLD = 5
  val HQ_GUARANTEED_SLOTS = 20 //at least 20 slots for HQ
  val BASE_GUARANTEED_SLOTS = 10 //at least 10 slots for base
  val NON_BASE_MAX_SLOT = 70
  
  import FlightType._
  val qualityExpectationFlightTypeAdjust = 
  Map(SHORT_HAUL_DOMESTIC -> LinkClassValues.getInstance(-15, -5, 5),
        SHORT_HAUL_INTERNATIONAL ->  LinkClassValues.getInstance(-10, 0, 10),
        SHORT_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(-5, 5, 15),
        LONG_HAUL_DOMESTIC -> LinkClassValues.getInstance(0, 5, 15),
        LONG_HAUL_INTERNATIONAL -> LinkClassValues.getInstance(5, 10, 20),
        LONG_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(10, 15, 20),
        ULTRA_LONG_HAUL_INTERCONTINENTAL -> LinkClassValues.getInstance(10, 15, 20))
}

case class Runway(length : Int, runwayType : RunwayType.Value)

object RunwayType extends Enumeration {
    type RunwayType = Value
    val Asphalt, Concrete, Gravel = Value
}


