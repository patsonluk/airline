package com.patson

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.io.Codec

package object init {
  implicit val actorSystem = ActorSystem("init-stream")
  implicit val materializer = ActorMaterializer()
  implicit val codec = Codec.UTF8
}