package websocket

import java.util.concurrent.TimeUnit
import akka.actor._
import com.patson.data.{CycleSource, UserSource}
import com.patson.model.notice.{AirlineNotice, LoyalistNotice, NoticeCategory}
import com.patson.stream._

import java.util.concurrent.atomic.AtomicLong
import com.patson.util.{AirlineCache, AirplaneOwnershipCache, AirportCache}
import controllers.{AirlineTutorial, AirportUtil, PromptUtil}
import models.{PendingAction, PendingActionCategory}
import play.api.libs.json.JsNumber
import play.api.libs.json.Json
import websocket.chat.TriggerPing

import scala.concurrent.duration.Duration

object MyWebSocketActor {
  val counter = new AtomicLong()

  def props(out: ActorRef, airlineId: Int) = Props(new MyWebSocketActor(out, airlineId))

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



class MyWebSocketActor(out: ActorRef, airlineId : Int) extends Actor {
  var subscriberId : Option[String] = None
  override def preStart = {
    BroadcastActor.subscribe(self, AirlineCache.getAirline(airlineId).get)
  }

  def receive = {
    case Notification(message) =>
//      println("going to send " + message + " back to the websocket")
      out ! message
    case JsNumber(_) => //directly receive message from the websocket (the only message the websocket client send down now is the airline id
      try {
          val subscriberId = MyWebSocketActor.nextSubscriberId(airlineId)
          RemoteSubscribe.subscribe( (topic: SimulationEvent, payload: Any) => Some(topic).collect {
            case CycleCompleted(cycle, cycleEndTime) =>
              MyWebSocketActor.lastSimulatedCycle = cycle
              //TODO invalidate the caches -> not the best thing to do it here, as this runs for each connected user. we should subscribe to remote with another separate actor. For now this is a quick fix
              AirlineCache.invalidateAll()
              AirportCache.invalidateAll()
              AirplaneOwnershipCache.invalidateAll()
              AirportUtil.refreshAirports()

              //println("Received cycle completed: " + cycle)
              out ! Json.obj("messageType" -> "cycleCompleted", "cycle" -> cycle) //if a CycleCompleted is published to the stream, notify the out(websocket) of the cycle
              BroadcastActor.checkPrompts(airlineId)
            case CycleInfo(cycle, fraction, cycleDurationEstimation) =>
              //println("Received cycle info on cycle: " + cycle)
              out ! Json.obj("messageType" -> "cycleInfo", "cycle" -> cycle, "fraction" -> fraction, "cycleDurationEstimation" -> cycleDurationEstimation)
          }, subscriberId)

          actorSystem.eventStream.subscribe(self, classOf[TriggerPing])

          this.subscriberId = Some(subscriberId)

          //MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)
          BroadcastActor.checkPrompts(airlineId) //check notice on connect
      } catch {
        case _ : NumberFormatException => println("Received websocket message " +  airlineId + " which is not numeric!")
      }
    case TriggerPing() =>
      out ! Json.obj("ping" -> true)
    case BroadcastMessage(text) =>
      out ! Json.obj("messageType" -> "broadcastMessage", "message" -> text)
    case AirlineMessage(airline, text) =>
      out ! Json.obj("messageType" -> "airlineMessage", "message" -> text)
    case AirlineNotice(airline, notice, description) =>
      println(s"Sending notice $notice to $airline")
      notice.category match {
        case NoticeCategory.LEVEL_UP =>
          out ! Json.obj("messageType" -> "notice", "category" -> notice.category.toString, "id" -> notice.id, "level" -> notice.id, "description" -> description)
        case NoticeCategory.LOYALIST =>
          out ! Json.obj("messageType" -> "notice", "category" -> notice.category.toString, "id" -> notice.id, "level" -> notice.id, "description" -> description)

      }
    case AirlineTutorial(airline, tutorial) =>
      println(s"Sending tutorial $tutorial to $airline")
      out ! Json.obj("messageType" -> "tutorial", "category" -> tutorial.category, "id" -> tutorial.id)
    case AirlinePendingActions(airline, pendingActions : List[PendingAction]) =>
      out ! Json.obj("messageType" -> "pendingAction", "actions" -> Json.toJson(pendingActions.map(_.category.toString)))
    case any =>
      println("received " + any + " not handled")  
  }

  override def aroundPostStop() = {
    subscriberId.foreach { RemoteSubscribe.unsubscribe(_) }
    actorSystem.eventStream.unsubscribe(self)
    
    //MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}