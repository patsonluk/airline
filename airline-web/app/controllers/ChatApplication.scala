package controllers

import javax.inject._
import org.apache.pekko.actor._
import org.apache.pekko.stream.Materializer
import play.api._
import play.api.mvc._

import scala.concurrent.Future
import com.patson.data.UserSource
import play.api.libs.streams.ActorFlow
import websocket.chat.ChatControllerActor
import websocket.chat.ChatClientActor

@Singleton
class ChatApplication @Inject()(cc: ControllerComponents)(implicit actorSystem: ActorSystem, mat : Materializer) extends AbstractController(cc) {
  val logger = Logger(this.getClass)
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
    Future.successful(request.session.get("userToken").flatMap(SessionUtil.getUserId(_)) match {
      case None =>
        logger.debug("Chatsocket rejected")
        Left(Forbidden)
      case Some(userId) =>
        UserSource.loadUserById(userId.toInt) match {
          case None =>
            logger.info("Chatsocket rejected : user not found for id " + userId)
            Left(Forbidden)
          case Some(user) =>
            logger.info("Chatsocket, client connected with userId " + userId)
            Right(ActorFlow.actorRef(
              props = { out => Props(new ChatClientActor(out, chatControllerActor, user))},
              bufferSize = 1024 //otherwise it drops the message when it couldn't send fast enough...
            ))
        }
    })
  }
}
