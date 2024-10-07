

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.dispatch.MessageDispatcher

import scala.concurrent.ExecutionContext
package object websocket {
  implicit val actorSystem : ActorSystem = ActorSystem("airline-websocket-actor-system")
  //implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val executionContext : MessageDispatcher = actorSystem.dispatchers.lookup("my-pinned-dispatcher")
}