package models

import com.patson.model.{Airport, Airline, TransportType}

case class LinkHistory(watchedLinkId : Int, relatedLinks : List[List[RelatedLink]], invertedRelatedLinks : List[List[RelatedLink]])
case class RelatedLink(relatedLinkId: Int, fromAirport : Airport, toAirport : Airport, airline : Airline, passengers : Int, transportType : TransportType.Value)
