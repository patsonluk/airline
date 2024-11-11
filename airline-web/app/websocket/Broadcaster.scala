package websocket

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.event.{EventBus, LookupClassification}
import com.patson.model.Airline
import com.patson.util.AirlineCache
import controllers.{PendingActionUtil, PromptUtil}
import models.PendingAction

import java.util.Calendar

case class BroadcastMessage(text : String)
abstract class AirlineMessage(airline : Airline) {
  val getAirline = airline
}
case class AirlineDirectMessage(airline: Airline, text : String) extends AirlineMessage(airline)
case class AirlinePendingActions(airline : Airline, actions : List[PendingAction]) extends AirlineMessage(airline)
case class AirlinePrompts(airline : Airline, prompts : controllers.Prompts) extends AirlineMessage(airline)


case class BroadcastSubscribe(subscriber : ActorRef, airline : Airline, remoteAddress: String) {
  val creationTime = Calendar.getInstance().getTime
}

//case class BroadcastWrapper(message : Any) {
//  val classType = message.getClass.getName
//}

class BroadcastEventBus extends EventBus with LookupClassification {
  type Event = BroadcastMessage
  type Classifier = String
  type Subscriber = ActorRef

  // is used for extracting the classifier from the incoming events
  override protected def classify(event: Event): Classifier = ""

  // will be invoked for each event for all subscribers which registered themselves
  // for the event’s classifier
  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  // must define a full order over the subscribers, expressed as expected from
  // `java.lang.Comparable.compare`
  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int =
    a.compareTo(b)

  // determines the initial size of the index data structure
  // used internally (i.e. the expected number of different classifiers)
  override protected def mapSize(): Int = 128
}

class AirlineEventBus extends EventBus with LookupClassification {
  type Event = AirlineMessage
  type Classifier = Int
  type Subscriber = ActorRef

  // is used for extracting the classifier from the incoming events
  override protected def classify(event: Event): Classifier = event.getAirline.id

  // will be invoked for each event for all subscribers which registered themselves
  // for the event’s classifier
  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  // must define a full order over the subscribers, expressed as expected from
  // `java.lang.Comparable.compare`
  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int =
    a.compareTo(b)

  // determines the initial size of the index data structure
  // used internally (i.e. the expected number of different classifiers)
  override protected def mapSize(): Int = 128
}



object Broadcaster {
  val broadcastEventBus = new BroadcastEventBus()
  val airlineEventBus = new AirlineEventBus()

  def broadcastMessage(message : String): Unit = {
    broadcastEventBus.publish(BroadcastMessage(message))
  }
  def sendMessage(airline : Airline, message : String) = {
    airlineEventBus.publish(AirlineDirectMessage(airline, message))
  }

  def subscribeToBroadcaster(subscriber : ActorRef, airlineId : Int) = {
    //localMainActor ! BroadcastWrapper(BroadcastSubscribe(subscriber, airline, remoteAddress))
    broadcastEventBus.subscribe(subscriber, "")
    airlineEventBus.subscribe(subscriber, airlineId)
  }

  def checkPrompts(airlineId : Int) = {

    val airline = AirlineCache.getAirline(airlineId).get
    val prompts = PromptUtil.getPrompts(airline)
//    prompts.notices.foreach(localMainActor ! BroadcastWrapper(_))
//    prompts.tutorials.foreach(localMainActor ! BroadcastWrapper(_))
    airlineEventBus.publish(AirlinePrompts(airline, prompts))
    airlineEventBus.publish(AirlinePendingActions(airline, PendingActionUtil.getPendingActions(airline))) //should send empty list if none, so front end can clear
  }

  def unsubscribeFromBroadcaster(subscribe: ActorRef) = {
    broadcastEventBus.unsubscribe(subscribe)
    airlineEventBus.unsubscribe(subscribe)
  }
}


//class Broadcaster() extends Actor {
//  def tag = "[ACTOR] " + Thread.currentThread().toString
//
//  override def receive = {
//    case message : BroadcastMessage => {
//      println(s"$tag Broadcasting message $message")
//      airlineActors.map(_._1).foreach( actor => actor ! message)
//      println(s"$tag Finished Broadcasting message $message")
//    }
//    case message : AirlineMessage => {
//      println(s"$tag Sending airline message $message")
//      airlineActors.find(_._2.id == message.airline.id).foreach( actor => actor._1 ! message)
//      println(s"$tag Sent airline message $message")
//    }
//    case notice : AirlineNotice => {
//      println(s"$tag Sending airline notice $notice")
//      airlineActors.find(_._2.id == notice.airline.id).foreach( actor => actor._1 ! notice)
//      println(s"$tag Sent airline notice $notice")
//    }
//    case tutorial : AirlineTutorial => {
//      println(s"$tag Sending airline tutorial $tutorial")
//      airlineActors.find(_._2.id == tutorial.airline.id).foreach( actor => actor._1 ! tutorial)
//      println(s"$tag Sent airline tutorial $tutorial")
//    }
//    case airlinePendingActions : AirlinePendingActions => {
//      println(s"$tag Sending airline pendingActions $airlinePendingActions")
//      airlineActors.find(_._2.id == airlinePendingActions.airline.id).foreach( actor => actor._1 ! airlinePendingActions)
//      println(s"$tag Sent airline pendingActions $airlinePendingActions")
//    }
//    case message : BroadcastSubscribe => {
//      println(s"$tag Adding subscriber to broadcast actor $message")
//      airlineActors.add((message.subscriber, message.airline))
//      context.watch(message.subscriber)
//      println(s"$tag ${Calendar.getInstance().getTime} : Joining $message. Active broadcast subscribers ${airlineActors.size} of remote address ${message.remoteAddress} message creation time ${message.creationTime}" )
//    }
//
//    case Terminated(clientActor) => {
//      println(s"$tag Unwatching $clientActor")
//      context.unwatch(clientActor)
//      airlineActors.find(_._1 == clientActor).foreach(airlineActors.remove(_))
//      println(s"$tag Unwatched $clientActor")
//    }
//
//  }
//}

