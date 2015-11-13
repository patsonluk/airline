package websocket

import akka.actor._

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  val backgroundActor = actorSystem.actorOf(Props[BackgroundActor])
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case Notification(message) =>
//      println("going to send " + message + " back to the websocket")
      out ! message
    case airlineId : String =>
        MyWebSocketActor.backgroundActor ! RegisterToBackground(airlineId.toInt)
    case any =>
      println("received " + any) 
  }
  
  override def aroundPostStop() = {
    println("actor stopping")
    MyWebSocketActor.backgroundActor ! RemoveFromBackground
  }
}