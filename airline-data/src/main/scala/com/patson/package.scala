package com

import akka.actor.ActorSystem
//import akka.stream.FlowMaterializer
import scala.concurrent.ExecutionContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
package object patson {
  implicit val actorSystem = ActorSystem("rabbit-akka-stream", None, None, Some(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())))

  import actorSystem.dispatcher

  //implicit val materializer = FlowMaterializer()
}