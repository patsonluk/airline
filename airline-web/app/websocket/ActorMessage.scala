package websocket

object ActorMessage {
  
}

case class RegisterToBackground(airlineId : Int)
case class RemoveFromBackground()
case class Notification(message : String)
