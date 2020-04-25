package websocket.chat

import java.time.Duration
import java.util.concurrent.{ConcurrentHashMap, Executor, Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import akka.stream.ActorMaterializer

import scala.collection.mutable.{ListBuffer, Map, Queue}
import com.patson.model.User
import com.patson.data.AllianceSource
import javax.inject.Inject
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext}

// our domain message protocol
case class Join(user : User, lastMessageId : Option[Long])
case class Leave(user : User)
case class TriggerPing()
class Message
final case class ClientSentMessage(text: String)

final case class IncomingMessage(message : ChatMessage, allianceId : Option[Int])
final case class OutgoingMessage(id: Long, timestamp : Long, message : ChatMessage, allianceId : Option[Int])



/**
 *  a single actor that handles when a ClientActor joins or leaves
 *  
 *  When a message is received from a ClientActor it would notify this actor, and this actor will send it out to all the corresponding subscribers (ClientActors)
 */

class ChatControllerActor extends Actor {
  // initial message-handling behavior
  val logger = Logger(this.getClass)
  val maxMessagePerRoom = 100
  val ec: ExecutionContext = ExecutionContext.global

  val generalMessageHistory = Queue[OutgoingMessage]()
  val penaltyBoxMessageHistory = Queue[OutgoingMessage]()
  val allianceMessageHistory = Map[Int, Queue[OutgoingMessage]]()
  val clientActors = mutable.LinkedHashSet[ActorRef]()

  def receive = process(Set.empty)

  val messageIdCounter = new AtomicLong(0)

  context.system.scheduler.schedule(Duration.ZERO, Duration.ofSeconds(10), self, TriggerPing, ec, self)

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join(user, lastMessageId) => {
      // replaces message-handling behavior by the new one
      //context become process(subscribers + sender)

      clientActors += sender
      context.watch(sender)
      ChatControllerActor.addActiveUser(sender, user)
      //You can turn these loggers off if needed
      logger.info("Chat socket connected " + sender + " for user " + user.userName + " current active sessions : " + clientActors.size + " unique users : " + ChatControllerActor.getActiveUsers().size)


      // resend the Archived Message
      if (!user.isChatBanned) {
        generalMessageHistory.filter(message => lastMessageId.isEmpty || message.id > lastMessageId.get).foreach(sender ! _)
      } else {
        penaltyBoxMessageHistory.filter(message => lastMessageId.isEmpty || message.id > lastMessageId.get).foreach(sender ! _)
      }

      user.getAccessibleAirlines().foreach { airline =>
        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          allianceMessageHistory.get(allianceMember.allianceId).foreach { archivedMessages =>
            archivedMessages.filter(message => lastMessageId.isEmpty || message.id > lastMessageId.get).foreach(sender ! _)
          }
        }
      }
    }

    //    case Leave => {
    //      context become process(subscribers - sender)
    //	  //You can turn these loggers off if needed
    //	    logger.info("Chat socket disconnected " + sender)
    //    }
    case Terminated(chatClientActor) => {
      context.unwatch(chatClientActor)
      clientActors -= chatClientActor
      ChatControllerActor.removeActiveUser(chatClientActor)
    }

    case IncomingMessage(chatMessage, allianceRoomIdOption) => {
      val processedMessage = processMessage(chatMessage)
      val outMessage = OutgoingMessage(messageIdCounter.incrementAndGet(), System.currentTimeMillis(), processedMessage, allianceRoomIdOption)

      //put message into history and send to subscribers
      allianceRoomIdOption match {
        case None => {
          if (!chatMessage.user.isChatBanned) { //only put in main chat room if user is not banned for chats
            generalMessageHistory.enqueue(outMessage)
          } else {
            println(s"sending message ${chatMessage.text} from ${chatMessage.airline.name} user ${chatMessage.user.userName} to penalty box only")
          }
          penaltyBoxMessageHistory.enqueue(outMessage) //always put it in penalty box - penalty box can see the outside worlds

          while (generalMessageHistory.size > maxMessagePerRoom) {
            generalMessageHistory.dequeue()
          }
          while (penaltyBoxMessageHistory.size > maxMessagePerRoom) {
            penaltyBoxMessageHistory.dequeue()
          }

          clientActors.foreach {
            _ ! outMessage
          }
        }
        case Some(allianceRoomId) =>
          val messageQueue = allianceMessageHistory.getOrElseUpdate(allianceRoomId, Queue[OutgoingMessage]())
          messageQueue.enqueue(outMessage)
          while (messageQueue.size > maxMessagePerRoom) {
            messageQueue.dequeue()
          }

          clientActors.foreach {
            _ ! outMessage
          } //not the best, as we should be able to filter based on alliance Id and banned user here
      }
      //You can turn these loggers off if needed
      //Logger.info("Message:" + msg.text)
    }
    case TriggerPing => { //ping all clients, since play does NOT have ping support yet...https://github.com/playframework/playframework/issues/3861
      clientActors.foreach(_ ! TriggerPing)
    }
  }

  def processMessage(message: ChatMessage): ChatMessage = {
    //check commands
    ChatControllerActor.commands.foreach { command =>
      if (command.hasPermission(message) && command.isCommand(message)) {
        return command.execute(message) //only match first command
      }
    }
    message
  }
}



