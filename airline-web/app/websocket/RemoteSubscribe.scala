package websocket

import akka.actor.{Actor, Props, ActorSystem}
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.remote.AssociationEvent
import akka.remote.RemotingLifecycleEvent
import akka.actor.ActorSelection
import com.patson.stream.SimulationEvent
import akka.remote.DisassociatedEvent
import akka.remote.AssociatedEvent
import akka.actor.PoisonPill

sealed class LocalActor(f: (SimulationEvent, Any) => Unit) extends Actor {
  override def receive = { 
      case (topic: SimulationEvent, payload: Any) => 
        f(topic, payload) 
      case Resubscribe(remoteActor) =>
        println(self.path +  " Attempting to resubscribe")
        remoteActor ! "subscribe"
  }
}

sealed class ReconnectActor(remoteActor : ActorSelection) extends Actor {
  var disconnected = false
  override def preStart = { 
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])  
    remoteActor ! "ping" //establish connection
  }
  override def receive = { 
    case lifeCycleEvent : DisassociatedEvent => {
      if (!disconnected) {
        println("Disassociated. Start pinging the remote actor!")
        disconnected = true
        startPing(remoteActor)
      }
    }
    case lifeCycleEvent : AssociatedEvent => {
      if (disconnected) { //if previously disconnected
        val system = context.system
        val localSubscribers = system.actorSelection(system./("local-subscriber-*"))
        localSubscribers ! Resubscribe(remoteActor)
        disconnected = false
      }
    }
      
       
  }
  def startPing(remoteActor : ActorSelection) = {
    new Thread() {
      override def run() = {
        while (disconnected) {
          remoteActor ! "ping"
          Thread.sleep(5000)
        }
        println("Reconnected! stop pinging")
      }
    }.start()
  }
  def stopPing() = {
    disconnected = false
  }
}


object RemoteSubscribe {
  val REMOTE_SYSTEM_NAME = "websocketActorSystem"
  val BRIDGE_ACTOR_NAME = "bridgeActor"
  implicit val system = ActorSystem("localWebsocketSystem")
  
  val remoteActor = system.actorSelection("akka.tcp://" + REMOTE_SYSTEM_NAME + "@127.0.0.1:2552/user/" + BRIDGE_ACTOR_NAME)
  val reconnectActor = system.actorOf(Props(classOf[ReconnectActor], remoteActor), "reconnect-actor")
  reconnectActor ! remoteActor
//  sealed class PingActor extends Actor {
//    override def preStart = {
//      system.eventStream.subscribe(system.actorOf(Props[PingActor]), classOf[AssociationEvent])
//      reconnect(Duration.zero)
//    }
//    override def receive = {
//      case event : AssociationEvent  => println(event)
//    }
//  }

  def subscribe(f: (SimulationEvent, Any) => Option[Unit], subscriberId: String) = {
    val props = Props(classOf[LocalActor], f)
    val localSubscriber = system.actorOf(props, name = getLocalSubscriberName(subscriberId))
    remoteActor.!("subscribe")(localSubscriber)
    println("Subscriber " + localSubscriber.path + " subscribed")
  }
  
  
  def unsubscribe(subscriberid : String) = {
    system.actorSelection(system./(getLocalSubscriberName(subscriberid))).resolveOne()(Timeout(1000)).map {
      actorRef =>
        println("Unsubscribing " + actorRef.path)
        remoteActor.!("unsubscribe")(actorRef)
        actorRef ! PoisonPill 
        actorRef
    }
  }
  
  def getLocalSubscriberName(subscriberId : String) = {
    "local-subscriber-" + subscriberId
  }
}

case class RemoteActor(remoteActor : ActorSelection)
case class Resubscribe(remoteActor : ActorSelection)