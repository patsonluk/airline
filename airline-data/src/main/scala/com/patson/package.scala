package com

import akka.actor.ActorSystem

import akka.stream.FlowMaterializer
package object patson {
  
  implicit val actorSystem = ActorSystem("rabbit-akka-stream")

  import actorSystem.dispatcher

  implicit val materializer = FlowMaterializer()
}