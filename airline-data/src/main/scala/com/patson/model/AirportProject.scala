package com.patson.model


//import ProjectType.ProjectType
import ProjectStatus.ProjectStatus
import scala.collection.mutable.Map


case class AirportProject(airport : Airport, projectType : ProjectType, status : ProjectStatus, progress : Double, duration : Int, level : Int, var id : Int = 0) extends IdObject

//object ProjectType extends Enumeration {
//    type ProjectType = Value
//    val AIRPORT_EXPANSION, AIRPORT_UPGRADE = Value
//}

class ProjectType(val name: String, val isSingleton : Boolean) {
  ProjectType.allTypes(name) = this
}

object ProjectType {
  val allTypes = Map[String, ProjectType]()
  //have to list all the types here other the lookup map wouldn't instantiate...
  val AirportExpansion = new ProjectType("AIRPORT_EXPANSION", true)
  val AirportUpgrade = new ProjectType("AIRPORT_UPGRADE", true)
  
  val withName = (value : String) => {
      allTypes(value)
   }
}

object ProjectStatus extends Enumeration {
    type ProjectStatus = Value
    val INITIATED, BUILDING, COMPLETED = Value
}

object Test extends App {
  println(ProjectType.withName("AIRPORT_EXPANSION"))
}