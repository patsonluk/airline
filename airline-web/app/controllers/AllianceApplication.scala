package controllers

import scala.math.BigDecimal.int2bigDecimal
import com.patson.data.AirlineSource
import com.patson.data.AirplaneSource
import com.patson.data.airplane.ModelSource
import com.patson.model.airplane._
import com.patson.model._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import com.patson.data.CycleSource
import controllers.AuthenticationObject.AuthenticatedAirline
import com.patson.data.CountrySource
import com.patson.data.AirportSource
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import com.patson.data.BankSource
import com.patson.model.Loan
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import com.patson.data.AllianceSource
import com.patson.model.AllianceMember
import com.patson.model.AllianceRole._
import com.patson.model.AllianceEvent._
import com.patson.model.AllianceStatus._
import com.patson.data.AllianceSource
import com.patson.data.AllianceSource
import com.patson.model.AllianceHistory
import play.api.libs.json.JsBoolean


class AllianceApplication extends Controller {
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
      "alliance" -> Json.toJson(allianceMember.alliance)))
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
        allianceName => !AllianceSource.loadAllAlliances(false).keySet.map { _.name.toLowerCase() }.contains(allianceName.toLowerCase())
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
        val allianceName = formAllianceInput.allianceName
        val currentCycle = CycleSource.loadCycle()
        val newAlliance = Alliance(name = allianceName, status = AllianceStatus.FORMING, creationCycle = currentCycle)
        AllianceSource.saveAlliance(newAlliance)
        
        val allianceMember = AllianceMember(alliance = newAlliance, airline = request.user, role = LEADER, joinedCycle = currentCycle)
        AllianceSource.saveAllianceMember(allianceMember)
        
        val history = AllianceHistory(allianceName = newAlliance.name, airline = request.user, event = FOUND_ALLIANCE, cycle = currentCycle)
        AllianceSource.saveAllianceHistory(history)
        
        Ok(Json.toJson(newAlliance))
      }
    )
  }
  
  
  def getAlliances() = Action { request =>
    val alliances : Map[Alliance, List[AllianceMember]] = AllianceSource.loadAllAlliances(true)
    
    var result = Json.arr()
    
    val countryChampionsByAirline : Map[Int, List[(Country, Double)]] = getCountryChampions()
    
    alliances.foreach {
      case(alliance, allianceMembers) => 
        var allianceJson = Json.toJson(alliance).asInstanceOf[JsObject]
        var allianceMemberJson = Json.arr()
        var allianceChampionPoints : BigDecimal = 0.0
        allianceMembers.foreach { allianceMember =>
          allianceMemberJson = allianceMemberJson.append(Json.toJson(allianceMember))
          val memberChampiontPoints : BigDecimal = countryChampionsByAirline.get(allianceMember.airline.id) match {
            case Some(championedCountries) => {
              championedCountries.map(boostEntry => BigDecimal.valueOf(boostEntry._2)).sum
            }
            case None => 0
          }
          allianceChampionPoints = allianceChampionPoints + memberChampiontPoints 
          if (allianceMember.role == LEADER) {
            allianceJson = allianceJson.asInstanceOf[JsObject] + ("leader" -> Json.toJson(allianceMember.airline))
          }
        }
        allianceJson = allianceJson + ("members" -> allianceMemberJson)
        allianceJson = allianceJson + ("championPoints" -> JsNumber(allianceChampionPoints))
        
        val historyEntries : List[AllianceHistory] = AllianceSource.loadAllianceHistoryByAllianceName(alliance.name)
        allianceJson = allianceJson + ("history" -> Json.toJson(historyEntries))
        result = result.append(allianceJson)
    }
    
    Ok(result)
  }
  
  def evaluateAlliance(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AllianceSource.loadAllianceById(allianceId, true) match {
      case None => NotFound("Alliance with id " + allianceId + " is not found")
      case Some(allianceInfo) =>
        var result = Json.obj() 
        allianceInfo._2.find( _.airline.id == airlineId) match {
          case Some(_) => result = result + ("isMember" -> JsBoolean(true)) //already a member
          case None => getApplyRejection(request.user, allianceInfo) match {
            case Some(rejection) => result = result + ("rejection" -> JsString(rejection))
            case None =>
              //nothing
          }
            
        }
        
        Ok(result)
    }
  }
  
   def applyForAlliance(airlineId : Int, allianceId : Int) = AuthenticatedAirline(airlineId) { implicit request =>
    AllianceSource.loadAllianceById(allianceId, true) match {
      case None => NotFound("Alliance with id " + allianceId + " is not found")
      case Some(allianceInfo) =>
        getApplyRejection(request.user, allianceInfo) match {
            case Some(rejection) => BadRequest(rejection)
            case None => //ok
              val currentCycle = CycleSource.loadCycle
              val newMember = AllianceMember(alliance = allianceInfo._1, airline = request.user, role = APPLICANT, joinedCycle = currentCycle)
              AllianceSource.saveAllianceMember(newMember)
              val history = AllianceHistory(allianceName = allianceInfo._1.name, airline = request.user, event = APPLY_ALLIANCE, cycle = currentCycle)
              AllianceSource.saveAllianceHistory(history)
              Ok(Json.toJson(newMember))
        }
    }
  }
  
  def getApplyRejection(airline : Airline, allianceInfo : (Alliance, List[AllianceMember])) : Option[String] = {
    val allianceMembers = allianceInfo._2
    
    if (airline.getHeadQuarter.isEmpty) { 
      return Some("Cannot join an alliance without headquarters for your airline")
    }
    
    if (allianceMembers.size >= Alliance.MAX_MEMBER_COUNT) {
      return Some("Alliance has reached max member size " + Alliance.MAX_MEMBER_COUNT + " already")
    }
    
    
    val allAllianceHeadquarters = allianceMembers.flatMap(_.airline.getHeadQuarter).map(_.airport)
    
   
    
    val airlineHeadquarters = airline.getHeadQuarter.get.airport
    
    if (allAllianceHeadquarters.contains(airlineHeadquarters)) {
      return Some("One of the alliance members has Headquarters at " + getAirportText(airlineHeadquarters) + " which is same as your airline's headquarters")  
    }
    
    val allAllianceBases = allianceMembers.flatMap { _.airline.getBases().filter( !_.headquarter) }.map(_.airport)
    val airlineBases = airline.getBases.filter(!_.headquarter).map(_.airport) 
    val overlappingBases = allAllianceBases.filter(allianceBase => airlineBases.contains(allianceBase))
   
     println("ALL " + allAllianceBases)
     println("YOURS " + airlineHeadquarters)
    
    if (!overlappingBases.isEmpty) {
      var message = "Alliance members overlap with your airport bases: "
      overlappingBases.foreach { overlappingBase =>
        message += getAirportText(overlappingBase) + "; "
      }
       
      return Some(message)
    }
    
     AllianceSource.loadAllianceMemberByAirline(airline) match {
       case Some(allianceMember) =>
         return Some("Airline is already member of alliance " + allianceMember.alliance.name)
       case None =>
         return None
     }
  }
  
  def getAirportText(airport : Airport) = {
    airport.city + " - " + airport.name
  }
  
  /**
   * returns Map[AirlineId, List[CountryCode, ReputationBoost]]
   */
  def getCountryChampions() : Map[Int, List[(Country, Double)]] = {
    val topChampionsByCountryCode : List[(String, List[((Int, Long), Int)])]= CountrySource.loadMarketSharesByCriteria(List()).map {
      case CountryMarketShare(countryCode, airlineShares) => (countryCode, airlineShares.toList.sortBy(_._2)(Ordering.Long.reverse).take(3).zipWithIndex)
    }
    
    val championedCountryByAirline: scala.collection.mutable.Map[Int, ListBuffer[(Country, Double)]] = scala.collection.mutable.Map[Int, ListBuffer[(Country, Double)]]()  
      
    val countriesByCode = CountrySource.loadAllCountries().map(country => (country.countryCode, country)).toMap
    topChampionsByCountryCode.foreach { //(country, reputation boost)
      case (countryCode, champions) => champions.foreach {
        case ((championAirlineId, passengerCount), rankingIndex) =>
          val country = countriesByCode(countryCode)
          val ranking = rankingIndex + 1
          val reputationBoost = Computation.computeReputationBoost(country, ranking)
          val existingBoosts : ListBuffer[(Country, Double)] = championedCountryByAirline.getOrElseUpdate(championAirlineId, ListBuffer[(Country, Double)]())
          existingBoosts.append((country, reputationBoost))
      }
    }
    
    championedCountryByAirline.mapValues( _.toList).toMap
//    .filter {
//      case (countryCode, thisAirlineRankingOption) => thisAirlineRankingOption.isDefined
//    }.map {
//      case (countryCode, thisAirlineRankingOption) => {
//        val country = CountrySource.loadCountryByCode(countryCode).get
//        val ranking = thisAirlineRankingOption.get._2 + 1
//        val passengerCount = thisAirlineRankingOption.get._1._2 
//        (country, ranking, passengerCount, Computation.computeReputationBoost(country, ranking))
//      }
//    }.sortBy {
//      case (countryCode, ranking, passengerCount, reputationBoost) => ranking
//    }
  }
  
 
  

  
}
