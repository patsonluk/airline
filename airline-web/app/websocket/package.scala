

import akka.actor.ActorSystem
package object websocket {
  implicit val actorSystem = ActorSystem("airline-websocket-actor-system")
}