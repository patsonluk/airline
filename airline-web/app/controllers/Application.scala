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


class Application extends Controller {
 implicit object AirportFormat extends Format[Airport] {
    def reads(json: JsValue): JsResult[Airport] = {
      val airport = Airport.fromId((json \ "id").as[Int])
      JsSuccess(airport)
    }
    
    def writes(airport: Airport): JsValue = {
      val averageIncome = airport.power / airport.population
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
      "city" -> JsString(airport.city),
      "size" -> JsNumber(airport.size),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "population" -> JsNumber(airport.population),
      "slots" -> JsNumber(airport.slots),
      "radius" -> JsNumber(Computation.calculateAirportRadius(airport)),
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
      
      airportObject = airportObject + ("citiesServed" -> Json.toJson(airport.citiesServed.toList.map(_._1)))
      
      airportObject
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
         val links = LinkSource.loadLinksByFromAirport(airportId) ++ LinkSource.loadLinksByToAirport(airportId)
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
    //group things up
    val flightsFromThisAirport = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airportId)
    val flightsToThisAirport = LinkStatisticsSource.loadLinkStatisticsByToAirport(airportId)
    val (flightsInitialDeparture, flightsConnectionFrom) = flightsFromThisAirport.partition { _.key.isDeparture }
    val (flightsFinalDestination, flightsConnectionTo) = flightsToThisAirport.partition { _.key.isDestination }
    
    val flightDepartureByAirline = flightsFromThisAirport.groupBy { _.key.airline }
    val flightDestinationByAirline = flightsToThisAirport.groupBy { _.key.airline }
    
    
    //fold them to get total numbers
    val statisticsInitialDeparture : Map[Airport, Int] = flightsInitialDeparture.foldRight(Map[Airport, Int]()) { (linkStatisticsEntry, foldMap) =>
      val airport = linkStatisticsEntry.key.toAirport
      foldMap + (airport -> (foldMap.getOrElse(airport, 0) + linkStatisticsEntry.passengers))
    }
    val statisticsFinalDestination : Map[Airport, Int] = flightsFinalDestination.foldRight(Map[Airport, Int]()) { (linkStatisticsEntry, foldMap) =>
      val airport = linkStatisticsEntry.key.fromAirport
      foldMap + (airport -> (foldMap.getOrElse(airport, 0) + linkStatisticsEntry.passengers))
    }
    val statisticsConnectionFrom : Map[Airport, Int] = flightsConnectionFrom.foldRight(Map[Airport, Int]()) { (linkStatisticsEntry, foldMap) =>
      val airport = linkStatisticsEntry.key.toAirport
      foldMap + (airport -> (foldMap.getOrElse(airport, 0) + linkStatisticsEntry.passengers))
    }
    val statisticsConnectionTo :Map[Airport, Int] = flightsConnectionTo.foldRight(Map[Airport, Int]()) { (linkStatisticsEntry, foldMap) =>
      val airport = linkStatisticsEntry.key.fromAirport
      foldMap + (airport -> (foldMap.getOrElse(airport, 0) + linkStatisticsEntry.passengers))
    }
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
    Ok(Json.obj("departure" -> Json.toJson(statisticsInitialDeparture), 
                "destination" -> Json.toJson(statisticsFinalDestination),
                "connectionFrom" -> Json.toJson(statisticsConnectionFrom),
                "connectionTo" -> Json.toJson(statisticsConnectionTo),
                "airlineDeparture" -> Json.toJson(statisticsDepartureByAirline),
                "airlineArrival" -> Json.toJson(statisticsArrivalByAirline)))
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
