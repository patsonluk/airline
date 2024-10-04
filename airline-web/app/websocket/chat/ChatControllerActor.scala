package websocket.chat

import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}
import org.apache.pekko.actor._
import org.apache.pekko.stream.ActorMaterializer
import com.patson.data.{AllianceSource, ChatSource}
import com.patson.model.chat.ChatMessage
import com.patson.model.{Airline, AllianceRole, User}
import play.api.Logger
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import java.util.Date
import scala.collection.mutable
import scala.collection.mutable.{Map, Queue}
import scala.concurrent.{Await, ExecutionContext}

// our domain message protocol
case class Join(user : User)
case class Leave(user : User)
case class TriggerPing() {
  val creationDate = new Date()
}
class Message
final case class ClientSentMessage(text: String)

final case class IncomingMessage(message : ChatMessage)
final case class OutgoingMessage(message : ChatMessage, var latest : Boolean)
final case class SessionStart(lastMessageId : Long, unreadMessageCount: Int, messages : List[ChatMessage])
final case class PreviousMessagesRequest(airline : Airline, previousFirstMessageId : Long, roomId : Int)
final case class PreviousMessagesResponse(previousMessages : List[ChatMessage])



/**
 *  a single actor that handles when a ClientActor joins or leaves
 *  
 *  When a message is received from a ClientActor it would notify this actor, and this actor will send it out to all the corresponding subscribers (ClientActors)
 */

class ChatControllerActor extends Actor {
  // initial message-handling behavior
  val logger = Logger(this.getClass)
  val maxMessagePerRoom = 1000
  val ec: ExecutionContext = ExecutionContext.global

  val generalMessageHistory = Queue[ChatMessage]()
  val penaltyBoxMessageHistory = Queue[ChatMessage]()
  val allianceMessageHistory = Map[Int, Queue[ChatMessage]]()

  val INIT_MESSAGE_COUNT = 5000
  val MESSAGE_BATCH_COUNT = 20

  initMessages()

  val clientActors = mutable.LinkedHashSet[ActorRef]()

  def receive = process(Set.empty)

  val messageIdCounter = new AtomicLong(0)

  context.system.scheduler.schedule(Duration.ZERO, Duration.ofMinutes(1), self, TriggerPing, ec, self)



  def initMessages() = {
    ChatSource.loadLatestChatMessagesWithLimit(INIT_MESSAGE_COUNT).groupBy(_.roomId).foreach {
      case((roomId, messages)) =>
        if (roomId == GENERAL_ROOM_ID) {
          val (bannedMessages, generalMessages) = messages.partition(_.user.isChatBanned)
          generalMessageHistory.addAll(generalMessages)
          penaltyBoxMessageHistory.addAll(bannedMessages)
        } else { //alliance
          val queue = Queue[ChatMessage]()
          queue.addAll(messages)
          allianceMessageHistory.put(roomId, queue)
        }
    }
  }

  def getSessionStartMessage(generalQueue : Queue[ChatMessage], allianceQueue : List[ChatMessage], lastMessageIdOption :Option[Long]) : SessionStart =  {
    val generalMessages = generalQueue.takeRight(MESSAGE_BATCH_COUNT).toList
    val unreadGeneralMessageCount = lastMessageIdOption match {
      case Some(lastMessageId) => generalQueue.count(_.id > lastMessageId)
      case None => Math.min(generalQueue.length, MESSAGE_BATCH_COUNT)
    }
    val allianceMessages = allianceQueue.takeRight(MESSAGE_BATCH_COUNT)
    val unreadAllianceMessageCount = lastMessageIdOption match {
      case Some(lastMessageId) => allianceQueue.count(_.id > lastMessageId)
      case None => Math.min(allianceQueue.length, MESSAGE_BATCH_COUNT)
    }

    val messages = (generalMessages ++ allianceMessages).sortBy(_.id)
    SessionStart(lastMessageIdOption.getOrElse(if (generalMessages.isEmpty) 0 else generalMessages(0).id), unreadGeneralMessageCount + unreadAllianceMessageCount, messages)
  }

