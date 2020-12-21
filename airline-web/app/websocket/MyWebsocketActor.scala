package websocket

import java.util.concurrent.TimeUnit

import akka.actor._
import com.patson.data.{CycleSource, UserSource}
import com.patson.stream._
import java.util.concurrent.atomic.AtomicLong

import com.patson.util.{AirlineCache, AirportCache}
import play.api.libs.json.JsNumber
import play.api.libs.json.Json
import websocket.chat.TriggerPing

import scala.concurrent.duration.Duration

object MyWebSocketActor {
  val counter = new AtomicLong()

  def props(out: ActorRef, userId: Int) = Props(new MyWebSocketActor(out, userId))

  def nextSubscriberId(userId: Int) = {
    userId.toString + "-" + counter.getAndIncrement
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



class MyWebSocketActor(out: ActorRef, userId : Int) extends Actor {
  var subscriberId : Option[String] = None
  def receive = {
    case Notification(message) =>
//      println("going to send " + message + " back to the websocket")
      out ! message
    case JsNumber(airlineId) => //directly recieve message from the websocket (the only message the websocket client send down now is the airline id
      try {
        UserSource.loadUserById(userId).foreach { user => 
          if (user.hasAccessToAirline(airlineId.toInt)) {
            val subscriberId = MyWebSocketActor.nextSubscriberId(userId)
            RemoteSubscribe.subscribe( (topic: SimulationEvent, payload: Any) => Some(topic).collect {
              case CycleCompleted(cycle) =>
                MyWebSocketActor.lastSimulatedCycle = cycle
                //TODO invalidate the caches -> not the best thing to do it here, as this runs for each connected user. we should subscribe to remote with another separate actor. For now this is a quick fix
                AirlineCache.invalidateAll()
                AirportCache.invalidateAll()
                //println("Received cycle completed: " + cycle)
                out ! Json.obj("messageType" -> "cycleCompleted", "cycle" -> cycle) //if a CycleCompleted is published to the stream, notify the out(websocket) of the cycle
              case CycleInfo(cycle, fraction, cycleDurationEstimation) =>
                //println("Received cycle info on cycle: " + cycle)
                out ! Json.obj("messageType" -> "cycleInfo", "cycle" -> cycle, "fraction" -> fraction, "cycleDurationEstimation" -> cycleDurationEstimation)
            }, subscriberId)

            actorSystem.eventStream.subscribe(self, classOf[TriggerPing])
            
            this.subscriberId = Some(subscriberId)

            //MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)      
          } else {
            println("user " + userId + " has no access to airline " + airlineId)
          }
        }
      } catch {
        case _ : NumberFormatException => println("Received websocket message " +  airlineId + " which is not numeric!")
      }
    case TriggerPing() =>
      out ! Json.obj("ping" -> true)
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