package com.patson.model

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

object Scheduling {
  private[this] val MAX_MINUTE = 60
  private[this] val MAX_HOUR = 24
  private[this] val MAX_DAY_OF_WEEK = 7
  private[this] val TIME_SLOT_INCREMENT = 5 //5 minutes
  
  private[this] val smallAirportTimeSlots = getAllAvailableTimeSlots(Some(TimeSlotRetriction(6, 23))).toIndexedSeq
  private[this] val largeAirportTimeSlots = getAllAvailableTimeSlots(None).toIndexedSeq
  
  
  
  
  def getLinkSchedule(link : Transport) : Array[TimeSlot] = {
    val airportTimeslots = if (link.from.size >= Airport.MAJOR_AIRPORT_LOWER_THRESHOLD) largeAirportTimeSlots else smallAirportTimeSlots  
    
    val slotCount = airportTimeslots.size
    val pseudoRandomOffset= (link.distance + link.airline.id) % slotCount //just want to create a pseudo random number
    
    //start from the offset, find all the index
    
    if (link.frequency > 0) { //just in case 
      val interval = slotCount / link.frequency 
  
      val timeSlots = ArrayBuffer[TimeSlot]()
      for (i <- 0 until link.frequency) {
         var targetTimeSlot = airportTimeslots((pseudoRandomOffset + i * interval) % slotCount)
         timeSlots += targetTimeSlot
      }
      
      timeSlots.toArray
    } else {
      //println("This link has zero frequency: " + link)
      Array()
    }
  }
  
  private[this] def getAllAvailableTimeSlots(restriction : Option[TimeSlotRetriction]) : List[TimeSlot] = {
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
  case class TimeSlot(dayOfWeek : Int, hour : Int, minute : Int) extends Ordered[TimeSlot] {
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

    override def compare(that: TimeSlot) : Int = {
      if (this.dayOfWeek != that.dayOfWeek) {
        this.dayOfWeek - that.dayOfWeek
      } else {
        if (this.hour != that.hour) {
          this.hour - that.hour
        } else {
          this.minute - that.minute
        }
      }
    }
      
      lazy val totalMinutes = dayOfWeek * MAX_HOUR * MAX_MINUTE + hour * MAX_MINUTE + minute 
  }
  
  case class TimeSlotStatus(code : String, text : String)
}