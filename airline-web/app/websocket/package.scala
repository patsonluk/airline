

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
package object websocket {
  implicit val actorSystem = ActorSystem("airline-websocket-actor-system")
  implicit val ec: ExecutionContext = ExecutionContext.global
}