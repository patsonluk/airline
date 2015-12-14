package com.patson

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import com.typesafe.config.ConfigFactory
package object stream {
  val REMOTE_SYSTEM_NAME = "websocketActorSystem"
  val BRIDGE_ACTOR_NAME = "bridgeActor"
}