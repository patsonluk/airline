package com.patson.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

object Scheduling {
  private[this] val DEFAULT_TIME_SLOT_RESTRICTION : TimeSlotRetriction = TimeSlotRetriction(6, 23)
  private[this] val MAX_MINUTE = 60
  private[this] val MAX_HOUR = 24
  private[this] val MAX_DAY_OF_WEEK = 7
  private[this] val TIME_SLOT_INCREMENT = 5 //5 minutes
  
  private[this] val indexedTimeSlots = getAllAvailableTimeSlots(Some(DEFAULT_TIME_SLOT_RESTRICTION)).toIndexedSeq
  private[this] val slotCount = indexedTimeSlots.length
  
  
  
  def getLinkSchedule(link : Link) : Array[TimeSlot] = {
    val pseudoRandomOffset= (link.distance + link.airline.id) % slotCount //just want to create a pseudo random number
    
    //start from the offset, find all the index
    
    if (link.frequency > 0) { //just in case 
      val interval = slotCount / link.frequency 
  
      val timeSlots = ArrayBuffer[TimeSlot]()
      for (i <- 0 until link.frequency) {
         var targetTimeSlot = indexedTimeSlots((pseudoRandomOffset + i * interval) % slotCount)
         timeSlots += targetTimeSlot
      }
      
      timeSlots.toArray
    } else {
      println("This link has zero frequency: " + link)
      Array()
    }
  }
  
  
  
  
  private[this] def getAllAvailableTimeSlots(restriction : Some[TimeSlotRetriction]) : List[TimeSlot] = {
    val fromHour = restriction.fold(0)(_.fromHour)
    val toHour = restriction.fold(MAX_HOUR)(_.toHour)
    
    val availableTimeSlots = ListBuffer[TimeSlot]()
    for (day <- 0 until MAX_DAY_OF_WEEK) {
      for (hour <- fromHour until toHour) {
        for (minute <- 0 until MAX_MINUTE by TIME_SLOT_INCREMENT) {
          availableTimeSlots += TimeSlot(day, hour, minute)
        }
      }
    }
    availableTimeSlots.toList
  }
  
  case class TimeSlotRetriction(fromHour : Int, toHour : Int) {
    def adjustTimeSlot(timeSlot : TimeSlot) : TimeSlot = {
      if (timeSlot.hour < fromHour) {
        TimeSlot(dayOfWeek = timeSlot.dayOfWeek, hour = fromHour, minute = 0)
      } else if (timeSlot.hour >= toHour) {
        val newDayOfWeek = (timeSlot.dayOfWeek + 1) % MAX_DAY_OF_WEEK 
        TimeSlot(dayOfWeek = newDayOfWeek, hour = fromHour, minute = 0)
      } else {
        timeSlot
      }
    }
  }
  case class TimeSlot(dayOfWeek : Int, hour : Int, minute : Int) {
      def increment(minute : Int, restriction : Option[TimeSlotRetriction]) : TimeSlot = {
        restriction match {
          case Some(restriction) => restriction.adjustTimeSlot(increment(minute))
          case None =>  increment(minute)
        }
      }
      
      def increment(minuteIncrement : Int) : TimeSlot = {
         var newMinute = this.minute + minuteIncrement
         var newHour = this.hour
         var newDayOfWeek = this.dayOfWeek
         if (newMinute >= MAX_MINUTE) {
           val hourIncrement = newMinute / MAX_MINUTE
           newMinute = newMinute % MAX_MINUTE
           newHour += hourIncrement
           if (newHour >= MAX_HOUR) {
             val dayIncrement = newHour / MAX_HOUR
             newHour = newHour % MAX_HOUR
             newDayOfWeek += dayIncrement
             newDayOfWeek = newDayOfWeek % MAX_DAY_OF_WEEK 
           } 
         }
         
         TimeSlot(dayOfWeek = newDayOfWeek, hour = newHour, minute = newMinute)
      }
      
      lazy val totalMinutes = dayOfWeek * MAX_HOUR * MAX_MINUTE + hour * MAX_MINUTE + minute 
  }
}