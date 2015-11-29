package websocket

import akka.actor._
import com.patson.data.UserSource

object MyWebSocketActor {
  def props(out: ActorRef, userId : Int) = Props(new MyWebSocketActor(out, userId))
  val backgroundActor = actorSystem.actorOf(Props[BackgroundActor])
}

class MyWebSocketActor(out: ActorRef, userId : Int) extends Actor {
  def receive = {
    case Notification(message) =>
//      println("going to send " + message + " back to the websocket")
      out ! message
    case airlineId : String =>
        UserSource.loadUserById(userId).foreach { user => 
          if (user.hasAccessToAirline(airlineId.toInt)) {
            MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)      
          } else {
            println("user " + userId + " has no access to airline " + airlineId)
          }
        }
    case any =>
      println("received " + any) 
  }
  
  override def aroundPostStop() = {
    println("actor stopping")
    MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}