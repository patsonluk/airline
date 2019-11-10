package controllers

import javax.inject._
import akka.actor._
import akka.stream.Materializer
import play.api._
import play.api.mvc._

import scala.concurrent.Future
import com.patson.data.UserSource
import play.api.libs.streams.ActorFlow
import websocket.chat.ChatControllerActor
import websocket.chat.ChatClientActor

@Singleton
class ChatApplication @Inject()(cc: ControllerComponents)(implicit actorSystem: ActorSystem, mat : Materializer) extends AbstractController(cc) {
  
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
   def chatSocket = WebSocket.acceptOrResult[String, String] { request =>
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
            Right(ActorFlow.actorRef { out => Props(new ChatClientActor(out, chatControllerActor, user))})
        }
        
    })
  }
}