abstract class ChatCommand(val command : String) {
  val commandToken = "/" + command
  def execute(message: ChatMessage) : ChatMessage
  val isCommand = (message : ChatMessage) => message.text.contains(commandToken)
  val hasPermission : (ChatMessage => Boolean)

}


object ImgCommand extends ChatCommand("img") {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val ws = StandaloneAhcWSClient()
  val MAX_MESSAGE_SIZE = 8 * 1024 * 1024 //8M
  val TIME_OUT = 5 //wait max 5 seconds
  implicit val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))
  override def execute(message: ChatMessage): ChatMessage = {
    val commandIndex = message.text.indexOf(commandToken)

    val argument = message.text.substring(commandIndex + commandToken.length).trim
    try {
      val mappedFuture = ws.url(argument).withMethod("GET").stream().map { response =>
        if (response.status == play.api.http.Status.OK) {
          response.headers.get("Content-Type") match {
            case Some(Seq(contentType)) => {
              if (contentType.startsWith("image")) {
                response.headers.get("Content-Length") match {
                  case Some(Seq(length)) => {
                    if (length.toLong > MAX_MESSAGE_SIZE) {
                      message.copy(text = s"(Image is too large : ${length.toLong / 1024} KB) ${argument}")
                    } else {
                      message
                    }
                  }
                  case None =>
                    message.copy(text = s"(Image length not defined) : $argument")
                }
              } else {
                message.copy(text = s"(Link is not an image) : $argument")
              }
            }
            case None =>
              message.copy(text = s"(Link is not an image) : $argument")
          }
        } else {
          message.copy(text = s"(Image not found) : $argument")
        }
      }
      Await.result(mappedFuture, scala.concurrent.duration.Duration(TIME_OUT, TimeUnit.SECONDS))
    } catch {
      case e : Exception => {
        println(s"Failed to retrieve img for $message : ${e.getMessage}")
        message.copy(text = s"(Error loading image) : $argument")
      }
    }
  }
  override val hasPermission = (message : ChatMessage) => {
    message.user.level > 0
  }
}





object ChatControllerActor {
  import scala.jdk.CollectionConverters._
  val activeUsers = new ConcurrentHashMap[ActorRef, User]().asScala
  val commands = List(ImgCommand)

  def getActiveUsers() : Set[User] = {
    activeUsers.values.toSet
  }

  def addActiveUser(sender : ActorRef, user : User) : Unit = {
    activeUsers.put(sender, user)
  }

  def removeActiveUser(sender : ActorRef) : Option[User] = {
    activeUsers.remove(sender)
  }
}
