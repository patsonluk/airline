package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json.Json
import com.patson.model.Airport
import com.patson.model.Airline
import com.patson.data.AirportSource
import com.patson.Util
import com.patson.model.Link
import com.patson.data.LinkSource
import com.patson.data.AirlineSource
import com.patson.data.CycleSource
import com.patson.model.AirlineBase
import com.patson.model.AirlineBase
import controllers.AuthenticationObject.Authenticated
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.IncomeSource
import com.patson.model.Period
import com.patson.model.AirlineIncome
import com.patson.model.LinksIncome
import com.patson.model.TransactionsIncome
import com.patson.model.OthersIncome
import com.patson.AirlineSimulation
import play.api.mvc.Security.AuthenticatedRequest
import com.patson.data.CountrySource
import com.patson.model.Country
import com.patson.model.CountryMarketShare
import com.patson.model.Computation
import com.patson.data.BankSource
import models.EntrepreneurProfile
import com.patson.data.AirplaneSource
import com.patson.model.Bank
import java.awt.Color
import com.patson.util.LogoGenerator
import java.nio.file.Files
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.patson.model.FlightCategory
import com.patson.data.AllianceSource
import com.patson.model.AllianceMember
import com.patson.model.AirlineCashFlowItem
import com.patson.model.CashFlowType
import com.patson.data.CashFlowSource
import com.patson.data.CashFlowSource
import com.patson.model.AllianceRole
import models.AirportFacility
import models.FacilityType
import com.patson.model.LoungeStatus
import com.patson.model.Lounge
import models.Consideration
import com.patson.data.LinkStatisticsSource
import com.patson.model.LinkStatistics
import com.patson.data.LoungeHistorySource
import scala.collection.mutable.ListBuffer
import com.patson.model.LoungeConsumptionDetails
import models.FacilityType.FacilityType
import com.patson.data.UserSource
import com.patson.model.User
import com.patson.model.Alliance
import com.patson.util.ChampionUtil
import com.patson.util.ChampionInfo
import com.patson.data.OilSource


