package com.patson

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
package object init {
  implicit val actorSystem = ActorSystem("init-stream")
  implicit val materializer = FlowMaterializer()
}