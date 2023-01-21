package websocket

import akka.actor.{Actor, ActorRef, ActorSelection, Props, Terminated}
import akka.remote.{AssociatedEvent, DisassociatedEvent, RemotingLifecycleEvent}
import com.patson.model.Airline
import com.patson.model.notice.{AirlineNotice, NoticeCategory}
import com.patson.stream.{CycleCompleted, CycleInfo, KeepAlivePing, KeepAlivePong, ReconnectPing, SimulationEvent}
import com.patson.util.{AirlineCache, AirplaneOwnershipCache, AirportCache}
import com.typesafe.config.ConfigFactory
import controllers.{AirlineTutorial, AirportUtil, GooglePhotoUtil, SearchUtil}
import models.PendingAction
import play.api.libs.json.{JsNumber, Json}
import websocket.chat.TriggerPing

import java.util.{Date, Timer, TimerTask}
import scala.collection.mutable

//Instead of maintaining a new actor connection whenever someone logs in, we will only maintain one connection between sim and web app, once sim finishes a cycle, it will send one message the the web app actor, and the web app actor will relay the message in an event stream, which is subscribed by each login section.
//
//For new login, the web app local actor will directly send one message to the remote actor, and the remote actor will in this case reply directly to the web app local actor - this is the ONLY time that the 2 talk directly

sealed class LocalActor(out : ActorRef, airlineId : Int) extends Actor {
  override def preStart() = {
    Broadcaster.subscribeToBroadcaster(self, airlineId)
    actorSystem.eventStream.subscribe(self, classOf[TriggerPing])
    actorSystem.eventStream.subscribe(self, classOf[(SimulationEvent, Any)])

    ActorCenter.remoteMainActor ! "getCycleInfo" //get cycle info once on start
  }

  def receive = {
    case Notification(message) =>
      //      println("going to send " + message + " back to the websocket")
      out ! message
    case (topic: SimulationEvent, payload: Any) => Some(topic).collect {
      case CycleCompleted(cycle, cycleEndTime) =>
        println(s"${self.path} Received cycle completed: $cycle")
        out ! Json.obj("messageType" -> "cycleCompleted", "cycle" -> cycle) //if a CycleCompleted is published to the stream, notify the out(websocket) of the cycle
        Broadcaster.checkPrompts(airlineId)
      case CycleInfo(cycle, fraction, cycleDurationEstimation) =>
        println(s"${self.path} Received cycle info on cycle: " + cycle)
        out ! Json.obj("messageType" -> "cycleInfo", "cycle" -> cycle, "fraction" -> fraction, "cycleDurationEstimation" -> cycleDurationEstimation)
    }
    case ping : TriggerPing =>
      //println(s"${new Date()} - ${self.path} trigger ping created at ${ping.creationDate}")
      out ! Json.obj("ping" -> "event")
    case BroadcastMessage(text) =>
      out ! Json.obj("messageType" -> "broadcastMessage", "message" -> text)
    case AirlineDirectMessage(airline, text) =>
      out ! Json.obj("messageType" -> "airlineMessage", "message" -> text)
    case AirlinePrompts(_, prompts) =>
      //println(s"$self get prompts")
      prompts.notices.foreach {
        case AirlineNotice(airline, notice, description) =>
          println(s"Sending notice $notice to $airline")
          notice.category match {
            case NoticeCategory.LEVEL_UP =>
              out ! Json.obj("messageType" -> "notice", "category" -> notice.category.toString, "id" -> notice.id, "level" -> notice.id, "description" -> description)
            case NoticeCategory.LOYALIST =>
              out ! Json.obj("messageType" -> "notice", "category" -> notice.category.toString, "id" -> notice.id, "level" -> notice.id, "description" -> description)

          }
      }
      prompts.tutorials.foreach {
        case AirlineTutorial(airline, tutorial) =>
          println(s"Sending tutorial $tutorial to $airline")
          out ! Json.obj("messageType" -> "tutorial", "category" -> tutorial.category, "id" -> tutorial.id)
      }
    case AirlinePendingActions(airline, pendingActions : List[PendingAction]) =>
      //println(s"$self get pending actions")
      out ! Json.obj("messageType" -> "pendingAction", "actions" -> Json.toJson(pendingActions.map(_.category.toString)))
    case any =>
      println("received " + any + " not handled")
  }

  override def postStop() = {
    Broadcaster.unsubscribeFromBroadcaster(self)
    actorSystem.eventStream.unsubscribe(self)

    println(self.path.toString + " stopped (post stop), unsubscribed from all event streams")
  }
}

class ResetTask(localActor : ActorRef, remoteActor : ActorSelection) extends TimerTask {
  override def run() : Unit = {
    println(s"${localActor.path} resubscribe due to ping timeout")
    remoteActor.tell("subscribe", localActor)
  }
}

