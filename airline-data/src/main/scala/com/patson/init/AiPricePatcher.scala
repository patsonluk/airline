package com.patson.init

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Source, Sink}
import akka.stream.scaladsl.Flow
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import com.patson.data.Constants._
import com.patson.model.City
import com.patson.model.Airport
import com.patson.data.AirportSource
import com.patson.data.CitySource
import com.patson.Util
import com.patson.model.Runway
import com.patson.model.RunwayType
import com.patson.model.Computation
import scala.collection.mutable.ArrayBuffer
import com.patson.model.Country
import com.patson.data.CountrySource
import com.patson.data.UserSource
import com.patson.data.AirlineSource
import com.patson.data.LinkSource
import com.patson.model.Pricing
import com.patson.model.ECONOMY
import com.patson.model.LinkClassValues
import scala.collection.mutable.ListBuffer
import com.patson.model.Link

object AiPricePatcher extends App {
  mainFlow
  
  def mainFlow() {
    val updatingLinks = ListBuffer[Link]()
    AirlineSource.loadAllAirlines(false).filter(_.isGenerated).foreach {  aiAirline =>
      LinkSource.loadLinksByAirlineId(aiAirline.id).foreach { link =>
        updatingLinks += link.copy(price = LinkClassValues.getInstance(Pricing.computeStandardPrice(link, ECONOMY), 0, 0))
      }
    }
    
    LinkSource.updateLinks(updatingLinks.toList)
    actorSystem.shutdown()
  }
  

}