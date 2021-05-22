package controllers

import java.util.Random
import com.patson.AirportSimulation
import com.patson.data._
import com.patson.model.Scheduling.{TimeSlot, TimeSlotStatus}
import com.patson.model.{Link, _}
import com.patson.util.{AirlineCache, AirportCache, ChampionUtil}
import controllers.AuthenticationObject.AuthenticatedAirline
import controllers.NegotiationUtil.FlightTypeGroup
import controllers.WeatherUtil.{Coordinates, Weather}

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms.{mapping, number}
import play.api.libs.json.{Json, _}
import play.api.mvc._

import scala.collection.mutable.{ListBuffer, Set}
import scala.math.BigDecimal.RoundingMode


class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  object AirportSimpleWrites extends Writes[Airport] {
    def writes(airport: Airport): JsValue = {
      JsObject(List(
      "id" -> JsNumber(airport.id),
      "name" -> JsString(airport.name),
      "iata" -> JsString(airport.iata),
      "city" -> JsString(airport.city),
      "latitude" -> JsNumber(airport.latitude),
      "longitude" -> JsNumber(airport.longitude),
      "countryCode" -> JsString(airport.countryCode),
      "zone" -> JsString(airport.zone)))
      
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
      "airlineId" -> JsNumber(link.airline.id),
      "flightCode" -> JsString(LinkUtil.getFlightCode(link.airline, link.flightNumber)),
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

  implicit object LoyalistWrites extends Writes[Loyalist] {
    def writes(loyalist: Loyalist): JsValue = {
      Json.obj(
        "airportId" -> loyalist.airport.id,
        "airlineId" -> loyalist.airline.id,
        "airlineName" -> loyalist.airline.name,
        "amount" -> loyalist.amount
      )
    }
  }

  implicit object LoyalistHistoryWrites extends Writes[LoyalistHistory] {
    def writes(entry: LoyalistHistory): JsValue = {
      Json.toJson(entry.entry).asInstanceOf[JsObject] + ("cycle" -> JsNumber(entry.cycle))
    }
  }

  implicit object AirportGradeInfoWrites extends Writes[AirportRating] {
    def writes(entry: AirportRating): JsValue = {
      Json.obj(
        "economicRating" -> entry.economicPowerRating,
        "competitionRating" -> entry.competitionRating,
        "countryRating" -> entry.countryPowerRating,
        "difficulty" -> entry.overallDifficulty,
        "features" -> JsArray(entry.features.sortBy(_.featureType.id).map { airportFeature =>
          Json.obj("type" -> airportFeature.featureType.toString(), "strength" -> airportFeature.strength, "title" -> airportFeature.getDescription)
        })
      )
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

  def getCurrentCycle() = Action  {
    Ok(Json.obj("cycle" -> CycleSource.loadCycle()))
  }




  def getAirports(@deprecated count : Int) = Action { //count is no longer used
    //val selectedAirports = cachedAirportsByPower.takeRight(count)
    Ok(Json.toJson(AirportUtil.visibleAirports))
  }
  
  def getAirport(airportId : Int, image : Boolean) = Action {
     AirportCache.getAirport(airportId, true) match {
       case Some(airport) =>
         var result = Json.toJson(airport).asInstanceOf[JsObject]
         //find links going to this airport too, send simplified data
         val links = LinkSource.loadFlightLinksByFromAirport(airportId, LinkSource.ID_LOAD) ++ LinkSource.loadFlightLinksByToAirport(airportId, LinkSource.ID_LOAD)
         val linkCountJson = links.groupBy { _.airline.id }.foldRight(Json.obj()) { 
           case((airlineId, links), foldJson) => foldJson + (airlineId.toString() -> JsNumber(links.length)) 
         }
         result = result + ("linkCounts" -> linkCountJson)

         if (image) {
           val cityImageUrl = GoogleImageUtil.getCityImageUrl(airport);
           if (cityImageUrl != null) {
             result = result + ("cityImageUrl" -> JsString(cityImageUrl.toString))
           }
           val airportImageUrl = GoogleImageUtil.getAirportImageUrl(airport);
           if (airportImageUrl != null) {
             result = result + ("airportImageUrl" -> JsString(airportImageUrl.toString))
           }
         }

         Ok(result)
       case None => NotFound
     }
  }

  def getAirportImages(airportId : Int) = Action {
    AirportCache.getAirport(airportId, false) match {
      case Some(airport) =>
        var result = Json.obj()
        val cityImageUrl = GoogleImageUtil.getCityImageUrl(airport);
        if (cityImageUrl != null) {
          result = result + ("cityImageUrl" -> JsString(cityImageUrl.toString))
        }
        val airportImageUrl = GoogleImageUtil.getAirportImageUrl(airport);
        if (airportImageUrl != null) {
          result = result + ("airportImageUrl" -> JsString(airportImageUrl.toString))
        }

        Ok(result)
      case None => NotFound
    }
  }


  def getImage(airport : Airport, phrases : List[String]) = {
    airport.name
  }


//  def getAirportSlotsByAirline(airportId : Int, airlineId : Int) = Action {
//    AirportCache.getAirport(airportId, true) match {
//       case Some(airport) =>
//         val maxSlots = airport.getMaxSlotAssignment(airlineId)
//         val assignedSlots = airport.getAirlineSlotAssignment(airlineId)
//         val preferredSlots = airport.getPreferredSlotAssignment(airlineId)
//         Ok(Json.obj("assignedSlots" -> JsNumber(assignedSlots), "maxSlots" -> JsNumber(maxSlots), "preferredSlots" -> JsNumber(preferredSlots)))
//       case None => NotFound
//     }
//  }
  
  def getAirportSharesOnCity(cityId : Int) = Action {
    Ok(Json.toJson(AirportSource.loadAirportSharesOnCity(cityId)))
  }
  
  def getAirportLinkStatistics(airportId : Int) = Action {
    AirportCache.getAirport(airportId, true) match {
      case Some(airport) => { 
        //group things up
        val flightsFromThisAirport = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airportId, LinkStatisticsSource.SIMPLE_LOAD)
        val flightsToThisAirport = LinkStatisticsSource.loadLinkStatisticsByToAirport(airportId, LinkStatisticsSource.SIMPLE_LOAD)
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
        
        val links = LinkSource.loadFlightLinksByFromAirport(airportId) ++ LinkSource.loadFlightLinksByToAirport(airportId)
        
        val servedCountries = Set[String]()
        val servedAirports = Set[Airport]()
        val airlines = Set[Airline]()
        var flightFrequency = 0;
        val linkCountByAirline = links.groupBy(_.airline.id).view.mapValues(_.size).toMap
        
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
        
        val loungesStats = LoungeHistorySource.loadLoungeConsumptionsByAirportId(airport.id)
        val loungesWithVisitors = loungesStats.map { _.lounge.airline.id }
        val emptyLoungesStats = ListBuffer[LoungeConsumptionDetails]() 
        //now some lounge might be newly built or have no visitors
        AirlineSource.loadLoungesByAirportId(airportId).foreach { lounge =>
          if (!loungesWithVisitors.contains(lounge.airline.id)) {
            emptyLoungesStats += LoungeConsumptionDetails(lounge = lounge, selfVisitors = 0, allianceVisitors = 0, cycle = 0)
          }
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
                    "lounges" -> Json.toJson(loungesStats ++ emptyLoungesStats),
                    "departureOrArrivalPassengers" -> departureOrArrivalPassengers, 
                    "transitPassengers" -> transitPassengers,
                    "airlineDeparture" -> Json.toJson(statisticsDepartureByAirline),
                    "airlineArrival" -> Json.toJson(statisticsArrivalByAirline),
                    "rating" -> Json.toJson(AirportRating.rateAirport(airport))))
      }
      case None => NotFound
    }
    
  }
  
  def getDepartures(airportId : Int, dayOfWeek : Int, hour : Int, minute : Int) = Action {
    val links = LinkSource.loadFlightLinksByFromAirport(airportId, LinkSource.SIMPLE_LOAD) ++ (LinkSource.loadFlightLinksByToAirport(airportId, LinkSource.SIMPLE_LOAD).map { link => link.copy(from = link.to, to = link.from) })
    
    val map = Map[Int, String]()
    
    val currentTime = TimeSlot(dayOfWeek = dayOfWeek, hour = hour, minute = minute)
    
    val linkConsumptions : Map[Int, LinkConsumptionDetails] = LinkSource.loadLinkConsumptionsByLinksId(links.map(_.id)).map( linkConsumption => (linkConsumption.link.id, linkConsumption)).toMap
    
    val airport = AirportCache.getAirport(airportId, false).get
    val weather = WeatherUtil.getWeather(new Coordinates(airport.latitude, airport.longitude))
    
    val random = new Random()
    random.setSeed(airport.id) //so generate same result every time
    
    val timeSlotLinkList : List[(TimeSlot, Link, TimeSlotStatus)] = links.flatMap { link => link.schedule.map { scheduledTimeSlot => (scheduledTimeSlot, link) }}.map {
      case (timeslot, link) => (timeslot, link, if (dayOfWeek == 6 && timeslot.dayOfWeek == 0) { timeslot.totalMinutes + 7 * 24 * 60 } else { timeslot.totalMinutes })
    }.filter {
      case(timeslot, _, wrappedMinutes) => wrappedMinutes >= currentTime.totalMinutes && wrappedMinutes <= currentTime.totalMinutes + 24 * 60   
    }.map {
      case (timeslot, link, wrappedMinutes) => (timeslot, link, getTimeSlotStatus(linkConsumptions.get(link.id), timeslot, currentTime, weather, random), wrappedMinutes)
    }.sortBy {
      case (timeslot, _, _, wrappedMinutes) => wrappedMinutes 
    }.map {
      case (timeslot, link, status, wrappedMinutes) => (timeslot, link, status) 
    }
    
    var result = Json.obj("timeslots" -> Json.toJson(timeSlotLinkList))
    
    if (weather != null) {
      result = result + ("weatherIcon" -> JsString("http://openweathermap.org/img/w/" + weather.getIcon + ".png")) + ("weatherDescription" -> JsString(weather.getDescription)) + ("temperature" -> JsNumber(weather.getTemperature))
    }
    
    Ok(result)
  }
  
  def getTimeSlotStatus(linkConsumptionOption : Option[LinkConsumptionDetails], scheduledTime : TimeSlot, currentTime : TimeSlot, weather : Weather, random : Random) : TimeSlotStatus = {
    var isMinorDelay = false
    var isMajorDelay = false
    var isCancelled = false
    var delayAmount = 0
    
    linkConsumptionOption.map { linkConsumption =>
      getWeatherError(weather, random) match {
        case(r1, r2, r3) => {
          isMinorDelay = r1
          isMajorDelay = r2
          isCancelled = r3
        }
      }
      
      val cancellationMarker = linkConsumption.link.cancellationCount
      val majorDelayMarker = cancellationMarker + linkConsumption.link.majorDelayCount
      val minorDelayMarker = majorDelayMarker +  linkConsumption.link.minorDelayCount
      
      val flightInterval = 60 * 24 * 7 / linkConsumption.link.frequency 
      val flightIndex = scheduledTime.totalMinutes / flightInterval  //nth flight on this route within this week
      
      val randomizedFlightIndex = (flightIndex + random.nextInt(linkConsumption.link.frequency)) % linkConsumption.link.frequency
      
      //println(cancellationMarker + "|" + majorDelayMarker + "|" + minorDelayMarker + " RI " + randomizedFlightIndex)
      //if u r unlucky enough to be smaller or equal to marker than BOOM!
      if (randomizedFlightIndex < cancellationMarker) {
        isCancelled = true
      } else if  (randomizedFlightIndex < majorDelayMarker) {
        isMajorDelay = true
      } else if  (randomizedFlightIndex < minorDelayMarker) {
        isMinorDelay = true
      }
      
      
      if (isMajorDelay) {
        delayAmount = 5 * 60 + randomizedFlightIndex * 30        
      } else if (isMinorDelay) {
        delayAmount =  20 + 100 / (randomizedFlightIndex + 1) //within 2 hours        
      }
    }
    
    if (isCancelled) {
      TimeSlotStatus("CANCELLED", "Cancelled")
    } else if (isMajorDelay || isMinorDelay) {
      val newTime = scheduledTime.increment(delayAmount)
      TimeSlotStatus("DELAY", "Delayed " + "%02d".format(newTime.hour) + ":" + "%02d".format(newTime.minute)) 
    } else if (scheduledTime.totalMinutes - currentTime.totalMinutes < 0) { // wrap around time, thats ok 
      TimeSlotStatus("ON_TIME", "On Time")
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
  
  def getWeatherError(weather : Weather, random : Random) : (Boolean, Boolean, Boolean) = {
    
    val errorChance : Double = //chance for Major delay/cancellation
    if (weather.getWindSpeed() >= 30) { //hurricane
      1; //all cancelled or major delay
    } else if (weather.getWindSpeed() >= 25) {
      0.9;
    } else if (weather.getWindSpeed() >= 20) {
      0.6;
    } else if (weather.getWindSpeed() >= 20) {
      0.3;
    } else { //other weather conditions
      val weatherId : Int = weather.getWeatherId()
      if (weatherId / 100 == 2) { //thunderstorm
        if (weatherId == 202 || weatherId == 212 || weatherId == 221) {
          0.8
        } else {
          0.2
        }
      } else if (weatherId / 100 == 5) { //rain
        if (weatherId == 502 || weatherId == 503) {
          0.30
        } else if (weatherId == 504) {
          0.50 
        } else if (weatherId == 522) {
          0.30
        } else if (weatherId == 531) {
          0.50
        } else {
          0.0
        }
      } else if (weatherId / 100 == 6) { //snow
        if (weatherId == 601) {
          0.30
        } else if (weatherId == 602) {
          0.80 
        } else {
          0.10
        }
      } else {
        0
      }
    }   
    
    if (errorChance == 0) {
      (false, false, false)
    } else {
       var randomNumber : Double = random.nextDouble()
       
       randomNumber = randomNumber * 100 - (randomNumber * 100).toInt  //somehow nextDouble doesnt give evenly distributed number...
       if (randomNumber <= errorChance) { //too bad...HIT!
         if (random.nextDouble < 0.3) { 
           (false, false, true) // cancellation
         } else {
           (false, true, false)  //major delay
         }
       } else if (randomNumber / 2 <= errorChance) { //ok..minor delay
         (true, false, false)
       } else {
         (false, false, false)  //safe...
       }
    }
  }
  
  
  def getAirportLinkConsumptions(fromAirportId : Int, toAirportId : Int) = Action {
    val competitorLinkConsumptions = (LinkSource.loadFlightLinksByAirports(fromAirportId, toAirportId, LinkSource.ID_LOAD) ++ LinkSource.loadFlightLinksByAirports(toAirportId, fromAirportId, LinkSource.ID_LOAD)).flatMap { link =>
      LinkSource.loadLinkConsumptionsByLinkId(link.id, 1)
    }
    Ok(Json.toJson(competitorLinkConsumptions.filter(_.link.capacity.total > 0).map { linkConsumption => Json.toJson(linkConsumption)(SimpleLinkConsumptionWrite) }.toSeq))
  }
  
  def getLinkConsumptionsByAirport(airportId : Int) = Action {
    val passengersByRemoteAirport : Map[Airport, Int] = HistoryUtil.loadConsumptionByAirport(airportId)
    Ok(Json.toJson(passengersByRemoteAirport.toList.map {
      case (remoteAirport, passengers) => Json.obj("remoteAirport" -> Json.toJson(remoteAirport)(AirportSimpleWrites), ("passengers" -> JsNumber(passengers)))
    }))
  }

  val MAX_LOYALIST_HISTORY_AIRLINE = 5

  def getAirportLoyalistData(airportId : Int, airlineIdOption : Option[Int]) = Action {
    val currentLoyalistEntries = LoyalistSource.loadLoyalistsByAirportId(airportId)
    val currentLoyalistByAirlineId = currentLoyalistEntries.map(entry => (entry.airline.id,  entry)).toMap
    val historyEntries = LoyalistSource.loadLoyalistsHistoryByAirportId(airportId)
    val airlineDeltas = ListBuffer[(Airline, Int)]()
    val airport = AirportCache.getAirport(airportId).get
    val currentCycle = CycleSource.loadCycle()
    var result =
      historyEntries.toList.sortBy(_._1).lastOption match {
        case Some((lastCycle, lastEntry)) =>
          val topAirlineIds = lastEntry.sortBy(_.entry.amount).takeRight(MAX_LOYALIST_HISTORY_AIRLINE).map(_.entry.airline.id).toSet
          val reportingAirlineIds : List[Int] = airlineIdOption match {
            case Some(airlineId) => (topAirlineIds + airlineId).toList
            case None => topAirlineIds.toList
          }

          val processedEntries : List[(Int, List[LoyalistHistory])] = historyEntries.toList.sortBy(_._1).map {
            case((cycle, entries)) =>
              val entriesByAirlineId = entries.map(entry => (entry.entry.airline.id, entry)).toMap
              val paddedEntries = reportingAirlineIds.map {  reportingAirlineId =>
                entriesByAirlineId.getOrElse(reportingAirlineId, LoyalistHistory(Loyalist(airport, AirlineCache.getAirline(reportingAirlineId).get, 0), cycle)) //pad with zero entries
              }
              val cycleDelta = currentCycle - cycle
              if (cycleDelta > 1 &&  cycleDelta <= AirportSimulation.LOYALIST_HISTORY_SAVE_INTERVAL + 1) { //then it is the closest historical entry from current turn
                reportingAirlineIds.foreach { reportingAirlineId =>
                  val previousLoyalistCount = entriesByAirlineId.get(reportingAirlineId).map(_.entry.amount).getOrElse(0)
                  airlineDeltas.append((AirlineCache.getAirline(reportingAirlineId).get, (currentLoyalistByAirlineId.get(reportingAirlineId).map(_.amount).getOrElse(0) - previousLoyalistCount) / cycleDelta))
                }
              }

              (cycle, paddedEntries)
          }

          Json.obj("current" -> currentLoyalistEntries, "history" -> processedEntries, "airlineDeltas" -> airlineDeltas.toList.sortBy(_._2)(Ordering[Int].reverse))
        case None =>
          Json.obj("current" -> currentLoyalistEntries)
      }
    Ok(result)
  }

  def getScaleDetails() = Action {
    var scaleProgressionResult = Json.arr()
    (1 to 15).map { scale =>
      var perScaleResult = Json.obj("scale" -> scale)
      var maxFrequencyJson = Json.obj()
      FlightTypeGroup.values.foreach { group =>
        maxFrequencyJson = maxFrequencyJson + (group.toString -> JsNumber(NegotiationUtil.getMaxFrequencyByGroup(scale, group)))
      }

      perScaleResult =  perScaleResult +
        ("maxFrequency" -> maxFrequencyJson) +
        ("baseStaffCapacity" -> JsNumber(AirlineBase.getOfficeStaffCapacity(scale, false))) +
        ("headquartersStaffCapacity" -> JsNumber(AirlineBase.getOfficeStaffCapacity(scale, true)))

      scaleProgressionResult = scaleProgressionResult.append(perScaleResult)
    }

    var groupInfoJson = Json.obj()
    FlightType.values.toList.groupBy(NegotiationUtil.getFlightTypeGroup(_)).foreach {
      case (group, flightTypes) => groupInfoJson = groupInfoJson + (group.toString -> JsString(flightTypes.map(FlightType.label(_)).mkString(", ")))
    }
    var result = Json.obj("scaleProgression" -> scaleProgressionResult, "groupInfo" -> groupInfoJson)


    Ok(result)
  }

  def getLookups() = Action {
    val airlineGradeLookup = AirlineGrade.allGrades.map(grade => (grade.value.toString, grade.reputationCeiling)).toMap

    Ok(Json.obj("airlineGradeLookup" -> airlineGradeLookup))
  }

  def getAirportChampions(airportId : Int) = Action {
    var result = Json.arr()
    val airport = AirportCache.getAirport(airportId, true).get
    ChampionUtil.loadAirportChampionInfoByAirport(airportId).sortBy(_.ranking).foreach { info =>
      result = result.append(Json.toJson(info).asInstanceOf[JsObject] + ("loyalty" -> JsNumber(BigDecimal(airport.getAirlineLoyalty(info.loyalist.airline.id)).setScale(2, RoundingMode.HALF_EVEN))))
    }
    Ok(result)
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

  def redirect(path: String, any : String) = Action {
    Redirect(path)
  }

  case class LinkInfo(fromId : Int, toId : Int, price : Double, capacity : Int)
}
