

import org.apache.pekko.actor.ActorSystem

import scala.concurrent.ExecutionContext
package object websocket {
  implicit val actorSystem = ActorSystem("airline-websocket-actor-system")
  //implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val executionContext = actorSystem.dispatchers.lookup("my-pinned-dispatcher")
}