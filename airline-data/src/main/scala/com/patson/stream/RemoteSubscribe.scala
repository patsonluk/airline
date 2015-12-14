package com.patson.stream

import akka.actor.{Actor, Props, ActorSystem}
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory

sealed class LocalActor(f: (SimulationEvent, Any) => Unit) extends Actor {
  override def receive = { case (topic: SimulationEvent, payload: Any) => f(topic, payload) }
}

object RemoteSubscribe {
  implicit val system = ActorSystem("localWebsocketSystem")
  
  val remoteActor = system.actorSelection("akka.tcp://" + REMOTE_SYSTEM_NAME + "@127.0.0.1:2552/user/" + BRIDGE_ACTOR_NAME)
  
  def subscribe(f: (SimulationEvent, Any) => Option[Unit], name: String) = {
    val props = Props(classOf[LocalActor], f)
    val localSubscriber = system.actorOf(props, name = name)
    remoteActor.!("subscribe")(localSubscriber)
    println("Subscriber " + localSubscriber.path + " subscribed")
  }
  
  
  def unsubscribe(name: String) = {
    system.actorSelection(system./(name)).resolveOne()(Timeout(1000)).map {
      actorRef =>
        println("Unsubscribing " + actorRef.path)
        remoteActor.!("unsubscribe")(actorRef)
        actorRef
    }
  }
}