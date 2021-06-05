package controllers

import com.patson.data.{AirlineSource, UserSource}
import com.patson.model.UserStatus
import com.patson.model.UserStatus.UserStatus
import com.patson.util.{AirlineCache, AirportCache}
import controllers.AuthenticationObject.{Authenticated, AuthenticatedAirline}
import controllers.GoogleImageUtil.{AirportKey, CityKey}

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.json.{Json, _}
import websocket.BroadcastActor


class AdminApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {



  def adminAction(action : String, targetUserId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      action match {
        case "ban" =>
          changeUserStatus(UserStatus.BANNED, targetUserId)
          Ok(Json.obj("action" -> action))
        case "ban-chat" =>
          changeUserStatus(UserStatus.CHAT_BANNED, targetUserId)
          Ok(Json.obj("action" -> action))
        case "un-ban" =>
          changeUserStatus(UserStatus.ACTIVE, targetUserId)
          Ok(Json.obj("action" -> action))
        case "switch" =>
          if (request.user.isSuperAdmin) {
            Ok(Json.obj("action" -> action)).withSession("userId" -> String.valueOf(targetUserId))
          } else {
            Forbidden("Not a super admin user")
          }
        case _ =>
          println(s"unknown admin action $action")
          BadRequest(Json.obj("action" -> action))
      }

    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def invalidateCustomization(airlineId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      LiveryUtil.deleteLivery(airlineId)
      AirlineSource.saveSlogan(airlineId, "")
      Ok(Json.obj("result" -> airlineId))
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def invalidateImage(imageType : String, airportId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      AirportCache.getAirport(airportId) match {
        case Some(airport) =>
          val key : controllers.GoogleImageUtil.Key =
            if (imageType == "airport") {
              new AirportKey(airport.id, airport.name, airport.latitude, airport.longitude)
            } else {
              new CityKey(airport.id, airport.city, airport.latitude, airport.longitude)
            }
          GoogleImageUtil.invalidate(key)
          Ok(Json.obj("result" -> airport))
        case None => NotFound(s"Airport $airportId not found")
      }


    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def changeUserStatus(userStatus: UserStatus, targetUserId: Int) = {
    UserSource.loadUserById(targetUserId) match {
      case Some(user) =>
        val updatingUser = user.copy(status = userStatus)
        UserSource.updateUser(updatingUser)
        println(s"ADMIN - updated user status $userStatus on user $updatingUser")
      case None => println(s"Failed to update user status $userStatus on user id $targetUserId, the user is not found!")
    }
  }

  def sendBroadcastMessage() = Authenticated { implicit request =>
    if (request.user.isSuperAdmin) {
      val message = request.body.asInstanceOf[AnyContentAsJson].json.\("message").as[String]
      BroadcastActor.broadcastMessage(message)
      Ok(Json.obj())
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not a super admin user")
    }
  }

  def sendAirlineMessage(targetAirlineId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      AirlineCache.getAirline(targetAirlineId) match {
        case Some(airline) =>
          val message = request.body.asInstanceOf[AnyContentAsJson].json.\("message").as[String]
          BroadcastActor.sendMessage(airline, message)
          Ok(Json.obj())
        case None =>
          NotFound(s"Airline with id $targetAirlineId not found")
      }
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }

  }


}
