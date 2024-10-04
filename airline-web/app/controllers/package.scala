import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ActorMaterializer
import com.patson.{AllianceMissionSimulation, Util}
import com.patson.data._
import com.patson.data.airplane._
import com.patson.model.{AirlineBaseSpecialization, AirlineCashFlow, AirlineIncome, Computation, _}
import com.patson.model.airplane._
import com.patson.model.alliance.{AllianceMission, AllianceMissionStatus, AllianceMissionReward}
import com.patson.model.event.EventReward
import com.patson.util.{AirlineCache, AirportCache, AirportChampionInfo, ChampionUtil, CountryChampionInfo}
import models.{AirportFacility, AirportWithChampion, FacilityType}
import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode



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
      var result = Json.obj(
        "id" -> airline.id,
        "name" -> airline.name,
        "reputation" -> airline.getReputation(),
        "gradeValue" -> airline.airlineGrade.value,
        "gradeDescription" -> airline.airlineGrade.description,
        "airlineCode" -> airline.getAirlineCode(),
        "baseCount" -> airline.getBases().size,
        "isGenerated" -> airline.isGenerated
      )

      if (airline.getCountryCode.isDefined) {
        result = result + ("countryCode" -> JsString(airline.getCountryCode.get))
      }
      airline.getHeadQuarter().foreach { headquarters =>
        result = result +
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
      "family" -> JsString(airplaneModel.family),
      "capacity" -> JsNumber(airplaneModel.capacity),
      "fuelBurn" -> JsNumber(airplaneModel.fuelBurn),
      "speed" -> JsNumber(airplaneModel.speed),
      "range" -> JsNumber(airplaneModel.range),
      "price" -> JsNumber(airplaneModel.price),
      "lifespan" -> JsNumber(airplaneModel.lifespan),
      "airplaneType" -> JsString(airplaneModel.airplaneTypeLabel),
      "turnaroundTime" -> JsNumber(airplaneModel.turnaroundTime),
      "runwayRequirement" -> JsNumber(airplaneModel.runwayRequirement),
      "badConditionThreshold" -> JsNumber(Airplane.BAD_CONDITION), //same for all models for now
      "criticalConditionThreshold" -> JsNumber(Airplane.CRITICAL_CONDITION), //same for all models for now
      "constructionTime" -> JsNumber(airplaneModel.constructionTime),
      "imageUrl" -> JsString(airplaneModel.imageUrl),
      "countryCode" -> JsString(airplaneModel.manufacturer.countryCode),
      "manufacturer" -> JsString(airplaneModel.manufacturer.name)
          ))

    }
  }

  object SimpleAirplaneWrite extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
      JsObject(List(
        "id" -> JsNumber(airplane.id),
        "ownerId" -> JsNumber(airplane.owner.id),
        "ownerName" -> JsString(airplane.owner.name),
        "name" -> JsString(airplane.model.name),
        "modelId" -> JsNumber(airplane.model.id),
        "capacity" -> JsNumber(airplane.model.capacity),
        "fuelBurn" -> JsNumber(airplane.model.fuelBurn),
        "speed" -> JsNumber(airplane.model.speed),
        "range" -> JsNumber(airplane.model.range),
        "price" -> JsNumber(airplane.model.price),
        "lifespan" -> JsNumber(airplane.model.lifespan),
        "condition" -> JsNumber(airplane.condition),
        "constructedCycle" -> JsNumber(airplane.constructedCycle),
        "purchasedCycle" ->  JsNumber(airplane.purchasedCycle),
        "isReady" ->  JsBoolean(airplane.isReady),
        "constructionTime" -> JsNumber(airplane.model.constructionTime),
        "value" -> JsNumber(airplane.value),
        "sellValue" -> JsNumber(Computation.calculateAirplaneSellValue(airplane)),
        "dealerValue" -> JsNumber(airplane.dealerValue),
        "dealerRatio" -> JsNumber(airplane.dealerRatio),
        "configurationId" -> JsNumber(airplane.configuration.id),
        "configuration" -> Json.obj("economy" -> airplane.configuration.economyVal, "business" -> airplane.configuration.businessVal, "first" -> airplane.configuration.firstVal),
        //"availableFlightMinutes" -> JsNumber(airplane.availableFlightMinutes),
        "maxFlightMinutes" -> JsNumber(Airplane.MAX_FLIGHT_MINUTES),
        "homeAirportId" -> JsNumber(airplane.home.id),
        "badConditionThreshold" -> JsNumber(Airplane.BAD_CONDITION),
        "criticalConditionThreshold" -> JsNumber(Airplane.CRITICAL_CONDITION)
      ))
    }
  }

  implicit object AirplaneWrites extends Writes[Airplane] {
    def writes(airplane: Airplane): JsValue = {
      Json.toJson(airplane)(SimpleAirplaneWrite).asInstanceOf[JsObject] + ("availableFlightMinutes" -> JsNumber(airplane.availableFlightMinutes))
    }
  }

  implicit object LinkClassValuesFormat extends Format[LinkClassValues] {
    def reads(json: JsValue): JsResult[LinkClassValues] = {
      val value = LinkClassValues.getInstance((json \ "economy").as[Int], (json \ "business").as[Int], (json \ "first").as[Int])
      JsSuccess(value)
    }

    def writes(linkClassValues: LinkClassValues): JsValue = JsObject(List(
      "economy" -> JsNumber(linkClassValues(ECONOMY)),
      "business" -> JsNumber(linkClassValues(BUSINESS)),
      "first" -> JsNumber(linkClassValues(FIRST)),
      "total" -> JsNumber(linkClassValues.total)))
  }
  object AirplaneAssignmentsRead extends Reads[Map[Airplane, Int]] {
    def reads(json : JsValue) : JsResult[Map[Airplane, Int]] = {
      JsSuccess(json.asInstanceOf[JsObject].keys.map { airplaneId =>
        val frequency = json.\(airplaneId).as[Int]
        AirplaneSource.loadAirplaneById(airplaneId.toInt) match {
          case Some(airplane) => (airplane, frequency)
          case None => throw new IllegalStateException("Airplane with id " + airplaneId + " does not exist")
        }
      }.toMap)
    }
  }

  implicit object LinkFormat extends Format[Link] {
    def reads(json: JsValue): JsResult[Link] = {
      val fromAirportId = json.\("fromAirportId").as[Int]
      val toAirportId = json.\("toAirportId").as[Int]
      val airlineId = json.\("airlineId").as[Int]
      //val capacity = json.\("capacity").as[Int]
      val price = LinkClassValues.getInstance(json.\("price").\("economy").as[Int], json.\("price").\("business").as[Int], json.\("price").\("first").as[Int])
      val fromAirport = AirportCache.getAirport(fromAirportId, true).get
      val toAirport = AirportCache.getAirport(toAirportId, true).get
      val airline = AirlineCache.getAirline(airlineId).get
      val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
      val flightType = Computation.getFlightType(fromAirport, toAirport, distance)
      val airplaneAssignments = json.\("airplanes").as[Map[Airplane, Int]](AirplaneAssignmentsRead)

      val modelId = json.\("model").as[Int]
      val modelOption = ModelSource.loadModelById(modelId)
      val duration = modelOption match {
        case Some(model) => Computation.calculateDuration(model, distance)
        case None => 0
      }
      val flightMinutesRequiredPerFlight = modelOption match {
        case Some(model) => Computation.calculateFlightMinutesRequired(model, distance)
        case None => 0
      }


      var rawQuality =  json.\("rawQuality").as[Int]
      if (rawQuality > Link.MAX_QUALITY) {
        rawQuality = Link.MAX_QUALITY
      } else if (rawQuality < 0) {
        rawQuality = 0
      }

      val link = Link(fromAirport, toAirport, airline, price, distance, capacity = LinkClassValues.getInstance(), rawQuality, duration, frequency = 0, flightType) //compute frequency and capacity after validating the assigned airplanes
      link.setAssignedAirplanes(airplaneAssignments.toList.map {
        case(airplane, frequency) => (airplane, LinkAssignment(frequency, frequency * flightMinutesRequiredPerFlight))
      }.toMap)
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
      "airlineName" -> JsString(link.airline.name),
      "price" -> Json.toJson(link.price),
      "distance" -> JsNumber(link.distance),
      "flightType" -> JsString(FlightType.label(link.flightType)),
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
      "flightCode" -> JsString(LinkUtil.getFlightCode(link.airline, link.flightNumber))
      ))

      var airplanesJson = Json.arr()
      link.getAssignedAirplanes().foreach {
        case (airplane, assignment: LinkAssignment) => airplanesJson = airplanesJson.append(Json.obj("airplane" -> airplane, "frequency" -> assignment.frequency))
      }

      json = json + ("assignedAirplanes" -> airplanesJson)

      link.getAssignedModel().foreach { model =>
        json = json + ("modelId" -> JsNumber(model.id))
        json = json + ("modelName" -> JsString(model.name))
      }

      val constructingAirplanes = link.getAssignedAirplanes().filter(!_._1.isReady)
      if (!constructingAirplanes.isEmpty) {
        val waitTime = CycleSource.loadCycle() - constructingAirplanes.keys.map(_.constructedCycle).max
        json = json + ("future" -> Json.obj("frequency" -> link.futureFrequency(), "capacity" -> link.futureCapacity(), "waitTime" -> waitTime))
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
        "airlineName" -> JsString(AirlineCache.getAirline(linkConsumption.link.airline.id).fold("<unknown airline>") { _.name }),
        "price" -> priceJson,
        "capacity" -> Json.toJson(linkConsumption.link.capacity),
        "frequency" -> JsNumber(linkConsumption.link.frequency),
        "soldSeats" -> JsNumber(linkConsumption.link.soldSeats.total),
        "quality" -> JsNumber(linkConsumption.link.computedQuality)))
    }
  }

  /**
    * Do not expose sold seats
    */
  object MinimumLinkConsumptionWrite extends Writes[LinkConsumptionDetails] {
    def writes(linkConsumption : LinkConsumptionDetails): JsValue = {
      var priceJson = Json.obj()
      LinkClass.values.foreach { linkClass =>
        if (linkConsumption.link.capacity(linkClass) > 0) { //do not report price for link class that has no capacity
          priceJson = priceJson + (linkClass.label -> JsNumber(linkConsumption.link.price(linkClass)))
        }
      }


      JsObject(List(
        "cycle" -> JsNumber(linkConsumption.cycle),
        "linkId" -> JsNumber(linkConsumption.link.id),
        "airlineId" -> JsNumber(linkConsumption.link.airline.id),
        "airlineName" -> JsString(AirlineCache.getAirline(linkConsumption.link.airline.id).fold("<unknown airline>") { _.name }),
        "price" -> priceJson,
        "capacity" -> Json.toJson(linkConsumption.link.capacity)))
    }
  }

  case class AirlineBaseSpecializationWrites(airport : Airport) extends OWrites[AirlineBaseSpecialization.Value] {
    override def writes(specialization : AirlineBaseSpecialization.Value) : JsObject = {
      Json.obj(
        "id" -> specialization.toString,
        "label" -> specialization.label,
        "descriptions" -> specialization.descriptions(airport),
      )
    }
  }

  implicit object AirlineBaseFormat extends Format[AirlineBase] {
    def reads(json: JsValue): JsResult[AirlineBase] = {
      val airport = AirportCache.getAirport((json \ "airportId").as[Int], true).get
      val airline = AirlineCache.getAirline((json \ "airlineId").as[Int]).get
      val scale = (json \ "scale").as[Int]
      val headquarter = (json \ "headquarter").as[Boolean]
      JsSuccess(AirlineBase(airline, airport, airport.countryCode, scale, 0, headquarter))
    }

    def writes(base: AirlineBase): JsValue = {
      var jsObject = JsObject(List(
      "airportId" -> JsNumber(base.airport.id),
      "airportName" -> JsString(base.airport.name),
      "airportCode" -> JsString(base.airport.iata),
      "airportRunwayLength" -> JsNumber(base.airport.runwayLength),
      "countryCode" -> JsString(base.airport.countryCode),
      "airportZone" -> JsString(base.airport.zone),
      "city" -> JsString(base.airport.city),
      "airlineId" -> JsNumber(base.airline.id),
      "airlineName" -> JsString(base.airline.name),
      "scale" -> JsNumber(base.scale),
      "upkeep" -> JsNumber(base.getUpkeep),
      "value" -> JsNumber(base.getValue),
      "delegatesRequired" -> JsNumber(base.delegatesRequired),
      "headquarter" -> JsBoolean(base.headquarter),
      "foundedCycle" -> JsNumber(base.foundedCycle)))

      base.airline.getCountryCode().foreach { countryCode =>
        jsObject = jsObject + ("airlineCountryCode" -> JsString(countryCode))
      }

      if (!base.specializations.isEmpty) {
        implicit val writes = AirlineBaseSpecializationWrites(base.airport)
        jsObject = jsObject + ("specializations" -> Json.toJson(base.specializations))
      }

      jsObject
    }
  }

  implicit object FacilityReads extends Reads[AirportFacility] {
    def reads(json: JsValue): JsResult[AirportFacility] = {
      val airport = AirportCache.getAirport((json \ "airportId").as[Int], true).get
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
        "othersOvertimeCompensation" -> JsNumber(airlineIncome.others.overtimeCompensation),
        "othersLoungeUpkeep" -> JsNumber(airlineIncome.others.loungeUpkeep),
        "othersLoungeCost" -> JsNumber(airlineIncome.others.loungeCost),
        "othersLoungeIncome" -> JsNumber(airlineIncome.others.loungeIncome),
        "othersAssetExpense" -> JsNumber(airlineIncome.others.assetExpense),
        "othersAssetRevenue" -> JsNumber(airlineIncome.others.assetRevenue),
        "othersServiceInvestment" -> JsNumber(airlineIncome.others.serviceInvestment),
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
        "assetTransactions" -> JsNumber(airlineCashFlow.assetTransactions),
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
        "incomeLevel" -> Computation.getIncomeLevel(country.income).toInt,
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
        "incomeLevel" -> Computation.getIncomeLevel(country.income).toInt,
        "openness" ->  country.openness,
        "mutualRelationship" -> mutualRelationship
      )
    }
  }

  implicit object ChampionedCountriesWrites extends Writes[CountryChampionInfo] {
    def writes(info : CountryChampionInfo): JsValue = {
      Json.obj(
              "country" -> Json.toJson(info.country),
              "airlineId" -> JsNumber(info.airline.id),
              "airlineName" -> JsString(info.airline.name),
              "ranking" -> JsNumber(info.ranking),
              "passengerCount" -> JsNumber(info.passengerCount)
              )
    }
  }

  implicit object AirportChampionInfoWrites extends Writes[AirportChampionInfo] {
    def writes(info : AirportChampionInfo): JsValue = {
      var result =
      Json.obj(
        "airportId" -> Json.toJson(info.loyalist.airport.id),
        "airportText" -> Json.toJson(info.loyalist.airport.displayText),
        "countryCode" -> JsString(info.loyalist.airport.countryCode),
        "airlineId" -> JsNumber(info.loyalist.airline.id),
        "airlineName" -> JsString(info.loyalist.airline.name),
        "ranking" -> JsNumber(info.ranking),
        "loyalistCount" -> JsNumber(info.loyalist.amount),
        "reputationBoost" -> JsNumber(info.reputationBoost)
      )

      info.loyalist.airline.getCountryCode().foreach { countryCode =>
        result = result + ("airlineCountryCode" -> JsString(countryCode))
      }
      result
    }
  }

  implicit object CityWrites extends Writes[City] {
    def writes(city: City): JsValue = {
      val incomeLevel = Computation.getIncomeLevel(city.income).toInt
      JsObject(List(
        "id" -> JsNumber(city.id),
        "name" -> JsString(city.name),
        "latitude" -> JsNumber(city.latitude),
        "longitude" -> JsNumber(city.longitude),
        "countryCode" -> JsString(city.countryCode),
        "population" -> JsNumber(city.population),
        "incomeLevel" -> JsNumber(incomeLevel)))
    }
  }

  implicit object AirportAssetBoostWrites extends Writes[AirportBoost] {
    def writes(entry : AirportBoost): JsValue = {
      Json.obj(
        "boostType" -> entry.boostType.toString,
        "label" -> AirportBoostType.getLabel(entry.boostType),
        "value" -> entry.value,
      )
    }
  }

  object SimpleAirportWrites extends Writes[Airport] {
    override def writes(airport : Airport) : JsValue = {
      Json.obj(
        "id" -> airport.id,
        "name" -> airport.name,
        "iata" -> airport.iata,
        "icao" -> airport.icao,
        "city" -> airport.city,
        "size" -> airport.size,
        "latitude" -> airport.latitude,
        "longitude" -> airport.longitude,
        "countryCode" -> airport.countryCode,
        "population" -> airport.population,
        "incomeLevel" -> airport.incomeLevel.toInt)
    }
  }

  implicit object AirportFormat extends Format[Airport] {
    def reads(json: JsValue): JsResult[Airport] = {
      val airport = Airport.fromId((json \ "id").as[Int])
      JsSuccess(airport)
    }

    def writes(airport: Airport): JsValue = {
      //      val appealMap = airport.airlineAppeals.foldRight(Map[Airline, Int]()) {
      //        case(Tuple2(airline, appeal), foldMap) => foldMap + Tuple2(airline, appeal.loyalty)
      //      }

      var airportObject = JsObject(List(
        "id" -> JsNumber(airport.id),
        "name" -> JsString(airport.name),
        "iata" -> JsString(airport.iata),
        "icao" -> JsString(airport.icao),
        "city" -> JsString(airport.city),
        "size" -> JsNumber(airport.size),
        "runwayLength" -> JsNumber(airport.runwayLength),
        "latitude" -> JsNumber(airport.latitude),
        "longitude" -> JsNumber(airport.longitude),
        "countryCode" -> JsString(airport.countryCode),
        "population" -> JsNumber(airport.population),
        "radius" -> JsNumber(airport.airportRadius),
        "zone" -> JsString(airport.zone),
        "incomeLevel" -> JsNumber(airport.incomeLevel.toInt)))


      if (airport.isAirlineAppealsInitialized) {
        airportObject = airportObject + ("appealList" -> JsArray(airport.getAirlineAdjustedAppeals().toList.map {
          case (airlineId, appeal) => Json.obj("airlineId" -> airlineId, "airlineName" -> AirlineCache.getAirline(airlineId).fold("<unknown>")(_.name), "loyalty" -> BigDecimal(appeal.loyalty).setScale(2, RoundingMode.HALF_EVEN))
        }
        ))

        var bonusJson = Json.obj()
        airport.getAllAirlineBonuses.toList.foreach {
          case (airlineId, bonuses) => {
            var totalLoyaltyBonus = 0.0
            var loyaltyBreakdownJson = Json.arr(Json.obj("description" -> "Basic", "value" -> BigDecimal(airport.getAirlineBaseAppeal(airlineId).loyalty).setScale(2, RoundingMode.HALF_EVEN)))
            bonuses.foreach { entry =>
              val loyaltyBonus = entry.bonus.loyalty
              if (loyaltyBonus != 0) {
                totalLoyaltyBonus += loyaltyBonus
                loyaltyBreakdownJson = loyaltyBreakdownJson.append(Json.obj("description" -> BonusType.description(entry.bonusType), "value" -> BigDecimal(loyaltyBonus).setScale(2, RoundingMode.HALF_EVEN)))
              }
            }
            bonusJson = bonusJson + (airlineId.toString ->
              Json.obj("loyalty" -> BigDecimal(totalLoyaltyBonus).setScale(2, RoundingMode.HALF_EVEN),
                "loyaltyBreakdown" -> loyaltyBreakdownJson))
          }
        }
        airportObject = airportObject + ("bonusList" -> bonusJson)
      }
      if (airport.isFeaturesLoaded) {
        implicit val boostWriter = new Writes[(String, Double)] {
          override def writes(o : (String, Double)) : JsValue = {
            Json.obj("description" -> o._1, "boost" -> o._2)
          }
        }

        airportObject = airportObject + ("features" -> JsArray(airport.getFeatures().sortBy(_.featureType.id).map { airportFeature =>
          var featureJson = Json.obj("type" -> airportFeature.featureType.toString(), "strength" -> airportFeature.strength, "title" -> airportFeature.getDescription)
          airportFeature match {
            case f : InternationalHubFeature => featureJson = featureJson + ("boosts" -> Json.toJson(airport.boostFactorsByType.get(AirportBoostType.INTERNATIONAL_HUB)))
            case f : FinancialHubFeature => featureJson = featureJson + ("boosts" -> Json.toJson(airport.boostFactorsByType.get(AirportBoostType.FINANCIAL_HUB)))
            case f : VacationHubFeature => featureJson = featureJson + ("boosts" -> Json.toJson(airport.boostFactorsByType.get(AirportBoostType.VACATION_HUB)))
            case _ =>
          }
          featureJson
        }
        ))

        if (airport.boostFactorsByType.get(AirportBoostType.POPULATION).size > 0) {
          airportObject = airportObject + ("populationBoost" -> Json.toJson(airport.boostFactorsByType.get(AirportBoostType.POPULATION)))
        }
        if (airport.boostFactorsByType.get(AirportBoostType.INCOME).size > 0) {
          airportObject = airportObject + ("incomeLevelBoost" -> Json.toJson(airport.boostFactorsByType.get(AirportBoostType.INCOME)))
        }
      }
      if (airport.isGateway()) {
        airportObject = airportObject + ("isGateway" -> JsBoolean(true))
      }


      if (airport.getRunways().length > 0) {
        airportObject = airportObject + ("runways" -> JsArray(airport.getRunways().sortBy(_.length).reverse.map { runway =>
          Json.obj("type" -> runway.runwayType.toString(), "length" -> runway.length, "code" -> runway.code)
        }
        ))
      }

      val citiesServed = airport.citiesServed.sortBy(_._1.population).takeRight(20)


      airportObject = airportObject + ("citiesServed" -> Json.toJson(citiesServed.map(_._1).toList))

      airportObject
    }
  }

  implicit object AirportWithChampionWrites extends Writes[AirportWithChampion] {
    override def writes(o : AirportWithChampion) : JsValue = {
      var result = Json.toJson(o.airport).asInstanceOf[JsObject]
      o.champion.foreach { champion =>
        result = result + ("championAirlineId" -> JsNumber(champion.id)) + ("championAirlineName" -> JsString(champion.name))
        o.contested.foreach { contestingAirline =>
          result = result + ("contested" -> JsString(contestingAirline.name))
        }
      }
      result
    }
  }

  implicit object EventRewardWrite extends Writes[EventReward] {
    def writes(reward : EventReward): JsValue = JsObject(List(
      "description" -> JsString(reward.description),
      "optionId" -> JsNumber(reward.rewardOption.id),
      "categoryId" -> JsNumber(reward.rewardCategory.id)
    ))
  }

  implicit object NegotiationRequirementWrites extends Writes[NegotiationRequirement] {
    def writes(requirement : NegotiationRequirement) : JsValue = {
      Json.obj(
        "description" -> JsString(requirement.description),
        "value" -> JsNumber(requirement.value))
    }
  }

  case class NegotiationInfoWrites(link : Link) extends Writes[NegotiationInfo] {
    def writes(info : NegotiationInfo): JsValue = {
      var fromAirportRequirementsJson = Json.arr()
      info.fromAirportRequirements.foreach { requirement =>
        fromAirportRequirementsJson = fromAirportRequirementsJson.append(Json.toJson(requirement))
      }

      var toAirportRequirementsJson = Json.arr()
      info.toAirportRequirements.foreach { requirement =>
        toAirportRequirementsJson = toAirportRequirementsJson.append(Json.toJson(requirement))
      }

      var fromAirportDiscountsJson = Json.arr()
      //val requirementWrites = NegotiationRequirementWrites(link)
      info.fromAirportDiscounts.foreach { discount =>
        fromAirportDiscountsJson = fromAirportDiscountsJson.append(Json.toJson(discount)(NegotiationDiscountWrites(link.from)))
      }
      var toAirportDiscountsJson = Json.arr()
      //val requirementWrites = NegotiationRequirementWrites(link)
      info.toAirportDiscounts.foreach { discount =>
        toAirportDiscountsJson = toAirportDiscountsJson.append(Json.toJson(discount)(NegotiationDiscountWrites(link.to)))
      }

//      var oddsJson = Json.obj().asInstanceOf[JsObject]
//      info.odds.foreach {
//        case (delegateCount, odds) => oddsJson = oddsJson + (delegateCount -> JsNumber(odds))
//      }


      var result = Json.obj(
        "odds" -> info.odds.map {
          case (delegateCount : Int, odds : Double) => (delegateCount.toString(), odds)
        },
        "toAirportRequirements" -> toAirportRequirementsJson,
        "fromAirportRequirements" -> fromAirportRequirementsJson,
        "fromAirportDiscounts" -> fromAirportDiscountsJson,
        "toAirportDiscounts" -> toAirportDiscountsJson,
        "finalFromDiscountValue" -> info.finalFromDiscountValue,
        "finalToDiscountValue" -> info.finalToDiscountValue,
        "finalRequirementValue" -> info.finalRequirementValue)

      info.remarks.foreach { remarks =>
        result = result + ("remarks" -> JsString(remarks))
      }

      result
    }
  }

  implicit object DelegateInfoWrites extends Writes[DelegateInfo] {
    def writes(delegateInfo : DelegateInfo): JsValue = {
      val currentCycle = CycleSource.loadCycle()
      var boosts = Json.arr()
      delegateInfo.boosts.foreach { boost =>
        boosts = boosts.append(Json.obj("amount" -> boost.amount, "remainingCycles" -> (boost.expiryCycle.get - currentCycle)))
      }

      var result = Json.obj("availableCount" -> delegateInfo.availableCount, "permanentAvailableCount" -> delegateInfo.permanentAvailableCount, "boosts" -> boosts).asInstanceOf[JsObject]
      var busyDelegatesJson = Json.arr()
      val delegateWrites = new BusyDelegateWrites(currentCycle)
      delegateInfo.busyDelegates.foreach { busyDelegate =>
        busyDelegatesJson = busyDelegatesJson.append(Json.toJson(busyDelegate)(delegateWrites))
      }
      result = result + ("busyDelegates", busyDelegatesJson)
      result
    }
  }

  class BusyDelegateWrites(currentCycle : Int) extends Writes[BusyDelegate] {
    override def writes(busyDelegate: BusyDelegate): JsValue = {
      var busyDelegateJson = Json.obj("id" -> busyDelegate.id, "taskDescription" -> busyDelegate.assignedTask.description, "completed" -> busyDelegate.taskCompleted).asInstanceOf[JsObject]
      busyDelegate.availableCycle.map(_ - currentCycle).foreach {
        coolDown : Int => busyDelegateJson = busyDelegateJson + ("coolDown" -> JsNumber(coolDown))
      }

      busyDelegateJson
    }
  }

  case class NegotiationDiscountWrites(airport : Airport) extends Writes[NegotiationDiscount] {
    def writes(discount : NegotiationDiscount): JsValue = {
      Json.obj(
        "description" -> JsString(discount.description(airport)),
        "value" -> JsNumber(discount.value))
    }
  }



  implicit object NegotiationResultWrites extends Writes[NegotiationResult] {
    def writes(result : NegotiationResult): JsValue = {
      val negotiationSessions = result.getNegotiationSessions()
      Json.obj(
        "odds" -> JsNumber(1 - result.threshold),
        "passingScore" -> JsNumber(negotiationSessions.passingScore),
        "sessions" -> Json.toJson(negotiationSessions.sessionScores),
        "isSuccessful" -> JsBoolean(result.isSuccessful),
        "isGreatSuccess" -> JsBoolean(result.isGreatSuccess))
    }
  }

  implicit object AirlineCountryRelationshipWrites extends Writes[AirlineCountryRelationship] {
    def writes(relationship : AirlineCountryRelationship) : JsValue = {
      val baseJson = Json.obj(
        "total" -> relationship.relationship,
      )
      var factorsJson = Json.arr()
      relationship.factors.foreach {
        case (factor, value) =>
          factorsJson = factorsJson.append(Json.obj("description" -> factor.getDescription, "value" -> value))
      }
      baseJson + ("factors", factorsJson)
    }
  }

  implicit object CountryAirlineTitleWrites extends Writes[CountryAirlineTitle] {
    def writes(title : CountryAirlineTitle) : JsValue = {
      Json.obj(
        "title" -> title.title.toString,
        "description" -> title.description

      )
    }
  }

  implicit object AllianceMissionWrites extends Writes[AllianceMission] {
    def writes(allianceMission : AllianceMission) : JsValue = {
      val currentCycle = CycleSource.loadCycle()
      val statusText = allianceMission.status match {
        case AllianceMissionStatus.SELECTED => "Selected"
        case AllianceMissionStatus.CANDIDATE => "Candidate"
        case AllianceMissionStatus.IN_PROGRESS => s"In Progress ${allianceMission.endCycle - currentCycle} week(s) remaining"
        case AllianceMissionStatus.CONCLUDED => s"Concluded"
      }

      val historyCycle = Math.min(currentCycle - 1, allianceMission.endCycle)

      Json.obj(
        "id" -> allianceMission.id,
        "description" -> allianceMission.description,
        "stats" -> allianceMission.stats(historyCycle).properties,
        "tillNextPhase" -> AllianceMissionSimulation.cycleToNextPhase(allianceMission, currentCycle),
        "progress" -> allianceMission.progress(historyCycle),
        "difficulty" -> allianceMission.difficulty,
        "statusText" -> statusText,
        "status" -> allianceMission.status.toString

      )
    }
  }

  implicit object AllianceMissionRewardWrites extends Writes[AllianceMissionReward] {
    def writes(reward : AllianceMissionReward) : JsValue = {
      Json.obj(
        "id" -> reward.id,
        "description" -> reward.description,
        "isClaimed" -> reward.claimed,
        "isAvailable" -> reward.available,
      )
    }
  }

  class LoanWrites(currentCycle : Int) extends OWrites[Loan] {
    //case class Loan(airlineId : Int, borrowedAmount : Long, interest : Long, var remainingAmount : Long, creationCycle : Int, loanTerm : Int, var id : Int = 0) extends IdObject
    def writes(loan: Loan) : JsObject = JsObject(List(
      "airlineId" -> JsNumber(loan.airlineId),
      "borrowedAmount" -> JsNumber(loan.principal),
      "interest" -> JsNumber(loan.interest),
      "interestRate" -> JsNumber(loan.annualRate),
      "remainingAmount" -> JsNumber(loan.remainingPayment(currentCycle)),
      "earlyRepaymentFee" -> JsNumber(loan.earlyRepaymentFee(currentCycle)),
      "earlyRepayment" -> JsNumber(loan.earlyRepayment(currentCycle)),
      "remainingTerm" -> JsNumber(loan.remainingTerm(currentCycle)),
      "weeklyPayment" -> JsNumber(loan.weeklyPayment),
      "creationCycle" -> JsNumber(loan.creationCycle),
      "loanTerm" ->  JsNumber(loan.term),
      "id" -> JsNumber(loan.id)))
  }

  val cachedAirportsByPower = AirportSource.loadAllAirports(fullLoad = false, loadFeatures = true).filter(_.population > 0).sortBy(_.power)


  val allAirplaneModels = ModelSource.loadAllModels()
  val allCountryRelationships = CountrySource.getCountryMutualRelationships()

  object LoginStatus extends Enumeration {
    type LoginStatus = Value
    val ONLINE, ACTIVE_7_DAYS, ACTIVE_30_DAYS, INACTIVE = Value
  }
}