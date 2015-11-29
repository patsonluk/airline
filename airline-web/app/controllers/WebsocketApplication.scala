package controllers

import javax.inject.Inject
import play.api.Play.current
import play.api._
import play.api.libs.ws._
import play.api.mvc._
import websocket.MyWebSocketActor
import scala.concurrent.Future

class WebsocketApplication @Inject()(ws: WSClient) extends Controller {
  def wsWithActor = WebSocket.tryAcceptWithActor[String, String] { request => 
    Future.successful(request.session.get("userId") match {
      case None =>
        Logger.info("websocket rejected")
        Left(Forbidden)
      case Some(userId) => 
        Logger.info("wsWithActor, client connected with userId " + userId)
        Right(out => MyWebSocketActor.props(out, userId.toInt))
    })
  }
}
