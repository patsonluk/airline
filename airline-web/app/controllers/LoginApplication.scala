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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.mvc.Security.AuthenticatedBuilder
import controllers.Authetication.Authenticated

class LoginApplication extends Controller {
  
  // then in a controller
  def login = Authenticated { implicit request =>
    Ok("Hello " + request.user).withHeaders("Access-Control-Allow-Origin" -> "http://localhost:9000", "Access-Control-Allow-Credentials" -> "true").withCookies(Cookie("test-cookie", "tasty"))
  }
  
  
}
