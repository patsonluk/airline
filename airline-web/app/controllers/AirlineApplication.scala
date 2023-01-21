package controllers

import com.patson.AirlineSimulation
import com.patson.data._
import com.patson.model.Computation.ResetAmountInfo
import com.patson.model._
import com.patson.util._
import controllers.AuthenticationObject.{Authenticated, AuthenticatedAirline}
import models.{AirportFacility, Consideration, FacilityType}
import play.api.libs.json.{Json, _}
import play.api.mvc._
import websocket.chat.ChatControllerActor

import java.awt.Color
import java.nio.file.Files
import java.util.{Calendar, Date}
import javax.inject.Inject
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.concurrent.duration.{DAYS, Duration, MILLISECONDS}
import scala.util.{Failure, Success, Try}


class AirlineApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  object OwnedAirlineWrites extends Writes[Airline] {
    def writes(airline: Airline): JsValue =  {
      var values = List(
      "id" -> JsNumber(airline.id),
      "name" -> JsString(airline.name),
      "balance" -> JsNumber(airline.airlineInfo.balance),
      "reputation" -> JsNumber(BigDecimal(airline.airlineInfo.reputation).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)),
      "serviceQuality" -> JsNumber(airline.airlineInfo.currentServiceQuality),
      "targetServiceQuality" -> JsNumber(airline.airlineInfo.targetServiceQuality),
      "maintenanceQuality" -> JsNumber(airline.airlineInfo.maintenanceQuality),
      "gradeDescription" -> JsString(airline.airlineGrade.description),
      "gradeValue" -> JsNumber(airline.airlineGrade.value),
      "airlineCode" -> JsString(airline.getAirlineCode()),
      "skipTutorial" -> JsBoolean(airline.isSkipTutorial),
      "initialized" -> JsBoolean(airline.isInitialized))
      
      airline.getCountryCode.foreach { countryCode =>
        values = values :+ ("countryCode" -> JsString(countryCode))
      }

