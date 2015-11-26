package controllers

import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.Results._
import play.api.mvc.RequestHeader



  
object Authetication {
  object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req), _ => 
    Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured Area"""", 
                              "Access-Control-Allow-Origin" -> "*"))
  def getUserFromRequest(request : RequestHeader) = {
    println("HERE!")
    if (!request.headers.get("Authorization").isEmpty) {
      val result = request.headers.get("Authorization").flatMap { authorization =>
        authorization.split(" ").drop(1).headOption.flatMap { encoded =>
          new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
            case u :: p :: Nil  => 
              println(u + " : " + p) 
              Some(new MyUser)
            case _ => None
          }
        }
      }
      println(result.get)
      result
    } else {
      None
    }
  }
  class MyUser
}