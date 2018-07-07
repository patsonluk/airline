package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json
import com.patson.model._
import com.patson.data.AirportSource
import com.patson.Util
import com.patson.model.Link
import com.patson.data.LinkSource
import com.patson.data.AirlineSource
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import com.patson.data.CitySource
import com.patson.data.LinkStatisticsSource
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import com.patson.model.Scheduling.TimeSlot
import controllers.AuthenticationObject.AuthenticatedAirline
import scala.collection.mutable.Set
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.Scheduling.TimeSlotStatus
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.Scheduling.TimeSlotStatus
import java.util.Random
import com.patson.model.Scheduling.TimeSlot
import com.patson.model.Scheduling.TimeSlotStatus


class Application extends Controller {
 implicit object AirportFormat extends Format[Airport] {
    def reads(json: JsValue): JsResult[Airport] = {
      val airport = Airport.fromId((json \ "id").as[Int])
      JsSuccess(airport)
    }
    
    def writes(airport: Airport): JsValue = {
      val averageIncome = if (airport.population > 0) { airport.power / airport.population } else 0
      val incomeLevel = Computation.getIncomeLevel(airport.income)
//      val appealMap = airport.airlineAppeals.foldRight(Map[Airline, Int]()) { 
//        case(Tuple2(airline, appeal), foldMap) => foldMap + Tuple2(airline, appeal.loyalty)  
//      }
//      val awarenessMap = airport.airlineAppeals.foldRight(Map[Airline, Int]()) { 
//        case(Tuple2(airline, appeal), foldMap) => foldMap + Tuple2(airline, appeal.awareness)  
//      }
      
      var airportObject = JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "iata" -> JsString(airport.iata),
      "icao" -> JsString(airport.icao),
      "city" -> JsString(airport.city),
      "size" -> JsNumber(airport.size),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "population" -> JsNumber(airport.population),
      "slots" -> JsNumber(airport.slots),
      "radius" -> JsNumber(airport.airportRadius),
      "zone" -> JsString(airport.zone),
      "incomeLevel" -> JsNumber(if (incomeLevel < 0) 0 else incomeLevel)))
      
      
      if (airport.isSlotAssignmentsInitialized) {
        airportObject = airportObject + ("availableSlots" -> JsNumber(airport.availableSlots))
        airportObject = airportObject + ("slotAssignmentList" -> JsArray(airport.getAirlineSlotAssignments().toList.map {  
          case (airlineId, slotAssignment) => Json.obj("airlineId" -> airlineId, "airlineName" -> AirlineSource.loadAirlineById(airlineId).fold("<unknown>")(_.name), "slotAssignment" -> slotAssignment)
          }
        ))
      }
      if (airport.isAirlineAppealsInitialized) {
        airportObject = airportObject + ("appealList" -> JsArray(airport.getAirlineAppeals().toList.map {  
          case (airlineId, appeal) => Json.obj("airlineId" -> airlineId, "airlineName" -> AirlineSource.loadAirlineById(airlineId).fold("<unknown>")(_.name), "loyalty" -> BigDecimal(appeal.loyalty).setScale(2, BigDecimal.RoundingMode.HALF_EVEN), "awareness" -> BigDecimal(appeal.awareness).setScale(2,  BigDecimal.RoundingMode.HALF_EVEN))
          }
        ))
      }
      if (airport.isFeaturesLoaded) {
        airportObject = airportObject + ("features" -> JsArray(airport.getFeatures().map { airportFeature =>
            Json.obj("type" -> airportFeature.featureType.toString(), "strength" -> airportFeature.strength, "title" -> AirportFeatureType.getDescription(airportFeature.featureType))
          }
        ))
      }
      if (airport.getAirportImageUrl.isDefined) {
        airportObject = airportObject + ("airportImageUrl" -> JsString(airport.getAirportImageUrl.get))
      }
      if (airport.getCityImageUrl.isDefined) {
        airportObject = airportObject + ("cityImageUrl" -> JsString(airport.getCityImageUrl.get))
      }
      