  def buildPreviousMessagesResponse(airline : Airline, previousFirstMessageId : Long, roomId : Int) : PreviousMessagesResponse = {
    val messageSource : List[ChatMessage] =
      if (roomId == GENERAL_ROOM_ID) {
        generalMessageHistory.toList
      } else {
        AllianceSource.loadAllianceMemberByAirline(airline) match {
          case Some(allianceMember) => allianceMessageHistory.get(allianceMember.allianceId) match {
            case Some(allianceMessageHistory) => allianceMessageHistory.toList
            case None => List.empty[ChatMessage]
          }
          case None => List.empty[ChatMessage]
        }
      }

    val messageMarker = messageSource.lastIndexWhere(_.id < previousFirstMessageId)
    val previousMessages =
      if (messageMarker != -1) {
        messageSource.splitAt(messageMarker + 1)._1.takeRight(MESSAGE_BATCH_COUNT)
      } else {
        List.empty[ChatMessage]
      }
    PreviousMessagesResponse(previousMessages.sortBy(_.id))
  }

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join(user) => {
      // replaces message-handling behavior by the new one
      //context become process(subscribers + sender)

      val lastMessageIdOption = ChatSource.getLastChatId(user.id)

      clientActors += sender
      context.watch(sender)
      ChatControllerActor.addActiveUser(sender, user)
      //You can turn these loggers off if needed
      logger.info("Chat socket connected " + sender + " for user " + user.userName + " current active sessions : " + clientActors.size + " unique users : " + ChatControllerActor.getActiveUsers().size)

      // resend the Archived Message
      val allianceArchivedMessages =
        user.getAccessibleAirlines().flatMap { airline =>
        AllianceSource.loadAllianceMemberByAirline(airline).flatMap { allianceMember =>
          if (allianceMember.role != AllianceRole.APPLICANT) {
            allianceMessageHistory.get(allianceMember.allianceId)
          } else {
            None
          }
        }
      }.flatten

      val generalMessageSource = if (user.isChatBanned) penaltyBoxMessageHistory else generalMessageHistory
      val sessionStart = getSessionStartMessage(generalMessageSource, allianceArchivedMessages, lastMessageIdOption)
      sender ! sessionStart
    }



    case PreviousMessagesRequest(airline, previousFirstMessageId, roomId) =>
      sender ! buildPreviousMessagesResponse(airline, previousFirstMessageId, roomId)

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

    case IncomingMessage(chatMessage) => {
      val processedMessage = processMessage(chatMessage)
      val outMessage = OutgoingMessage(processedMessage, true)

      //put message into history and send to subscribers
      chatMessage.roomId match {
        case x if x == GENERAL_ROOM_ID => {
          if (!chatMessage.user.isChatBanned) { //only put in main chat room if user is not banned for chats
            generalMessageHistory.enqueue(processedMessage)
          } else {
            println(s"sending message ${chatMessage.text} from ${chatMessage.airline.name} user ${chatMessage.user.userName} to penalty box only")
          }
          penaltyBoxMessageHistory.enqueue(processedMessage) //always put it in penalty box - penalty box can see the outside worlds

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
        case allianceRoomId =>
          val messageQueue = allianceMessageHistory.getOrElseUpdate(allianceRoomId, Queue[ChatMessage]())
          messageQueue.enqueue(chatMessage)
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

  private[this] val CHAT_HISTORY_ENTRIES = 10000
  def processMessage(message: ChatMessage): ChatMessage = {
    //check commands
    val processedMessage = ChatControllerActor.commands.find( command => command.hasPermission(message) && command.isCommand(message)) match {
      case Some(command) => command.execute(message) //only match first command
      case None => message
    }

    ChatSource.insertChatMessage(processedMessage)
    if (processedMessage.id % 1000 == 0) { //purge some older message
      ChatSource.deleteChatMessagesBeforeId(processedMessage.id - CHAT_HISTORY_ENTRIES)
    }


    processedMessage
  }
}



abstract class ChatCommand(val command : String) {
  val commandToken = "/" + command
  def execute(message: ChatMessage) : ChatMessage
  val isCommand = (message : ChatMessage) => message.text.startsWith(commandToken)
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
