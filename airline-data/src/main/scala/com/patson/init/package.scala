package com.patson

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ActorMaterializer
import scala.io.Codec

package object init {
  implicit val actorSystem = ActorSystem("init-stream")
  implicit val materializer = ActorMaterializer()
  implicit val codec = Codec.UTF8
}