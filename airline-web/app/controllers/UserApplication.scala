package controllers

import controllers.AuthenticationObject.Authenticated
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.mvc._
import play.api.libs.json.Writes
import com.patson.model.{Airline, User, UserStatus}
import play.api.libs.json._
import com.patson.data.{AllianceSource, IpSource, SettingsSource, UserSource, UserUuidSource}
import com.patson.util.AllianceCache

import java.util.UUID
import javax.inject.Inject

class UserApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
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

      user.adminStatus.foreach { adminStatus =>
        result = result + ("adminStatus" -> JsString(adminStatus.toString))
      }
      
      if (user.getAccessibleAirlines().isDefinedAt(0)) {
        AllianceSource.loadAllianceMemberByAirline(user.getAccessibleAirlines()(0)).foreach { allianceMember => //if this airline belongs to an alliance
          val allianceId = allianceMember.allianceId
          AllianceCache.getAlliance(allianceId).foreach { alliance =>
            result = result + ("allianceId" -> JsNumber(allianceId)) + ("allianceName" -> JsString(alliance.name)) + ("allianceRole" -> JsString(allianceMember.role.toString))
          }
        }
      }
        
      result
    }
  }
  // then in a controller
  def login = Authenticated { implicit request =>
    var isSuperAdmin = false
    //check if it's super admin switching
    val adminTokenOption = request.session.get("adminToken")
    adminTokenOption.foreach{ adminToken =>
      SessionUtil.getUserId(adminToken).foreach { adminId =>
        UserSource.loadUserById(adminId).foreach { user =>
          isSuperAdmin = user.isSuperAdmin
          println(s"Admin ${user.userName} is logging in on behalf of ${request.user.userName}")
        }
      }
    }

    if (!isSuperAdmin) { //do not track if admin is switching, otherwise that would be confusing
      IpSource.saveUserIp(request.user.id, request.remoteAddress)

      UserSource.updateUserLastActive(request.user)
      if (request.user.status == UserStatus.INACTIVE) {
        UserSource.updateUser(request.user.copy(status = UserStatus.ACTIVE))
      }
    }


    if (request.user.isBanned && !isSuperAdmin) {
      println(s"Banned user ${request.user.userName} tried to login")
      Forbidden("User is banned")
    } else {
      val result = Json.toJson(request.user).asInstanceOf[JsObject] + ("hasWallpaper" -> JsBoolean(SettingsSource.hasWallpaper(request.user.id)))

      val uuid : String = request.cookies.get("uuid").map(_.value).getOrElse {
        val newUuid = UUID.randomUUID().toString
        newUuid
      }
      if (!isSuperAdmin) {
        UserUuidSource.saveUserUuid(request.user.id, uuid)
      }

      var response = Ok(result).withCookies(Cookie("uuid", uuid, maxAge = Some(Integer.MAX_VALUE))).withHeaders("Access-Control-Allow-Credentials" -> "true").withSession("userToken" -> SessionUtil.addUserId(request.user.id))
      adminTokenOption.foreach { adminToken =>
        response = response.addingToSession("adminToken"-> adminToken)
      }

      response
    }
  }
  
  def logout = Authenticated { implicit request =>
    Ok("logged out for " + request.user.id).withNewSession 
  }
  
 
}
