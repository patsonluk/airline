package controllers

import controllers.AuthenticationObject.Authenticated
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.mvc._
import play.api.libs.json.Writes
import com.patson.model.User
import com.patson.model.Airline
import play.api.libs.json._

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
    var result = Json.toJson(request.user).asInstanceOf[JsObject]
    result = result + ("annoucements" -> JsArray(getAnnoucements().map(JsString(_)))) 
    Ok(result).withHeaders("Access-Control-Allow-Credentials" -> "true").withSession("userId" -> String.valueOf(request.user.id))
  }
  
  def logout = Authenticated { implicit request =>
    Ok("logged out for " + request.user.id).withNewSession 
  }
  
  def getAnnoucements() : List[String] = {
    List("- Introducing flight delays and cancellations! Airplane with low condition will trigger delays and cancellation, they could reduce your profit and also loyalty. Old airplanes can be replaced in the airplane tab",
         "- Passengers are more sensitive to high ticket price - reduced demand on overly expensive tickets",
         "- Bigger airplane models now consume more fuels - only profitable for longer routes",
         "- Reduced profit margin for intercontinental routes for game balance",
         "- Max loan at 500,000,000 now",
         "Next up: Light/Dark theme, Departures screen, Option to restart an airline, Airline Code and Flight Number!",
         "<b>Many thanks for playing again! Please leave your feedback and suggestions in FAQ/Forum</b>")
  }
}
