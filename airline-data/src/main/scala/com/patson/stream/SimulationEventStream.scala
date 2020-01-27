package com.patson.stream

import akka.actor._
import com.patson.MainSimulation
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.{ListBuffer, Set}

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
    var previousCycleStartTime : Long = 0
    var cycleDurationHistory = ListBuffer[Long]()
    var cycleDurationAverage : Long = 0
    var cycleCount : Int = 0
    val MAX_DURATION_SAMPLE = 10

    //keep stats of all the cycles so far

    def receive = {
      case "subscribe" =>
        println("subscribing actor " + sender().path)
        var elapsedFraction = (System.currentTimeMillis() - previousCycleStartTime).toDouble / cycleDurationAverage
        if (elapsedFraction > 1) { //if this time is slower than average, it could be bigger than 1
          elapsedFraction = 1
        }
        sender() ! (CycleInfo(currentCycle, elapsedFraction, cycleDurationAverage), None)
        registeredActors += sender()
      case "unsubscribe" =>
        println("unsubcribing actor " + sender().path)
        registeredActors -= sender()
      case (topic: SimulationEvent, payload: Any) =>
        topic match {
          case CycleStart(cycle, newCycleStartTime) => //notified by the simulation process that a cycle has started
            currentCycle = cycle
            if (cycleCount > 0) { //with previous record, calculate the average then
              val durationSinceLastCycle = newCycleStartTime - previousCycleStartTime

              cycleDurationHistory.append(durationSinceLastCycle)
              if (cycleDurationHistory.length > MAX_DURATION_SAMPLE) { //drop the first record
                cycleDurationHistory = cycleDurationHistory.drop(1)
              }

              cycleDurationAverage = cycleDurationHistory.sum / cycleDurationHistory.length
            }
            previousCycleStartTime = newCycleStartTime
            cycleCount += 1
            registeredActors.foreach { registeredActor => //now notify the browser client of updated CycleInfo
              val message = CycleInfo(currentCycle, 0, cycleDurationAverage)
              println("Bridge actor: forwarding " + message + " back to " + registeredActor.path)
              registeredActor ! (message, None) //send to actors on the airline-web side
            }
          case cycleCompleted: CycleCompleted =>
            registeredActors.foreach { registeredActor => //now notify the browser client of updated CycleInfo
              println("Bridge actor: forwarding " + cycleCompleted + " back to " + registeredActor.path)
              registeredActor ! (cycleCompleted, None)
            }

          case _ => //nothing
        }
        
        println("Bridge actor: received from simulation that " + topic)

      case "ping" => //do nothing
      case _ => println("UNKNOWN message")
      
    }
  }
}


class SimulationEvent
case class CycleCompleted(cycle : Int) extends SimulationEvent //main simulation send this, this will be relayed directly to client
case class CycleStart(cycle: Int, cycleStartTime : Long) extends SimulationEvent //main simulation send this, this will NOT be relay back to client
case class CycleInfo(cycle: Int, fraction : Double, cycleDurationEstimation : Long) extends SimulationEvent  //bridge actor convert a CycleStart into CycleInfo and send back to client