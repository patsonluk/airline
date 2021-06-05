package com.patson.data

import java.sql.Statement

import com.patson.data.Constants._
import com.patson.model.{Airport, Airline}
import com.patson.model.campaign._
import com.patson.util.{AirlineCache, AirportCache}

import scala.collection.mutable.ListBuffer


object CampaignSource {
  val saveCampaign = (campaign : Campaign) => {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("INSERT INTO " + CAMPAIGN_TABLE + "(airline, principal_airport, population_coverage, radius) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    try {
      preparedStatement.setInt(1, campaign.airline.id)
      preparedStatement.setInt(2, campaign.principalAirport.id)
      preparedStatement.setLong(3, campaign.populationCoverage)
      preparedStatement.setInt(4, campaign.radius)

      val updateCount = preparedStatement.executeUpdate()
      if (updateCount > 0) {
        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          campaign.id = generatedId
          updateCampaignArea(campaign.id, campaign.area)
        }
      }
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

  val updateCampaign = (campaign : Campaign) => {
    val connection = Meta.getConnection()
    val preparedStatement = connection.prepareStatement("UPDATE " + CAMPAIGN_TABLE + " SET population_coverage = ?, radius = ? WHERE id = ?")
    try {
      preparedStatement.setLong(1, campaign.populationCoverage)
      preparedStatement.setInt(2, campaign.radius)
      preparedStatement.setInt(3, campaign.id)

      preparedStatement.executeUpdate()

      updateCampaignArea(campaign.id, campaign.area)
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

  val updateCampaignArea = (campaignId : Int, area : List[Airport]) => {
    val connection = Meta.getConnection()
    try {

      val purgeStatement = connection.prepareStatement("DELETE FROM " + CAMPAIGN_AREA_TABLE + " WHERE campaign = ?")
      purgeStatement.setInt(1, campaignId)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      connection.setAutoCommit(false)
      val insertStatement = connection.prepareStatement("REPLACE INTO " + CAMPAIGN_AREA_TABLE + "(campaign, airport) VALUES (?,?)")
      area.foreach { airport =>
          insertStatement.setInt(1, campaignId)
          insertStatement.setInt(2, airport.id)

          insertStatement.addBatch()
      }
      insertStatement.executeBatch()
      insertStatement.close()

      connection.commit()
    } finally {
      connection.close()
    }
  }

  def loadCampaignById(id : Int) = {
    val result = loadCampaignsByCriteria(List(("id", id)))
    if (result.length > 0) {
      Some(result(0))
    } else {
      None
    }
  }

  def loadCampaignsByCriteria(criteria : List[(String, Any)], loadArea : Boolean = false) = {
    var queryString = "SELECT * FROM " + CAMPAIGN_TABLE

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadCampaignsByQueryString(queryString, criteria.map(_._2), loadArea)
  }

  def loadCampaignsByQueryString(queryString : String, parameters : List[Any], loadArea : Boolean = false) : List[Campaign] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[Campaign]()

      while (resultSet.next()) {
        val campaignId = resultSet.getInt("id")
        val principalAirportId = resultSet.getInt("principal_airport")
        val airlineId = resultSet.getInt("airline")
        val radius = resultSet.getInt("radius")
        val populationCoverage = resultSet.getLong("population_coverage")
        val principalAirport = AirportCache.getAirport(principalAirportId).get
        val airline = AirlineCache.getAirline(airlineId).get
        val area =
          if (loadArea) {
            loadCampaignArea(campaignId)
          } else {
            List.empty
          }
        entries += Campaign(airline, principalAirport, radius, populationCoverage, area, campaignId)
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  private def loadCampaignArea(campaignId : Int) : List[Airport] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + CAMPAIGN_AREA_TABLE + " WHERE campaign = ?")
      preparedStatement.setInt(1, campaignId)

      val resultSet = preparedStatement.executeQuery()

      val entries = ListBuffer[Airport]()

      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        entries += AirportCache.getAirport(airportId).get //do NOT use full load here otherwise we running into infinite loop: airport.init bonus -> load campaign -> load area airport -> (if full load) airport.init bonus again
      }

      resultSet.close()
      preparedStatement.close()

      entries.toList
    } finally {
      connection.close()
    }
  }

  def loadCampaignsByAreaAirport(airportId : Int, fullLoad : Boolean = false) : List[Campaign] = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("SELECT * FROM " + CAMPAIGN_AREA_TABLE + " WHERE airport = ?")
      preparedStatement.setInt(1, airportId)

      val resultSet = preparedStatement.executeQuery()

      val campaignIds = ListBuffer[Int]()

      while (resultSet.next()) {
        campaignIds += resultSet.getInt("campaign")
      }

      val result =
        if (campaignIds.isEmpty) {
          List.empty
        } else {
          val queryString = s"SELECT * FROM $CAMPAIGN_TABLE WHERE id IN (${campaignIds.mkString(",")})";
          loadCampaignsByQueryString(queryString, List.empty, fullLoad)
        }

      resultSet.close()
      preparedStatement.close()

      result
    } finally {
      connection.close()
    }
  }




  def deleteCampaignsByAirline(airlineId : Int) = {
    loadCampaignsByCriteria(List(("airline", airlineId))).foreach { campaign =>
      deleteCampaign(campaign.id)
    }
  }

  def deleteCampaign(campaignId : Int) = {
    val connection = Meta.getConnection()
    val statement = connection.prepareStatement("DELETE FROM " + CAMPAIGN_TABLE + " WHERE id = ?")

    try {
      statement.setInt(1, campaignId)
      statement.executeUpdate()

      val campaign = Campaign.fromId(campaignId)
      DelegateSource.loadBusyDelegatesByCampaigns(List(campaign)).get(campaign).foreach { delegates =>
        DelegateSource.deleteBusyDelegates(delegates)
      }
    } finally {
      statement.close()
      connection.close()
    }
  }
}