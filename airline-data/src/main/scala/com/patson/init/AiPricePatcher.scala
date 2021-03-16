package com.patson.init

import com.patson.data.{AirlineSource, LinkSource}
import com.patson.model.{ECONOMY, Link, LinkClassValues}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AiPricePatcher extends App {
  mainFlow
  
  def mainFlow() {
    val updatingLinks = ListBuffer[Link]()
    AirlineSource.loadAllAirlines(false).filter(_.isGenerated).foreach {  aiAirline =>
      LinkSource.loadFlightLinksByAirlineId(aiAirline.id).foreach { link =>
        updatingLinks += link.copy(price = LinkClassValues.getInstance(link.standardPrice(ECONOMY), 0, 0))
      }
    }
    
    LinkSource.updateLinks(updatingLinks.toList)
    //actorSystem.shutdown()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }
  

}