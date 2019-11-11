import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.patson.data._
import com.patson.data.airplane._
import com.patson.model._
import com.patson.model.airplane._
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsValue
import com.patson.model.Computation
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import com.patson.Util
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.JsBoolean
import com.patson.model.AirlineIncome
import com.patson.model.AirlineIncome
import com.patson.model.AirlineCashFlow
import play.api.libs.json.Reads
import models.AirportFacility
import models.FacilityType
import models.AirportFacility
import models.FacilityType.FacilityType
import com.patson.util.ChampionInfo

import scala.concurrent.ExecutionContext



package object controllers {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem("patson-web-app-system")
  implicit val materializer = ActorMaterializer()
  implicit val order = Ordering.Double.IeeeOrdering

  implicit object AirlineFormat extends Format[Airline] {
    def reads(json: JsValue): JsResult[Airline] = {
      val airline = Airline.fromId((json \ "id").as[Int])
      JsSuccess(airline)
    }
    
    def writes(airline: Airline): JsValue = {
      var result = JsObject(List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "reputation" -> JsNumber(airline.getReputation()),
      "gradeValue" -> JsNumber(airline.airlineGrade.value),
      "airlineCode" -> JsString(airline.getAirlineCode()),
      "baseCount" -> JsNumber(airline.getBases().size),
      "isGenerated" -> JsBoolean(airline.isGenerated)))
      
      if (airline.getCountryCode.isDefined) {
        result = result.asInstanceOf[JsObject] + ("countryCode" -> JsString(airline.getCountryCode.get))
      }
      airline.getHeadQuarter().foreach { headquarters =>
        result = result.asInstanceOf[JsObject] + 
        ("headquartersAirportName" -> JsString(headquarters.airport.name)) + 
        ("headquartersCity" -> JsString(headquarters.airport.city)) + 
        ("headquartersAirportIata" -> JsString(headquarters.airport.iata))
      }
      
      result
    }
  }
  
  implicit object AirplaneModelWrites extends Writes[Model] {
    def writes(airplaneModel: Model): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplaneModel.id),
      "name" -> JsString(airplaneModel.name),
      "capacity" -> JsNumber(airplaneModel.capacity),
      "fuelBurn" -> JsNumber(airplaneModel.fuelBurn),
      "speed" -> JsNumber(airplaneModel.speed),
      "range" -> JsNumber(airplaneModel.range),
      "price" -> JsNumber(airplaneModel.price),
      "lifespan" -> JsNumber(airplaneModel.lifespan),
      "airplaneType" -> JsString(airplaneModel.airplaneTypeLabel),
      "minAirportSize" -> JsNumber(airplaneModel.minAirportSize),
      "badConditionThreshold" -> JsNumber(Airplane.BAD_CONDITION), //same for all models for now
      "criticalConditionThreshold" -> JsNumber(Airplane.CRITICAL_CONDITION), //same for all models for now
      "constructionTime" -> JsNumber(airplaneModel.constructionTime),
      "imageUrl" -> JsString(airplaneModel.imageUrl),
      "countryCode" -> JsString(airplaneModel.countryCode)
          ))
      
    }
  }
  
  implicit object AirplaneWrites extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
          JsObject(List(
      "id" -> JsNumber(airplane.id),
      "ownerId" -> JsNumber(airplane.owner.id), 
      "name" -> JsString(airplane.model.name),
      "modelId" -> JsNumber(airplane.model.id),
      "capacity" -> JsNumber(airplane.model.capacity),
      "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
      "speed" -> JsNumber(airplane.model.speed),
      "range" -> JsNumber(airplane.model.range),
      "price" -> JsNumber(airplane.model.price),
      "condition" -> JsNumber(airplane.condition),
      "age" -> JsNumber(Computation.calculateAge(airplane.constructedCycle)),
      "value" -> JsNumber(airplane.value),
      "sellValue" -> JsNumber(Computation.calculateAirplaneSellValue(airplane)),
      "dealerValue" -> JsNumber(airplane.dealerValue)
      ))
    }
  }
  
  implicit object LinkClassValuesWrites extends Writes[LinkClassValues] {
    def writes(linkClassValues: LinkClassValues): JsValue = JsObject(List(
      "economy" -> JsNumber(linkClassValues(ECONOMY)),
      "business" -> JsNumber(linkClassValues(BUSINESS)),
      "first" -> JsNumber(linkClassValues(FIRST))))
  }
  
  implicit object LinkFormat extends Format[Link] {
    def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      //val capacity = json.\("capacity").as[Int]
      val price = LinkClassValues.getInstance(json.\("price").\("economy").as[Int], json.\("price").\("business").as[Int], json.\("price").\("first").as[Int])
      val fromAirport = AirportSource.loadAirportById(fromAirportId, true).get
      val toAirport = AirportSource.loadAirportById(toAirportId, true).get
      val airline = AirlineSource.loadAirlineById(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
      val airplaneIds = json.\("airplanes").as[Array[Int]]
      val frequency = json.\("frequency").as[Int]
      val modelId = json.\("model").as[Int]
      
      val capacity = LinkClassValues.getInstance(frequency * json.\("configuration").\("economy").as[Int],
                                              frequency * json.\("configuration").\("business").as[Int],
                                              frequency * json.\("configuration").\("first").as[Int])
                                
      val duration = ModelSource.loadModelById(modelId).fold(Integer.MAX_VALUE)(Computation.calculateDuration(_, distance))
       
      val airplanes = airplaneIds.foldRight(List[Airplane]()) { (airplaneId, foldList) =>
        AirplaneSource.loadAirplanesWithAssignedLinkByAirplaneId(airplaneId, AirplaneSource.LINK_SIMPLE_LOAD) match { 
          case Some((airplane, Some(link))) if (link.from.id != fromAirport.id || link.to.id != toAirport.id) =>
            throw new IllegalStateException("Airplane with id " + airplaneId + " is assigned to other link")
          case Some((airplane, _)) =>
            airplane :: foldList
          case None => 
            throw new IllegalStateException("Airplane with id " + airplaneId + " does not exist")
        }
      }
      var rawQuality =  json.\("rawQuality").as[Int]
      if (rawQuality > Link.MAX_QUALITY) {
        rawQuality = Link.MAX_QUALITY
      } else if (rawQuality < 0) {
        rawQuality = 0
      }
         
      val link = Link(fromAirport, toAirport, airline, price, distance, capacity, rawQuality, duration, frequency, flightType)
      link.setAssignedAirplanes(airplanes)
      //(json \ "id").asOpt[Int].foreach { link.id = _ } 
      JsSuccess(link)
    }
    
    def writes(link: Link): JsValue = {
      var json = JsObject(List(
      "id" -> JsNumber(link.id),
      "fromAirportId" -> JsNumber(link.from.id),
      "toAirportId" -> JsNumber(link.to.id),
      "fromAirportCode" -> JsString(link.from.iata),
      "toAirportCode" -> JsString(link.to.iata),
      "fromAirportName" -> JsString(link.from.name),
      "toAirportName" -> JsString(link.to.name),
      "fromAirportCity" -> JsString(link.from.city),
      "toAirportCity" -> JsString(link.to.city),
      "fromCountryCode" -> JsString(link.from.countryCode),
      "toCountryCode" -> JsString(link.to.countryCode),
      "airlineId" -> JsNumber(link.airline.id),
      "price" -> Json.toJson(link.price),
      "distance" -> JsNumber(link.distance),
      "capacity" -> Json.toJson(link.capacity),
      "rawQuality" -> JsNumber(link.rawQuality),
      "computedQuality" -> JsNumber(link.computedQuality),
      "duration" -> JsNumber(link.duration),
      "frequency" -> JsNumber(link.frequency),
      "availableSeat" -> Json.toJson(link.availableSeats),
      "fromLatitude" -> JsNumber(link.from.latitude),
      "fromLongitude" -> JsNumber(link.from.longitude),
      "toLatitude" -> JsNumber(link.to.latitude),
      "toLongitude" -> JsNumber(link.to.longitude),
      "flightType" -> JsString(link.flightType.toString()),
      "flightCode" -> JsString(LinkApplication.getFlightCode(link.airline, link.flightNumber)),
      "assignedAirplanes" -> Json.toJson(link.getAssignedAirplanes())))
      
      link.getAssignedModel().foreach { model =>
        json = json + ("modelId" -> JsNumber(model.id))
      }
      json
    }
  }
  
  object SimpleLinkConsumptionWrite extends Writes[LinkConsumptionDetails] {
     def writes(linkConsumption : LinkConsumptionDetails): JsValue = { 
       var priceJson = Json.obj() 
       LinkClass.values.foreach { linkClass =>
         if (linkConsumption.link.capacity(linkClass) > 0) { //do not report price for link class that has no capacity
           priceJson = priceJson + (linkClass.label -> JsNumber(linkConsumption.link.price(linkClass)))
         }  
       }
       
       
      JsObject(List(
        "linkId" -> JsNumber(linkConsumption.link.id),
        "airlineId" -> JsNumber(linkConsumption.link.airline.id),
        "airlineName" -> JsString(AirlineSource.loadAirlineById(linkConsumption.link.airline.id).fold("<unknown airline>") { _.name }),
        "price" -> priceJson,
        "capacity" -> Json.toJson(linkConsumption.link.capacity),
        "soldSeats" -> JsNumber(linkConsumption.link.soldSeats.total),
        "quality" -> JsNumber(linkConsumption.link.computedQuality)))
    }
  }
  
  implicit object AirlineBaseFormat extends Format[AirlineBase] {
    def reads(json: JsValue): JsResult[AirlineBase] = {
      val airport = AirportSource.loadAirportById((json \ "airportId").as[Int]).get
      val airline = Airline.fromId((json \ "airlineId").as[Int])
      val scale = (json \ "scale").as[Int]
      val headquarter = (json \ "headquarter").as[Boolean]
      JsSuccess(AirlineBase(airline, airport, "", scale, 0, headquarter))
    }
    
    def writes(base: AirlineBase): JsValue = {
      var jsObject = JsObject(List(
      "airportId" -> JsNumber(base.airport.id),
      "airportName" -> JsString(base.airport.name),
      "countryCode" -> JsString(base.airport.countryCode),
      "airportZone" -> JsString(base.airport.zone),
      "city" -> JsString(base.airport.city),
      "airlineId" -> JsNumber(base.airline.id),
      "airlineName" -> JsString(base.airline.name),
      "scale" -> JsNumber(base.scale),
      "upkeep" -> JsNumber(base.getUpkeep),
      "value" -> JsNumber(base.getValue),
      "headquarter" -> JsBoolean(base.headquarter),
      "foundedCycle" -> JsNumber(base.foundedCycle)))
      
      base.airline.getCountryCode().foreach { countryCode =>
        jsObject = jsObject + ("airlineCountryCode" -> JsString(countryCode))
      }
      
      jsObject
    }
        
  }
  
  implicit object FacilityReads extends Reads[AirportFacility] {
    def reads(json: JsValue): JsResult[AirportFacility] = {
      val airport = AirportSource.loadAirportById((json \ "airportId").as[Int]).get
      val airline = Airline.fromId((json \ "airlineId").as[Int])
      val name = (json \ "name").as[String]
      val level = (json \ "level").as[Int]
      JsSuccess(AirportFacility(airline, airport, name, level, FacilityType.withName((json \ "type").as[String])))
    }
  }
  
  implicit object LoungeWrites extends Writes[Lounge] {
    def writes(lounge: Lounge): JsValue = {
      var jsObject = JsObject(List(
      "airportId" -> JsNumber(lounge.airport.id),
      "airportName" -> JsString(lounge.airport.name),
      "airlineId" -> JsNumber(lounge.airline.id),
      "airlineName" -> JsString(lounge.airline.name),
      "name" ->  JsString(lounge.name),
      "level" -> JsNumber(lounge.level),
      "upkeep" -> JsNumber(lounge.getUpkeep),
      "status" -> JsString(lounge.status.toString()),
      "type" -> JsString(FacilityType.LOUNGE.toString()),
      "foundedCycle" -> JsNumber(lounge.foundedCycle)))
      
      jsObject
    }
  }
  
  implicit object LoungeConsumptionDetailsWrites extends Writes[LoungeConsumptionDetails] {
    def writes(details: LoungeConsumptionDetails): JsValue = {
      var jsObject = JsObject(List(
      "lounge" -> Json.toJson(details.lounge),
      "selfVisitors" -> JsNumber(details.selfVisitors),
      "allianceVisitors" -> JsNumber(details.allianceVisitors),
      "cycle" -> JsNumber(details.cycle)))
      
      jsObject
    }
  }
  
  implicit object AirlineIncomeWrite extends Writes[AirlineIncome] {
     def writes(airlineIncome : AirlineIncome): JsValue = {
      JsObject(List(
        "airlineId" -> JsNumber(airlineIncome.airlineId),
        "totalProfit" -> JsNumber(airlineIncome.profit),
        "totalRevenue" -> JsNumber(airlineIncome.revenue),
        "totalExpense" -> JsNumber(airlineIncome.expense),
        "linksProfit" -> JsNumber(airlineIncome.links.profit),
        "linksRevenue" -> JsNumber(airlineIncome.links.revenue),
        "linksExpense" -> JsNumber(airlineIncome.links.expense),
        "linksTicketRevenue" -> JsNumber(airlineIncome.links.ticketRevenue),
        "linksAirportFee" -> JsNumber(airlineIncome.links.airportFee),
        "linksFuelCost" -> JsNumber(airlineIncome.links.fuelCost),
        "linksCrewCost" -> JsNumber(airlineIncome.links.crewCost),
        "linksInflightCost" -> JsNumber(airlineIncome.links.inflightCost),
        "linksDelayCompensation" -> JsNumber(airlineIncome.links.delayCompensation),
        "linksMaintenanceCost" -> JsNumber(airlineIncome.links.maintenanceCost),
        "linksLoungeCost" -> JsNumber(airlineIncome.links.loungeCost),
        "linksDepreciation" -> JsNumber(airlineIncome.links.depreciation),
        "transactionsProfit" -> JsNumber(airlineIncome.transactions.profit),
        "transactionsRevenue" -> JsNumber(airlineIncome.transactions.revenue),
        "transactionsExpense" -> JsNumber(airlineIncome.transactions.expense),
        "transactionsCapitalGain" -> JsNumber(airlineIncome.transactions.capitalGain),
        "transactionsCreateLink" -> JsNumber(airlineIncome.transactions.createLink),
        "othersProfit" -> JsNumber(airlineIncome.others.profit),
        "othersRevenue" -> JsNumber(airlineIncome.others.revenue),
        "othersExpense" -> JsNumber(airlineIncome.others.expense),
        "othersLoanInterest" -> JsNumber(airlineIncome.others.loanInterest),
        "othersBaseUpkeep" -> JsNumber(airlineIncome.others.baseUpkeep),
        "othersLoungeUpkeep" -> JsNumber(airlineIncome.others.loungeUpkeep),
        "othersLoungeCost" -> JsNumber(airlineIncome.others.loungeCost),
        "othersLoungeIncome" -> JsNumber(airlineIncome.others.loungeIncome),
        "othersServiceInvestment" -> JsNumber(airlineIncome.others.serviceInvestment),
        "othersMaintenanceInvestment" -> JsNumber(airlineIncome.others.maintenanceInvestment),
        "othersAdvertisement" -> JsNumber(airlineIncome.others.advertisement),
        "othersFuelProfit" -> JsNumber(airlineIncome.others.fuelProfit),
        "othersDepreciation" -> JsNumber(airlineIncome.others.depreciation),
        "period" -> JsString(airlineIncome.period.toString()),
        "cycle" -> JsNumber(airlineIncome.cycle)))
    }
  }

  implicit object AirlineCashFlowWrite extends Writes[AirlineCashFlow] {
     def writes(airlineCashFlow : AirlineCashFlow): JsValue = {
      JsObject(List(
        "airlineId" -> JsNumber(airlineCashFlow.airlineId),
        "totalCashFlow" -> JsNumber(airlineCashFlow.cashFlow),
        "operation" -> JsNumber(airlineCashFlow.operation),
        "loanInterest" -> JsNumber(airlineCashFlow.loanInterest),
        "loanPrincipal" -> JsNumber(airlineCashFlow.loanPrincipal),
        "baseConstruction" -> JsNumber(airlineCashFlow.baseConstruction),
        "buyAirplane" -> JsNumber(airlineCashFlow.buyAirplane),
        "sellAirplane" -> JsNumber(airlineCashFlow.sellAirplane),
        "createLink" -> JsNumber(airlineCashFlow.createLink),
        "facilityConstruction" -> JsNumber(airlineCashFlow.facilityConstruction),
        "oilContract" -> JsNumber(airlineCashFlow.oilContract),
        "period" -> JsString(airlineCashFlow.period.toString()),
        "cycle" -> JsNumber(airlineCashFlow.cycle)))
    }
  }
  
  implicit object CountryWrites extends Writes[Country] {
    def writes(country : Country): JsValue = {
      Json.obj(
        "countryCode" -> country.countryCode,
        "name" -> country.name,
        "airportPopulation" -> country.airportPopulation,
        "incomeLevel" -> Computation.getIncomeLevel(country.income),
        "openness" ->  country.openness
      )
    }
  }
  
  implicit object CountryWithMutualRelationshipWrites extends Writes[(Country, Int)] {
    def writes(countryWithMutualRelationship : (Country, Int)): JsValue = {
      val (country, mutualRelationship) = countryWithMutualRelationship
      Json.obj(
        "countryCode" -> country.countryCode,
        "name" -> country.name,
        "airportPopulation" -> country.airportPopulation,
        "incomeLevel" -> Computation.getIncomeLevel(country.income),
        "openness" ->  country.openness,
        "mutualRelationship" -> mutualRelationship
      )
    }
  }
  
  implicit object ChampionedCountriesWrites extends Writes[ChampionInfo] {
    def writes(info : ChampionInfo): JsValue = {
      Json.obj(
              "country" -> Json.toJson(info.country),
              "airlineId" -> JsNumber(info.airline.id),
              "airlineName" -> JsString(info.airline.name),
              "ranking" -> JsNumber(info.ranking),
              "passengerCount" -> JsNumber(info.passengerCount),
              "reputationBoost" -> JsNumber(info.reputationBoost))
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
}