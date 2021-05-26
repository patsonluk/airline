package com.patson.stream

import akka.actor._
import com.patson.MainSimulation
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Set}

object SimulationEventStream{
  val config = ConfigFactory.load()
  implicit val system = ActorSystem(REMOTE_SYSTEM_NAME, config.getConfig(REMOTE_SYSTEM_NAME).withFallback(config))
  
  //spin off a remote actor
  var registeredActor = Set[ActorRef]()
  val mainActor = system.actorOf(Props[BridgeActor], BRIDGE_ACTOR_NAME) //this actor receives subscriptions (from client) and also subscribe to the event stream and forward it back to subscribers from client
  
  system.eventStream.subscribe(mainActor, classOf[(SimulationEvent, Any)])
  
  println("registered the mainActor to the event stream!")
  
  def publish(topic: SimulationEvent, payload: Any) {
    system.eventStream.publish(topic, payload)
  }
  
  
  class BridgeActor extends Actor {
    var currentCycle : Int = 0
    var previousCycleEndTime : Long = 0
    var cycleDurationHistory = ListBuffer[Long]()
    var cycleDurationAverage : Long = 0
    var cycleCount : Int = 0
    val MAX_DURATION_SAMPLE = 10

    //keep stats of all the cycles so far

//    Instead of maintaining a new actor connection whenever someone logs in, we will only maintain one connnection between sim and web app, once sim finishes a cycle, it will send one message the the web app actor, and the web app actor will relay the message in an event stream, which is subscribed by each login section.
//
//      For new login, the web app local actor will directly send one message to the remote actor, and the remote actor will in this case reply directly to the web app local actor - this is the ONLY time that the 2 talks directly
    def receive = {
      case "subscribe" =>
        //only allow one subscriber (the web application actor) for now
        System.out.println(s"$sender() subscribed")
        registeredActor = Set(sender())

//      case "unsubscribe" =>
//        println("unsubscribing actor " + sender().path)
//        registeredActors -= sender()
//        context.unwatch(sender())
//        sender() ! PoisonPill

      case "getCycleInfo" =>
        var elapsedFraction = (System.currentTimeMillis() - previousCycleEndTime).toDouble / cycleDurationAverage
        if (elapsedFraction > 1) { //if this time is slower than average, it could be bigger than 1
          elapsedFraction = 1
        }
        sender() ! (CycleInfo(currentCycle, elapsedFraction, cycleDurationAverage), None)
      case Terminated(actor) =>
        println("Watched actor is terminated " + sender().path)
        context.unwatch(actor)
        registeredActor = Set.empty
      case (topic: SimulationEvent, payload: Any) =>
        topic match {
          case CycleStart(cycle, newCycleStartTime) => //notified by the simulation process that a cycle has started
            currentCycle = cycle
          case cycleComplete : CycleCompleted =>
            val CycleCompleted(cycle, cycleEndTime) = cycleComplete
            if (cycleCount > 0) { //with previous record, calculate the average then
              val durationSinceLastCycle = cycleEndTime - previousCycleEndTime

              cycleDurationHistory.append(durationSinceLastCycle)
              if (cycleDurationHistory.length > MAX_DURATION_SAMPLE) { //drop the first record
                cycleDurationHistory = cycleDurationHistory.drop(1)
              }

              cycleDurationAverage = cycleDurationHistory.sum / cycleDurationHistory.length
            }
            previousCycleEndTime = cycleEndTime
            cycleCount += 1

            registeredActor.foreach { registeredActor => //now notify the browser client of updated CycleInfo
              println("Bridge actor: forwarding " + cycleComplete + " back to " + registeredActor.path)
              registeredActor ! (cycleComplete, None)
            }

            registeredActor.foreach { registeredActor => //now notify the browser client of updated CycleInfo
              val message = CycleInfo(currentCycle, 0, cycleDurationAverage)
              println("Bridge actor: forwarding " + message + " back to " + registeredActor.path)
              registeredActor ! (message, None) //send to actors on the airline-web side
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
case class CycleCompleted(cycle : Int, cycleEndTime : Long) extends SimulationEvent //main simulation send this, this will be relayed directly to client
case class CycleStart(cycle: Int, cycleStartTime : Long) extends SimulationEvent //main simulation send this, this will NOT be relay back to client
case class CycleInfo(cycle: Int, fraction : Double, cycleDurationEstimation : Long) extends SimulationEvent  //bridge actor convert a CycleStart into CycleInfo and send back to client

