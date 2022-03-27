package com.patson.model.alliance

import com.patson.model.LinkClassValues
import com.patson.model.Alliance

case class AllianceStats(alliance : Alliance,
                         totalPax : LinkClassValues,
                         totalLoungeVisit : Long,
                         totalLoyalist : Long,
                         totalRevenue : Long,
                         airportRankingStats : List[AirportRankingCount],
                         countryRankingStats : List[CountryRankingCount],
                         cycle : Int) {
  lazy val properties = Map(
    "economyPax" -> totalPax.economyVal.toLong,
    "businessPax" -> totalPax.businessVal.toLong,
    "firstPax" -> totalPax.firstVal.toLong,
    "loyalist" -> totalLoyalist,
    "revenue" -> totalRevenue,
    "loungeVisit" -> totalLoungeVisit,
  ) ++ getAirportRankingProperties(airportRankingStats) ++ getCountryRankingProperties(countryRankingStats)


  def getAirportRankingProperties(airportRankingStats : List[AirportRankingCount]) = {
    airportRankingStats.map {
      case AirportRankingCount(airportScale, ranking, count) => (s"airportRanking-$airportScale-$ranking", count.toLong)
    }.toMap
  }

  def getCountryRankingProperties(countryRankingStats : List[CountryRankingCount]) = {
    countryRankingStats.map {
      case CountryRankingCount(populationThreshold, ranking, count) => (s"countryRanking-$populationThreshold-$ranking", count.toLong)
    }.toMap
  }

  def airportRankingCount(rankingRequirement : Option[Long], scaleRequirement : Option[Long]) : Long = {
    var stats = airportRankingStats
    rankingRequirement.foreach { requirement =>
      stats = stats.filter(_.ranking <= requirement)
    }
    scaleRequirement.foreach { scaleRequirement =>
      stats = stats.filter(_.airportScale >= scaleRequirement)
    }
    stats.map(_.count).sum
  }

  def countryRankingCount(rankingRequirement : Option[Long], populationRequirement : Option[Long]) : Long = {
    var stats = countryRankingStats
    rankingRequirement.foreach { requirement =>
      stats = stats.filter(_.ranking <= requirement)
    }
    populationRequirement.foreach { populationRequirement =>
      stats = stats.filter(_.populationThreshold >= populationRequirement)
    }
    stats.map(_.count).sum
  }
}

object AllianceStats {
  def buildAllianceStats(alliance : Alliance, properties : Map[String, Long], cycle : Int) : AllianceStats = {
    val totalPax = LinkClassValues.getInstance(properties("economyPax").toInt, properties("businessPax").toInt, properties("firstPax").toInt)
    val totalLoungeVisit = properties("loungeVisit")
    val airportRankingStats = properties.filter(_._1.startsWith("airportRanking")).map {
      case (key, value) => {
        val tokens = key.split("-") //airportRanking-<airport scale>-<ranking>
        val airportScale = tokens(1).toInt
        val ranking = tokens(2).toInt
        AirportRankingCount(airportScale, ranking, value.toInt)
      }
    }.toList
    val countryRankingStats = properties.filter(_._1.startsWith("countryRanking")).map {
      case (key, value) => {
        val tokens = key.split("-") //airportRanking-<pop threshold>-<ranking>
        val populationThreshold = tokens(1).toInt
        val ranking = tokens(2).toInt
        CountryRankingCount(populationThreshold, ranking, value.toInt)
      }
    }.toList
    val totalLoyalist = properties("loyalist")
    val totalRevenue = properties("revenue")
    AllianceStats(alliance, totalPax, totalLoungeVisit, totalLoyalist, totalRevenue, airportRankingStats, countryRankingStats, cycle)
  }

  val empty = (alliance: Alliance, cycle: Int) => AllianceStats(alliance, LinkClassValues.getInstance(), 0, 0, 0, List.empty, List.empty, cycle)
}

case class AirportRankingCount(airportScale : Int, ranking : Int, count : Int)
case class CountryRankingCount(populationThreshold : Long, ranking : Int, count : Int)

