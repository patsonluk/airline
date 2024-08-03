package controllers

import com.patson.AllianceSimulation
import com.patson.AllianceMissionSimulation
import com.patson.data.{AirlineSource, AllianceMissionSource, AllianceSource, CycleSource, LinkSource, UserSource}
import com.patson.model.AllianceEvent._
import com.patson.model.AllianceRole._
import com.patson.model.AllianceStatus._
import com.patson.model.{AllianceHistory, AllianceMember, _}
import com.patson.model.alliance.{AirportRankingCount, AllianceMission, AllianceMissionReward, AllianceMissionStatus, AllianceStats, CountryRankingCount}
import com.patson.util.{AirlineCache, AirportChampionInfo, AllianceCache, AllianceRankingUtil, ChampionUtil, CountryChampionInfo, UserCache}
import controllers.AuthenticationObject.AuthenticatedAirline

import javax.inject.Inject
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._
import websocket.chat.ChatControllerActor

import java.util.Calendar
import scala.collection.{MapView, mutable}
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
        case CO_LEADER => "Co-leader"
        case MEMBER => "Member"
        case APPLICANT => "Applicant"
      }),
      "isAdmin" -> JsBoolean(AllianceRole.isAdmin(allianceMember.role)),
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
      val eventAction = AllianceUtil.getAllianceEventText(history.event)
      history.airline.name + " " + eventAction + " " + history.allianceName
    }
  }

  implicit object AllianceStatsWrites extends Writes[AllianceStats] {
    def writes(stats: AllianceStats): JsValue = {
      var result = Json.obj(
        "pax" -> stats.totalPax,
        "loyalist" -> stats.totalLoyalist,
        "loungeVisit" -> stats.totalLoungeVisit,
        "revenue" -> stats.totalRevenue,
      )
      //fold airport by scales
      var airportStatsJson = Json.arr()

      val smallAirportThreshold = 3
      val largeAirportThreshold = 8

      //For airport scale 3-, key is ranking (1 - 3) value is count for that ranking
      val (smallAirportTopRankings, smallAirportOtherRankings) = stats.airportRankingStats.filter(_.airportScale <= smallAirportThreshold).groupBy(_.ranking).partition(_._1 <= 3)
      val smallAirportScaleTopStats : List[(Int, Int)] = smallAirportTopRankings.view.mapValues(_.map(_.count).sum).toList.sortBy(_._1)
      val smallAirportScaleOtherCount : Int = smallAirportOtherRankings.view.mapValues(_.map(_.count).sum).map(_._2).sum

      smallAirportScaleTopStats.sortBy(_._1).foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${smallAirportThreshold}-", "ranking" -> "other", "count" -> smallAirportScaleOtherCount))

      //For airport scale 4-7, key is ranking (1 - 3) value is count for that ranking
      for (scale <- smallAirportThreshold + 1 to largeAirportThreshold - 1) {
        val (topRankings, otherRankings) = stats.airportRankingStats.filter(_.airportScale == scale).partition(_.ranking <= 3)
        topRankings.sortBy(_.ranking).foreach {
          case AirportRankingCount(_, ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> ranking, "count" -> count))
        }
        airportStatsJson = airportStatsJson.append(Json.obj("scale" -> scale, "ranking" -> "other", "count" -> otherRankings.map(_.count).sum))
      }

      //For airport scale 8+, key is ranking (1 - 3) value is count for that ranking
      val (largeAirportTopRankings, largeAirportOtherRankings) = stats.airportRankingStats.filter(_.airportScale >= largeAirportThreshold).groupBy(_.ranking).partition(_._1 <= 3)
      val largeAirportScaleTopStats : List[(Int, Int)] = largeAirportTopRankings.view.mapValues(_.map(_.count).sum).toList.sortBy(_._1)
      val largeAirportScaleOtherCount : Int = largeAirportOtherRankings.view.mapValues(_.map(_.count).sum).map(_._2).sum

      largeAirportScaleTopStats.sortBy(_._1).foreach {
        case (ranking, count) => airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> ranking, "count" -> count))
      }
      airportStatsJson = airportStatsJson.append(Json.obj("scale" -> s"${largeAirportThreshold}+", "ranking" -> "other", "count" -> largeAirportScaleOtherCount))

      result = result + ("airportStats" -> airportStatsJson)

      result = result + ("championedAirports" -> JsNumber(stats.airportRankingStats.filter(_.ranking == 1).map(_.count).sum))

      val thresholds = 0 :: AllianceSimulation.COUNTRY_POPULATION_THRESHOLD
      var countryStatsJson = Json.arr()
      var index = 0
      val countryRankingStats = stats.countryRankingStats.sortBy(_.populationThreshold)
      val formatter = java.text.NumberFormat.getIntegerInstance
      thresholds.foreach { threshold =>
        val populationDescription =
          if (thresholds.last != threshold) {
            s"${formatter.format(threshold)} - ${formatter.format(thresholds(index + 1))}"
          } else {
            s"${formatter.format(threshold)}+"
          }
        for (ranking <- 1 to 3) {
          val count = countryRankingStats.find(entry => entry.populationThreshold == threshold && entry.ranking == ranking).map(_.count).getOrElse(0)
          countryStatsJson = countryStatsJson.append(Json.obj("population" -> populationDescription, "ranking" -> ranking, "count" -> count))
        }
        val lowRankingCount = countryRankingStats.filter(entry => entry.populationThreshold == threshold && entry.ranking > 3).map(_.count).sum
        countryStatsJson = countryStatsJson.append(Json.obj("population" -> populationDescription, "ranking" -> "other", "count" -> lowRankingCount))
        index += 1
      }

      result = result + ("countryStats" -> countryStatsJson)
      result = result + ("championedCountries" -> JsNumber(stats.countryRankingStats.filter(_.ranking == 1).map(_.count).sum))

      result
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
      case Some(allianceMember) =>
        result = result ++ Json.toJson(allianceMember).asInstanceOf[JsObject]
        if (AllianceRole.isAccepted(allianceMember.role)) {
          AllianceCache.getAlliance(allianceMember.allianceId).foreach { alliance =>
            if (alliance.status == AllianceStatus.ESTABLISHED) {
              val cycle = CycleSource.loadCycle()
              val stats = AllianceSource.loadAllianceStatsByCycle(allianceMember.allianceId, cycle - 1)
              result = result +
                ("stats" -> Json.toJson(stats)) +
                ("current" -> AllianceMissionUtil.buildCurrentMissionJson(allianceMember)) +
                ("previous" -> AllianceMissionUtil.buildPreviousMissionJson(allianceMember))
            }
          }
        }
        result = result + ("isAdmin" -> JsBoolean(AllianceRole.isAdmin(allianceMember.role)))

      case None => //do nothing 
    }
    
    val history = AllianceSource.loadAllianceHistoryByAirline(airlineId)
    if (!history.isEmpty) {
      result = result + ("history" -> Json.toJson(history.sortBy(_.cycle)(Ordering.Int.reverse)))
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
        allianceJson = allianceJson + ("history" -> Json.toJson(historyEntries.sortBy(_.cycle)(Ordering.Int.reverse)))
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

  object AllianceAirlinesWrites extends Writes[List[(Airline, AllianceRole.Value)]] { //a bit more info -nothing confidential tho! since this is accessible to public
    def writes(entries: List[(Airline, AllianceRole.Value)]): JsValue =  {
      var result = Json.arr()
      entries.foreach { case(airline, role) =>
        var airlineJson = Json.toJson(airline).asInstanceOf[JsObject]
        //then add base info
        airlineJson = airlineJson + ("bases", Json.toJson(airline.getBases())) + ("role" -> JsString(role.toString))
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
          if (allianceMember.role != AllianceRole.APPLICANT) {
            LinkSource.loadFlightLinksByAirlineId(allianceMember.airline.id)
          } else {
            List.empty
          }
        }

        Ok(Json.obj("links" -> Json.toJson(links)(SimpleLinkWrites),
          "members" -> Json.toJson(alliance.members.map(member => (member.airline, member.role)))(AllianceAirlinesWrites)))
      }
    }
  }

  def getMemberLoginStatus(allianceId : Int) = Action { request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with " + allianceId + " is not found")
      case Some(alliance) => {
        val userByAirlineId = mutable.HashMap[Int, User]()
        alliance.members.foreach { allianceMember =>
          UserSource.loadUserByAirlineId(allianceMember.airline.id).foreach { user =>
            userByAirlineId.put(allianceMember.airline.id, user)
          }
        }

        val onlineUserIds = ChatControllerActor.getActiveUsers().map(_.id)
        val sevenDaysAgo = Calendar.getInstance();
        sevenDaysAgo.add(Calendar.DATE, -7)
        val thirtyDaysAgo = Calendar.getInstance()
        thirtyDaysAgo.add(Calendar.DATE, -30)

        val loginStatusIdByAirlineId = userByAirlineId.map {
          case (airlineId, user) =>
            val loginStatus =
              if (onlineUserIds.contains(user.id)) {
                LoginStatus.ONLINE
              } else if (user.lastActiveTime.after(sevenDaysAgo)) {
                LoginStatus.ACTIVE_7_DAYS
              } else if (user.lastActiveTime.after(thirtyDaysAgo)) {
                LoginStatus.ACTIVE_30_DAYS
              } else {
                LoginStatus.INACTIVE
              }
            (airlineId.toString, loginStatus.id)
        }.toMap

        Ok(Json.toJson(loginStatusIdByAirlineId))
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

  def getRemoveConsideration(targetMember : AllianceMember, currentMember : AllianceMember) : Either[String, String] = {
    if (targetMember.allianceId != currentMember.allianceId) {
      Left(s"Airline ${targetMember.airline} does not belong to alliance ${currentMember.allianceId}")
    } else if (targetMember.airline.id == currentMember.airline.id) { //remove self
      Right(s"Leave this alliance?")
    } else if (!AllianceRole.isAdmin(currentMember.role)) {
      Left(s"Current airline ${currentMember.airline} cannot remove airline ${targetMember.airline} from alliance as current airline is not admin")
    } else if (currentMember.role.id >= targetMember.role.id) { //higher the id, lower the rank. Can only remove people at the lower rank
      Left(s"Current airline ${currentMember.airline} of role ${currentMember.role} cannot remove airline ${targetMember.airline} of role ${targetMember.role} from alliance")
    } else {
      Right(s"Remove ${targetMember.airline.name} from the alliance?")
    }
  }

  def getPromoteConsideration(targetMember : AllianceMember, currentMember : AllianceMember) : Either[String, String] = {
    if (targetMember.allianceId != currentMember.allianceId) {
      Left(s"Airline ${targetMember.airline} does not belong to alliance ${currentMember.allianceId}")
    } else if (!AllianceRole.isAdmin(currentMember.role)) {
      Left(s"Current airline ${currentMember.airline} cannot promote airline ${targetMember.airline} from alliance as current airline is not admin")
    } else if (targetMember.role.id > AllianceRole.MEMBER.id) {
      Left(s"Cannot promote non-member airline ${targetMember.airline} of role ${targetMember.role}")
    } else if (currentMember.role.id >= targetMember.role.id) { //higher the id, lower the rank. Can only promote people at the lower rank
      Left(s"Current airline ${currentMember.airline} of role ${currentMember.role} cannot promote airline ${targetMember.airline} of role ${targetMember.role} from alliance")
    } else {
      if (AllianceRole(targetMember.role.id - 1) == AllianceRole.LEADER) {
        Right(s"Promote ${targetMember.airline.name} as the new Alliance Leader? Your airline will be demoted to Co-leader!")
      } else {
        Right(s"Promote ${targetMember.airline.name} as Alliance Co-Leader?")
      }
    }
  }

  def getDemoteConsideration(targetMember : AllianceMember, currentMember : AllianceMember) : Either[String, String] = {
    if (targetMember.allianceId != currentMember.allianceId) {
      Left(s"Airline ${targetMember.airline} does not belong to alliance ${currentMember.allianceId}")
    } else if (currentMember.airline.id == targetMember.airline.id) {
      Left(s"Airline ${targetMember.airline} cannot demote self")
    } else if (targetMember.role.id >= AllianceRole.MEMBER.id) {
      Left(s"Current airline ${currentMember.airline} of role ${currentMember.role} cannot demote airline ${targetMember.airline} of role ${targetMember.role} any further")
    } else if (!AllianceRole.isAdmin(currentMember.role)) {
      Left(s"Current airline ${currentMember.airline} cannot demote airline ${targetMember.airline} from alliance as current airline is not admin")
    } else if (currentMember.role.id >= targetMember.role.id) { //higher the id, lower the rank. Can only remove people at the lower rank
      Left(s"Current airline ${currentMember.airline} of role ${currentMember.role} cannot demote airline ${targetMember.airline} of role ${targetMember.role} in alliance")
    } else {
      Right(s"Demote ${targetMember.airline.name}?")
    }
  }

  def evaluateAlliance(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AllianceCache.getAlliance(allianceId, true) match {
      case None => NotFound("Alliance with id " + allianceId + " is not found")
      case Some(alliance) =>
        var result = Json.obj()
        alliance.members.find( _.airline.id == airlineId) match {
          case Some(currentMember) =>  //already a member
            //get member actions
            var memberActionsJson = Json.arr()
            alliance.members.foreach { targetMember =>
              var memberJson = Json.obj("airlineId" -> targetMember.airline.id)
              getRemoveConsideration(targetMember, currentMember) match {
                case Left(rejection) => memberJson = memberJson + ("removeRejection" -> JsString(rejection))
                case Right(prompt) =>  memberJson = memberJson + ("removePrompt" -> JsString(prompt))
              }
              getPromoteConsideration(targetMember, currentMember) match {
                case Left(rejection) => memberJson = memberJson + ("promoteRejection" -> JsString(rejection))
                case Right(prompt) =>  memberJson = memberJson + ("promotePrompt" -> JsString(prompt))
              }
              getDemoteConsideration(targetMember, currentMember) match {
                case Left(rejection) => memberJson = memberJson + ("demoteRejection" -> JsString(rejection))
                case Right(prompt) =>  memberJson = memberJson + ("demotePrompt" -> JsString(prompt))
              }

              if (AllianceRole.isAdmin(currentMember.role) && targetMember.role == AllianceRole.APPLICANT) {
                getApplyRejection(targetMember.airline, alliance) match {
                  case Some(rejection) => memberJson = memberJson + ("acceptRejection" -> JsString(rejection))
                  case None => memberJson = memberJson + ("acceptPrompt" -> JsString(s"Accept ${targetMember.airline.name} into the alliance?"))
                }

              }

              memberActionsJson = memberActionsJson.append(memberJson)
            }
            result = result + ("isMember" -> JsBoolean(true)) + ("memberActions" -> memberActionsJson)
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
     AllianceSource.loadAllianceMemberByAirline(request.user) match {
       case None => BadRequest("Current airline " + request.user + " cannot remove airline id " + targetAirlineId + " from alliance as current airline does not belong to any alliance")
       case Some(currentAirlineAllianceMember) =>
         val alliance = AllianceCache.getAlliance(currentAirlineAllianceMember.allianceId, false).get
         if (airlineId == targetAirlineId) { //removing itself, ok!
           alliance.removeMember(currentAirlineAllianceMember, true)

           Ok(Json.obj("removed" -> "alliance"))
         } else { //check if current airline has the right permission and the target airline is within this alliance
           AirlineCache.getAirline(targetAirlineId) match {
             case None => NotFound("Airline with id " + targetAirlineId + " not found")
             case Some(targetAirline) =>
               AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
                 case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
                 case Some(allianceMember) =>
                   getRemoveConsideration(allianceMember, currentAirlineAllianceMember) match {
                     case Left(rejection) => BadRequest(rejection)
                     case Right(_) => //OK ..removing
                       alliance.removeMember(allianceMember, false)

                       Ok(Json.obj("removed" -> "member"))
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
           if (!AllianceRole.isAdmin(currentAirlineAllianceMember.role)) {
             BadRequest("Current airline " + request.user + " cannot accept airline id "+ targetAirlineId + " from alliance as current airline is not leader")
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
                           AllianceSource.saveAllianceMember(allianceMember.copy(role = MEMBER, joinedCycle = currentCycle))
                           AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = allianceMember.airline, event = JOIN_ALLIANCE, cycle = currentCycle))

                           Ok(Json.toJson(allianceMember))
                       }
                     }
                  }
             }
           }
    }
  }

  def promoteMember(airlineId : Int, targetAirlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    val currentCycle = CycleSource.loadCycle
    AllianceSource.loadAllianceMemberByAirline(request.user) match {
      case None => BadRequest("Current airline " + request.user + " cannot promote airline id " + targetAirlineId + " to alliance as current airline does not belong to any alliance")
      case Some(currentMember) =>
        val alliance = AllianceCache.getAlliance(currentMember.allianceId, false).get
        AirlineCache.getAirline(targetAirlineId) match {
          case None => NotFound("Airline with id " + targetAirlineId + " not found")
          case Some(targetAirline) =>
            AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
              case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
              case Some(targetMember) =>
                getPromoteConsideration(targetMember, currentMember) match {
                  case Left(rejection) => BadRequest(rejection)
                  case Right(_) =>
                    //OK ..promoting
                    val newRole = AllianceRole(targetMember.role.id - 1)
                    AllianceSource.saveAllianceMember(targetMember.copy(role = newRole))

                    if (newRole == AllianceRole.LEADER && currentMember.role == AllianceRole.LEADER) { //promotion to leader, have to demote current leader then
                      AllianceSource.saveAllianceMember(currentMember.copy(role = AllianceRole(AllianceRole.LEADER.id + 1)))
                      AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = targetMember.airline, event = PROMOTE_LEADER, cycle = currentCycle))
                    } else { //otherwise assume it is always to co-leader
                      AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = targetMember.airline, event = PROMOTE_CO_LEADER, cycle = currentCycle))
                    }

                    Ok(Json.toJson(targetMember))
                }
            }

        }
    }
  }

  def demoteMember(airlineId : Int, targetAirlineId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    val currentCycle = CycleSource.loadCycle
    AllianceSource.loadAllianceMemberByAirline(request.user) match {
      case None => BadRequest("Current airline " + request.user + " cannot demote airline id " + targetAirlineId + " to alliance as current airline does not belong to any alliance")
      case Some(currentMember) =>
        val alliance = AllianceCache.getAlliance(currentMember.allianceId, false).get
        AirlineCache.getAirline(targetAirlineId) match {
          case None => NotFound("Airline with id " + targetAirlineId + " not found")
          case Some(targetAirline) =>
            AllianceSource.loadAllianceMemberByAirline(targetAirline) match {
              case None => NotFound("Airline " + targetAirline + " does not belong to any alliance!")
              case Some(targetMember) =>
                getDemoteConsideration(targetMember, currentMember) match {
                  case Left(rejection) => BadRequest(rejection)
                  case Right(_) =>
                    //OK ..demoting
                    val newRole = AllianceRole(targetMember.role.id + 1)
                    AllianceSource.saveAllianceMember(targetMember.copy(role = newRole))
                    AllianceSource.saveAllianceHistory(AllianceHistory(allianceName = alliance.name, airline = targetMember.airline, event = DEMOTE, cycle = currentCycle))

                    Ok(Json.toJson(targetMember))
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
