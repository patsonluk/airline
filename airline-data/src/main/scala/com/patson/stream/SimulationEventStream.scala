package com.patson.stream

import akka.actor._
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Set
import com.typesafe.config.ConfigFactory

object SimulationEventStream{
  val config = ConfigFactory.load()
  implicit val system = ActorSystem(REMOTE_SYSTEM_NAME, config.getConfig(REMOTE_SYSTEM_NAME).withFallback(config))
  
  //spin off a remote actor
  val registeredActors = Set[ActorRef]()  
  val mainActor = system.actorOf(Props[BridgeActor], BRIDGE_ACTOR_NAME) //this actor receives subscriptions (from client) and also subscribe to the event stream and forward it back to subscribers from client
  
  system.eventStream.subscribe(mainActor, classOf[(SimulationEvent, Any)])
  
  println("registered the mainActor to the event stream!")
  
  def publish(topic: SimulationEvent, payload: Any) {
    system.eventStream.publish(topic, payload)
  }
  
  class BridgeActor extends Actor {
    def receive = {
      case "subscribe" =>
        println("subcribing actor " + sender().path)
        registeredActors += sender()
      case "unsubscribe" =>
        println("unsubcribing actor " + sender().path)
        registeredActors -= sender()
      case (topic: SimulationEvent, payload: Any) =>
        println("received " + topic)
        registeredActors.foreach { registeredActor =>
          println("forwarding message back to " + registeredActor.path)
          registeredActor ! (topic, payload) 
        }
      case _ =>
        println("UNKNOWN???")
    }
  }
}


class SimulationEvent
case class CycleCompleted(cycle : Int) extends SimulationEvent