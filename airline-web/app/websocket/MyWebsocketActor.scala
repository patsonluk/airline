package websocket

import java.util.concurrent.TimeUnit
import org.apache.pekko.actor._
import org.apache.pekko.util.Timeout
import com.patson.data.{CycleSource, UserSource}
import com.patson.model.{UserModifier, UserStatus}
import com.patson.model.notice.{AirlineNotice, LoyalistNotice, NoticeCategory}
import com.patson.stream._

import java.util.concurrent.atomic.AtomicLong
import com.patson.util.{AirlineCache, AirplaneOwnershipCache, AirportCache, UserCache}
import controllers.{AirlineTutorial, AirportUtil, PromptUtil}
import models.{PendingAction, PendingActionCategory}
import play.api.libs.json.JsNumber
import play.api.libs.json.Json
import websocket.MyWebSocketActor.nextSubscriberId
import websocket.chat.TriggerPing

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object MyWebSocketActor {
  val counter = new AtomicLong()

  def props(out: ActorRef, airlineId: Int, remoteAddress : String) = Props(classOf[MyWebSocketActor], out, airlineId, remoteAddress)

  def nextSubscriberId(airlineId: Int) = {
    airlineId.toString + "-" + counter.getAndIncrement
  }

  startBackgroundPingTrigger()

  def startBackgroundPingTrigger(): Unit = {
    actorSystem.scheduler.schedule(Duration.Zero, Duration(30, TimeUnit.SECONDS), new Runnable {
      def run(): Unit = {
        actorSystem.eventStream.publish(TriggerPing())
      }
    })
  }

  var lastSimulatedCycle = CycleSource.loadCycle()
}


/**
 * This should ONLY handle incoming socket message. this actor acts so weird:
 * To send message to itself => Send: OK ; Receive: OK
 * To send message to remote actor => Send : OK
 * To receive message from remote actor => Receive : Failed
 *
 * Not quite sure why it does not work, perhaps it can only work with web socket traffic
 *
 * Tried to create a child actor, same thing, but if we create another actor that is NOT a child of this, then it seems to work fine...
 *
 *
 * @param out
 * @param airlineId
 * @param remoteAddress
 */
class MyWebSocketActor(out: ActorRef, airlineId : Int, remoteAddress : String) extends Actor {
  val outActor = actorSystem.actorOf(Props(classOf[LocalActor], out, airlineId), nextSubscriberId(airlineId)) //do NOT create as a child, otherwise it cannot receive message from remote actor...

  override def preStart = {
    val airline = AirlineCache.getAirline(airlineId).get
    println(s"Starting websocket on airline $airline with remoteAddress $remoteAddress path ${self}. With output actor ${outActor.path}")
  }

  def receive = {
    case JsNumber(_) => //directly receive message from the websocket (the only message the websocket client send down now is the airline id
      try {
        Broadcaster.checkPrompts(airlineId) //check notice on connect
        checkWarnings(airlineId)
      } catch {
        case _ : NumberFormatException => println("Received websocket message " +  airlineId + " which is not numeric!")
      }
    case any =>
      println("received " + any + " not handled")
  }

  override def postStop() = {
    //subscriberId.foreach { ActorCenter.unsubscribe(_) }
    println(s"${self.path} is stopped")
    //actorSystem.eventStream.unsubscribe(self)
    outActor ! PoisonPill //have to explicitly kill the output actor since it is not a child
    //MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }

  def checkWarnings(airlineId : Int) = {
    UserSource.loadUserByAirlineId(airlineId).foreach { user =>
      if (user.modifiers.contains(UserModifier.WARNED)) {
        Broadcaster.sendMessage(AirlineCache.getAirline(airlineId).get, s"Our systems have detected you own more airlines than the allowed ${user.maxAirlinesAllowed} airlines limit. Please take actions to reset your airlines and maintain only active airlines according to the limit.  Otherwise we might ban all your accounts after 3 days. Please contact our admins on discord for disputes.")
      }
    }
  }
}
