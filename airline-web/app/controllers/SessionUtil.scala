package controllers

import java.time.LocalDateTime
import java.util.UUID

object SessionUtil {
  def getUserId(token : String) = {
    Session.extractUserId(token)
  }

  def addUserId(userId : Int) = {
    Session.generateToken(userId)
  }
}

case class Session(token: String, userId : Int, var expiration: LocalDateTime)

object Session {
  private val sessions= scala.collection.mutable.Map.empty[String, Session]

  def generateToken(userId : Int): String = {
    // we use UUID to make sure randomness and uniqueness on tokens
    val token = s"$userId-token-${UUID.randomUUID().toString}"
    sessions.put(token, Session(token, userId, LocalDateTime.now().plusHours(24)))

    token
  }

  def extractUserId(token : String) : Option[Int] = {
    sessions.get(token) match {
      case Some(session) =>
        val now = LocalDateTime.now()
        if (session.expiration.isAfter(now)) {
          //refresh the session
          session.expiration = now.plusHours(24)
          Some(session.userId)
        } else { //expired, purge
          sessions.remove(token)
          None
        }
      case None => None
    }
  }

}
