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
    
    alliances.foreach {
      case(alliance, allianceMembers) => 
        var allianceJson = Json.toJson(alliance)
        var allianceMemberJson = Json.arr()
        allianceMembers.foreach { allianceMember =>
          allianceMemberJson = allianceMemberJson.append(Json.toJson(allianceMember))
          if (allianceMember.role == LEADER) {
            allianceJson = allianceJson.asInstanceOf[JsObject] + ("leader" -> Json.toJson(allianceMember.airline))
          }
        }
        allianceJson = allianceJson.asInstanceOf[JsObject] + ("members" -> allianceMemberJson)
        
        result = result.append(allianceJson)
    }
    
    Ok(result)
  }
  
 
  

  
}
