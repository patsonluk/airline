package controllers

import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.Results._
import play.api.mvc.RequestHeader

import scala.util.Random
import java.security.spec.KeySpec

import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.util.Base64

import com.patson.Authentication
import com.patson.data.UserSource
import com.patson.model._
import play.api.mvc._

import scala.concurrent.Future
import play.api.mvc.Security.AuthenticatedRequest
import com.patson.data.AirlineSource
import com.patson.util.AirlineCache

object AuthenticationObject {
  //  object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req), _ =>
  //    Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured Area"""")) {
  //  }
  val defaultBodyParser = new BodyParsers.Default

  object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req), defaultBodyParser, unauthorizedHandler)

  case class AuthenticatedAirline(airlineId : Int) extends AuthenticatedBuilder(req => getUserAirlineFromRequest(req, airlineId), defaultBodyParser, unauthorizedHandler)

  val unauthorizedHandler = (request : RequestHeader) =>
    if (!request.session.isEmpty && request.session.get("userToken").isDefined) {
      BadRequest("User token is invalid").removingFromSession("userToken")(request)
    } else {
      Unauthorized("Invalid login")
    }


  def getUserFromRequest(request : RequestHeader) : Option[User] = {
    if (!request.session.isEmpty && request.session.get("userToken").isDefined) {
      request.session.get("userToken").foreach{ userToken =>
        SessionUtil.getUserId(userToken) match {
          case Some(userId) => return UserSource.loadUserById(userId)//success!
          case None => println(s"Invalid token $userToken")
        }
      }
    }
    if (!request.headers.get("Authorization").isEmpty) {
      val result = request.headers.get("Authorization").flatMap { authorization =>
        authorization.split(" ").drop(1).headOption.flatMap { encoded =>
          new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
            case userName :: password :: Nil  =>
//                println("from header " + userName + " : " + password)
              if (Authentication.authenticate(userName, password)) {
                UserSource.loadUserByUserName(userName)
              } else {
                println("Invalid userName and password on user " + userName)
                request.session.+(("rejection", "password"))
                None
              }
            case _ => None
          }
        }
      }
      return result
    } else {
      return None
    }

  }
  
  def getUserAirlineFromRequest(request : RequestHeader, airlineId : Int) = {
    getUserFromRequest(request) match {
      case Some(user) =>
        if (user.hasAccessToAirline(airlineId)) {
          AirlineCache.getAirline(airlineId, true)
        } else {
          println(user.userName + " trying to access airline " + airlineId + " which he does not have access to!")
          None
        }
      case None =>
        None
    }
  }
  
  def getHashedPassword(plainPassword : String) : Unit = {
    val salt = new Array[Byte](16);
    Random.nextBytes(salt);
    val spec = new PBEKeySpec(plainPassword.toCharArray(), salt, 65536, 128);
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    val hash = f.generateSecret(spec).getEncoded();
    val enc = Base64.getEncoder();
    System.out.printf("salt: %s%n", enc.encodeToString(salt));
    System.out.printf("hash: %s%n", enc.encodeToString(hash));
  }
}