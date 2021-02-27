package websocket

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.patson.model.Airline
import com.patson.model.notice.{AirlineNotice, Notice}
import com.patson.util.AirlineCache
import controllers.{AirlineTutorial, PromptUtil}
import play.api.libs.json.Json
import websocket.RemoteSubscribe.system

import scala.collection.mutable

case class BroadcastMessage(text : String)
case class AirlineMessage(airline : Airline, text : String)
case class Subscribe(subscriber : ActorRef, airline : Airline)

object BroadcastActor {
  private[this] val broadcastActor = system.actorOf(Props(classOf[BroadcastActor]), "broadcast-actor")
  def broadcastMessage(message : String): Unit = {
    broadcastActor ! BroadcastMessage(message)
  }
  def sendMessage(airline : Airline, message : String) = {
    broadcastActor ! AirlineMessage(airline, message)
  }

  def subscribe(subscriber : ActorRef, airline : Airline) = {
    broadcastActor ! Subscribe(subscriber, airline)
  }

  def checkPrompts(airlineId : Int) = {
    val airline = AirlineCache.getAirline(airlineId).get
    val prompts = PromptUtil.getPrompts(airline)
    prompts.notices.foreach(broadcastActor ! _)
    prompts.tutorials.foreach(broadcastActor ! _)
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

