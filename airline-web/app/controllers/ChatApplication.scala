package controllers

import javax.inject._
import akka.actor._
import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json.JsValue

import models._

@Singleton
class ChatApplication @Inject()(actorSystem: ActorSystem) extends Controller {
// creates actor of type chat described above
  val chat = actorSystem.actorOf(Props[Chat], "chat")

  /*
   Specifies how to wrap an out-actor that will represent
   WebSocket connection for a given request.
  */
 // def chatSocket = WebSocket.acceptWithActor[String, String] {
 //   (request: RequestHeader) =>
 //   (out: ActorRef) =>
 //   Props(new ClientActor(out, chat))
 //  }	 
   def chatSocket = WebSocket.tryAcceptWithActor[String, String] { request => 
    Future.successful(request.session.get("userId") match {
      case None =>
        Logger.info("Chatsocket rejected")
        Left(Forbidden)
      case Some(userId) => 
        Logger.info("Chatsocket, client connected with userId " + userId)
        Right((out: ActorRef) => Props(new ClientActor(out, chat, userId.toInt)))
    })
  }
}
