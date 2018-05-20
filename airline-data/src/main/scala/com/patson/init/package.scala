package com.patson

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import scala.io.Codec

package object init {
  implicit val actorSystem = ActorSystem("init-stream")
  implicit val materializer = FlowMaterializer()
  implicit val codec = Codec.UTF8
}