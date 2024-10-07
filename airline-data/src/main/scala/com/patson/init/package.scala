package com.patson

import org.apache.pekko.actor.ActorSystem
import scala.io.Codec

package object init {
  implicit val actorSystem : ActorSystem = ActorSystem("init-stream")
  implicit val codec : Codec = Codec.UTF8
}