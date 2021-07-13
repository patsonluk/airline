package websocket

import java.util.concurrent.TimeUnit
import akka.actor._
import akka.util.Timeout
import com.patson.data.{CycleSource, UserSource}
import com.patson.model.notice.{AirlineNotice, LoyalistNotice, NoticeCategory}
import com.patson.stream._

import java.util.concurrent.atomic.AtomicLong
import com.patson.util.{AirlineCache, AirplaneOwnershipCache, AirportCache}
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
    actorSystem.scheduler.schedule(Duration.Zero, Duration(10, TimeUnit.SECONDS), new Runnable {
      def run(): Unit = {
        actorSystem.eventStream.publish(TriggerPing())
      }
    })
  }

  var lastSimulatedCycle = CycleSource.loadCycle()
}



class MyWebSocketActor(out: ActorRef, airlineId : Int, remoteAddress : String) extends Actor {
  val outActor = actorSystem.actorOf(Props(classOf[LocalActor], out, airlineId), nextSubscriberId(airlineId))

  override def preStart = {
    val airline = AirlineCache.getAirline(airlineId).get
    println(s"Starting websocket on airline $airline with remoteAddress $remoteAddress path ${self}. With output actor ${outActor.path}")
//    Broadcaster.subscribeToBroadcaster(self, airline, remoteAddress)
//    actorSystem.eventStream.subscribe(self, classOf[TriggerPing])
//    actorSystem.eventStream.subscribe(self, classOf[(SimulationEvent, Any)])
  }

  def receive = {
    case JsNumber(_) => //directly receive message from the websocket (the only message the websocket client send down now is the airline id
      try {
        Broadcaster.checkPrompts(airlineId) //check notice on connect
      } catch {
        case _ : NumberFormatException => println("Received websocket message " +  airlineId + " which is not numeric!")
      }
    case any =>
      println("received " + any + " not handled")  
  }

  override def aroundPostStop() = {
    //subscriberId.foreach { ActorCenter.unsubscribe(_) }
    println(s"${self.path} is stopped")
    actorSystem.eventStream.unsubscribe(self)
    outActor ! PoisonPill
    //MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}

