package models

import akka.actor._
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class ClientActor(out: ActorRef, chat: ActorRef) extends Actor {
  val bannedwords = List("anal","anus","arse","ass","ballsack","balls","bastard","bitch","biatch","bloody","blowjob","blow job","bollock","bollok","boner","boob","bugger","bum","butt","buttplug","clitoris","cock","coon","crap","cunt","damn","dick","dildo","dyke","fag","feck","fellate","fellatio","felching","fuck","f u c k","fudgepacker","fudge packer","flange","Goddamn","God damn","hell","homo","jerk","jizz","knobend","knob end","labia","lmao","lmfao","muff","nigger","nigga","omg","penis","piss","poop","prick","pube","pussy","queer","scrotum","sex","shit","s hit","sh1t","slut","smegma","spunk","tit","tosser","turd","twat","vagina","wank","whore","wtf")
  chat ! Join

  override def postStop() = chat ! Leave

  def receive = {
    // this handles messages from the websocket
    case text: String =>
		if (!bannedwords.exists(text.toLowerCase.contains)) {
		  val sdf = new SimpleDateFormat("HH:mm:ss")
		  if (text.indexOf("[LOGGED]") > -1) {
		   chat ! ClientSentMessage(text)
		  } else {
		  chat ! ClientSentMessage("[" + sdf.format(Calendar.getInstance().getTime())+ "] " + text)
		  }
		}   
    case ClientSentMessage(text) =>
      out ! text.replaceFirst("\\W*(\\[LOGGED\\])","")
  }
}
