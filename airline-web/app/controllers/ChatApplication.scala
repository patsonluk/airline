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
import com.patson.data.UserSource
import websocket.chat.ChatControllerActor
import websocket.chat.ChatClientActor

@Singleton
class ChatApplication @Inject()(actorSystem: ActorSystem) extends Controller {
  
  val chatControllerActor = actorSystem.actorOf(Props[ChatControllerActor], "chatControllerActor")

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
        
        UserSource.loadUserById(userId.toInt) match {
          case None =>
            Logger.info("Chatsocket rejected : user not found for id " + userId)
            Left(Forbidden)
          case Some(user) =>
            Logger.info("Chatsocket, client connected with userId " + userId)
            Right((out: ActorRef) => Props(new ChatClientActor(out, chatControllerActor, user)))
        }
        
    })
  }
}
