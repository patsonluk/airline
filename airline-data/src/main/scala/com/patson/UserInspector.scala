package com.patson

import java.util.Calendar

import com.patson.data.{AirlineSource, AirplaneSource, AllianceSource, LinkSource, UserSource}
import com.patson.model.{AllianceRole, Computation, UserStatus}

/**
  * @author tcai on 2019-11-14
  */
object UserInspector {
  def resetInactiveUsers(): Unit = {
    val expireDate = Calendar.getInstance()
    expireDate.add(Calendar.DATE, -60)

    val users = UserSource.loadUsersByCriteria(List(("status", "ACTIVE")))
    users.filter(u => !u.email.isEmpty && u.lastActiveTime.before(expireDate)).foreach {
      user => {
        UserSource.setUserStatus(user, UserStatus.INACTIVE)
        val airlineList = user.getAccessibleAirlines()
        airlineList.foreach {
            airline =>
              println(s"Rebuilding ${airline.name}")
              val balance = Computation.getResetAmount(airline.id).overall
              //remove all links
              LinkSource.loadLinksByAirlineId(airline.id).foreach {
                link => LinkSource.deleteLink(link.id)
              }
              //remove all airplanes
              AirplaneSource.deleteAirplanesByCriteria(List(("owner", airline.id)))
              //remove all bases
              AirlineSource.deleteAirlineBaseByCriteria(List(("airline", airline.id)))
              //remove all facilities
              AirlineSource.deleteLoungeByCriteria(List(("airline", airline.id)))

              AllianceSource.loadAllianceMemberByAirline(airline).foreach {
                allianceMember =>
                  AllianceSource.deleteAllianceMember(airline.id)
                  if (allianceMember.role == AllianceRole.LEADER) { //remove the alliance
                    AllianceSource.deleteAlliance(allianceMember.allianceId)
                  }
              }

              //reset balance
              airline.setBalance(balance)
              //unset country code
              airline.removeCountryCode()
              //unset service investment
              airline.setServiceFunding(0)
              airline.setServiceQuality(0)

              AirlineSource.saveAirlineInfo(airline)
          }
      }
    }

  }
}