//only 1 locally, fan out message to all local actors to reduce connections required
//also manage the broadcast actor
sealed class LocalMainActor(remoteActor : ActorSelection) extends Actor {
  //also create BroadcastActor
//  val broadcastActor = context.actorOf(Props(classOf[BroadcastActor]).withDispatcher("my-pinned-dispatcher"), "broadcast-actor")
//  context.watch(broadcastActor)

  val pingInterval = 60000 //how often do we check
  val resetTimeout = 10000
  var pendingResetTask : Option[ResetTask] = None
  val timer = new Timer()
  val configFactory = ConfigFactory.load()
  val bannerEnabled = if (configFactory.hasPath("bannerEnabled")) configFactory.getBoolean("bannerEnabled") else false

  override def receive = {
    case (topic: SimulationEvent, payload: Any) =>
      println(s"Local main actor received topic $topic, re-publishing to ${actorSystem}")
      Some(topic).collect {
        case CycleCompleted(cycle, cycleEndTime) =>
          println(s"${self.path} invalidating cache")
          MyWebSocketActor.lastSimulatedCycle = cycle
          AirlineCache.invalidateAll()
          AirportCache.invalidateAll()
          AirplaneOwnershipCache.invalidateAll()
          AirportUtil.refreshAirports()
          SearchUtil.refreshAlliances() //as sim might have deleted alliances
          if (bannerEnabled) {
            println("Banner is enabled. Refreshing banner on cycle complete")
            GooglePhotoUtil.refreshBanners()
          }
          println(s"${self.path} invalidated cache")
      }

      actorSystem.eventStream.publish(topic, payload) //relay to local event stream... since i don't know if I can subscribe to remote event stream...
    case Resubscribe(remoteActor) =>
      println(self.path.toString +  " Attempting to resubscribe")
      remoteActor ! "subscribe"
    case Terminated(actor) =>
      println(s"$actor is terminated!!")
//    case BroadcastWrapper(message) => {
//      broadcastActor ! message
//    }
    case KeepAlivePing =>
      remoteActor ! KeepAlivePing
      val resetTask = new ResetTask(self, remoteActor)
      timer.schedule(resetTask, resetTimeout)
      pendingResetTask = Some(resetTask)
    case KeepAlivePong => //diffuse the reset!
      println("Connection to sim is healthy!")
      pendingResetTask.foreach( _.cancel() )
    case unknown : Any => println(s"Unknown message for local main actor : $unknown")
  }



  override def preStart = {
    super.preStart()
    remoteActor ! "subscribe"
    timer.scheduleAtFixedRate(new TimerTask {
      override def run() : Unit = {
          self ! KeepAlivePing
      }
    }, 0, pingInterval)
  }

  override def postStop() = {
    println(self.path.toString + " local main actor stopped (post stop)")
    timer.cancel()
  }
}

sealed class ReconnectActor(remoteActor : ActorSelection) extends Actor {
  var disconnected = false

  override def preStart = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
    remoteActor ! KeepAlivePing() //establish connection
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
          remoteActor ! ReconnectPing()
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


object ActorCenter {
  val REMOTE_SYSTEM_NAME = "websocketActorSystem"
  val BRIDGE_ACTOR_NAME = "bridgeActor"
  implicit val system = actorSystem //ActorSystem("localWebsocketSystem")

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

//  def subscribe(f: (SimulationEvent, Any) => Option[Unit], subscriberId: String) = {
//    val props = Props(classOf[LocalActor], f)
//    val localSubscriber = system.actorOf(props, name = getLocalSubscriberName(subscriberId))
//    system.eventStream.subscribe(localSubscriber, classOf[(SimulationEvent, Any)])
//
//    println("Subscriber " + localSubscriber.path + " subscribed to system event stream")
//
//    //now get updated cycle info once
//    remoteMainActor.resolveOne()(Timeout(5000, TimeUnit.MILLISECONDS)).onComplete {
//      case Success(actor) => actor.!("getCycleInfo")(localSubscriber)
//      case Failure(exception) =>
//        println(s"Remote main actor is no longer found... $exception")
//    }
//
//  }


//  def unsubscribe(subscriberid : String) = {
//    system.actorSelection(system./(getLocalSubscriberName(subscriberid))).resolveOne()(Timeout(10, TimeUnit.SECONDS)).map {
//      actorRef =>
//        println("Unsubscribing " + actorRef.path)
//        system.eventStream.unsubscribe(actorRef)
//        actorRef ! PoisonPill
//        actorRef
//    }
//  }

  def getLocalSubscriberName(subscriberId : String) = {
    "local-subscriber-" + subscriberId
  }


}

case class RemoteActor(remoteActor : ActorSelection)
case class Resubscribe(remoteActor : ActorSelection)