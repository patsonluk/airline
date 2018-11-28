package com.patson.data
import com.patson.data.Constants._
import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import com.patson.model.airplane.Airplane
import java.sql.PreparedStatement
import com.patson.model._
import java.sql.Statement
import java.sql.ResultSet
import play.api.libs.json._
import scala.collection.mutable.HashMap


//----- New Table Code
//CREATE TABLE `server_scratchpad` (
//	`ID` INT(11) NOT NULL AUTO_INCREMENT,
//	`DATA` LONGTEXT NOT NULL,
//	`USED` TINYINT(4) NOT NULL DEFAULT '0',
//	PRIMARY KEY (`ID`)
//)


object ConsumptionHistorySource {
  val updateConsumptions = (consumptions : Map[(PassengerGroup, Airport, Route), Int]) => {
    val connection = Meta.getConnection()
    
    connection.setAutoCommit(false)
    
    connection.createStatement().executeUpdate("DELETE FROM " + PASSENGER_HISTORY_TABLE);
    
    var routeId = 0
	val rOut : ListBuffer[(Int, Int, Int, Int, String, Boolean)] = ListBuffer()
    try {
   	  consumptions.foreach { 
        case((passengerGroup, _, route), passengerCount) => {
          routeId += 1
            route.links.foreach { linkConsideration =>  
          	rOut += ((passengerGroup.passengerType.id, passengerCount, routeId, linkConsideration.link.id, linkConsideration.linkClass.code,linkConsideration.inverted ))
          }
        }
      }
	  
	  // Convert this to scratchpad constant  .. also add that 
	  connection.createStatement().executeUpdate("UPDATE server_scratchpad Set DATA ='" + rOut.mkString("", "\n","")  +"', USED = 1 WHERE ID = 1");
	  connection.commit()
    } finally {
      connection.close()
    }
  }
  
  // Should work with old and new table styles

  def loadAllConsumptions() : List[(PassengerType.Value, Int, Route)] = {
    val connection = Meta.getConnection()
    val linkMap = LinkSource.loadAllLinks(LinkSource.SIMPLE_LOAD).map { link => (link.id , link) }.toMap
    try {  
      val routeConsumptions = new HashMap[Int, (PassengerType.Value, Int)]() 
	  val linkConsiderations = new ListBuffer[(Int, LinkConsideration)] //route_id, linkConsideration	
	  
	  val preparedStatement = connection.prepareStatement("SELECT USED FROM server_scratchpad where ID = 1")

      val resultSet = preparedStatement.executeQuery()
      
	  var isScratchpad = false
	  while (resultSet.next()) {
		isScratchpad = resultSet.getBoolean("USED")
      }
      // Not sure if this works as it never gets called but the scratchpad should be more effcient
	  if (isScratchpad) {
		  val preparedStatement = connection.prepareStatement("SELECT DATA FROM server_scratchpad where ID = 1")
		  val resultSet = preparedStatement.executeQuery()
		  println("Using scratchpad table")
		  while (resultSet.next()) {
		    val pOut : ListBuffer[(Int, Int, Int, Int, String, Boolean)] = ListBuffer()
			val inText = resultSet.getString("DATA")
			inText.replace("(", "").replace(")", "").split("\n").foreach(line => { 
				val words=line.split(",")
				linkMap.get(words(3).toInt).foreach { link =>
				  val routeId = words(2).toInt
				  val passengerType = PassengerType.apply(words(0).toInt)
				  val passengerCount = words(1).toInt
				  val linkConsideration = new LinkConsideration(link, 0, LinkClass.fromCode(words(4)), words(5).toBoolean)
				  linkConsiderations += ((routeId,  linkConsideration))
				  routeConsumptions.put(routeId, (passengerType, passengerCount))
				}
			} )
			
		  }
      } else {
		  println("Using passenger history table")
		  val preparedStatement = connection.prepareStatement("SELECT * FROM " + PASSENGER_HISTORY_TABLE)

		  val resultSet = preparedStatement.executeQuery()
		  

		  
		  while (resultSet.next()) {
			linkMap.get(resultSet.getInt("link")).foreach { link =>
			  val routeId = resultSet.getInt("route_id")
			  val passengerType = PassengerType.apply(resultSet.getInt("passenger_type"))
			  val passengerCount = resultSet.getInt("passenger_count")
			  val linkConsideration = new LinkConsideration(link, 0, LinkClass.fromCode(resultSet.getString("link_class")), resultSet.getBoolean("inverted"))
			  linkConsiderations += ((routeId,  linkConsideration))
			  routeConsumptions.put(routeId, (passengerType, passengerCount))
			}
		  }
	  }
      val allRoutes = linkConsiderations.groupBy(_._1).map {
        case (routeId, linkConsiderationsByRoute) => new Route(linkConsiderationsByRoute.map(_._2).toList, 0, routeId)
      }
      
      allRoutes.map { route => 
        val consumption = routeConsumptions(route.id) 
        (consumption._1, consumption._2, route)  
      }.toList
    } finally {
      connection.close()
    }
  }
}
