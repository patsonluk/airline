package com.patson

import com.patson.data.{AirlineViolationSource, IpSource, UserSource, UserUuidSource}
import com.patson.model.AirlineViolation

import java.util.Calendar

object DetectAirlineViolations {
  private val SHARING_THRESHOLD = 3
  private val LOOKBACK_DAYS = 7

  def detect() : Unit = {
    println("Detecting airline violations")

    val cutoff = {
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -LOOKBACK_DAYS)
      cal.getTime
    }

    val allUsers = UserSource.loadUsersByCriteria(List.empty)
    // Map userId -> (level, list of airlineIds owned with HQ)
    val userInfoById : Map[Int, (Int, List[Int])] = allUsers.map { user =>
      user.id -> (user.level, user.getAccessibleAirlines().filter(_.getHeadQuarter().isDefined).map(_.id))
    }.toMap

    val detectedViolations : Set[(Int, AirlineViolation.Value)] =
      detectByIdentifier(IpSource.loadUserIdsByIpSince(cutoff), userInfoById, AirlineViolation.IP) ++
      detectByIdentifier(UserUuidSource.loadUserIdsByUuidSince(cutoff), userInfoById, AirlineViolation.UUID)

    detectedViolations.foreach { case (airlineId, violation) =>
      AirlineViolationSource.saveViolation(airlineId, violation)
    }

    val existingViolations : Set[(Int, AirlineViolation.Value)] =
      AirlineViolationSource.loadAllViolations().flatMap { case (airlineId, violations) =>
        violations.map { case (violation, _) => (airlineId, violation) }
      }.toSet

    val stale = existingViolations -- detectedViolations
    stale.foreach { case (airlineId, violation) =>
      AirlineViolationSource.deleteViolation(airlineId, violation)
    }

    println(s"Detected ${detectedViolations.size} violations, purged ${stale.size} stale violations")
  }

  private def detectByIdentifier(identifierToUserIds : Map[String, List[Int]],
                                  userInfoById : Map[Int, (Int, List[Int])],
                                  violation : AirlineViolation.Value) : Set[(Int, AirlineViolation.Value)] = {
    identifierToUserIds.flatMap { case (_, userIds) =>
      val airlineIds = userIds.flatMap(userId => userInfoById.get(userId).map(_._2).getOrElse(List.empty)).distinct
      if (airlineIds.size >= SHARING_THRESHOLD) {
        userIds
          .filter(userId => userInfoById.get(userId).exists(_._1 == 0))
          .flatMap(userId => userInfoById.get(userId).map(_._2).getOrElse(List.empty))
          .distinct
          .map(airlineId => (airlineId, violation))
      } else {
        List.empty
      }
    }.toSet
  }
}
