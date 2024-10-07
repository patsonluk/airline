package com

import org.apache.pekko.actor.ActorSystem
//import org.apache.pekko.stream.FlowMaterializer
import scala.concurrent.ExecutionContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
package object patson {
  implicit val actorSystem : ActorSystem = ActorSystem("rabbit-akka-stream", None, None, Some(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())))

  import actorSystem.dispatcher

  //implicit val materializer = FlowMaterializer()
}