package controllers

import javax.inject.Inject
import play.api.Play.current
import play.api._
import play.api.libs.ws._
import play.api.mvc._
import websocket.MyWebSocketActor

class WebsocketApplication @Inject()(ws: WSClient) extends Controller {
  def wsWithActor = WebSocket.acceptWithActor[String, String] {
    request =>
      out => {
        Logger.info("wsWithActor, client connected")
        MyWebSocketActor.props(out)
      }
  }
  

}
