package com.patson.model

import com.patson.model.airplane.Model
import com.patson.model.airplane.Model.Type
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import com.patson.data.AirlineSource
import com.patson.data.CountrySource

case class Airport(iata : String, icao : String, name : String, latitude : Double, longitude : Double, countryCode : String, city : String, zone : String, var size : Int, var power : Long, var population : Long, var slots : Int, var id : Int = 0) extends IdObject {
  val citiesServed = scala.collection.mutable.MutableList[(City, Double)]()
  private[this] val airlineAppeals = new java.util.HashMap[Int, AirlineAppeal]()//scala.collection.mutable.Map[Int, AirlineAppeal]()
  private[this] var airlineAppealsLoaded = false
  private[this] val slotAssignments = scala.collection.mutable.Map[Int, Int]()
  private[this] var slotAssignmentsLoaded = false
  private[this] val airlineBases = scala.collection.mutable.Map[Int, AirlineBase]()
  private[this] var airlineBasesLoaded = false
  private[this] val features = ListBuffer[AirportFeature]()
  private[this] var featuresLoaded = false
  
  private[this] var airportImageUrl : Option[String] = None
  private[this] var cityImageUrl : Option[String] = None
  
  private[model] var country : Option[Country] = None
  
  val income = if (population > 0) (power / population).toInt  else 0
  
  def availableSlots : Int = {
    if (slotAssignmentsLoaded) {
      slots - slotAssignments.foldLeft(0)(_ + _._2)
    } else {
      throw new IllegalStateException("airline slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
  }
  
  def addCityServed(city : City, share : Double) {
    citiesServed += Tuple2(city, share)
  }
  
  def getAirlineAppeals() : Map[Int, AirlineAppeal] = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineAppeals.toMap
  }
  
  def setAirlineLoyalty(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrElse(airlineId, AirlineAppeal(0, 0))
    airlineAppeals.put(airlineId, AirlineAppeal(value, oldAppeal.awareness))
  }
  def setAirlineAwareness(airlineId : Int, value : Double) = {
    if (!airlineAppealsLoaded) {
      throw new IllegalStateException("airline appeal is not properly initialized! If loaded from DB, please use fullload")
    }
    val oldAppeal = airlineAppeals.getOrElse(airlineId, AirlineAppeal(0, 0))
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
  def isSlotAssignmentsInitialized = slotAssignmentsLoaded
  
  def getAirlineSlotAssignment(airlineId : Int) = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    slotAssignments.getOrElse(airlineId, 0)
  }
  
  
  def getMaxSlotAssignment(airlineId : Int) : Int = {
    AirlineSource.loadAirlineById(airlineId, true) match {
      case Some(airline) => getMaxSlotAssignment(airline)
      case None => 0
    }
    
  }
  /**
   * Get max slots that can be assigned to this airline (including existing ones)
   */
  def getMaxSlotAssignment(airline : Airline) : Int = {
    val airlineId = airline.id
    val reservedSlots = (slots * 0.2).toInt //airport always keep 20% spare
    val currentAssignedSlotToThisAirline = getAirlineSlotAssignment(airlineId)
    
    //find out the country where this airline is from
    val airlineFromCountry = airline.getCountryCode() match {
      case Some(country) => country
      case None => return 0
    }
    
    val countryRelationship = CountrySource.getCountryMutualRelationship(airlineFromCountry, countryCode)
    
    if (countryRelationship <= Country.HOSTILE_RELATIONSHIP_THRESHOLD) {
      return 0
    }
    
    
    if (availableSlots < reservedSlots) { //at reserved range already...cannot assign any new slots to existing airline
      if (currentAssignedSlotToThisAirline > 0) {
        currentAssignedSlotToThisAirline
      } else if (availableSlots > 0) { //some hope for new airline...
        getGuaranteedSlots(airlineFromCountry, countryRelationship)
      } else { //sry all full
        0
      } 
    } else { //calculate how many can be assigned
      val airlineBaseAtThisAirportOption = getAirlineBase(airlineId)
      
      
      //calculate guaranteed slots
      val guaranteedSlots = airlineBaseAtThisAirportOption match {
        case Some(base) if (base.headquarter) => 10 //at least 10 slots for HQ
        case Some(base) if (!base.headquarter) => 5 //at least 5 slots for base
        case None => getGuaranteedSlots(airlineFromCountry, countryRelationship)
      }
      
      
       //if it's not a base, give it 50 slots max
      //if it's a base (not HQ), give it 1/10 max
      //if it's a base (HQ), give it 1/3 max
      val maxSlotsByBase =
        getAirlineBase(airlineId) match {
          case Some(base) if (base.headquarter) => 200 * (base.scale + 1)
          case Some(base) if (!base.headquarter) => 100 * (base.scale + 1)
          case None => 50  
          
        }
      
      val maxSlotsByLoyalty = (maxSlotsByBase * (getAirlineLoyalty(airlineId) / AirlineAppeal.MAX_LOYALTY)).toInt //base on loyalty, at full loyalty get 100% of max slot available
//      val maxSlotsByAwareness = (getAirlineAwareness(airlineId) * 30 / AirlineAppeal.MAX_AWARENESS).toInt + minSlots// +30 at max awareness
      var maxSlotsByReputation : Int = (airline.getReputation() / Airline.MAX_REPUTATION * 9).toInt + 1 //max 10 with 100% reputation
      if (airlineFromCountry != countryCode) { //from a foreign country, openness affects slots by reputation
        //max openness 100 %, min openness 20 %
        maxSlotsByReputation = (maxSlotsByReputation * (0.2 + (getCountry().openness.toDouble / Country.MAX_OPENNESS * 0.8))).toInt 
      }
      
      
      var maxSlots = Math.max(maxSlotsByLoyalty, maxSlotsByReputation)
      
      if (maxSlots > 1 && countryRelationship < 0) {
        maxSlots /= 2
      }
      
      maxSlots = Math.max(maxSlots, guaranteedSlots)
      
      //now see whether this new max slot would violate anything
      if (maxSlots <= currentAssignedSlotToThisAirline) { //you can keep what you have but we cannot give u more as we don't like you anymore than before
        currentAssignedSlotToThisAirline
      } else {
        var increment = maxSlots - currentAssignedSlotToThisAirline
        if (availableSlots - increment < reservedSlots) { //nah cannot assign into the reserved range
           increment = availableSlots - reservedSlots  //can give u what is left before reserved range at most   
        }
        currentAssignedSlotToThisAirline + increment
      }
    }
  }
  
  def getGuaranteedSlots(airlineFromCountryCode : String, countryMutualRelationship: Int) : Int = {
    if (airlineFromCountryCode == this.countryCode) { //always at least 2 slots for home country airline
      2
    } else if (countryMutualRelationship < 0) {
      0
    } else if (getCountry.openness >= Country.SIXTH_FREEDOM_MIN_OPENNESS) { //only very free country will open up slots for ANY airline
      1
    } else {
      0
    }
  }
  
  def setAirlineSlotAssignment(airlineId : Int, value : Int) = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    val maxAssignment = getMaxSlotAssignment(airlineId)
    if (value > maxAssignment) {
      throw new IllegalArgumentException("Cannot assign that many slots to this airline!") 
    }
    slotAssignments.put(airlineId, value)
  }
  
  def getAirlineSlotAssignments() : Map[Int, Int] = {
    if (!slotAssignmentsLoaded) {
      throw new IllegalStateException("airport slot assignment is not properly initialized! If loaded from DB, please use fullload")
    }
    slotAssignments.toMap
  }
  
  def getAirlineBase(airlineId : Int) : Option[AirlineBase] = {
    if (!airlineBasesLoaded) {
      throw new IllegalStateException("airport base is not properly initialized! If loaded from DB, please use fullload")
    }
    airlineBases.get(airlineId)
  }
  
  def getAirlineBases() : Map[Int, AirlineBase] = {
    airlineBases.toMap
  }
  
  def isFeaturesLoaded = featuresLoaded
  
  def getFeatures() : List[AirportFeature] = {
    features.toList
  }
  
  def initAirlineAppeals(airlineAppeals : Map[Int, AirlineAppeal]) = {
    this.airlineAppeals.clear()
    this.airlineAppeals ++= airlineAppeals
    airlineAppealsLoaded = true
  }
  def initSlotAssignments(slotAssignments : Map[Int, Int]) = {
    this.slotAssignments.clear()
    this.slotAssignments ++= slotAssignments
    slotAssignmentsLoaded = true
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
      case MEDIUM => 15
      case LARGE => 20
      case JUMBO => 25
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
    import Model.Type._
    airplaneModel.airplaneType match {
      case LIGHT => true   
      case REGIONAL => true
      case SMALL => size >= 2
      case MEDIUM => size >= 3
      case LARGE => size >= 4
      case JUMBO => size >= 5
    }
    
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
}

case class Runway(length : Int, runwayType : RunwayType.Value)

object RunwayType extends Enumeration {
    type RunwayType = Value
    val Asphalt, Concrete, Gravel = Value
}


