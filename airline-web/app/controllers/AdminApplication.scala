package controllers

import com.patson.data.{AdminSource, AirlineSource, IpSource, UserSource, UserUuidSource}
import com.patson.model.UserStatus.UserStatus
import com.patson.model.{Airline, UserStatus, User}
import com.patson.util.{AirlineCache, AirportCache}
import controllers.AuthenticationObject.Authenticated
import controllers.GoogleImageUtil.{AirportKey, CityKey}
import play.api.libs.json.Json
import play.api.mvc._
import websocket.Broadcaster

import java.text.DateFormat
import java.util.Calendar
import javax.inject.Inject


class AdminApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def adminAction(action : String, targetUserId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      AdminSource.saveLog(action, request.user.userName, targetUserId)

      UserSource.loadUserById(targetUserId) match {
        case Some(targetUser) =>
          if (targetUser.isAdmin) {
            println(s"ADMIN - Forbidden action $action user ${targetUser.userName} as the target user is admin")
            BadRequest(s"ADMIN - Failed action $action as User ${targetUser.userName} is admin")
          } else {
            action match {
              case "ban" =>
                changeUserStatus(UserStatus.BANNED, targetUser)
                Ok(Json.obj("action" -> action))
              case "ban-chat" =>
                changeUserStatus(UserStatus.CHAT_BANNED, targetUser)
                Ok(Json.obj("action" -> action))
              case "ban-reset" =>
                changeUserStatus(UserStatus.BANNED, targetUser)

                targetUser.getAccessibleAirlines().foreach { airline =>
                  Airline.resetAirline(airline.id, newBalance = 0, true) match {
                    case Some(airline) =>
                      Ok(Json.obj("action" -> action))
                    case None => NotFound
                  }
                }

                Ok(Json.obj("action" -> action))
              case "un-ban" =>
                changeUserStatus(UserStatus.ACTIVE, targetUser)
                //unbanUserIp(targetUserId)
                Ok(Json.obj("action" -> action))
              case "switch" =>
                if (request.user.isSuperAdmin) {

                  request.session.get("userToken") match {
                    case Some(userToken) =>
                      SessionUtil.getUserId(userToken) match {
                        case Some(userId) => Ok(Json.obj("action" -> action)).withSession("userToken" -> SessionUtil.addUserId(targetUserId), "adminToken" -> userToken)
                        case None => BadRequest(s"Invalid token (admin) $userToken")
                      }
                    case None => BadRequest("no current admin token")
                  }
                } else {
                  Forbidden("Not a super admin user")
                }
              case _ =>
                println(s"unknown admin action $action")
                BadRequest(Json.obj("action" -> action))
            }
            BadRequest(s"ADMIN - Failed action $action as User $targetUserId is not found")
          }
        case None => BadRequest(s"ADMIN - Failed action $action as User $targetUserId is not found")
      }

    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

//  def banUserIp(userId : Int) = {
//    IpSource.saveBannedIps(userId)
//  }
//
//  def unbanUserIp(userId : Int) = {
//    IpSource.deleteBannedIps(userId)
//  }
  def getUserIps(userId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      val cutoff = Calendar.getInstance()
      cutoff.add(Calendar.DATE, -30)

      Ok(Json.toJson(IpSource.loadUserIps(userId).toList.sortBy(_._2.occurrence)(Ordering[Int].reverse).filter(_._2.lastUpdated.after(cutoff.getTime)).map {
        case (ip, ipDetails) => (ip, ipDetails.occurrence, ipDetails.lastUpdated)
      }))
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def getAirlinesByIp(ip : String) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      var result = Json.arr()
      IpSource.loadUsersByIp(ip).foreach {
        case (user, ipDetails) =>
          user.getAccessibleAirlines().foreach { airline =>
            result = result.append(Json.obj(
              "airlineName" -> airline.name,
              "airlineId" -> airline.id,
              "username" -> user.userName,
              "userStatus" -> user.status.toString,
              "lastUpdated" -> DateFormat.getInstance().format(ipDetails.lastUpdated),
              "occurrence" -> ipDetails.occurrence,
            ))
          }

      }

      Ok(result)
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def getUserUuids(userId : Int) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      val cutoff = Calendar.getInstance()
      cutoff.add(Calendar.DATE, -30)

      Ok(Json.toJson(UserUuidSource.loadUserUuids(userId).toList.sortBy(_._2.occurrence)(Ordering[Int].reverse).filter(_._2.lastUpdated.after(cutoff.getTime)).map {
        case (ip, ipDetails) => (ip, ipDetails.occurrence, ipDetails.lastUpdated)
      }))
    } else {
      println(s"Non admin ${request.user} tried to access admin operations!!")
      Forbidden("Not an admin user")
    }
  }

  def getAirlinesByUuid(uuid : String) = Authenticated { implicit request =>
    if (request.user.isAdmin) {
      var result = Json.arr()
      UserUuidSource.loadUsersByUuid(uuid).foreach {
        case (user, ipDetails) =>
          user.getAccessibleAirlines().foreach { airline =>
            result = result.append(Json.obj(
              "airlineName" -> airline.name,
              "airlineId" -> airline.id,
              "username" -> user.userName,
              "userStatus" -> user.status.toString,
              "lastUpdated" -> DateFormat.getInstance().format(ipDetails.lastUpdated),
              "occurrence" -> ipDetails.occurrence,
            ))
          }

      }

      Ok(result)
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

  def changeUserStatus(userStatus: UserStatus, targetUser: User) = {
    val updatingUser = targetUser.copy(status = userStatus)
    UserSource.updateUser(updatingUser)
    println(s"ADMIN - updated user status $userStatus on user ${updatingUser.userName}")
  }

  def sendBroadcastMessage() = Authenticated { implicit request =>
    if (request.user.isSuperAdmin) {
      val message = request.body.asInstanceOf[AnyContentAsJson].json.\("message").as[String]
      Broadcaster.broadcastMessage(message)
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
          Broadcaster.sendMessage(airline, message)
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
