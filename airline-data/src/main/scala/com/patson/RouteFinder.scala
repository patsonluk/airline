package com.patson

import scala.collection.mutable.ArrayBuffer
import com.patson.model.Link
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Failure


object RouteFinder {
  private[this] val MAX_LINKS = 3
  
//  def findRoute1(links : Seq[Link], fromId : Int, toId : Int) : Seq[Seq[Link]] = {
//    val allLinksByFromAirport = links.flatMap { link => Seq(link, link.copy(from = link.to, to = link.from))  }.groupBy { _.from.id }
//
//    var linkCount = 0
//    var potentialRoutes = ArrayBuffer[Seq[Link]]()
//    val validRoutes = ArrayBuffer[Seq[Link]]()
//    for (linkCount <- 0 until MAX_LINKS) {
//      val potentialFromAirportWithRoutes : Map[Int, Seq[Seq[Link]]] =
//        if (linkCount == 0) {
//          Map(fromId -> Seq(Seq.empty))
//        } else {
//          potentialRoutes.groupBy{ route => route.last.to.id }
//        }
//
//       potentialRoutes.clear()
//       potentialFromAirportWithRoutes.foreach {
//         case(fromAirportId, existingRoutes) =>
//           allLinksByFromAirport.get(fromAirportId) match {
//             case Some(links) =>
//               existingRoutes.foreach { existingRoute =>
//                   val traveledAirportIds : Seq[Int] = existingRoute.map{ _.from.id }
//                   links.foreach { newLink =>
//                     if (newLink.to.id == toId) { //found!
//                       validRoutes += (existingRoute :+ newLink)
//                     } else if (!traveledAirportIds.contains(newLink.to.id)) {
//                       potentialRoutes += (existingRoute :+ newLink)
//                     }
//                 }
//               }
//             case None => //dead end (remove routes by not adding back to potentialRoutes)
//           }
//       }
//    }
//
//    validRoutes
//  }
//
//
//  def findRoute2(links : Seq[Link], fromId : Int, toId : Int) : Seq[Seq[Link]] = {
//    val allLinksByFromAirport = links.flatMap { link => Seq(link, link.copy(from = link.to, to = link.from))  }.groupBy { _.from.id }
//
//    val fromRoutesFuture = Future { findPotentialRoutes(allLinksByFromAirport, fromId, toId, MAX_LINKS / 2) }
//    val toRoutesFuture = Future { findPotentialRoutes(allLinksByFromAirport, toId, fromId, MAX_LINKS / 2) }
//
//    val allValidRoutes = ArrayBuffer[Seq[Link]]()
//
//    Future.sequence(Seq(fromRoutesFuture, toRoutesFuture)).onComplete {
//      case Success(resultSequence) =>
//        val (validRoutes, potentialFromRoutes) = resultSequence(0)
//        val (_, potentialToRoutes) = resultSequence(1)
//
//        allValidRoutes ++= validRoutes
//
//        val fromInfo = potentialFromRoutes.map { potentialFromRoute =>
//          val traveledAirportIds = potentialFromRoute.map { _.to.id }
//          (traveledAirportIds, potentialFromRoute)
//        }
//
//        val toInfo = potentialToRoutes.map { potentialToRoute =>
//          val traveledAirportIds = potentialToRoute.map { _.to.id }
//          (traveledAirportIds, potentialToRoute)
//        }
//
//        fromInfo.foreach {
//          case (fromRouteTraveledAirportIds, fromRoute) =>
//            toInfo.foreach {
//              case (toRouteTraveledAirportIds, toRoute) =>
//                if (!fromRouteTraveledAirportIds.intersect(toRouteTraveledAirportIds).isEmpty) {
//                  allValidRoutes += (fromRoute ++ toRoute)
//                }
//            }
//        }
//      case Failure(f) =>
//    }
//
//    Seq.empty
//  }
//
//  def findPotentialRoutes(allLinksByFromAirport : Map[Int, Seq[Link]], fromId : Int, toId : Int, maxLink : Int) : (Seq[Seq[Link]], Seq[Seq[Link]]) = {
//    var linkCount = 0
//    var potentialRoutes = ArrayBuffer[Seq[Link]]()
//    val validRoutes = ArrayBuffer[Seq[Link]]()
//    for (linkCount <- 0 until maxLink) {
//      val potentialFromAirportWithRoutes : Map[Int, Seq[Seq[Link]]] =
//        if (linkCount == 0) {
//          Map(fromId -> Seq(Seq.empty))
//        } else {
//          potentialRoutes.groupBy{ route => route.last.to.id }
//        }
//
//       potentialRoutes.clear()
//       potentialFromAirportWithRoutes.foreach {
//         case(fromAirportId, existingRoutes) =>
//           allLinksByFromAirport.get(fromAirportId) match {
//             case Some(links) =>
//               existingRoutes.foreach { existingRoute =>
//                   val traveledAirportIds : Seq[Int] = existingRoute.map{ _.from.id }
//                   links.foreach { newLink =>
//                     if (newLink.to.id == toId) { //found!
//                       validRoutes += (existingRoute :+ newLink)
//                     } else if (!traveledAirportIds.contains(newLink.to.id)) {
//                       potentialRoutes += (existingRoute :+ newLink)
//                     }
//                 }
//               }
//             case None => //dead end (remove routes by not adding back to potentialRoutes)
//           }
//       }
//    }
//
//    (validRoutes, potentialRoutes)
//  }
}