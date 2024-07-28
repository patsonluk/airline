package controllers

import com.typesafe.config.ConfigFactory
import controllers.AuthenticationObject.AuthenticatedAirline
import play.api.libs.json.Json
import play.api.mvc._
import websocket.Broadcaster

import javax.inject.Inject

/**
 * Directly trigger events for Broadcaster. Usually broadcaster is triggered by ws subscription.
 *
 * But there are cases that we don't want to re-subscribe, yet want to trigger some actions in the Broadcaster
 * @param cc
 */
class BroadcasterDirectApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def triggerPromptsCheck(airlineId : Int) = AuthenticatedAirline(airlineId) { request =>
    Broadcaster.checkPrompts(airlineId)
    Ok(Json.obj())
  }
}
