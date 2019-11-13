package com.patson.stream

import akka.actor._
import com.patson.MainSimulation
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.Set

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
    var currentCycle : Int = 0
    var cycleStartTime : Long = 0
    
    def receive = {
      case "subscribe" =>
        println("subcribing actor " + sender().path)
        val elapsedFraction = (System.currentTimeMillis() - cycleStartTime).toDouble / (MainSimulation.CYCLE_DURATION * 1000)
        sender() ! (CycleInfo(currentCycle, elapsedFraction), None)
        registeredActors += sender()
      case "unsubscribe" =>
        println("unsubcribing actor " + sender().path)
        registeredActors -= sender()
      case (topic: SimulationEvent, payload: Any) =>
        topic match {
          case CycleStart(cycle) =>
            currentCycle = cycle
            cycleStartTime = System.currentTimeMillis()
          case _ => //nothing
        }
        
        println("received " + topic)
        registeredActors.foreach { registeredActor =>
          println("forwarding message back to " + registeredActor.path)
          registeredActor ! (topic, payload) 
        }
      case "ping" => //do nothing
      case _ => println("UNKNOWN message")
      
    }
  }
}


class SimulationEvent
case class CycleCompleted(cycle : Int) extends SimulationEvent
case class CycleStart(cycle: Int) extends SimulationEvent
case class CycleInfo(cycle: Int, fraction : Double) extends SimulationEvent