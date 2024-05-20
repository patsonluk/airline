package controllers

import com.patson.data.UserSource

import javax.inject.Inject
import play.api._
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import websocket.MyWebSocketActor

import scala.concurrent.Future

class WebsocketApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val logger = Logger(this.getClass)
  def wsWithActor = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful(request.session.get("userToken").flatMap(SessionUtil.getUserId(_)) match {
      case None =>
        logger.info("websocket rejected")
        Left(Forbidden)
      case Some(userId) =>
        logger.info("wsWithActor, client connected with userId " + userId)
        Right(ActorFlow.actorRef { out =>
          println(s"userid $userId has actor ${out.path}")
          val airline = UserSource.loadUserById(userId).get.getAccessibleAirlines()(0)
          MyWebSocketActor.props(out, airline.id, request.remoteAddress)
        })
    })
  }
}
