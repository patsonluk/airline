package websocket

import akka.actor.{Actor, ActorPath, ActorRef, Props, Terminated}
import akka.util.Timeout
import com.patson.model.{Airline, Alert}
import com.patson.model.notice.{AirlineNotice, Notice}
import com.patson.util.AirlineCache
import controllers.{AirlineTutorial, PendingActionUtil, PromptUtil}
import models.PendingAction
import play.api.libs.json.Json
import websocket.ActorCenter.{localMainActor, system}

import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.util.{Failure, Success}

case class BroadcastMessage(text : String)
case class AirlineMessage(airline : Airline, text : String)
case class BroadcastSubscribe(subscriber : ActorRef, airline : Airline, remoteAddress: String) {
  val creationTime = Calendar.getInstance().getTime
}
case class AirlinePendingActions(airline : Airline, actions : List[PendingAction])
case class BroadcastWrapper(message : Any)

object BroadcastActor {
  def broadcastMessage(message : String): Unit = {
    localMainActor ! BroadcastWrapper(BroadcastMessage(message))
  }
  def sendMessage(airline : Airline, message : String) = {
    localMainActor ! BroadcastWrapper(AirlineMessage(airline, message))
  }

  def subscribeToBroadcaster(subscriber : ActorRef, airline : Airline, remoteAddress : String) = {
    localMainActor ! BroadcastWrapper(BroadcastSubscribe(subscriber, airline, remoteAddress))
  }

  def checkPrompts(airlineId : Int) = {

    val airline = AirlineCache.getAirline(airlineId).get
    val prompts = PromptUtil.getPrompts(airline)
    prompts.notices.foreach(localMainActor ! BroadcastWrapper(_))
    prompts.tutorials.foreach(localMainActor ! BroadcastWrapper(_))
    localMainActor ! BroadcastWrapper(AirlinePendingActions(airline, PendingActionUtil.getPendingActions(airline))) //should send empty list if none, so front end can clear
  }
}


class BroadcastActor() extends Actor {
  val airlineActors = mutable.LinkedHashSet[(ActorRef, Airline)]()

  override def receive = {
    case message : BroadcastMessage => {
      println(s"[ACTOR] Broadcasting message $message")
      airlineActors.map(_._1).foreach( actor => actor ! message)
      println(s"Finished Broadcasting message $message")
    }
    case message : AirlineMessage => {
      println(s"[ACTOR] Sending airline message $message")
      airlineActors.find(_._2.id == message.airline.id).foreach( actor => actor._1 ! message)
      println(s"Sent airline message $message")
    }
    case notice : AirlineNotice => {
      println(s"[ACTOR] Sending airline notice $notice")
      airlineActors.find(_._2.id == notice.airline.id).foreach( actor => actor._1 ! notice)
      println(s"Sent airline notice $notice")
    }
    case tutorial : AirlineTutorial => {
      println(s"[ACTOR] Sending airline tutorial $tutorial")
      airlineActors.find(_._2.id == tutorial.airline.id).foreach( actor => actor._1 ! tutorial)
      println(s"Sent airline tutorial $tutorial")
    }
    case airlinePendingActions : AirlinePendingActions => {
      println(s"[ACTOR] Sending airline pendingActions $airlinePendingActions")
      airlineActors.find(_._2.id == airlinePendingActions.airline.id).foreach( actor => actor._1 ! airlinePendingActions)
      println(s"Sent airline pendingActions $airlinePendingActions")
    }
    case message : BroadcastSubscribe => {
      println(s"[ACTOR] Adding subscriber to broadcast actor $message")
      airlineActors.add((message.subscriber, message.airline))
      context.watch(message.subscriber)
      println(s"${Calendar.getInstance().getTime} : Joining $message. Active broadcast subscribers ${airlineActors.size} of remote address ${message.remoteAddress} message creation time ${message.creationTime}" )
    }

    case Terminated(clientActor) => {
      println(s"[ACTOR] Unwatching $clientActor")
      context.unwatch(clientActor)
      airlineActors.find(_._1 == clientActor).foreach(airlineActors.remove(_))
      println(s"Unwatched $clientActor")
    }

  }
}