      airportObject = airportObject + ("citiesServed" -> Json.toJson(airport.citiesServed.toList.map(_._1)))
      
      airportObject
    }
  }
 
  object AirportSimpleWrites extends Writes[Airport] {
    def writes(airport: Airport): JsValue = {
      JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "city" -> JsString(airport.city),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "zone" -> JsString(airport.zone)))
      
    }
  }
 
  implicit object CityWrites extends Writes[City] {
    def writes(city: City): JsValue = {
      val averageIncome = city.income
      val incomeLevel = (Math.log(averageIncome / 1000) / Math.log(1.1)).toInt
      JsObject(List(
      "id" -> JsNumber(city.id),    
      "name" -> JsString(city.name),
      "latitude" -> JsNumber(city.latitude),
      "longitude" -> JsNumber(city.longitude),
      "countryCode" -> JsString(city.countryCode),
      "population" -> JsNumber(city.population),
      "incomeLevel" -> JsNumber(if (incomeLevel < 0) 0 else incomeLevel)))
    }
  }
  
  implicit object AirportShareWrites extends Writes[(Airport, Double)] {
    def writes(airportShare: (Airport, Double)): JsValue = {
      JsObject(List(
      "airportName" -> JsString(airportShare._1.name),    
      "airportId" -> JsNumber(airportShare._1.id),
      "share" -> JsNumber(BigDecimal(airportShare._2).setScale(4, BigDecimal.RoundingMode.HALF_EVEN))))
    }
  }
  
   implicit object AirportPassengersWrites extends Writes[(Airport, Int)] {
    def writes(airportPassenger: (Airport, Int)): JsValue = {
      JsObject(List(
      "airportName" -> JsString(airportPassenger._1.name),    
      "airportId" -> JsNumber(airportPassenger._1.id),
      "passengers" -> JsNumber(airportPassenger._2)))
    }
  }
   implicit object AirlinePassengersWrites extends Writes[(Airline, Int)] {
     def writes(airlinePassenger: (Airline, Int)): JsValue = {
      JsObject(List(
      "airlineName" -> JsString(airlinePassenger._1.name),    
      "airlineId" -> JsNumber(airlinePassenger._1.id),
      "passengers" -> JsNumber(airlinePassenger._2)))
    }
  }
   
  implicit object TimeSlotAssignmentWrites extends Writes[(TimeSlot, Link, TimeSlotStatus)] {
     def writes(timeSlotAssignment: (TimeSlot, Link, TimeSlotStatus)): JsValue = {
      val link = timeSlotAssignment._2 
      JsObject(List(
      "timeSlotDay" -> JsNumber(timeSlotAssignment._1.dayOfWeek),    
      "timeSlotTime" -> JsString("%02d".format(timeSlotAssignment._1.hour) + ":" + "%02d".format(timeSlotAssignment._1.minute)),
      "airline" -> JsString(link.airline.name),
      "flightCode" -> JsString(LinkApplication.getFlightCode(link.airline, link.flightNumber)),
      "destination" -> JsString(if (!link.to.city.isEmpty()) { link.to.city } else { link.to.name }),
      "statusCode" -> JsString(timeSlotAssignment._3.code),
      "statusText" -> JsString(timeSlotAssignment._3.text)
      ))
    }
  }
  
  implicit object AirportProjectFormat extends Format[AirportProject] {
     def writes(project : AirportProject): JsValue = {
       Json.obj(
         "projectId" -> project.id,
         "airportId" -> project.airport.id,    
         "projectType" -> project.projectType.toString(),
         "status" -> project.status.toString(), 
         "progress" -> project.progress
       )
    }
    def reads(json: JsValue): JsResult[AirportProject] = {
      val airport = Airport.fromId((json \ "id").as[Int])
      val projectType = ProjectType.withName((json \ "projectType").as[String])
      JsSuccess(AirportProject(airport, projectType, ProjectStatus.INITIATED, progress = 0, duration = 0, level = 0)) //TODO not implemented
    }
  }

   
