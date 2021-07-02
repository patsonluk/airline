package websocket

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.util.Timeout
import com.patson.model.{Airline, Alert}
import com.patson.model.notice.{AirlineNotice, Notice}
import com.patson.util.AirlineCache
import controllers.{AirlineTutorial, PendingActionUtil, PromptUtil}
import models.PendingAction
import play.api.libs.json.Json
import websocket.RemoteSubscribe.system

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.util.{Failure, Success}

case class BroadcastMessage(text : String)
case class AirlineMessage(airline : Airline, text : String)
case class Subscribe(subscriber : ActorRef, airline : Airline)
case class AirlinePendingActions(airline : Airline, actions : List[PendingAction])

object BroadcastActor {
//  private[this] val broadcastActor = system.actorOf(Props(classOf[BroadcastActor]), "broadcast-actor")


  def broadcastMessage(message : String): Unit = {
    sendMessageToBroadcaster(BroadcastMessage(message))
  }
  def sendMessage(airline : Airline, message : String) = {
    sendMessageToBroadcaster(AirlineMessage(airline, message))
  }

  def subscribe(subscriber : ActorRef, airline : Airline) = {
    sendMessageToBroadcaster(Subscribe(subscriber, airline))
  }
  val counter = new AtomicInteger(0)
  var actorName = "broadcast-actor-" + counter.getAndIncrement()
  var actorPath = system.actorOf(Props(classOf[BroadcastActor]), actorName).path
  println(s"created new broadcast actor $actorName with path $actorPath")
  def sendMessageToBroadcaster(message : Any) = {
    system.actorSelection(actorPath).resolveOne()(Timeout(5000, TimeUnit.MILLISECONDS)).onComplete {
      case Success(actor) =>
        actor ! message
      case Failure(ex) =>
        actorName = "broadcast-actor-" + counter.getAndIncrement()
        val actor = system.actorOf(Props(classOf[BroadcastActor]), actorName)
        println(s"Recreated new broadcast actor $actorName with path $actorPath")
        actorPath = actor.path
        actor ! message
    }
  }

  def checkPrompts(airlineId : Int) = {

    val airline = AirlineCache.getAirline(airlineId).get
    val prompts = PromptUtil.getPrompts(airline)
    prompts.notices.foreach(sendMessageToBroadcaster)
    prompts.tutorials.foreach(sendMessageToBroadcaster)
    sendMessageToBroadcaster(AirlinePendingActions(airline, PendingActionUtil.getPendingActions(airline))) //should send empty list if none, so front end can clear
  }

}


class BroadcastActor() extends Actor {
  val airlineActors = mutable.LinkedHashSet[(ActorRef, Airline)]()

  override def receive = {
    case message : BroadcastMessage => {
      airlineActors.map(_._1).foreach( actor => actor ! message)
    }
    case message : AirlineMessage => {
      airlineActors.find(_._2.id == message.airline.id).foreach( actor => actor._1 ! message)
    }
    case notice : AirlineNotice => {
      airlineActors.find(_._2.id == notice.airline.id).foreach( actor => actor._1 ! notice)
    }
    case tutorial : AirlineTutorial => {
      airlineActors.find(_._2.id == tutorial.airline.id).foreach( actor => actor._1 ! tutorial)
    }
    case airlinePendingActions : AirlinePendingActions => {
      airlineActors.find(_._2.id == airlinePendingActions.airline.id).foreach( actor => actor._1 ! airlinePendingActions)
    }
    case message : Subscribe => {
      airlineActors.add((message.subscriber, message.airline))
      context.watch(message.subscriber)
      println(s"Joining $message. Active broadcast subscribers ${airlineActors.size}" )
    }

    case Terminated(clientActor) => {
      context.unwatch(clientActor)
      airlineActors.find(_._1 == clientActor).foreach(airlineActors.remove(_))
    }

  }
}

