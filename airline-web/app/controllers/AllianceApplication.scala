package controllers

import com.patson.data.{AirlineSource, AllianceSource, CycleSource, LinkSource}
import com.patson.model.AllianceEvent._
import com.patson.model.AllianceRole._
import com.patson.model.AllianceStatus._
import com.patson.model.{AllianceHistory, AllianceMember, _}
import com.patson.util.{AirlineCache, AirportChampionInfo, AllianceCache, AllianceRankingUtil, ChampionUtil, CountryChampionInfo}
import controllers.AuthenticationObject.AuthenticatedAirline
import javax.inject.Inject
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._

import scala.math.BigDecimal.{RoundingMode, int2bigDecimal}


class AllianceApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit object AllianceWrites extends Writes[Alliance] {
    //case class AllianceMember(alliance: Alliance, airline : Airline, role : AllianceRole.Value, joinedCycle : Int)
    def writes(alliance: Alliance): JsValue = JsObject(List(
      "id" -> JsNumber(alliance.id),
      "name" -> JsString(alliance.name),
      "status" -> JsString(alliance.status match {
        case ESTABLISHED => "Established"
        case FORMING => "Forming"
      })
      ))
  }
  
  implicit object AllianceMemberWrites extends Writes[AllianceMember] {
    //case class AllianceMember(alliance: Alliance, airline : Airline, role : AllianceRole.Value, joinedCycle : Int)
    def writes(allianceMember: AllianceMember): JsValue = JsObject(List(
      "airlineId" -> JsNumber(allianceMember.airline.id),
      "airlineName" -> JsString(allianceMember.airline.name),
      "allianceRole" -> JsString(allianceMember.role match {
        case LEADER => "Leader"
        case FOUNDING_MEMBER => "Founding member"
        case MEMBER => "Member"
        case APPLICANT => "Applicant"
      }),
      "allianceId" -> JsNumber(allianceMember.allianceId),
      "allianceName" -> JsString(AllianceCache.getAlliance(allianceMember.allianceId).get.name)))
  }
  
  
  
  implicit object HistoryWrites extends Writes[AllianceHistory] {
    //case class AllianceHistory(allianceName : String, airline : Airline, event : AllianceEvent.Value, cycle : Int, var id : Int = 0)
    def writes(history: AllianceHistory): JsValue = JsObject(List(
      "cycle" -> JsNumber(history.cycle),
      "description" -> JsString(getHistoryDescription(history))
      ))
      
    def getHistoryDescription(history : AllianceHistory) : String = {
      val eventAction = history.event match {
        case FOUND_ALLIANCE => "founded alliance"
        case APPLY_ALLIANCE => "applied for alliance" 
        case JOIN_ALLIANCE => "joined alliance"
        case REJECT_ALLIANCE => "was rejected by alliance"
        case LEAVE_ALLIANCE => "left alliance"
        case BOOT_ALLIANCE => "was removed from alliance"
        case PROMOTE_LEADER => "was promoted to alliance leader of"
      }
      history.airline.name + " " + eventAction + " " + history.allianceName
    }
  }
  
  case class FormAlliance(allianceName : String)
  val formAllianceForm : Form[FormAlliance] = Form(
    
    // Define a mapping that will handle User values
    mapping(
      "allianceName" -> text(minLength = 1, maxLength = 50).verifying(
        "Alliance name can only contain space and characters",
        allianceName => allianceName.forall(char => char.isLetter || char == ' ') && !"".equals(allianceName.trim())).verifying(
        "This Alliance name  is not available",  
        allianceName => !AllianceSource.loadAllAlliances(false).map { _.name.toLowerCase() }.contains(allianceName.toLowerCase())
      )
    )
    { //binding
      (allianceName) => FormAlliance(allianceName.trim) 
    } 
    { //unbinding
      formAlliance => Some(formAlliance.allianceName)
    }
  )
  
  
  def getAirlineAllianceDetails(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    var result = Json.obj()
    AllianceSource.loadAllianceMemberByAirline(request.user) match {
      case Some(allianceMember) => result = result ++ Json.toJson(allianceMember).asInstanceOf[JsObject] 
      case None => //do nothing 
    }
    
    val history = AllianceSource.loadAllianceHistoryByAirline(airlineId)
    if (!history.isEmpty) {
      result = result + ("history" -> Json.toJson(history))
    }

    Ok(result)
  }
  
  def formAlliance(airlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    formAllianceForm.bindFromRequest.fold(
      // Form has errors, redisplay it
      erroredForm => Ok(Json.obj("rejection" -> JsString(erroredForm.error("allianceName").get.message))), { formAllianceInput =>
        //make sure the current airline is not in any alliance
        AllianceSource.loadAllianceMemberByAirline(request.user) match {
          case None =>
            val allianceName = formAllianceInput.allianceName
            val currentCycle = CycleSource.loadCycle()
            val newAlliance = Alliance(name = allianceName, creationCycle = currentCycle, members = List())
            AllianceSource.saveAlliance(newAlliance)
            SearchUtil.addAlliance(newAlliance)

            val allianceMember = AllianceMember(allianceId = newAlliance.id, airline = request.user, role = LEADER, joinedCycle = currentCycle)
            AllianceSource.saveAllianceMember(allianceMember)

            val history = AllianceHistory(allianceName = newAlliance.name, airline = request.user, event = FOUND_ALLIANCE, cycle = currentCycle)
            AllianceSource.saveAllianceHistory(history)


            Ok(Json.toJson(newAlliance.copy(members = List(allianceMember))))
          case Some(currentAirlineAllianceMember) =>
            BadRequest(s"Current airline is an alliance member of $currentAirlineAllianceMember, cannot form new alliance")
        }
      }
    )
  }
  
  
  def getAlliances(airlineId : Option[Int]) = Action { request =>
    val alliances : List[Alliance] = AllianceSource.loadAllAlliances(true)
    
    var result = Json.arr()
    
    val alliancesWithRanking : Map[Int, (Int, BigDecimal)] = AllianceRankingUtil.getRankings()
    
    alliances.foreach {
      alliance =>
        val isCurrentMember = airlineId match {
          case Some(airlineId) => alliance.members.map(_.airline.id).contains(airlineId)
          case None => false
        }

        var allianceJson = Json.toJson(alliance).asInstanceOf[JsObject]
        var allianceMemberJson = Json.arr()
        alliance.members.foreach { allianceMember =>
          var thisMemberJson = Json.toJson(allianceMember).asInstanceOf[JsObject]
          if (isCurrentMember && allianceMember.role == APPLICANT) { //current airline is within this alliance, get more info about applicant
            getApplyRejection(allianceMember.airline, alliance).foreach {
              rejection => thisMemberJson = thisMemberJson + ("rejection" -> JsString(rejection))
            }
          }
          allianceMemberJson = allianceMemberJson.append(thisMemberJson)
          if (allianceMember.role == LEADER) {
            allianceJson = allianceJson.asInstanceOf[JsObject] + ("leader" -> Json.toJson(allianceMember.airline))
          }
        }
        allianceJson = allianceJson + ("members" -> allianceMemberJson)
        alliancesWithRanking.get(alliance.id).foreach {
          case((ranking, championPoints)) => {
            allianceJson = allianceJson + ("ranking" -> JsNumber(ranking))
            allianceJson = allianceJson + ("championPoints" -> JsNumber(championPoints))
            allianceJson = allianceJson + ("reputationBonus" -> JsNumber(Alliance.getReputationBonus(ranking)))
            //allianceJson = allianceJson + ("maxFrequencyBonus" -> JsNumber(Alliance.getMaxFrequencyBonus(ranking)))
          }
        }
        
        
        
        val historyEntries : List[AllianceHistory] = AllianceSource.loadAllianceHistoryByAllianceName(alliance.name)
        allianceJson = allianceJson + ("history" -> Json.toJson(historyEntries))
        result = result.append(allianceJson)
    }
    
    Ok(result)
  }
  
  object SimpleLinkWrites extends Writes[List[Link]] {
    def writes(links: List[Link]): JsValue =  {
      var result = Json.arr()
      
      links.foreach { link =>
        var linkJson = JsObject(List(
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
        "frequency" -> JsNumber(link.frequency),
        "fromLatitude" -> JsNumber(link.from.latitude),
        "fromLongitude" -> JsNumber(link.from.longitude),
        "toLatitude" -> JsNumber(link.to.latitude),
        "toLongitude" -> JsNumber(link.to.longitude),
        "capacity" -> Json.toJson(link.capacity),
        "flightType" -> JsString(link.flightType.toString()),
        "flightCode" -> JsString(LinkUtil.getFlightCode(link.airline, link.flightNumber))))
        result = result.append(linkJson) 
      }
      result
    }
  }

  object AllianceAirlinesWrites extends Writes[List[Airline]] { //a bit more info -nothing confidential tho! since this is accessible to public
    def writes(airlines: List[Airline]): JsValue =  {
      var result = Json.arr()
      airlines.foreach { airline =>
        var airlineJson = Json.toJson(airline).asInstanceOf[JsObject]
        //then add base info
        airlineJson = airlineJson + ("bases", Json.toJson(airline.getBases()))
        result = result.append(airlineJson)
      }
      result
    }
  }
  
  def getAllAllianceDetails(allianceId : Int) = Action { request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with " + allianceId + " is not found")
      case Some(alliance) => {
        val links = alliance.members.flatMap { allianceMember =>
          LinkSource.loadFlightLinksByAirlineId(allianceMember.airline.id)
        }

        Ok(Json.obj("links" -> Json.toJson(links)(SimpleLinkWrites), "members" -> Json.toJson(alliance.members.map(_.airline))(AllianceAirlinesWrites)))
      }
    }
  }

  val MAX_CHAMPION_ENTRIES = 100
  def getAllianceAirportChampions(allianceId : Int) = Action { request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with " + allianceId + " is not found")
      case Some(alliance) => {
        val allianceChampions : List[AirportChampionInfo] = alliance.members.map { allianceMember =>
          ChampionUtil.loadAirportChampionInfoByAirline(allianceMember.airline.id)
        }.flatten

        val approvedMemberAirlineIds = alliance.members.filter(_.role != APPLICANT).map(_.airline.id)
        val topEntries = allianceChampions.sortBy(_.reputationBoost).reverse.take(MAX_CHAMPION_ENTRIES)
        val (topMemberChampions, topApplicantChampions) = topEntries.partition(entry => approvedMemberAirlineIds.contains(entry.loyalist.airline.id))

        val totalReputation = allianceChampions.filter(entry => approvedMemberAirlineIds.contains(entry.loyalist.airline.id)).map(_.reputationBoost).sum
        Ok(Json.obj("members" -> Json.toJson(topMemberChampions), "applicants" -> Json.toJson(topApplicantChampions), "totalReputation" -> BigDecimal(totalReputation).setScale(2, RoundingMode.HALF_UP), "truncatedEntries" -> Math.max(0, allianceChampions.length - MAX_CHAMPION_ENTRIES)))
      }
    }
  }

  def getAllianceCountryChampions(allianceId : Int) = Action { request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with " + allianceId + " is not found")
      case Some(alliance) => {
        val allianceChampions : List[CountryChampionInfo] = alliance.members.map { allianceMember =>
          ChampionUtil.getCountryChampionInfoByAirlineId(allianceMember.airline.id)
        }.flatten.sortBy(_.ranking).sortBy(entry => entry.country.airportPopulation.toLong * entry.country.income)(Ordering[Long].reverse)
        Ok(Json.toJson(allianceChampions))
      }
    }
  }
  
  def evaluateAlliance(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with id " + allianceId + " is not found")
      case Some(alliance) =>
        var result = Json.obj() 
        alliance.members.find( _.airline.id == airlineId) match {
          case Some(_) => result = result + ("isMember" -> JsBoolean(true)) //already a member
          case None => getApplyRejection(request.user, alliance) match {
            case Some(rejection) => result = result + ("rejection" -> JsString(rejection))
            case None =>
              //nothing
          }
            
        }
        
        Ok(result)
    }
  }
  
   def applyForAlliance(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
     AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with id " + allianceId + " is not found")
      case Some(alliance) =>
        getApplyRejection(request.user, alliance) match {
            case Some(rejection) => BadRequest(rejection)
            case None => //ok
              val currentCycle = CycleSource.loadCycle
              val newMember = AllianceMember(allianceId = alliance.id, airline = request.user, role = APPLICANT, joinedCycle = currentCycle)
              AllianceSource.saveAllianceMember(newMember)
              val history = AllianceHistory(allianceName = alliance.name, airline = request.user, event = APPLY_ALLIANCE, cycle = currentCycle)
              AllianceSource.saveAllianceHistory(history)
              Ok(Json.toJson(newMember))
        }
    }
  }
   
  def removeFromAlliance(airlineId : Int, targetAirlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
       val currentCycle = CycleSource.loadCycle
       AllianceSource.loadAllianceMemberByAirline(request.user) match {
          case None => BadRequest("Current airline " + request.user + " cannot remove airline id "+ targetAirlineId + " from alliance as current airline does not belong to any alliance")
          case Some(currentAirlineAllianceMember) =>
            val alliance = AllianceCache.getAlliance(currentAirlineAllianceMember.allianceId, false).get
            if (airlineId == targetAirlineId) { //removing itself, ok!
             AllianceSource.deleteAllianceMember(targetAirlineId)
             AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = request.user, event = LEAVE_ALLIANCE, cycle = currentCycle))
             if (currentAirlineAllianceMember.role == LEADER) { //remove the alliance
               AllianceSource.deleteAlliance(currentAirlineAllianceMember.allianceId)

               SearchUtil.removeAlliance(alliance.id)
             }
             
             Ok(Json.obj("removed" -> "alliance"))
           } else { //check if current airline is leader and the target airline is within this alliance
             if (currentAirlineAllianceMember.role != LEADER) {
               BadRequest("Current airline " + request.user + " cannot remove airline id "+ targetAirlineId + " from alliance as current airline is not leader")
             } else {
               AirlineCache.getAirline(targetAirlineId) match {
                 case None => NotFound("Airline with id " + targetAirlineId + " not found")
                 case Some(targetAirline) =>
                   AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
                     case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
                     case Some(allianceMember) =>
                       if (allianceMember.allianceId != currentAirlineAllianceMember.allianceId) {
                         BadRequest("Airline " + targetAirline + " does not belong to alliance " + alliance)
                       } else { //OK ..removing
                         AllianceSource.deleteAllianceMember(targetAirlineId)
                         if (allianceMember.role == APPLICANT) {
                           AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = allianceMember.airline, event = REJECT_ALLIANCE, cycle = currentCycle))  
                         } else {
                           AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = allianceMember.airline, event = BOOT_ALLIANCE, cycle = currentCycle))
                         }

                         Ok(Json.obj("removed" -> "member"))
                       }
                    }
               }
             }
          }
    }
  }
 
  def addToAlliance(airlineId : Int, targetAirlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
       val currentCycle = CycleSource.loadCycle

       AllianceSource.loadAllianceMemberByAirline(request.user) match {
          case None => BadRequest("Current airline " + request.user + " cannot add airline id "+ targetAirlineId + " to alliance as current airline does not belong to any alliance")
          case Some(currentAirlineAllianceMember) =>
            //check if current airline is leader and the target airline has applied to this alliance
           if (currentAirlineAllianceMember.role != LEADER) {
             BadRequest("Current airline " + request.user + " cannot remove airline id "+ targetAirlineId + " from alliance as current airline is not leader")
           } else {
             val alliance = AllianceCache.getAlliance(currentAirlineAllianceMember.allianceId, false).get
             AirlineCache.getAirline(targetAirlineId, true) match {
               case None => NotFound("Airline with id " + targetAirlineId + " not found")
               case Some(targetAirline) =>
                 AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
                   case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
                   case Some(allianceMember) =>
                     if (allianceMember.allianceId != currentAirlineAllianceMember.allianceId) {
                       BadRequest("Airline " + targetAirline + " does not belong to alliance " + alliance)
                     } else if (allianceMember.role != APPLICANT) {
                       BadRequest("Airline " + targetAirline + " is not applicant of " + alliance)
                     } else {
                       getApplyRejection(targetAirline, alliance) match {
                         case Some(rejection) => BadRequest(rejection) //confirm once more as there could be other approved applicant now with conflicting base
                         case None =>
                           //OK ..adding
                           AllianceSource.saveAllianceMember(allianceMember.copy(role = MEMBER))
                           AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = allianceMember.airline, event = JOIN_ALLIANCE, cycle = currentCycle))

                           Ok(Json.toJson(allianceMember))
                       }
                     }
                  }
             }
           }
    }
  }
  
  def promoteToAllianceLeader(airlineId : Int, targetAirlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
       val currentCycle = CycleSource.loadCycle
       AllianceSource.loadAllianceMemberByAirline(request.user) match {
          case None => BadRequest("Current airline " + request.user + " cannot add airline id "+ targetAirlineId + " to alliance as current airline does not belong to any alliance")
          case Some(currentAirlineAllianceMember) =>
            //check if current airline is leader and the target airline has applied to this alliance
           if (currentAirlineAllianceMember.role != LEADER) {
             BadRequest("Current airline " + request.user + " cannot promote airline id "+ targetAirlineId + " from alliance as current airline is not leader")
           } else {
             val alliance = AllianceCache.getAlliance(currentAirlineAllianceMember.allianceId, false).get
             AirlineCache.getAirline(targetAirlineId) match {
               case None => NotFound("Airline with id " + targetAirlineId + " not found")
               case Some(targetAirline) =>
                 AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
                   case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
                   case Some(allianceMember) =>
                     if (allianceMember.allianceId != currentAirlineAllianceMember.allianceId) {
                       BadRequest("Airline " + targetAirline + " does not belong to alliance " + alliance)
                     } else { //OK ..promoting
                       AllianceSource.saveAllianceMember(allianceMember.copy(role = LEADER))
                       AllianceSource.saveAllianceMember(currentAirlineAllianceMember.copy(role = MEMBER))
                       AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = allianceMember.airline, event = PROMOTE_LEADER, cycle = currentCycle))
                       
                       Ok(Json.toJson(allianceMember))
                     }
                  }
             }
           }
    }
  }
  
  
  
  def getApplyRejection(airline : Airline, alliance : Alliance) : Option[String] = {
    val approvedMembers = alliance.members.filter(_.role != AllianceRole.APPLICANT)
    
    if (airline.getHeadQuarter.isEmpty) { 
      return Some("Airline does not have headquarters")
    }
    
    if (approvedMembers.size >= Alliance.MAX_MEMBER_COUNT) {
      return Some("Alliance has reached max member size " + Alliance.MAX_MEMBER_COUNT + " already")
    }
    
    
    val allAllianceHeadquarters = approvedMembers.flatMap(_.airline.getHeadQuarter).map(_.airport)

    val airlineHeadquarters = airline.getHeadQuarter.get.airport
    
    if (allAllianceHeadquarters.contains(airlineHeadquarters)) {
      return Some("One of the alliance members has Headquarters at " + getAirportText(airlineHeadquarters))
    }
    
    val allAllianceBases = approvedMembers.flatMap { _.airline.getBases().filter( !_.headquarter) }.map(_.airport)
    val airlineBases = airline.getBases.filter(!_.headquarter).map(_.airport) 
    val overlappingBases = allAllianceBases.filter(allianceBase => airlineBases.contains(allianceBase))
   
//     println("ALL " + allAllianceBases)
//     println("YOURS " + airlineHeadquarters)
    
    if (!overlappingBases.isEmpty) {
      var message = "Alliance members have overlapping airport bases: "
      overlappingBases.foreach { overlappingBase =>
        message += getAirportText(overlappingBase) + "; "
      }
       
      return Some(message)
    }
    
     AllianceSource.loadAllianceMemberByAirline(airline) match {
       case Some(allianceMember) =>
         if (allianceMember.allianceId != alliance.id) {
           return Some("Airline is already a member of another alliance " + AllianceCache.getAlliance(allianceMember.allianceId).get.name)
         }
       case None =>

     }
    return None
  }
  
  def getAirportText(airport : Airport) = {
    airport.city + " - " + airport.name
  }
}