//  object SimpleLinkWrites extends Writes[Link] {
//    def writes(link: Link): JsValue = {
//      JsObject(List(
//      "id" -> JsNumber(link.id),    
//      "airlineId" -> JsNumber(link.airline.id)))
//    }
//  }
 
  
  case class AirportSlotData(airlineId: Int, slotCount: Int)
  val airportSlotForm = Form(
    mapping(
      "airlineId" -> number,
      "slotCount" -> number
    )(AirportSlotData.apply)(AirportSlotData.unapply)
  )
  
  
  
  def index = Action {
    Ok(views.html.index(""))
  }
  def test = Action {
    Ok(views.html.test())
  }
  
  def getAirports(count : Int) = Action {
    val airports = AirportSource.loadAllAirports()
    val selectedAirports = airports.takeRight(count)
    Ok(Json.toJson(selectedAirports))
  }
  
  def getAirport(airportId : Int) = Action {
     AirportSource.loadAirportById(airportId, true) match {
       case Some(airport) =>
         //find links going to this airport too, send simplified data
         val links = LinkSource.loadLinksByFromAirport(airportId, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByToAirport(airportId, LinkSource.ID_LOAD)
         val linkCountJson = links.groupBy { _.airline.id }.foldRight(Json.obj()) { 
           case((airlineId, links), foldJson) => foldJson + (airlineId.toString() -> JsNumber(links.length)) 
         }
         
         Ok(Json.toJson(airport).asInstanceOf[JsObject] + ("linkCounts" -> linkCountJson))
       case None => NotFound
     }
  }
  def getAirportSlotsByAirline(airportId : Int, airlineId : Int) = Action {
    AirportSource.loadAirportById(airportId, true) match {  
       case Some(airport) =>  
         val maxSlots = airport.getMaxSlotAssignment(airlineId)
         val assignedSlots = airport.getAirlineSlotAssignment(airlineId)
         Ok(Json.obj("assignedSlots" -> JsNumber(assignedSlots), "maxSlots" -> JsNumber(maxSlots)))
       case None => NotFound
     }
  }
  
  def getAirportSharesOnCity(cityId : Int) = Action {
    Ok(Json.toJson(AirportSource.loadAirportSharesOnCity(cityId)))
  }
  
  def getAirportLinkStatistics(airportId : Int) = Action {
    AirportSource.loadAirportById(airportId, true) match {
      case Some(airport) => { 
        //group things up
        val flightsFromThisAirport = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airportId)
        val flightsToThisAirport = LinkStatisticsSource.loadLinkStatisticsByToAirport(airportId)
        val departureOrArrivalFlights = flightsFromThisAirport.filter { _.key.isDeparture} ++ flightsToThisAirport.filter { _.key.isDestination }
        val connectionFlights = flightsFromThisAirport.filterNot { _.key.isDeparture} ++ flightsToThisAirport.filterNot { _.key.isDestination }
        
        val flightDepartureByAirline = flightsFromThisAirport.groupBy { _.key.airline }
        val flightDestinationByAirline = flightsToThisAirport.groupBy { _.key.airline }
        
        val departureOrArrivalPassengers = departureOrArrivalFlights.map{ _.passengers }.sum
        val transitPassengers = connectionFlights.map{ _.passengers }.sum
          
        
        val statisticsDepartureByAirline : List[(Airline, Int)] = flightDepartureByAirline.foldRight(List[(Airline, Int)]()) { 
          case ((airline, statistics), foldList) =>
            val totalPassengersOfThisAirline = statistics.foldLeft(0)( _ + _.passengers) //all the passengers of this airline
            (airline, totalPassengersOfThisAirline) :: foldList
        }
        val statisticsArrivalByAirline : List[(Airline, Int)] = flightDestinationByAirline.foldRight(List[(Airline, Int)]()) { 
          case ((airline, statistics), foldList) =>
            val totalPassengersOfThisAirline = statistics.foldLeft(0)( _ + _.passengers) //all the passengers of this airline
            (airline, totalPassengersOfThisAirline) :: foldList
        }
        
        val links = LinkSource.loadLinksByFromAirport(airportId) ++ LinkSource.loadLinksByToAirport(airportId)
        
        val servedCountries = Set[String]()
        val servedAirports = Set[Airport]()
        val airlines = Set[Airline]()
        var flightFrequency = 0;
        val linkCountByAirline = links.groupBy(_.airline.id).mapValues(_.size)
        
        links.foreach { link =>
          servedCountries.add(link.from.countryCode)
          servedCountries.add(link.to.countryCode)
          if (link.from.id != airportId) {
            servedAirports.add(link.from)
          } else {
            servedAirports.add(link.to)
          }
          airlines.add(link.airline)
          flightFrequency = flightFrequency + link.frequency
        }
        
        
        
        Ok(Json.obj("connectedCountryCount" -> servedCountries.size,
                    "connectedAirportCount" -> (servedAirports.size), //do not count itself
                    "airlineCount" -> airlines.size,
                    "linkCount" -> links.size,
                    "linkCountByAirline" -> linkCountByAirline.foldLeft(Json.arr()) {
                      case(jsonArray, (airlineId, linkCount)) => jsonArray :+ Json.obj("airlineId" -> JsNumber(airlineId), "linkCount"-> JsNumber(linkCount))
                    },
                    "flightFrequency" -> flightFrequency,
                    "bases" -> Json.toJson(airport.getAirlineBases().values),
                    "departureOrArrivalPassengers" -> departureOrArrivalPassengers, 
                    "transitPassengers" -> transitPassengers,
                    "airlineDeparture" -> Json.toJson(statisticsDepartureByAirline),
                    "airlineArrival" -> Json.toJson(statisticsArrivalByAirline)))
      }
      case None => NotFound
    }
    
  }
  
  def getDepartures(airportId : Int, dayOfWeek : Int, hour : Int, minute : Int) = Action {
    val links = LinkSource.loadLinksByFromAirport(airportId, LinkSource.SIMPLE_LOAD) ++ (LinkSource.loadLinksByToAirport(airportId, LinkSource.SIMPLE_LOAD).map { link => link.copy(from = link.to, to = link.from) })
    
    val map = Map[Int, String]()
    
    val currentTime = TimeSlot(dayOfWeek = dayOfWeek, hour = hour, minute = minute)
    
    val linkConsumptions : Map[Int, LinkConsumptionDetails] = LinkSource.loadLinkConsumptionsByLinksId(links.map(_.id)).map( linkConsumption => (linkConsumption.link.id, linkConsumption)).toMap 
    
    val timeSlotLinkList : List[(TimeSlot, Link, TimeSlotStatus)] = links.flatMap { link => link.schedule.map { scheduledTimeSlot => (scheduledTimeSlot, link, getTimeSlotStatus(linkConsumptions.get(link.id), scheduledTimeSlot, currentTime)) }}.sortBy {
      case (timeslot, link, _) => timeslot.totalMinutes
    }
    
    //TODO delays/cancellation ...load from link history
    
    val filteredList = timeSlotLinkList.dropWhile {
      case(timeslot, _, _) => currentTime.totalMinutes > timeslot.totalMinutes 
    }.takeWhile {
      case(timeslot, _, _) => timeslot.dayOfWeek == dayOfWeek //TODO take next day too?
    }
    
    Ok(Json.toJson(filteredList))
  }
  
  def getTimeSlotStatus(linkConsumptionOption : Option[LinkConsumptionDetails], scheduledTime : TimeSlot, currentTime : TimeSlot) : TimeSlotStatus = {
    var isMinorDelay = false
    var isMajorDelay = false
    var isCancelled = false
    var delayAmount = 0
    
    linkConsumptionOption.map { linkConsumption =>
      val cancellationMarker = linkConsumption.link.cancellationCount
      val majorDelayMarker = cancellationMarker + linkConsumption.link.majorDelayCount
      val minorDelayMarker = majorDelayMarker +  linkConsumption.link.minorDelayCount

      val flightInterval = 60 * 24 * 7 / linkConsumption.link.frequency 
      val flightIndex = currentTime.totalMinutes / flightInterval  //nth flight on this route within this week
      
      val randnum = new Random();
      randnum.setSeed(linkConsumption.id); // want pseudo random that gives same result every time!
      
      val randomizedFlightIndex = (flightIndex + randnum.nextInt(linkConsumption.link.frequency)) % linkConsumption.link.frequency
      
      //println(cancellationMarker + "|" + majorDelayMarker + "|" + minorDelayMarker + " RI " + randomizedFlightIndex)
      //if u r unlucky enough to be smaller or equal to marker than BOOM!
      if (randomizedFlightIndex < cancellationMarker) {
        isCancelled = true
      } else if  (randomizedFlightIndex < majorDelayMarker) {
        isMajorDelay = true
        delayAmount = 5 * 60 + randomizedFlightIndex * 30
      } else if  (randomizedFlightIndex < minorDelayMarker) {
        isMinorDelay = true
        delayAmount =  20 + 100 / (randomizedFlightIndex + 1) //within 2 hours
      }
    }
    
    if (isCancelled) {
      TimeSlotStatus("CANCELLED", "Cancelled")
    } else if (isMajorDelay || isMinorDelay) {
      val newTime = scheduledTime.increment(delayAmount)
      TimeSlotStatus("DELAY", "Delayed " + "%02d".format(newTime.hour) + ":" + "%02d".format(newTime.minute)) 
    } else if (scheduledTime.totalMinutes - currentTime.totalMinutes <= 10) {
      TimeSlotStatus("GATE_CLOSED", "Gate Closed") 
    } else if (scheduledTime.totalMinutes - currentTime.totalMinutes <= 20) {
      TimeSlotStatus("FINAL_CALL", "Final Call")
    } else if (scheduledTime.totalMinutes - currentTime.totalMinutes <= 30) {
      TimeSlotStatus("BOARDING", "Boarding")
    } else {
      TimeSlotStatus("ON_TIME", "On Time")
    }
     
  }
  
  def getAirportLinkConsumptions(fromAirportId : Int, toAirportId : Int) = Action {
    val competitorLinkConsumptions = (LinkSource.loadLinksByAirports(fromAirportId, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadLinksByAirports(toAirportId, fromAirportId, LinkSource.ID_LOAD)).flatMap { link =>
      LinkSource.loadLinkConsumptionsByLinkId(link.id, 1)
    }
    Ok(Json.toJson(competitorLinkConsumptions.map { linkConsumption => Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite) }.toSeq))
  }
  
  def getLinkConsumptionsByAirport(airportId : Int) = Action {
    val passengersByRemoteAirport : Map[Airport, Int] = HistoryUtil.loadConsumptionByAirport(airportId)
    Ok(Json.toJson(passengersByRemoteAirport.map {
      case (remoteAirport, passengers) => Json.obj("remoteAirport" -> Json.toJson(remoteAirport)(AirportSimpleWrites), ("passengers" -> JsNumber(passengers)))
    }))
  }
  
  def getAirportProjects(airportId : Int) = Action {
    val airportProjects = AirportSource.loadAirportProjectsByAirport(airportId)
    Ok(Json.toJson(airportProjects))
  }
  
  def addAirportProject(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
    
     //TODO validate airline can do it
     val newProject = request.body.asInstanceOf[AnyContentAsJson].json.as[AirportProject]
     AirportSource.saveAirportProject(newProject)
     Ok(Json.toJson(newProject))
  }
      
  
  def options(path: String) = Action {
    Ok("").withHeaders(
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With, Authorization",
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )
  }

  case class LinkInfo(fromId : Int, toId : Int, price : Double, capacity : Int)
}
