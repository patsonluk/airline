package controllers

import com.patson.data.{AdminSource, AirlineSource, CycleSource, IpSource, UserSource, UserUuidSource}
import com.patson.model.UserStatus.UserStatus
import com.patson.model.{Airline, AirlineModifier, AirlineModifierType, BannerLoyaltyAirlineModifier, User, UserModifier, UserStatus}
import com.patson.util.{AirlineCache, AirportCache}
import controllers.AuthenticationObject.Authenticated
import controllers.GoogleImageUtil.{AirportKey, CityKey}
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import websocket.Broadcaster

import java.text.DateFormat
import java.util.Calendar
import javax.inject.Inject


class AdminApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def adminMultiAction(action : String) = Authenticated { implicit request =>
    val userIds = request.body.asInstanceOf[AnyContentAsJson].json.\("userIds").get.asInstanceOf[JsArray].value.map(_.as[Int])
    doAdminActions(request, action, userIds.toList)
  }

  def doAdminActions(request : AuthenticatedRequest[AnyContent, User], action : String, userIds : List[Int]) : Result = {
    userIds.foreach { userId =>
      doAdminAction(request, action, userId) match {
        case Left(errorResult) => return errorResult
        case Right(okResult) =>
      }
    }
    return Ok(Json.obj("userIds" -> userIds))
  }


  def adminAction(action : String, targetUserId : Int) = Authenticated { implicit request =>
    doAdminAction(request, action, targetUserId) match {
      case Left(errorResult) => errorResult
      case Right(okResult) => okResult
    }
  }

  def addAirlineModifier(modifier : AirlineModifierType.Value, airlines : List[Airline]) = {
    val currentCycle = CycleSource.loadCycle()
    airlines.foreach { airline =>
      AirlineSource.saveAirlineModifier(airline.id, AirlineModifier.fromValues(modifier, currentCycle, None, Map.empty))
    }
  }

  def removeAirlineModifier(modifierType : AirlineModifierType.Value, airlines : List[Airline]) = {
    airlines.foreach { airline =>
      AirlineSource.deleteAirlineModifier(airline.id, modifierType)
    }
  }

  def doAdminAction(request : AuthenticatedRequest[AnyContent, User], action : String, targetUserId : Int) : Either[Result, Result] = {
    val adminUser = request.user
    if (adminUser.isAdmin) {
      AdminSource.saveLog(action, adminUser.userName, targetUserId)

      UserSource.loadUserById(targetUserId) match {
        case Some(targetUser) =>
          if (targetUser.isAdmin && adminUser.adminStatus.get.id <= targetUser.adminStatus.get.id) {
            println(s"ADMIN - Forbidden action $action user ${targetUser.userName} as the target user is ${targetUser.adminStatus} while current user is ${adminUser.adminStatus}")
            Left(BadRequest(s"ADMIN - Forbidden action $action user ${targetUser.userName} as the target user is ${targetUser.adminStatus} while current user is ${adminUser.adminStatus}"))
          } else {
            action match {
              case "warn" =>
                setUserModifier(UserModifier.WARNED, targetUser)
                Right(Ok(Json.obj("action" -> action)))
              case "ban" =>
                setUserModifier(UserModifier.BANNED, targetUser)
                Right(Ok(Json.obj("action" -> action)))
              case "ban-chat" =>
                setUserModifier(UserModifier.CHAT_BANNED, targetUser)
                Right(Ok(Json.obj("action" -> action)))
              case "nerf" =>
                //changeUserStatus(UserStatus.NERFED, targetUser)
                //changeAirlineStatus(AirlineStatus.NERFED,
                addAirlineModifier(AirlineModifierType.NERFED, targetUser.getAccessibleAirlines())
                Right(Ok(Json.obj("action" -> action)))
              case "ban-reset" =>
                setUserModifier(UserModifier.BANNED, targetUser)

                targetUser.getAccessibleAirlines().foreach { airline =>
                  Airline.resetAirline(airline.id, newBalance = 0, true) match {
                    case Some(airline) =>
                      Right(Ok(Json.obj("action" -> action)))
                    case None => Left(NotFound)
                  }
                }

                Right(Ok(Json.obj("action" -> action)))
              case "restore" =>
                clearUserModifiers(targetUser)
                removeAirlineModifier(AirlineModifierType.NERFED, targetUser.getAccessibleAirlines())
                //unbanUserIp(targetUserId)
                Right(Ok(Json.obj("action" -> action)))
              case "set-user-level" =>
                if (!adminUser.isSuperAdmin) {
                  Left(BadRequest(s"ADMIN - Forbidden action $action user ${targetUser.userName} as the current user is ${adminUser.adminStatus}"))
                }

                //add the new one
                val inputJson = request.body.asJson.get.asInstanceOf[JsObject]
                val level = inputJson("level").as[Int]

                UserSource.updateUser(targetUser.copy(level = level))
                Right(Ok(Json.obj("action" -> action)))
              case "set-banner-winner" =>
                if (!adminUser.isSuperAdmin) {
                  Left(BadRequest(s"ADMIN - Forbidden action $action user ${targetUser.userName} as the current user is ${adminUser.adminStatus}"))
                }

                //add the new one
                val inputJson = request.body.asJson.get.asInstanceOf[JsObject]
                val airlineId = inputJson("airlineId").as[Int]
                val strength = inputJson("strength").as[Int]

                AirlineSource.saveAirlineModifier(airlineId, BannerLoyaltyAirlineModifier(strength, CycleSource.loadCycle()))
                Right(Ok(Json.obj("action" -> action)))
              case "switch" =>
                if (adminUser.isSuperAdmin) {

                  request.session.get("userToken") match {
                    case Some(userToken) =>
                      SessionUtil.getUserId(userToken) match {
                        case Some(userId) => Right(Ok(Json.obj("action" -> action)).withSession("userToken" -> SessionUtil.addUserId(targetUserId), "adminToken" -> userToken))
                        case None => Left(BadRequest(s"Invalid token (admin) $userToken"))
                      }
                    case None => Left(BadRequest("no current admin token"))
                  }
                } else {
                  Left(Forbidden("Not a super admin user"))
                }
              case _ =>
                println(s"unknown admin action $action")
                Left(BadRequest(Json.obj("action" -> action)))
            }
          }
        case None => Left(BadRequest(s"ADMIN - Failed action $action as User $targetUserId is not found"))
      }

    } else {
      println(s"Non admin ${adminUser} tried to access admin operations!!")
      Left(Forbidden("Not an admin user"))
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
            val airlineModifiers = AirlineSource.loadAirlineModifierByAirlineId(airline.id)
            result = result.append(Json.obj(
              "airlineName" -> airline.name,
              "airlineId" -> airline.id,
              "userId" -> user.id,
              "username" -> user.userName,
              "userModifiers" -> user.modifiers,
              "userStatus" -> user.status,
              "airlineModifiers" -> airlineModifiers.map(_.modifierType),
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
            val airlineModifiers = AirlineSource.loadAirlineModifierByAirlineId(airline.id)
            result = result.append(Json.obj(
              "airlineName" -> airline.name,
              "airlineId" -> airline.id,
              "userId" -> user.id,
              "username" -> user.userName,
              "userStatus" -> user.status,
              "userModifiers" -> user.modifiers,
              "airlineModifiers" -> airlineModifiers.map(_.modifierType),
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

  def setUserModifier(userModifier: UserModifier.Value, targetUser: User) = {
    UserSource.saveUserModifier(targetUser.id, userModifier)
    println(s"ADMIN - updated user modifier $userModifier on user ${targetUser.userName}")
  }

  def clearUserModifiers(targetUser: User) = {
    UserSource.deleteUserModifiers(targetUser.id)
    println(s"ADMIN - clear user modifier on user ${targetUser.userName}")
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
