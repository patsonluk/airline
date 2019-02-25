package controllers

import controllers.AuthenticationObject.Authenticated
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.mvc._
import play.api.libs.json.Writes
import com.patson.model.User
import com.patson.model.Airline
import play.api.libs.json._
import com.patson.data.UserSource
import com.patson.data.AllianceSource

class UserApplication extends Controller {
  implicit object UserWrites extends Writes[User] {
    def writes(user: User): JsValue = {
      var result = JsObject(List(
        "id" -> JsNumber(user.id),
        "userName" -> JsString(user.userName),
        "email" -> JsString(user.email),
        "status" -> JsString(user.status.toString()),
        "level" -> JsNumber(user.level),
        "creationTime" -> JsString(user.creationTime.getTime.toString()),
        "lastActiveTime" -> JsString(user.lastActiveTime.getTime.toString()),
        "airlineIds" -> JsArray(user.getAccessibleAirlines().map { airline => JsNumber(airline.id) })))
      
      if (user.getAccessibleAirlines().isDefinedAt(0)) {
        AllianceSource.loadAllianceMemberByAirline(user.getAccessibleAirlines()(0)).foreach { allianceMember => //if this airline belongs to an alliance
          val allianceId = allianceMember.allianceId
          AllianceSource.loadAllianceById(allianceId).foreach { alliance =>
            result = result + ("allianceId" -> JsNumber(allianceId)) + ("allianceName" -> JsString(alliance.name))  
          }
        }
      }
        
      result
    }
  }
  // then in a controller
  def login = Authenticated { implicit request =>
    UserSource.updateUserLastActive(request.user)
    Ok(Json.toJson(request.user)).withHeaders("Access-Control-Allow-Credentials" -> "true").withSession("userId" -> String.valueOf(request.user.id))
  }
  
  def logout = Authenticated { implicit request =>
    Ok("logged out for " + request.user.id).withNewSession 
  }
  
 
}
