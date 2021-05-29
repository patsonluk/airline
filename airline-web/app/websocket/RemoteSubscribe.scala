package websocket

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props}
import akka.remote.{AssociatedEvent, DisassociatedEvent, RemotingLifecycleEvent}
import akka.util.Timeout
import com.patson.stream.SimulationEvent
import com.typesafe.config.ConfigFactory

import java.util.concurrent.TimeUnit
import scala.collection.mutable

//Instead of maintaining a new actor connection whenever someone logs in, we will only maintain one connnection between sim and web app, once sim finishes a cycle, it will send one message the the web app actor, and the web app actor will relay the message in an event stream, which is subscribed by each login section.
//
//For new login, the web app local actor will directly send one message to the remote actor, and the remote actor will in this case reply directly to the web app local actor - this is the ONLY time that the 2 talk directly
sealed class LocalActor(f: (SimulationEvent, Any) => Unit) extends Actor {
  override def receive = {
      case (topic: SimulationEvent, payload: Any) =>
        f(topic, payload)
      case unknown : Any => println(s"Unknown message for local actor : $unknown")
  }
  override def postStop() = {
    println(self.path.toString + " stopped (post stop)")
  }
}

sealed class LocalMainActor(remoteActor : ActorSelection) extends Actor { //only 1 locally, fan out message to all local actors to reduce connections required
  override def receive = {
    case (topic: SimulationEvent, payload: Any) =>
      println(s"Local main action received topic $topic")
      context.system.eventStream.publish(topic, payload) //relay to local event stream... since i don't know if I can subscribe to remote event stream...
    case Resubscribe(remoteActor) =>
      println(self.path.toString +  " Attempting to resubscribe")
      remoteActor ! "subscribe"
    case unknown : Any => println(s"Unknown message for local main actor : $unknown")
  }

  override def preStart = {
    super.preStart()
    remoteActor ! "subscribe"
  }

  override def postStop() = {
    println(self.path.toString + " stopped (post stop)")
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
//        val localSubscribers = system.actorSelection(system./("local-subscriber-*"))
//        localSubscribers ! Resubscribe(remoteActor)
        val localMainActor = system.actorSelection(system./("local-main-actor"))
        localMainActor ! Resubscribe(remoteActor)
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

  val subscribers = mutable.HashSet[ActorRef]()
  val remoteMainActor = system.actorSelection("akka.tcp://" + REMOTE_SYSTEM_NAME + "@" + actorHost + "/user/" + BRIDGE_ACTOR_NAME)
  val localMainActor = system.actorOf(Props(classOf[LocalMainActor], remoteMainActor), "local-main-actor")


  val reconnectActor = system.actorOf(Props(classOf[ReconnectActor], remoteMainActor), "reconnect-actor")
  reconnectActor ! remoteMainActor //why?




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
    system.eventStream.subscribe(localSubscriber, classOf[(SimulationEvent, Any)])

    println("Subscriber " + localSubscriber.path + " subscribed")

    //now get updated cycle info once
    remoteMainActor.!("getCycleInfo")(localSubscriber)
  }


  def unsubscribe(subscriberid : String) = {
    system.actorSelection(system./(getLocalSubscriberName(subscriberid))).resolveOne()(Timeout(10, TimeUnit.SECONDS)).map {
      actorRef =>
        println("Unsubscribing " + actorRef.path)
        system.eventStream.unsubscribe(actorRef)
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