package com.patson.model

case class Alert(airline : Airline, message : String, category : AlertCategory.Value, targetId : Option[Int], cycle : Int, duration : Int, var id : Int = 0)

object AlertCategory extends Enumeration {
    type AlertCategory = Value
    val LINK_CANCELLATION = Value
    
    val getDescription : AlertCategory.Value => String = {
      case LINK_CANCELLATION => "Link Cancellation Warning due to low load factor"
    }
}