      JsObject(values)
    }
  }

  object LoginStatus extends Enumeration {
    type LoginStatus = Value
    val ONLINE, ACTIVE_7_DAYS, ACTIVE_30_DAYS, INACTIVE = Value
  }
  
  implicit object AirlineWithUserWrites extends Writes[(Airline, User, Option[LoginStatus.Value], Option[Alliance], List[AirlineModifier], Boolean)] {
    def writes(entry: (Airline, User, Option[LoginStatus.Value], Option[Alliance], List[AirlineModifier], Boolean)): JsValue = {
      val (airline, user, loginStatus, alliance, airlineModifiers, isCurrentUserAdmin) = entry
      var result = Json.toJson(airline).asInstanceOf[JsObject] +
        ("userLevel" -> JsNumber(user.level)) +
        ("username" -> JsString(user.userName))
      user.adminStatus.foreach { adminStatus =>
        result = result + ("adminStatus" -> JsString(adminStatus.toString))
      }

      if (isCurrentUserAdmin) {
        result = result + ("userStatus" -> JsString(user.status.toString)) + ("userId" -> JsNumber(user.id))
        result = result + ("userModifiers" -> Json.toJson(user.modifiers.map(_.toString)))
      }

      loginStatus.foreach { status => //if there's a login status
        result = result + ("loginStatus" -> JsNumber(status.id))
      }

      result = result + ("slogan" -> JsString(airline.slogan.getOrElse("")))
        
      alliance.foreach { alliance =>
        result = result + ("allianceName" -> JsString(alliance.name)) +  ("allianceId" -> JsNumber(alliance.id))
      }

      if (!airlineModifiers.isEmpty) {
        result = result + ("airlineModifiers" -> Json.toJson(airlineModifiers.map(_.modifierType.toString)))
      }
        //("lastActiveTime" -> JsString(user.lastActive.getTime.toString)) //maybe last active time is still too sensitive
      result
    }
  }
  
   object ResetAmountInfoWrites extends Writes[ResetAmountInfo] {
    def writes(info: ResetAmountInfo): JsValue =  {
      JsObject(List(
      "airplanes" -> JsNumber(info.airplanes),
      "bases" -> JsNumber(info.bases),
      "assets" -> JsNumber(info.assets),
      "loans" -> JsNumber(info.loans),
      "oilContracts" -> JsNumber(info.oilContracts),
      "existingBalance" -> JsNumber(info.existingBalance),
      "overall" -> JsNumber(info.overall)))
    }
  }

  implicit object ReputationBreakdownsWrites extends Writes[ReputationBreakdowns] {
    def writes(entry: ReputationBreakdowns): JsValue = {
      var breakdownsJson = Json.arr()
      entry.breakdowns.foreach { breakdown =>
        breakdownsJson = breakdownsJson.append(Json.obj("description" -> breakdown.reputationType.label, "value" -> breakdown.value))
      }
      Json.obj("total" -> entry.total, "breakdowns" -> breakdownsJson)
    }
  }

  def getAllAirlines(loginStatus : Boolean, hideInactive : Boolean) = Authenticated { implicit request =>
     //val airlines = AirlineSource.loadAllAirlines(fullLoad = true)
    val airlinesByUser = scala.collection.mutable.Map[Airline, User]()
    val sevenDaysAgo = Calendar.getInstance();
    sevenDaysAgo.add(Calendar.DATE, -7)
    val thirtyDaysAgo = Calendar.getInstance()
    thirtyDaysAgo.add(Calendar.DATE, -30)

    UserSource.loadUsersByCriteria(List.empty).foreach { user =>
      if (!hideInactive || user.lastActiveTime.after(thirtyDaysAgo)) {
        user.getAccessibleAirlines().foreach { airline =>
          airlinesByUser.put(airline, user)
        }
      }
    }

    val userStatusMap: Map[User, LoginStatus.Value] =
      if (loginStatus) {

        val activeUsers = ChatControllerActor.getActiveUsers().map(_.id)
        airlinesByUser.values.map { user =>
          val status =
            if (activeUsers.contains(user.id)) {
              LoginStatus.ONLINE
            } else {
              if (user.lastActiveTime.after(sevenDaysAgo)) {
                LoginStatus.ACTIVE_7_DAYS
              } else if (user.lastActiveTime.after(thirtyDaysAgo)) {
                LoginStatus.ACTIVE_30_DAYS
              } else {
                LoginStatus.INACTIVE
              }
            }
          (user, status)
        }.toMap
      } else {
        Map.empty[User, LoginStatus.Value]
      }

    val airlineModifiers : Map[Int, List[AirlineModifier]]  =
      if (request.user.isAdmin) { //some modifiers are only visible to admins
        AirlineSource.loadAirlineModifiers().groupBy(_._1).view.mapValues(_.toList.map(_._2)).toMap
      } else {
        AirlineSource.loadAirlineModifiers().filter(!_._2.isHidden).groupBy(_._1).view.mapValues(_.toList.map(_._2)).toMap
      }


    val alliances = AllianceSource.loadAllAlliances().map(alliance => (alliance.id, alliance)).toMap
    Ok(Json.toJson(airlinesByUser.toList.map {
      case(airline, user) => (airline, user, userStatusMap.get(user), airline.getAllianceId.map(alliances(_)), airlineModifiers.getOrElse(airline.id, List.empty), request.user.isAdmin)
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
     val reputationBreakdowns = AirlineSource.loadReputationBreakdowns(airlineId)
     airlineJson = airlineJson + ("baseAirports"-> Json.toJson(bases)) + ("reputationBreakdowns" -> Json.toJson(reputationBreakdowns)) +
       ("delegatesInfo" -> Json.toJson(airline.getDelegateInfo()))
     AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMembership =>
       airlineJson = airlineJson +
         ("allianceId" -> JsNumber(allianceMembership.allianceId)) +
         ("allianceRole" -> JsString(allianceMembership.role.toString)) +
         ("isAllianceAdmin" -> JsBoolean(AllianceRole.isAdmin(allianceMembership.role)))
     }
     
     if (extendedInfo) {
       val links = LinkSource.loadFlightLinksByAirlineId(airlineId)
       val airportsServed = links.flatMap {
         link => List(link.from, link.to)
       }.toSet.size
       
       val destinations = if (airportsServed > 0) airportsServed - 1 else 0 //minus home base
       
       val currentCycle = CycleSource.loadCycle()
       val airplanes = AirplaneSource.loadAirplanesByOwner(airlineId).filter(_.isReady)
       
       val fleetSize = airplanes.length
       val fleetAge = if (fleetSize > 0) airplanes.map(currentCycle - _.constructedCycle).sum / fleetSize else 0
       val assets = Bank.getAssets(airlineId)

       airlineJson =
         airlineJson +
         ("linkCount" -> JsNumber(links.length)) +
           ("destinations"-> JsNumber(destinations)) +
           ("fleetSize"-> JsNumber(fleetSize)) +
           ("fleetAge"-> JsNumber(fleetAge)) +
           ("assets"-> JsNumber(assets))

       val cooldown = getRenameCooldown(airline)
       if (getRenameCooldown(airline) > 0) {
         airlineJson = airlineJson + ("renameCooldown" -> JsNumber(cooldown))
       }
     }

     Ok(airlineJson)
  }
  def getBases(airlineId : Int) = Authenticated { implicit request =>
    Ok(Json.toJson(AirlineSource.loadAirlineBasesByAirline(airlineId)))
  }
  def getBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj().asInstanceOf[JsObject]
    AirportCache.getAirport(airportId, true) match {
      case Some(airport) => {
        val existingBase = airport.getAirlineBase(airlineId)

        existingBase.foreach { base =>
            result = result + ("base" -> Json.toJson(base))

            val linksFromThisBase = LinkSource.loadFlightLinksByFromAirportAndAirlineId(base.airport.id, airlineId, LinkSource.SIMPLE_LOAD)
            val currentStaffRequired = linksFromThisBase.map(_.getCurrentOfficeStaffRequired).sum
            val futureStaffRequired =  linksFromThisBase.map(_.getFutureOfficeStaffRequired).sum
            val staffCapacity = base.getOfficeStaffCapacity
            result = result + ("officeCapacity"->
              Json.obj("staffCapacity" -> staffCapacity,
                "currentStaffRequired" -> currentStaffRequired,
                "futureStaffRequired" -> futureStaffRequired))
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
          val deleteRejection = getDeleteBaseRejection(request.user, existingBase.get)
          deleteRejection.foreach { rejection =>
            result = result + ("deleteRejection" -> JsString(rejection))
          }

        }

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
        AllianceSource.loadAllianceMemberByAirline(airline).filter(_.role != AllianceRole.APPLICANT).foreach { allianceMember =>
          AllianceCache.getAlliance(allianceMember.allianceId, true).foreach { alliance =>
            val allAllianceBaseAirports : List[(Airport, Airline)] = alliance.members.flatMap { allianceMember =>
              allianceMember.airline.getBases().filter(!_.headquarter).map { base =>
                (base.airport, allianceMember.airline)
              }
            }

            allAllianceBaseAirports.find(_._1.id == targetBase.airport.id).foreach {
              case (overlappingAirport, allianceAirline) => return Some("Alliance member " + allianceAirline.name + " already has a base in this airport")
            }
          }
        }
        //it should first has link to it
        if (LinkSource.loadFlightLinksByAirlineId(airline.id).find(link => link.from.id == airport.id || link.to.id == airport.id).isEmpty) {
          return Some("No active flight route operated by your airline flying to this city yet")
        }

        //check title
        targetBase.allowAirline(airline) match {
          case Left(requiredTitle) =>
            if (airport.isGateway()) {
              return Some(s"Can only build hub in this gateway airport when your airline attain ${Title.description(requiredTitle)} with this country")
            } else {
              return Some(s"Can only build hub in this non-gateway airport when your airline attain ${Title.description(requiredTitle)} with this country")
            }
          case Right(_) => //ok
        }
      }
    }

//
//        val existingBaseCount = airline.getBases().length
//        val allowedBaseCount = airline.airlineGrade.getBaseLimit
//        if (existingBaseCount >= allowedBaseCount) {
//          return Some("Only allow up to " + allowedBaseCount + " bases for your current airline grade " + airline.airlineGrade.description)
//        }
    //check delegates requirement
    val delegatesAssignedToThisCountry = airline.getDelegateInfo().busyDelegates.filter { delegate =>
      val targetCountryCode = targetBase.countryCode
      delegate.assignedTask.getTaskType == DelegateTaskType.COUNTRY && delegate.assignedTask.asInstanceOf[CountryDelegateTask].country.countryCode == targetCountryCode
    }

    val upgradeDelegatesRequired = if (targetBase.scale == 1) targetBase.delegatesRequired else targetBase.delegatesRequired - targetBase.copy(scale = targetBase.scale - 1).delegatesRequired

    val requiredDelegates = airline.getBases().filter(_.countryCode == targetBase.countryCode).map(_.delegatesRequired).sum + upgradeDelegatesRequired
    if (delegatesAssignedToThisCountry.length < requiredDelegates) {
      return Some(s"Cannot build/upgrade this base. Require $requiredDelegates delegate(s) assigned to ${CountryCache.getCountry(targetBase.countryCode).get.name} but only ${delegatesAssignedToThisCountry.length} assigned")
    }


    return None
  }

   def getLoungeConsideration(airline : Airline, inputFacility : AirportFacility) : Consideration[Lounge] = {
     val airport = inputFacility.airport

     var (cost, newLounge, levelChange) : (Long, Lounge, Int) = AirlineSource.loadLoungeByAirlineAndAirport(inputFacility.airline.id, inputFacility.airport.id) match {
      case Some(lounge) =>
        val newLounge = lounge.copy(level = inputFacility.level)
        val cost = newLounge.getValue - lounge.getValue
        (cost, newLounge, inputFacility.level - lounge.level)
      case None =>
        val newLounge = Lounge(airline, airline.getAllianceId, airport, name = inputFacility.name, level = 1, LoungeStatus.ACTIVE, CycleSource.loadCycle())
        val cost = newLounge.getValue
        (cost, newLounge, inputFacility.level)
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
           return Consideration(0, newLounge, Some("Require base at scale " + Lounge.getBaseScaleRequirement(newLounge.level) + " to build level " + newLounge.level + " Lounge "))
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
                                      .view.mapValues(_.map(_._2).flatten) //this gives Map[Airline, List[LinkStatistics]]
                                      .mapValues(_.map(_.passengers.toLong).sum).toMap

    val sortedPassengersOnThisAirport : List[(Airline, Long)] = passengersOnThisAirport.toList.sortBy(_._2)
    val eligibleAirlines : List[(Airline, Long)] = sortedPassengersOnThisAirport.takeRight(newLounge.getActiveRankingThreshold)

    if (levelChange > 0) { //upgrade - has to consider ranking
      eligibleAirlines.find(_._1.id == airline.id) match {
        case Some((airline, passengers)) => //ok
          return Consideration(cost, newLounge)
        case None => //does not make the cut
          var currentRank = 1
          sortedPassengersOnThisAirport.reverse.foreach {
            case (rankedAirline, passengers) =>
              if (rankedAirline.id == airline.id) {
                return Consideration(cost, newLounge, Some("Your passenger volume of " + passengers + " is ranked as number " + currentRank + ". Has to be top " + newLounge.getActiveRankingThreshold + " to build lounge in this airport"))
              }
              currentRank += 1
          }
          return Consideration(cost, newLounge, Some("Your have no passengers here. Has to be top " + newLounge.getActiveRankingThreshold + " to build lounge in this airport"))
      }
    } else {
      return Consideration(cost, newLounge)
    }
  }


   def getDowngradeRejection(base : AirlineBase) : Option[String] = {
     if (base.scale == 1) { //cannot downgrade any further
       return Some("Cannot downgrade this base any further")
     }
//     val airport = AirportCache.getAirport(base.airport.id, true).get
//     val assignedSlots = airport.getAirlineSlotAssignment(base.airline.id)
//     val preferredSlots = airport.getPreferredSlotAssignment(base.airline, scaleAdjustment = 0)
//     val preferredSlotsAfterDowngrade = airport.getPreferredSlotAssignment(base.airline, scaleAdjustment = -1)
//     if (assignedSlots > preferredSlotsAfterDowngrade) {
//       return Some("This base can only be downgraded if there are " + (preferredSlots - preferredSlotsAfterDowngrade)  + " free slots")
//     }
//
     val totalOfficeStaffRequired = LinkSource.loadFlightLinksByFromAirportAndAirlineId(base.airport.id, base.airline.id).map(_.getFutureOfficeStaffRequired).sum
     val capacityAfterDowngrade = base.copy(scale = base.scale - 1).getOfficeStaffCapacity
     if (capacityAfterDowngrade < totalOfficeStaffRequired) {
       return Some(s"Cannot downgrade this base, as the office staff capacity will become $capacityAfterDowngrade which is lower than the required $totalOfficeStaffRequired to maintain current flights from this base")
     }

     AirlineSource.loadLoungeByAirlineAndAirport(base.airline.id, base.airport.id).foreach { lounge =>
       if (Lounge.getBaseScaleRequirement(lounge.level) >= base.scale) { //cannot downgrade further unless Lounge is downgraded first
         return Some("This base can only be downgraded if lounge is first downgraded")
       }
     }

     AirportAssetSource.loadAirportAssetsByAirport(base.airport.id).filter(asset => asset.airline.isDefined && asset.airline.get.id == base.airline.id).foreach { ownedAsset =>
       if (ownedAsset.blueprint.assetType.baseRequirement > base.scale - 1) {
         return Some(s"This cannot be downgraded as asset ${ownedAsset.name} requires base level ${ownedAsset.blueprint.assetType.baseRequirement}")
       }
     }

     return None
  }

  def getDeleteBaseRejection(airline : Airline, base : AirlineBase) : Option[String] = {
    if (base.headquarter) {
      return Some("Cannot remove Headquarters")
    }
    //check connectivity
    val airlineBases = airline.getBases().filterNot(_.headquarter).map { _.airport.id} //non hq bases
    val links = LinkSource.loadFlightLinksByAirlineId(airline.id)
    links.filter(_.from.id == base.airport.id).foreach { link =>
      if (airlineBases.contains(link.to.id)) {
        //then make sure there's still some link other then this pointing to the target
        if (links.filter(_.to.id == link.to.id).size <= 1) {
          return Some(s"Cannot remove this base as this flies to ${link.to.displayText}. Removing this will create a disconnected network" )
        }
      }
    }
    return None
  }


  def deleteBase(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
      case Some(base) if base.headquarter => //no deleting head quarter for now
        BadRequest("Not allowed to delete headquarter for now")
      case Some(base) =>
        getDeleteBaseRejection(request.user, base) match {
          case Some(rejection) => BadRequest(rejection)
          case None =>
            //remove all links from that base
            val linksFromThisAirport = LinkSource.loadLinksByCriteria(List(("airline", airlineId))).filter(_.from.id == airportId)
            linksFromThisAirport.foreach { link =>
              LinkSource.deleteLink(link.id)
            }

            //assign all airplanes on this base to HQ
            val headquarters = request.user.getHeadQuarter().get
            val updatingAirplanes = AirplaneSource.loadAirplanesCriteria(List(("home", airportId), ("owner", airlineId))).map { airplane =>
              airplane.home = headquarters.airport
              airplane
            }
            AirplaneSource.updateAirplanes(updatingAirplanes)

            //delete assets
            AirportAssetSource.loadAirportAssetsByAirline(airlineId).filter(_.airport.id == airportId).foreach { asset =>
              AirportAssetSource.deleteAirportAsset(asset.id)
              AirlineSource.adjustAirlineBalance(airlineId, asset.sellValue)
              AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.ASSET_TRANSACTION, asset.sellValue))
            }

            base.delete()
            Ok(Json.toJson(base))
        }
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
                 AirportCache.getAirport(inputBase.airport.id, true).fold {
                   BadRequest("airport id " +  inputBase.airport.id + " not found!")
                 } {
                   airport => //TODO for now. Maybe update to Ad event later on
                   val newBase = inputBase.copy(foundedCycle = CycleSource.loadCycle(), countryCode = airport.countryCode)
                   AirlineSource.saveAirlineBase(newBase)

                   airline.setCountryCode(newBase.countryCode)
                   AirlineSource.saveAirlineInfo(airline, updateBalance = false)
                   AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                   AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))

                     //assign airlinese that are not yet assigned
                   AirplaneSource.updateAirplanesDetails(AirplaneOwnershipCache.getOwnership(airlineId).map {
                     airplane => airplane.home = airport
                     airplane
                   })

                   Created(Json.toJson(newBase))
                 }
              }
          } else {
            AirportCache.getAirport(inputBase.airport.id, true).fold {
              BadRequest("airport id " +  inputBase.airport.id + " not found!")
            } { airport =>
                  AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
                  case Some(base) => //updating
                    if (base.scale + 1 == inputBase.scale) { //only allow one level at a time now
                      val updateBase = base.copy(scale = inputBase.scale)
                      AirlineSource.saveAirlineBase(updateBase)
                      AirlineSource.adjustAirlineBalance(request.user.id, -1 * cost)
                      AirlineSource.saveCashFlowItem(AirlineCashFlowItem(airlineId, CashFlowType.BASE_CONSTRUCTION, -1 * cost))
                      Created(Json.toJson(updateBase))
                    } else {
                      BadRequest(s"Cannot upgrade existing base $base to $inputBase")
                    }
                  case None => //ok to add
                    AirportCache.getAirport(inputBase.airport.id, true).fold {
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

                val (updatingSpecs, purgingSpecs) = base.specializations.filter(!_.free).partition(_.scaleRequirement <= updateBase.scale) //remove spec that no longer able to support
                AirportSource.updateAirportBaseSpecializations(airportId, airlineId, updatingSpecs)
                purgingSpecs.foreach(_.unapply(request.user, base.airport))

                Ok("Base downgraded")
            }
          case None =>
            NotFound("Cannot downgrade base in airport id " + airportId + " . Base not found")
      }
  }

  def getFacilities(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user


    AirportCache.getAirport(airportId) match {
      case Some(airport) =>
        var loungeJson : JsObject = AirlineSource.loadLoungeByAirlineAndAirport(airlineId, airportId) match {
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
            loungeJson + ("profit" -> JsNumber(profit))
          case None =>
            //Ok(Json.obj())
            Json.toJson(Lounge(airline = airline, allianceId = airline.getAllianceId, airport = airport, name = "", level = 0, status = LoungeStatus.INACTIVE, foundedCycle = 0)).asInstanceOf[JsObject]
        }


        Ok(Json.obj("lounge" -> loungeJson))
      case None => NotFound
    }


  }

  def getFacilityConsideration(airlineId : Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline : Airline = request.user
    val inputFacility = request.body.asInstanceOf[AnyContentAsJson].json.as[AirportFacility]
    AirportCache.getAirport(airportId) match {
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
      val airport = inputFacility.airport

      if (inputFacility.airline.id != airline.id) {
        BadRequest("not the same user!")
      } else {
        val name = inputFacility.name.trim
        val nameRejection =
          if (inputFacility.facilityType == FacilityType.LOUNGE) {
            AirportFacility.getNameRejection(name)
          } else {
            None
          }

        nameRejection match {
          case Some(nameRejection) =>
            Ok(Json.obj("nameRejection" -> nameRejection))
          case None =>
            inputFacility.facilityType match {
              case FacilityType.LOUNGE =>
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
            }
        }
      }
    } else {
      BadRequest("Cannot build facitilty")
    }
  }


  def getOfficeCapacity(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airline = request.user
    //val titlesByCountryCode: Map[String, Title.Value] = CountryAirlineTitle.getTopTitlesByAirline(airlineId).map(entry => (entry.country.countryCode, entry.title)).toMap
    var result = Json.obj()
    airline.getBases().foreach { base =>
      val linksFromThisBase = LinkSource.loadFlightLinksByFromAirportAndAirlineId(base.airport.id, airlineId, LinkSource.SIMPLE_LOAD)
      val currentStaffRequired = linksFromThisBase.map(_.getCurrentOfficeStaffRequired).sum
      val futureStaffRequired =  linksFromThisBase.map(_.getFutureOfficeStaffRequired).sum
      val staffCapacity = base.getOfficeStaffCapacity
      val currentOvertimeCompensation = base.getOvertimeCompensation(currentStaffRequired)
      val futureOvertimeCompensation = base.getOvertimeCompensation(futureStaffRequired)
      result = result + (base.airport.id.toString() -> Json.obj(
        "staffCapacity" -> staffCapacity,
        "currentStaffRequired" -> currentStaffRequired,
        "futureStaffRequired" -> futureStaffRequired,
        "currentOvertimeCompensation" -> currentOvertimeCompensation,
        "futureOvertimeCompensation" -> futureOvertimeCompensation
      ))
    }
    Ok(result)
  }


  def getAirlineFinances(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val airline = request.user
     val incomes = IncomeSource.loadIncomesByAirline(airlineId)
     val cashFlows = CashFlowSource.loadCashFlowsByAirline(airlineId)
     Ok(Json.obj("incomes" -> Json.toJson(incomes), "cashFlows" -> Json.toJson(cashFlows)))
  }

  def getFleet(airlineId : Int) = Action { request =>
    var result = Json.arr()
    AirplaneSource.loadAirplanesByOwner(airlineId).groupBy(_.model).toList.sortBy(_._1.name).foreach {
      case(model, airplanes) => result = result.append(Json.obj("name" -> model.name, "quantity" -> airplanes.size))
    }

    Ok(result)
  }

  def getServiceFundingProjection(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
     val targetQuality = request.user.getTargetServiceQuality()

     val funding = AirlineSimulation.getServiceFunding(targetQuality, LinkSource.loadFlightLinksByAirlineId(airlineId))


     Ok(JsObject(List("fundingProjection" -> JsNumber(funding))))
  }

  def getChampionedCountries(airlineId : Int) = Authenticated { implicit request =>
    val championedCountryByThisAirline  = ChampionUtil.getCountryChampionInfoByAirlineId(airlineId).sortBy(_.ranking)


    Ok(Json.toJson(championedCountryByThisAirline))
  }

  def getChampionedAirports(airlineId : Int) = Authenticated { implicit request =>
    val championedAirportsByThisAirline  = ChampionUtil.loadAirportChampionInfoByAirline(airlineId).sortBy(_.reputationBoost)(Ordering[Double].reverse)


    Ok(Json.toJson(championedAirportsByThisAirline))
  }



  def getCountryAirlineTitles(airlineId : Int) = Authenticated { implicit request =>
    val titles  = CountryAirlineTitle.getTopTitlesByAirline(airlineId)
    var nationalAirlinesJson = Json.arr()
    var partneredAirlinesJson = Json.arr()
    titles.foreach { title =>
      val valueJson = Json.obj("countryCode" -> title.country.countryCode, "bonus" -> title.loyaltyBonus)
      title.title match {
        case Title.NATIONAL_AIRLINE => nationalAirlinesJson = nationalAirlinesJson.append(valueJson)
        case Title.PARTNERED_AIRLINE => partneredAirlinesJson = partneredAirlinesJson.append(valueJson)
      }
    }

    Ok(Json.obj("nationalAirlines" -> nationalAirlinesJson, "partneredAirlines" -> partneredAirlinesJson))
  }

  def resetAirline(airlineId : Int, rebuild : Boolean) = AuthenticatedAirline(airlineId) { request =>
    if (airlineId != request.user.id) {
      Forbidden
    } else {
      getResetRejection(request.user, rebuild) match {
        case Some(rejection) => Ok(Json.obj("rejection" -> rejection))
        case None =>
          val resetBalance = if (rebuild) Computation.getResetAmount(airlineId).overall else 0 //do it here before deleting everything
          Airline.resetAirline(airlineId, newBalance = resetBalance, !rebuild) match {
            case Some(airline) =>
              Ok(Json.toJson(airline))
            case None => NotFound
          }
      }
    }
  }

  def resetAirlineConsideration(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.toJson(Computation.getResetAmount(airlineId))(ResetAmountInfoWrites).asInstanceOf[JsObject]
    getResetRejection(request.user, false).foreach { rejection =>
      result = result + ("bankruptRejection" -> JsString(rejection))
    }
    getResetRejection(request.user, true).foreach { rejection =>
      result = result + ("rebuildRejection" -> JsString(rejection))
    }
    Ok(result)
  }

  def getResetRejection(airline : Airline, rebuild: Boolean) : Option[String] = {
    val allianceMemberOption = AllianceSource.loadAllianceMemberByAirline(airline)
    if (allianceMemberOption.isDefined && allianceMemberOption.get.role == AllianceRole.LEADER) {
        return Some("Cannot reset airline as your airline is the leader of an alliance. Either promote another member as leader or disband the alliance before proceeding")
    }
    if (rebuild && airline.getReputation() < AirlineGrade.CONTINENTAL.reputationCeiling) {
        return Some(s"Cannot rebuild airline when reputation is lower than ${AirlineGrade.CONTINENTAL.reputationCeiling}")
    }
    return None
  }

 def setAirlineCode(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      var airlineCode = request.body.asInstanceOf[AnyContentAsJson].json.\("airlineCode").as[String]

      if (airlineCode.length != 2) {
        BadRequest("Should be 2 characters")
      } else if (airlineCode.filter(character => Character.isLetter(character) && character <= 'z').length != 2) {
        BadRequest("Should be all letters")
      }

      airlineCode = airlineCode.toUpperCase()

      val airline = request.user
      airline.setAirlineCode(airlineCode)
      AirlineSource.saveAirlineCode(airlineId, airlineCode)
      SearchUtil.updateAirline(AirlineCache.getAirline(airlineId).get)
      Ok(Json.toJson(airline))
    } else {
      BadRequest("Cannot Set airline Code")
    }
  }

  def setAirlineName(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      val newName = request.body.asInstanceOf[AnyContentAsJson].json.\("airlineName").as[String]
      //Validate
      getRenameRejection(request.user, newName) match {
        case Some(rejection) => BadRequest(Json.obj("reason"-> rejection))
        case None =>
          AirlineSource.updateAirlineName(airlineId, request.user.name, newName)
          SearchUtil.updateAirline(AirlineCache.getAirline(airlineId).get)
          Ok(Json.obj("name" -> newName))
      }
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

        val logoFile = data.file("logoFile").get.ref.path
        LogoUtil.validateUpload(logoFile) match {
          case Some(rejection) =>
            Ok(Json.obj("error" -> JsString(rejection))) //have to send ok as the jquery plugin's error cannot read the response
          case None =>
            val data =Files.readAllBytes(logoFile)
            LogoUtil.saveLogo(airlineId, data)

            println("Uploaded logo for airline " + request.user)
            Ok(Json.obj("success" -> JsString("File uploaded")))
        }
      }.getOrElse {
            Ok(Json.obj("error" -> JsString("Cannot find uploaded contents"))) //have to send ok as the jquery plugin's error cannot read the response
      }
    }
  }

  def uploadLivery(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    if (request.user.getReputation < 40) {
      Ok(Json.obj("error" -> JsString("Cannot upload img at current reputation"))) //have to send ok as the jquery plugin's error cannot read the response
    } else {
      request.body.asMultipartFormData.map { data =>

        val file = data.file("liveryFile").get.ref.path
        LiveryUtil.validateUpload(file) match {
          case Some(rejection) =>
            Ok(Json.obj("error" -> JsString(rejection))) //have to send ok as the jquery plugin's error cannot read the response
          case None =>
            val data =Files.readAllBytes(file)
            LiveryUtil.saveLivery(airlineId, data)

            println("Uploaded livery for airline " + request.user)
            Ok(Json.obj("success" -> JsString("File uploaded")))
        }
      }.getOrElse {
        Ok(Json.obj("error" -> JsString("Cannot find uploaded contents"))) //have to send ok as the jquery plugin's error cannot read the response
      }
    }
  }

  def deleteLivery(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    LiveryUtil.deleteLivery(airlineId)
    Ok(Json.obj("success" -> JsString("File deleted")))
  }

  def getLivery(airlineId : Int) = Action {
    Ok(LiveryUtil.getLivery(airlineId)).as("image/png").withHeaders(
      CACHE_CONTROL -> "max-age=3600"
    )
    //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
  }

  val MAX_SLOGAN_LENGTH = 200
  def saveSlogan(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    var slogan = request.body.asInstanceOf[AnyContentAsJson].json.\("slogan").as[String]

    if (slogan.length > MAX_SLOGAN_LENGTH) {
      slogan = slogan.substring(0, MAX_SLOGAN_LENGTH)
    }
    AirlineSource.saveSlogan(airlineId, slogan)
    Ok(Json.obj("slogan" -> slogan))

  }


  def getSlogan(airlineId : Int) = Action {
    val slogan : String = AirlineSource.loadSlogan(airlineId).getOrElse("")
    Ok(Json.obj("slogan" -> slogan))
    //Ok(ImageUtil.generateLogo("/logo/p0.bmp", Color.BLACK.getRGB, Color.BLUE.getRGB)).as("image/png")
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

  def getBaseSpecializationInfo(airlineId: Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val airport = AirportCache.getAirport(airportId, true).get
    val base = airport.getAirlineBase(airlineId).get
    val activeSpecializations : List[AirlineBaseSpecialization.Value] = base.specializations
    val specializationByScaleRequirement : List[(Int, List[AirlineBaseSpecialization.Value])] = AirlineBaseSpecialization.values.toList.groupBy(_.scaleRequirement).toList.sortBy(_._1)
    val cooldown =
      AirportSource.loadAirportBaseSpecializationsLastUpdate(airportId, airlineId) match {
        case Some(lastUpdate) =>
          val currentCycle = CycleSource.loadCycle()
          if (BaseSpecializationType.COOLDOWN + lastUpdate <= currentCycle) {
            0
          } else {
            BaseSpecializationType.COOLDOWN + lastUpdate - currentCycle
          }
        case None => 0
      }

    var specializationJson = Json.arr()
    implicit val specializationWrites = AirlineBaseSpecializationWrites(airport)

    specializationByScaleRequirement.foreach {
      case(scaleRequirement, specializations) =>
        var specializationsJson = Json.arr()
        specializations.foreach { specialization =>
          specializationsJson = specializationsJson.append(Json.toJsObject(specialization) +
            ("active" -> JsBoolean(activeSpecializations.contains(specialization))) +
            ("available" -> JsBoolean(base.scale >= specialization.scaleRequirement)) +
            ("free" -> JsBoolean(specialization.free))
          )
        }
        specializationJson = specializationJson.append(Json.obj("scaleRequirement" -> scaleRequirement, "specializations" -> specializationsJson))
    }

    Ok(Json.obj("specializations" -> specializationJson, "cooldown" -> cooldown, "defaultCooldown" -> BaseSpecializationType.COOLDOWN))

  }

  def setBaseSpecializations(airlineId: Int, airportId : Int) = AuthenticatedAirline(airlineId) { request =>
    val inputSpecializations = request.body.asInstanceOf[AnyContentAsJson].json.\("selectedSpecializations").as[List[String]].map(AirlineBaseSpecialization.withName(_))

    val cooldown =
      AirportSource.loadAirportBaseSpecializationsLastUpdate(airportId, airlineId).map { lastUpdate =>
        val currentCycle = CycleSource.loadCycle()
        BaseSpecializationType.COOLDOWN + lastUpdate - currentCycle
      }.getOrElse(0)

    if (cooldown > 0) {
      BadRequest(s"cooldown $cooldown")
    } else {
      //validate
      AirlineSource.loadAirlineBaseByAirlineAndAirport(airlineId, airportId) match {
        case Some(base) =>
          val specializationByScale = mutable.HashMap[Int, AirlineBaseSpecialization.Value]() //use a map by scale, to avoid selecting multiple spec per scale
          inputSpecializations.foreach { specialization =>
            specializationByScale.put(specialization.scaleRequirement, specialization)

          }
          val selectedSpecializations = specializationByScale.values.toList.filter(!_.free)
          val existingSpecializations = base.specializations.filter(!_.free)
          val newSpecializations = selectedSpecializations.filter(!existingSpecializations.contains(_))
          val removedSpecializations = existingSpecializations.filter(!selectedSpecializations.contains(_))

          if (!(newSpecializations.isEmpty && removedSpecializations.isEmpty)) {
            AirportSource.updateAirportBaseSpecializations(airportId, airlineId, selectedSpecializations)
            val airport = AirportCache.getAirport(airportId, true).get
            removedSpecializations.foreach(_.unapply(request.user, airport))
            newSpecializations.foreach(_.apply(request.user, airport))
          }

        case None => NotFound("Base $airlineId / $airportId not found")
      }

      Ok(Json.obj())
    }
  }

  def getAirlineFormerNames(airlineId : Int) = Action {
    Ok(Json.toJson(AirlineCache.getAirline(airlineId).get.previousNames))
  }

  val RENAME_COOLDOWN = Duration(30, DAYS)
  def getRenameRejection(airline : Airline, newName : String) : Option[String] = {
    UserSource.loadUserByAirlineId(airline.id) match {
      case Some(user) =>
        if (user.level <= 0) {
          return Some(s"User is not allowed to rename airline, username ${user.userName}")
        }
        val cooldown = getRenameCooldown(airline)
        if (cooldown > 0) {
          return Some(s"Cannot rename yet. Cooldown: ${ cooldown }") //TODO
        }

        //check if the name is valid then
        if (AirlineSource.loadAirlinesByCriteria(List(("name", newName))).isEmpty) {
          return None
        } else {
          return Some(s"Airline name $newName is already taken")
        }

      case None => return Some(s"No user found for airline ${airline.name}")
    }

  }

  def getRenameCooldown(airline : Airline): Long = {
    AirlineSource.loadPreviousNameHistory(airline.id).sortBy(_.updateTimestamp).lastOption match {
      case Some(NameHistory(name, updateTimestamp)) =>
        val sinceLastUpdate = new Date().getTime - updateTimestamp.getTime
        Math.max(0, RENAME_COOLDOWN.toUnit(MILLISECONDS).toLong - sinceLastUpdate)
      case None =>
        0
    }
  }
}