class AirlineApplication extends Controller {
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue =  {
      var values = List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance),
      "reputation" -> JsNumber(BigDecimal(airline.airlineInfo.reputation).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)),
      "serviceQuality" -> JsNumber(airline.airlineInfo.serviceQuality),
      "serviceFunding" -> JsNumber(airline.airlineInfo.serviceFunding),
      "maintenanceQuality" -> JsNumber(airline.airlineInfo.maintenanceQuality),
      "gradeDescription" -> JsString(airline.airlineGrade.description),
      "gradeValue" -> JsNumber(airline.airlineGrade.value),
      "airlineCode" -> JsString(airline.getAirlineCode()))
      
      airline.getCountryCode.foreach { countryCode =>
        values = values :+ ("countryCode" -> JsString(countryCode))
      }
      
      JsObject(values)
    }
  }
  
  
  implicit object AirlineWithUserWrites extends Writes[(Airline, User, Option[Alliance])] {
    def writes(entry: (Airline, User, Option[Alliance])): JsValue = {
      val (airline, user, alliance) = entry
      var result = Json.toJson(airline).asInstanceOf[JsObject] + 
        ("userLevel" -> JsNumber(user.level)) +
        ("username" -> JsString(user.userName))
        
      alliance.foreach { alliance =>
        result = result + ("allianceName" -> JsString(alliance.name))
      }
        //("lastActiveTime" -> JsString(user.lastActive.getTime.toString)) //maybe last active time is still too sensitive
      result
    }
  }
  
  def getAllAirlines() = Authenticated { implicit request =>
     //val airlines = AirlineSource.loadAllAirlines(fullLoad = true)
    val airlinesByUser = scala.collection.mutable.Map[Airline, User]() 
    UserSource.loadUsersByCriteria(List.empty).foreach { user =>
      user.getAccessibleAirlines().foreach { airline =>
        airlinesByUser.put(airline, user)  
      }
    }
    
    val alliances = AllianceSource.loadAllAlliances().map(alliance => (alliance.id, alliance)).toMap
    Ok(Json.toJson(airlinesByUser.toList.map {
      case(airline, user) => (airline, user, airline.getAllianceId.map(alliances(_)))
    })).withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "http://localhost:9000",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
  
  def getAirline(airlineId : Int, extendedInfo : Boolean) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     var airlineJson = Json.toJson(airline)(OwnedAirlineWrites).asInstanceOf[JsObject]
     AirlineSource.loadAirlineHeadquarter(airlineId).foreach { headquarter => 
       airlineJson = airlineJson + ("headquarterAirport"-> Json.toJson(headquarter))
     }
     val bases = AirlineSource.loadAirlineBasesByAirline(airlineId)
     airlineJson = airlineJson + ("baseAirports"-> Json.toJson(bases))
     
     if (extendedInfo) {
       val links = LinkSource.loadLinksByAirlineId(airlineId)
       val airportsServed = links.flatMap {
         link => List(link.from, link.to)
       }.toSet.size
       
       val linkFlightCategories = links.map {
         link => Computation.getFlightCategory(link.from, link.to)
       }
       
       val airlineGrade = airline.airlineGrade
       airlineJson = airlineJson + 
       ("domesticLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.DOMESTIC))) +
       ("regionalLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.REGIONAL))) +
       ("intercontinentalLinkCount" -> JsNumber(linkFlightCategories.count( _ == FlightCategory.INTERCONTINENTAL))) +
       ("domesticLinkMax" -> JsNumber(airline.getLinkLimit(FlightCategory.DOMESTIC))) +
       ("regionalLinkMax" -> JsNumber(airline.getLinkLimit(FlightCategory.REGIONAL))) +
       ("intercontinentalLinkMax" -> JsNumber(airline.getLinkLimit(FlightCategory.INTERCONTINENTAL)))  
              
       
       
       val destinations = if (airportsServed > 0) airportsServed - 1 else 0 //minus home base
       
       val currentCycle = CycleSource.loadCycle()
       val airplanes = AirplaneSource.loadAirplanesByOwner(airlineId).filter(_.isReady(currentCycle))
       
       val fleetSize = airplanes.length
       val fleetAge = if (fleetSize > 0) airplanes.map(currentCycle - _.constructedCycle).sum / fleetSize else 0
       val assets = Bank.getAssets(airlineId)
       
       airlineJson = airlineJson + ("destinations"-> JsNumber(destinations)) + ("fleetSize"-> JsNumber(fleetSize)) + ("fleetAge"-> JsNumber(fleetAge)) + ("assets"-> JsNumber(assets))
     }
     
     Ok(airlineJson)
  }
  def getBases(airlineId : Int) = Authenticated { implicit request =>
    Ok(Json.toJson(AirlineSource.loadAirlineBasesByAirline(airlineId)))
  }
  def getBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj().asInstanceOf[JsObject]
    AirportSource.loadAirportById(airportId) match {
      case Some(airport) => {
        val existingBase = AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId)
        if (existingBase.isDefined) {
          result = result + ("base" -> Json.toJson(existingBase)) 
        }
       
        val targetBase = existingBase match {
          case Some(base) => base.copy(scale = base.scale + 1)
          case None => AirlineBase(airline = request.user, airport = airport, countryCode = airport.countryCode, scale = 1, foundedCycle = CycleSource.loadCycle(), headquarter = request.user.getHeadQuarter.isEmpty)
        }
        
        result = result + ("targetBase" -> Json.toJson(targetBase))
        val baseRejection = getBaseRejection(request.user, targetBase)
        baseRejection.foreach { rejection => 
          result = result + ("rejection" -> JsString(rejection))
        }
        
        if (existingBase.isDefined) {
          val downgradeRejection = getDowngradeRejection(existingBase.get)
          downgradeRejection.foreach{ rejection =>
            result = result + ("downgradeRejection" -> JsString(rejection))
          }
        }
        
        
        val baseLinkLimit = Country.getLimitByCountryCode(airport.countryCode)
        result = result ++ Json.obj("linkLimitDomestic" -> baseLinkLimit.domestic, "linkLimitRegional" -> baseLinkLimit.regional)
        
        Ok(result)
      }
      case None => NotFound
    }
  }
  
  def getBaseRejection(airline : Airline, targetBase : AirlineBase) : Option[String] = {
    val airport = targetBase.airport
    val cost = targetBase.getValue
    if (cost > airline.getBalance) {
      return Some("Not enough cash to build/upgrade the base")
    }
    
    if (targetBase.scale == 1) { //building something new
      if (airline.getHeadQuarter().isDefined) { //building non-HQ
        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          AllianceSource.loadAllianceById(allianceMember.allianceId, true).foreach { alliance =>
            val allAllianceBaseAirports : List[(Airport, Airline)] = alliance.members.flatMap { allianceMember =>
              allianceMember.airline.getBases().filter( !_.headquarter).map { base =>
                (base.airport, allianceMember.airline)
              }
            }
            
            allAllianceBaseAirports.find(_._1.id == targetBase.airport.id).foreach { 
              case (overlappingAirport, allianceAirline) => return Some("Alliance member " + allianceAirline.name + " already has a base in this airport")
            }
          }
        }
        //it should first has link to it
        if (LinkSource.loadLinksByAirlineId(airline.id).find( link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
          return Some("No active flight route operated by your airline flying to this city yet")
        }
        
        if (airport.countryCode != airline.getCountryCode().get) {
          val mutalRelationshipToAirlineCountry = CountrySource.getCountryMutualRelationship(airline.getCountryCode().get, airport.countryCode)
          if (CountrySource.loadCountryByCode(airport.countryCode).get.openness + mutalRelationshipToAirlineCountry < Country.OPEN_DOMESTIC_MARKET_MIN_OPENNESS) {
            return Some("This country does not allow airline base from your country")
          }
        }
        
        val existingBaseCount = airline.getBases().length
        val allowedBaseCount = airline.airlineGrade.getBaseLimit
        if (existingBaseCount >= allowedBaseCount) {
          return Some("Only allow up to " + allowedBaseCount + " bases for your current airline grade " + airline.airlineGrade.description)
        } 
      }
    }
        
    return None
  }
  
   def getLoungeConsideration(airline : Airline, inputFacility : AirportFacility) : Consideration[Lounge] = {
     val airport = inputFacility.airport
     
     var (cost, newLounge) : (Long, Lounge) = AirlineSource.loadLoungeByAirlineAndAirport(inputFacility.airline.id, inputFacility.airport.id) match {
      case Some(lounge) =>
        val newLounge = lounge.copy(level = inputFacility.level)
        val cost = newLounge.getValue - lounge.getValue
        (cost, newLounge)
      case None =>
        val newLounge = Lounge(airline, airline.getAllianceId, airport, name = inputFacility.name, level = 1, LoungeStatus.ACTIVE, CycleSource.loadCycle())
        val cost = newLounge.getValue
        (cost, newLounge)
     }
     
     if (newLounge.level < 0) {
       return Consideration(0, newLounge.copy(level = 0), Some("Cannot downgrade further"))
     } else if (newLounge.level > Lounge.MAX_LEVEL) {
       return Consideration(0, newLounge.copy(level = Lounge.MAX_LEVEL), Some("Cannot upgrade further"))
     }
     
     //check base requirement
     AirlineSource.loadAirlineBaseByAirlineAndAirport(airline.id, airport.id) match {
       case Some(base) => 
         if (base.scale < Lounge.getBaseScaleRequirement(newLounge.level)) {
           return Consideration(0, newLounge, Some("Require base at scale at " + Lounge.getBaseScaleRequirement(newLounge.level) + " to build level " + newLounge.level + " Lounge "))
         }
       case None => return Consideration(0, newLounge, Some("Cannot build Lounge without a base in this airport"))
     }
     
    if (cost < 0) { //no refund
      cost = 0
    }
     
    if (cost > 0 && cost > airline.getBalance) {
      return Consideration(cost, newLounge, Some("Not enough cash to build/upgrade the lounge"))
    }
    
    //check whether there is a base
    if (airline.getBases().find( _.airport.id ==  airport.id).isEmpty) {
      return Consideration(cost, newLounge, Some("Cannot build lounge without a base"))
    }
    
    //check whether it fulfills ranking requirement
    val linkStatisticsFromThisAirport : Map[Airline, List[LinkStatistics]] = LinkStatisticsSource.loadLinkStatisticsByFromAirport(airport.id).groupBy(_.key.airline)
    val linkStatisticsToThisAirport : Map[Airline, List[LinkStatistics]] = LinkStatisticsSource.loadLinkStatisticsByToAirport(airport.id).groupBy(_.key.airline)
    val passengersOnThisAirport : Map[Airline, Long] = (linkStatisticsFromThisAirport.toList ++ linkStatisticsToThisAirport.toList).groupBy(_._1) //this gives Map[Airline, List[(Airline, List[LinkStatistics])]]
                                      .mapValues(_.map(_._2).flatten) //this gives Map[Airline, List[LinkStatistics]]
                                      .mapValues(_.map(_.passengers).sum)
                                      
    val sortedPassengersOnThisAirport : List[(Airline, Long)] = passengersOnThisAirport.toList.sortBy(_._2)
    val eligibleAirlines : List[(Airline, Long)] = sortedPassengersOnThisAirport.takeRight(newLounge.getActiveRankingThreshold)
    
    eligibleAirlines.find(_._1.id == airline.id) match { //ok
      case Some((airline, passengers)) =>
        return Consideration(cost, newLounge)
      case None => //does not make the cut
        var currentRank = 1
        sortedPassengersOnThisAirport.reverse.foreach {
          case(rankedAirline, passengers) => 
            if (rankedAirline.id == airline.id) {
              return Consideration(cost, newLounge, Some("Your passenger volume of " + passengers + " is ranked as number " + currentRank + ". Has to be top " + newLounge.getActiveRankingThreshold + " to build lounge in this airport"))
            }
            currentRank += 1
        }
        return Consideration(cost, newLounge, Some("Your have no passengers here. Has to be top " + newLounge.getActiveRankingThreshold + " to build lounge in this airport"))
    }
  }
  
   def getDowngradeRejection(base : AirlineBase) : Option[String] = {
     if (base.scale == 1) { //cannot downgrade any further
       return Some("Cannot downgrade this base any further")
     }
     val airport = AirportSource.loadAirportById(base.airport.id, true).get
     val assignedSlots = airport.getAirlineSlotAssignment(base.airline.id)
     val preferredSlots = airport.getPreferredSlotAssignment(base.airline, scaleAdjustment = 0)
     val preferredSlotsAfterDowngrade = airport.getPreferredSlotAssignment(base.airline, scaleAdjustment = -1)
     if (assignedSlots > preferredSlotsAfterDowngrade) {
       return Some("This base can only be downgraded if there are " + (preferredSlots - preferredSlotsAfterDowngrade)  + " free slots")
     } 
     
     AirlineSource.loadLoungeByAirlineAndAirport(base.airline.id, base.airport.id).foreach { lounge =>
       if (Lounge.getBaseScaleRequirement(lounge.level) >= base.scale) { //cannot downgrade further unless Lounge is downgraded first
         return Some("This base can only be downgraded if lounge is first downgraded")
       }
     }
     
     
     return None
  }
  
  def deleteBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) if base.headquarter => //no deleting head quarter for now
        BadRequest("Not allowed to delete headquarter for now")
      case Some(base) =>
        //remove all links from that base
        val linksFromThisAirport = LinkSource.loadLinksByAirlineId(airlineId).filter(_.from.id == airportId)
        linksFromThisAirport.foreach { link =>
          LinkSource.deleteLink(link.id)
        }
        
        AirlineSource.loadLoungeByAirlineAndAirport(airlineId, airportId).foreach { lounge =>
          AirlineSource.deleteLounge(lounge)
        }
        
        AirlineSource.deleteAirlineBase(base)
        Ok(Json.toJson(base))
      case None => //
        NotFound 
    }
  }
  def putBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val inputBase = request.body.asInstanceOf[AnyContentAsJson].json.as[AirlineBase]
      //todo validate the user is the same
      val airline = request.user
      
      if (inputBase.airline.id != airline.id) {
        BadRequest("not the same user!")
      } else {
        val baseRejection = getBaseRejection(airline, inputBase)
        val cost = inputBase.getValue
        
        if (baseRejection.isDefined) {
          BadRequest("base request rejected: " + baseRejection.get)
        } else {
          if (inputBase.headquarter) {
             AirlineSource.loadAirlineHeadquarter(airlineId) match {
               case Some(headquarter) =>
               if (headquarter.airport.id != airportId) {
                 BadRequest("Not allowed to change headquarter for now")
               } else {
                 val updateBase = headquarter.copy(scale = inputBase.scale)
                 AirlineSource.saveAirlineBase(updateBase)
                 AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                 AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))
                 Created(Json.toJson(updateBase))
               }
               case None => //ok to add then
                 AirportSource.loadAirportById(inputBase.airport.id, true).fold {
                   BadRequest("airport id " +  inputBase.airport.id + " not found!")
                 } { airport =>//TODO for now. Maybe update to Ad event later on
                   val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
                   AirlineSource.saveAirlineBase(newBase)
                   if (airport.getAirlineAwareness(airlineId) < 20) { //update to 10 for hq
                     airport.setAirlineAwareness(airlineId, 20)
                     AirportSource.updateAirlineAppeal(List(airport))
                   }
                   
                   airline.setCountryCode(newBase.countryCode)
                   AirlineSource.saveAirlineInfo(airline, updateBalance = false)
                   AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                   AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))
                   Created(Json.toJson(newBase))
                 }
              }
          } else {
            AirportSource.loadAirportById(inputBase.airport.id, true).fold {
              BadRequest("airport id " +  inputBase.airport.id + " not found!")
            } { airport =>
                  AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match { 
                  case Some(base) => //updating
                    val updateBase = base.copy(scale = inputBase.scale)
                    AirlineSource.saveAirlineBase(updateBase)
                    AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                    AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))
                    Created(Json.toJson(updateBase))
                  case None => //ok to add
                    AirportSource.loadAirportById(inputBase.airport.id, true).fold {
                         BadRequest("airport id " +  inputBase.airport.id + " not found!")
                    } { airport =>
                      val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
                      AirlineSource.saveAirlineBase(newBase)
                      AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                      AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))
                      Created(Json.toJson(newBase))
                    }
                }
            }
          }
        }
      }
    } else {
      BadRequest("Cannot insert base")
    }
  }
  
  def downgradeBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
      AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match { 
          case Some(base) => //updating
            getDowngradeRejection(base) match {
              case Some(rejection) =>
                BadRequest("cannot downgrade this base: " + rejection)
              case None =>
                val updateBase = base.copy(scale = base.scale - 1)
                AirlineSource.saveAirlineBase(updateBase)
                Ok("Base downgraded")
            }
          case None =>
            NotFound("Cannot downgrade base in airport id " + airportId + " . Base not found")
      }
  }    
  
  def getFacilities(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    AirportSource.loadAirportById(airportId) match {
      case Some(airport) =>
        AirlineSource.loadLoungeByAirlineAndAirport(airlineId, airportId) match {
          case Some(lounge) =>
          var loungeJson = Json.toJson(lounge).asInstanceOf[JsObject]
            
            var profit = lounge.getUpkeep * -1
            LoungeHistorySource.loadLoungeConsumptionsByAirportId(airport.id).find(_.lounge.airline.id == airline.id).foreach { stats =>
              val visitors = stats.selfVisitors + stats.allianceVisitors
              val cost = visitors * Lounge.PER_VISITOR_COST
              val income = visitors * Lounge.PER_VISITOR_CHARGE
              profit = profit + income - cost
              loungeJson = loungeJson + ("stats" -> Json.toJson(stats))
              loungeJson = loungeJson + ("cost" -> JsNumber(cost)) + ("income" -> JsNumber(income))
            }
            loungeJson = loungeJson + ("profit" -> JsNumber(profit))
            
            Ok(Json.obj("lounge" -> loungeJson))
          case None =>
            //Ok(Json.obj())
            Ok(Json.obj("lounge" -> Json.toJson(Lounge(airline = airline, allianceId = airline.getAllianceId, airport = airport, name = "", level = 0, status = LoungeStatus.INACTIVE, foundedCycle = 0))))
        }
      case None => NotFound
    }
  }
  
  def getFacilityConsideration(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    val inputFacility = request.body.asInstanceOf[AnyContentAsJson].json.as[AirportFacility]
    AirportSource.loadAirportById(airportId) match {
      case Some(airport) =>
        inputFacility.facilityType match {
          case FacilityType.LOUNGE =>
            var result = Json.obj()
            val upgradeConsideration = getLoungeConsideration(airline, inputFacility.copy(level = inputFacility.level + 1))
            
            var upgradeJson = Json.obj("cost" -> JsNumber(upgradeConsideration.cost), "upkeep" -> JsNumber(upgradeConsideration.newFacility.getUpkeep))
            if (upgradeConsideration.isRejected) {
               upgradeJson += ("rejection" -> JsString(upgradeConsideration.rejectionReason)) 
            }
            result = result + ("upgrade" -> upgradeJson)
            
            val downgradeConsideration = getLoungeConsideration(airline, inputFacility.copy(level = inputFacility.level - 1))
            if (downgradeConsideration.isRejected) {
              result = result + ("downgrade" -> Json.obj("rejection" -> JsString(downgradeConsideration.rejectionReason))) 
            } else {
              result = result + ("downgrade" -> Json.obj())
            }
            
            Ok(result)
            
          case _ => BadRequest("unknown facility type : " + inputFacility.facilityType)
            //Ok(Json.obj("lounge" -> Json.toJson(Lounge(airline = airline, allianceId = airline.getAllianceId, airport = airport, level = 0, status = LoungeStatus.INACTIVE, foundedCycle = 0))))
        }
      case None => NotFound
    }
  }
  
  
  def putFacility(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val inputFacility = request.body.asInstanceOf[AnyContentAsJson].json.as[AirportFacility]
      //todo validate the user is the same
      val airline = request.user
      
      if (inputFacility.airline.id != airline.id) {
        BadRequest("not the same user!")
      } else {
        val name = inputFacility.name.trim
        AirportFacility.getNameRejection(name) match {
          case Some(nameRejection) =>
            Ok(Json.obj("nameRejection" -> nameRejection))
          case None =>
            if (inputFacility.facilityType == FacilityType.LOUNGE) {
              val consideration = getLoungeConsideration(airline, inputFacility)
              if (consideration.isRejected) {
                BadRequest(consideration.rejectionReason)
              } else {
                val lounge = consideration.newFacility
                if (lounge.level > 0) {
                  AirlineSource.saveLounge(lounge)
                } else{
                  AirlineSource.deleteLounge(lounge)
                }
                
                if (consideration.cost > 0) {
                  AirlineSource.adjustAirlineBalance(request.user.id, -1 * consideration.cost)
                  AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.FACILITY_CONSTRUCTION, -1 * consideration.cost))
                }
                Ok(Json.toJson(consideration.newFacility))
              }
              
            } else {
              BadRequest("Unrecognized facitility type " + inputFacility.facilityType)
            }
        } 
        
      }
    } else {
      BadRequest("Cannot build facitilty")
    }
  }
  
  
  def getAirlineFinances(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     val incomes = IncomeSource.loadIncomesByAirline(airlineId)
     val cashFlows = CashFlowSource.loadCashFlowsByAirline(airlineId)
     Ok(Json.obj("incomes" -> Json.toJson(incomes), "cashFlows" -> Json.toJson(cashFlows)))
  }
  
  def getServicePrediction(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     val targetQuality = AirlineSimulation.getTargetQuality(airline.getServiceFunding(), LinkSource.loadLinksByAirlineId(airlineId, LinkSource.FULL_LOAD))
     
     val delta = targetQuality - airline.getServiceQuality()
     val prediction =  
       if (delta >= 20) {
         "Increase rapidly"
       } else if (delta >= 10) {
         "Increase steadily"
       } else if (delta >= 5) {
         "Increase slightly"
       } else if (delta >= -5) {
         "Steady"
       } else if (delta >= -10) {
         "Decrease slightly"
       } else if (delta >= -20) {
         "Decrease steadily"
       } else {
         "Decrease rapidly"
       }
     
     
     Ok(JsObject(List("prediction" -> JsString(prediction))))
  }
  
  def getChampionedCountries(airlineId : Int) = Authenticated { implicit request =>
    val championedCountryByThisAirline  = ChampionUtil.getChampionInfoByAirlineId(airlineId).sortBy(_.ranking)
    
    
    Ok(Json.toJson(championedCountryByThisAirline))
  }
  
  def resetAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (airlineId != request.user.id) {
      Forbidden
    } else {
      //remove all links
      LinkSource.loadLinksByAirlineId(airlineId).foreach { link =>
        LinkSource.deleteLink(link.id)
      }
      //remove all airplanes
      AirplaneSource.deleteAirplanesByCriteria(List(("owner", airlineId)));
      //remove all bases
      AirlineSource.deleteAirlineBaseByCriteria(List(("airline", airlineId)))
      //remove all loans
      BankSource.loadLoansByAirline(airlineId).foreach { loan =>
        BankSource.deleteLoan(loan.id)
      }
      //remove all factilities
      AirlineSource.deleteLoungeByCriteria(List(("airline", airlineId)))
      
      //remove all oil contract
      OilSource.deleteOilContractByCriteria(List(("airline", airlineId)))
      
      AllianceSource.loadAllianceMemberByAirline(request.user).foreach { allianceMember =>
        AllianceSource.deleteAllianceMember(airlineId)
        if (allianceMember.role == AllianceRole.LEADER) { //remove the alliance
           AllianceSource.deleteAlliance(allianceMember.allianceId)
        }
      }  
      
      //reset balance
      val airline : Airline = request.user
      airline.setBalance(EntrepreneurProfile.INITIAL_BALANCE)
      
      //unset country code
      airline.removeCountryCode()
      //unset service investment
      airline.setServiceFunding(0)
      airline.setServiceQuality(0)
      
      AirlineSource.saveAirlineInfo(airline)
      Ok(Json.toJson(airline))
    }
  }
  
  def sellallAirline(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (airlineId != request.user.id) {
      Forbidden
    } else {
      //set balance
      val airline : Airline = request.user
      airline.setBalance(Bank.getAssets(airlineId) + airline.getBalance())
      
      //remove all links
      LinkSource.loadLinksByAirlineId(airlineId).foreach { link =>
        LinkSource.deleteLink(link.id)
      }
      //remove all airplanes
      AirplaneSource.deleteAirplanesByCriteria(List(("owner", airlineId)));
      //remove all bases
      AirlineSource.deleteAirlineBaseByCriteria(List(("airline", airlineId)))
      //remove all loans
      BankSource.loadLoansByAirline(airlineId).foreach { loan =>
        airline.setBalance(airline.getBalance()- loan.earlyRepayment)
        BankSource.deleteLoan(loan.id)
      }
      //remove all factilities
      AirlineSource.deleteLoungeByCriteria(List(("airline", airlineId)))
      
      //remove all oil contract
      OilSource.deleteOilContractByCriteria(List(("airline", airlineId)))
      
      AllianceSource.loadAllianceMemberByAirline(request.user).foreach { allianceMember =>
        AllianceSource.deleteAllianceMember(airlineId)
        if (allianceMember.role == AllianceRole.LEADER) { //remove the alliance
           AllianceSource.deleteAlliance(allianceMember.allianceId)
        }
      }  
      
      //unset country code
      airline.removeCountryCode()
      //unset service investment
      airline.setServiceFunding(0)
      airline.setServiceQuality(0)
      
      AirlineSource.saveAirlineInfo(airline)
      Ok(Json.toJson(airline))
    }
  }
  
    
 def setAirlineCode(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      var airlineCode = request.body.asInstanceOf[AnyContentAsJson].json.\("airlineCode").as[String]
      
      if (airlineCode.length != 2) {
        BadRequest("Should be 2 characters") 
      } else if (airlineCode.filter(Character.isLetter(_)).length != 2) {
        BadRequest("Should be all letters")
      } 
      
      airlineCode = airlineCode.toUpperCase()
      
      val airline = request.user
      airline.setAirlineCode(airlineCode)
      AirlineSource.saveAirlineCode(airlineId, airlineCode)
      Ok(Json.toJson(airline))
    } else {
      BadRequest("Cannot Set airline Code")
    }
  }
 def getLogo(airlineId : Int) = Action {
   Ok(LogoUtil.getLogo(airlineId)).as("image/png").withHeaders(
    CACHE_CONTROL -> "max-age=3600"
   )
   //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
 }
 
 def setLogo(airlineId : Int, templateIndex : Int, color1 : String, color2 : String) = AuthenticatedAirline(airlineId) { request =>
   val logo = LogoGenerator.generateLogo(templateIndex, Color.decode(color1).getRGB, Color.decode(color2).getRGB)
   LogoUtil.saveLogo(airlineId, logo)
   println("Updated logo for airline " + request.user)
   Ok(Json.obj())
 }	 
 
  def uploadLogo(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.user.getReputation < 40) {
      Ok(Json.obj("error" -> JsString("Cannot upload img at current reputation"))) //have to send ok as the jquery plugin's error cannot read the response
    } else {
      request.body.asMultipartFormData.map { data =>
        import java.io.File
        val logoFile = data.file("logoFile").get.ref.file
        LogoUtil.validateUpload(logoFile) match {
          case Some(rejection) =>
            Ok(Json.obj("error" -> JsString(rejection))) //have to send ok as the jquery plugin's error cannot read the response
          case None =>
            val data =Files.readAllBytes(logoFile.toPath)
            LogoUtil.saveLogo(airlineId, data)
            
            println("Uploaded logo for airline " + request.user)
            Ok(Json.obj("success" -> JsString("File uploaded")))
        }
      }.getOrElse {
            Ok(Json.obj("error" -> JsString("Cannot find uploaded contents"))) //have to send ok as the jquery plugin's error cannot read the response
      }
    }
  }
 
 
  def setColor(airlineId : Int, color : String) = AuthenticatedAirline(airlineId) { request =>
   val decodedColor = Color.decode(color) //just for validation
   AirlineSource.saveColor(airlineId, color)
   println("Updated color for airline " + request.user)
   Ok(Json.obj())
  }
  
  def getColors() = Action {
   val colors : Map[Int, String] =  AirlineSource.getColors()
   var result = Json.obj()
      colors.foreach {
        case (airlineId, color) =>
          result = result + (airlineId.toString -> JsString(color))
      }
   
   Ok(result)
   //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
 } 
  
  def updateAirplaneRenewal(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val thresholdTry : Try[Int] = Try(request.body.asInstanceOf[AnyContentAsJson].json.\("threshold").as[Int])
      
      thresholdTry match {
        case Success(threshold) =>
          if (threshold < 0) { //disable
            AirlineSource.deleteAirplaneRenewal(airlineId)
            Ok(Json.obj())
          } else if (threshold >= 100) {
            BadRequest("Cannot set threshold to >= 100")
          } else {
            AirlineSource.saveAirplaneRenewal(airlineId, threshold)
            Ok(Json.obj("threshold" -> JsNumber(threshold)))
          }
        case Failure(_) =>
          BadRequest("Cannot Update airplane renewal")
      }
      
    } else {
      BadRequest("Cannot Update airplane renewal")
    }
  }
  
  def getAirplaneRenewal(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val threshold : Option[Int] = AirlineSource.loadAirplaneRenewal(airlineId)
     threshold match {
       case Some(threshold) => Ok(Json.obj("threshold" -> JsNumber(threshold)))
       case None => Ok(Json.obj())
     } 
     
  }
}
