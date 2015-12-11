package com.patson.model

case class LinkHistory(watchedLinkId : Int, relatedLinks : Set[RelatedLink], invertedRelatedLinks : Set[RelatedLink])
case class RelatedLink(relatedLinkId: Int, fromAirport : Airport, toAirport : Airport, airline : Airline, passengers : Int)
