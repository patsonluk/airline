package websocket

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSelection, ActorSystem, PoisonPill, Props}
import akka.remote.{AssociatedEvent, DisassociatedEvent, RemotingLifecycleEvent}
import akka.util.Timeout
import com.patson.stream.SimulationEvent
import com.typesafe.config.ConfigFactory


sealed class LocalActor(f: (SimulationEvent, Any) => Unit) extends Actor {
  override def receive = { 
      case (topic: SimulationEvent, payload: Any) => 
        f(topic, payload) 
      case Resubscribe(remoteActor) =>
        println(self.path.toString +  " Attempting to resubscribe")
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
        println(s"Disassociated. Start pinging the remote actor! from reconnect actor $this")
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
        var sleepTime = 5000
        val MAX_SLEEP_TIME = 10 * 60 * 1000 //10 mins
        while (disconnected) {
          remoteActor ! "ping"
          sleepTime *= 2
          sleepTime = Math.min(MAX_SLEEP_TIME, sleepTime)
          Thread.sleep(sleepTime)
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
  
  val configFactory = ConfigFactory.load()
  val actorHost = if (configFactory.hasPath("airline.akka-actor.host")) configFactory.getString("airline.akka-actor.host") else "127.0.0.1:2552"
  println("!!!!!!!!!!!!!!!AKK ACTOR HOST IS " + actorHost)
  
  val remoteActor = system.actorSelection("akka.tcp://" + REMOTE_SYSTEM_NAME + "@" + actorHost + "/user/" + BRIDGE_ACTOR_NAME)
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
    system.actorSelection(system./(getLocalSubscriberName(subscriberid))).resolveOne()(Timeout(10, TimeUnit.SECONDS)).map {
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