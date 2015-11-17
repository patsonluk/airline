package websocket

import akka.actor._
import scala.collection.mutable.Map
import scala.util.Random
import com.patson.data.AirlineSource

object BackgroundActor {
  val backgroundJob = new BackgroundJob()
  backgroundJob.start()
}

class BackgroundActor() extends Actor {
  def receive = {
    case msg: String =>
      sender() ! ("I received your message: " + msg + " but im not going to do anything!")
    case RegisterToBackground(airlineId) =>
      println("registering " + sender() + " of airline id " + airlineId)
      BackgroundActor.backgroundJob.registerActor(sender(), airlineId)
    case RemoveFromBackground =>
      println("unregistering " + sender())
      BackgroundActor.backgroundJob.removeActor(sender()) 
  }
}

class BackgroundJob {
  val registeredActors = Map[ActorRef, Int]()
  def start() = {
    val thread = new Thread {
      override def run() {
        while (true) {
          registeredActors.foreach { 
            case(registeredActor, airlineId) =>
              AirlineSource.loadAirlineById(airlineId, true) match {
                case Some(airline) => registeredActor ! Notification(airline.airlineInfo.balance.toString)
                case None =>
              }
           }
          Thread.sleep(30000)
        }
      }
    }
    thread.start()
  }
  
  def registerActor(actor : ActorRef, airlineId : Int) = {
    registeredActors.put(actor, airlineId)
  }
  
  def removeActor(actor : ActorRef) = {
    registeredActors.remove(actor)
  }
}