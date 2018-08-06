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

class UserApplication extends Controller {
  implicit object UserWrites extends Writes[User] {
    def writes(user: User): JsValue = {
      JsObject(List(
        "id" -> JsNumber(user.id),
        "userName" -> JsString(user.userName),
        "email" -> JsString(user.email),
        "status" -> JsString(user.status.toString()),
        "creationTime" -> JsString(user.creationTime.getTime.toString()),
        "airlineIds" -> JsArray(user.getAccessibleAirlines().map { airline => JsNumber(airline.id) })))
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
