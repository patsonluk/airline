package com.patson.model

trait IdObject {
   def id : Int
   override def equals(other: Any) = other match {
      case that: IdObject  => if (id != 0) id == that.id else super.equals(that)
      case _                        => false
   }
   override def hashCode: Int = if (id != 0) id else super.hashCode()
}

