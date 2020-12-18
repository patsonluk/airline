package com.patson.model.chat

import java.util.Calendar

import com.patson.model.{Airline, IdObject, User}

case class ChatMessage(airline : Airline, user : User, roomId : Int, text : String, time : Calendar, var id : Long = 0)
