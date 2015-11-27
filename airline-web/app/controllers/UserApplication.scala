package controllers

import controllers.AuthenticationObject.Authenticated
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.mvc._

class UserApplication extends Controller {
  
  // then in a controller
  def login = Authenticated { implicit request =>
    Ok("Hello " + request.user).withHeaders(
        "Access-Control-Allow-Origin" -> "http://localhost:9000", "Access-Control-Allow-Credentials" -> "true"
        ).withSession("userId" -> String.valueOf(request.user.id))
  }
  
  
}
