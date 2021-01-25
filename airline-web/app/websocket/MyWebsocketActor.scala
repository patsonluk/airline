package websocket

import java.util.concurrent.TimeUnit
import akka.actor._
import com.patson.data.{CycleSource, UserSource}
import com.patson.model.notice.NoticeCategory
import com.patson.stream._

import java.util.concurrent.atomic.AtomicLong
import com.patson.util.{AirlineCache, AirportCache}
import controllers.{AirportUtil, NoticeUtil}
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
            case CycleCompleted(cycle) =>
              MyWebSocketActor.lastSimulatedCycle = cycle
              //TODO invalidate the caches -> not the best thing to do it here, as this runs for each connected user. we should subscribe to remote with another separate actor. For now this is a quick fix
              AirlineCache.invalidateAll()
              AirportCache.invalidateAll()
              AirportUtil.refreshAirports()

              //println("Received cycle completed: " + cycle)
              out ! Json.obj("messageType" -> "cycleCompleted", "cycle" -> cycle) //if a CycleCompleted is published to the stream, notify the out(websocket) of the cycle
              BroadcastActor.checkNotices(airlineId)
            case CycleInfo(cycle, fraction, cycleDurationEstimation) =>
              //println("Received cycle info on cycle: " + cycle)
              out ! Json.obj("messageType" -> "cycleInfo", "cycle" -> cycle, "fraction" -> fraction, "cycleDurationEstimation" -> cycleDurationEstimation)
          }, subscriberId)

          actorSystem.eventStream.subscribe(self, classOf[TriggerPing])

          this.subscriberId = Some(subscriberId)

          //MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)
          BroadcastActor.checkNotices(airlineId) //check notice on connect
      } catch {
        case _ : NumberFormatException => println("Received websocket message " +  airlineId + " which is not numeric!")
      }
    case TriggerPing() =>
      out ! Json.obj("ping" -> true)
    case BroadcastMessage(text) =>
      out ! Json.obj("messageType" -> "broadcastMessage", "message" -> text)
    case AirlineMessage(airline, text) =>
      out ! Json.obj("messageType" -> "airlineMessage", "message" -> text)
    case AirlineNotice(airline, notice) =>
      println(s"Sending notice $notice to $airline")
      out ! Json.obj("messageType" -> "levelNotice", "category" -> NoticeCategory.LEVEL_UP.toString, "level" -> airline.airlineGrade.value, "description" -> airline.airlineGrade.description)
    case any =>
      println("received " + any + " not handled")  
  }

  override def aroundPostStop() = {
    println("actor stopping")
    subscriberId.foreach { RemoteSubscribe.unsubscribe(_) }
    actorSystem.eventStream.unsubscribe(self)
    
    //MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}