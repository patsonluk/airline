package websocket

import akka.actor._
import com.patson.data.UserSource
import com.patson.stream._
import java.util.concurrent.atomic.AtomicLong

object MyWebSocketActor {
  val counter = new AtomicLong() 
  def props(out: ActorRef, userId : Int) = Props(new MyWebSocketActor(out, userId))
  def nextSubscriberId(userId : Int) = { userId.toString + "-" +   counter.getAndIncrement }
  //val backgroundActor = actorSystem.actorOf(Props[BackgroundActor])
}

class MyWebSocketActor(out: ActorRef, userId : Int) extends Actor {
  var subscriberId : Option[String] = None
  def receive = {
    case Notification(message) =>
//      println("going to send " + message + " back to the websocket")
      out ! message
    case airlineId : String => //directly recieve message from the websocket (the only message the websocket client send down now is the airline id
      try {
        UserSource.loadUserById(userId).foreach { user => 
          if (user.hasAccessToAirline(airlineId.toInt)) {
            val subscriberId = MyWebSocketActor.nextSubscriberId(userId)
            RemoteSubscribe.subscribe( (topic: SimulationEvent, payload: Any) => Some(topic).collect {
              case CycleCompleted(cycle) => 
                println("Received cycle completed: " + cycle)
                out ! cycle.toString() //if a CycleCompleted is published to the stream, notify the out(websocket) of the cycle  
            }, subscriberId)
            
            this.subscriberId = Some(subscriberId)
            //MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)      
          } else {
            println("user " + userId + " has no access to airline " + airlineId)
          }
        }
      } catch {
        case _ : NumberFormatException => println("Receieved websocket message " +  airlineId + " which is not numeric!")
      }
    case any =>
      println("received " + any) 
  }
  
  override def aroundPostStop() = {
    println("actor stopping")
    subscriberId.foreach { RemoteSubscribe.unsubscribe(_) }
    
    //MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}