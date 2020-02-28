package websocket.chat

import com.patson.model.{Airline, User}

case class ChatMessage(airline : Airline, user : User, text : String)
