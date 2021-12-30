package com.patson.data

import com.patson.data.Constants._
import com.patson.model.AirportAssetType.SkiResortAssetType
import com.patson.model.{AirlineAppeal, _}
import com.patson.util.{AirlineCache, AirportCache, AirportChampionInfo}

import java.sql.{Statement, Types}
import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

object AirportAssetSource {
  val blueprintQueryColumns = List("airport", "asset_type", "id")

  def loadAirportAssetsByAirline(airlineId : Int) = {
    loadAirportAssetsByAssetCriteria(List(("airline", airlineId)))
  }

  def loadAirportAssetsByIds(ids : List[Int]) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $AIRPORT_ASSET_TABLE where id IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      loadAirportAssetsByAssetQueryString(queryString.toString(), ids)
    }
  }

  private[this] def loadAirportAssetsByAssetCriteria(criteria : List[(String, Any)]) = {
    var queryString = s"SELECT * FROM $AIRPORT_ASSET_TABLE"

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }
    loadAirportAssetsByAssetQueryString(queryString, criteria.map(_._2))
  }


  private[this] def loadAirportAssetsByAssetQueryString(queryString : String, parameters : List[Any]) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()
      val assets = ListBuffer[AirportAsset]()

      val ids = ListBuffer[Int]()
      while (resultSet.next()) {
        val id = resultSet.getInt("id") //same id as blueprint
        ids.append(id)
      }

      val idToBoost = loadAirportBoostsByAssetIds(ids.toList)
      val idToProperties = loadAirportPropertiesByAssetIds(ids.toList)

      val currentCycle = CycleSource.loadCycle()
      resultSet.beforeFirst()
      while (resultSet.next()) {
        val assetType = AirportAssetType.withName(resultSet.getString("asset_type"))
        val id = resultSet.getInt("id") //same id as blueprint
        val airport = AirportCache.getAirport(resultSet.getInt("airport"), false).get

        val airline = AirlineCache.getAirline(resultSet.getInt("airline"))
        val name = resultSet.getString("name")
        val level = resultSet.getInt("level")
        val completionCycle = resultSet.getInt("completion_cycle")
        val revenue = resultSet.getLong("revenue")
        val expense = resultSet.getLong("expense")

        assets += AirportAsset.getAirportAsset(id, airport, assetType, airline, name, level, Some(completionCycle), idToBoost.getOrElse(id, List.empty), revenue, expense, idToProperties.getOrElse(id, Map.empty), currentCycle)
      }

      assets.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportAssetsByAirport(airportId : Int) = {
    loadAirportAssetsByBlueprintCriteria(List(("airport", airportId)))
  }


  private[this] def loadAirportAssetsByBlueprintCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    try {
      var queryString = s"SELECT * FROM $AIRPORT_ASSET_BLUEPRINT_TABLE"

      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }

      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }


      val currentCycle = CycleSource.loadCycle()
      val resultSet = preparedStatement.executeQuery()
      val idToAssetBlueprint = mutable.HashMap[Int, AirportAssetBlueprint]()
      while (resultSet.next()) {
        val assetType = AirportAssetType.withName(resultSet.getString("asset_type"))
        val id = resultSet.getInt("id") //same id as blueprint
        val airport = AirportCache.getAirport(resultSet.getInt("airport"), false).get

        idToAssetBlueprint.put(id, AirportAssetBlueprint(airport, assetType, id))
      }

      val idToOwnedAssets = loadAirportAssetsByIds(idToAssetBlueprint.keys.toList).map(entry => (entry.id, entry)).toMap

      val result = idToAssetBlueprint.map {
        case (id, blueprint) =>
          idToOwnedAssets.getOrElse(id, AirportAsset.getAirportAsset(blueprint, airline = None, name = "", level = 0, completionCycle = None,  boosts = List.empty, revenue = 0, expense = 0, properties = Map.empty, currentCycle))
      }

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportBoostsByAssetIds(ids : List[Int]) : Map[Int, List[AirportBoost]] = {
    if (ids.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $AIRPORT_ASSET_BOOST_TABLE where blueprint IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString.toString)

        for (i <- 0 until ids.size) {
          preparedStatement.setObject(i + 1, ids(i))
        }

        val resultSet = preparedStatement.executeQuery()
        val result = mutable.HashMap[Int, ListBuffer[AirportBoost]]()
        while (resultSet.next()) {
          val boostType = AirportBoostType.withName(resultSet.getString("boost_type"))
          val boost = AirportBoost(boostType, resultSet.getLong("value"))
          val blueprintId = resultSet.getInt("blueprint")
          val boosts = result.getOrElseUpdate(blueprintId, ListBuffer[AirportBoost]())
          boosts.append(boost)
        }
        result.view.mapValues(_.toList).toMap

      } finally {
        connection.close()
      }
    }
  }

  def loadAirportPropertiesByAssetIds(ids : List[Int]) : Map[Int, Map[String, Long]] = {
    if (ids.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $AIRPORT_ASSET_PROPERTY_TABLE where blueprint IN (");
      for (i <- 0 until ids.size - 1) {
        queryString.append("?,")
      }

      queryString.append("?)")
      val connection = Meta.getConnection()
      try {
        val preparedStatement = connection.prepareStatement(queryString.toString)

        for (i <- 0 until ids.size) {
          preparedStatement.setObject(i + 1, ids(i))
        }

        val resultSet = preparedStatement.executeQuery()
        val result = mutable.HashMap[Int, mutable.Map[String, Long]]()
        while (resultSet.next()) {
          val blueprintId = resultSet.getInt("blueprint")
          val property = result.getOrElseUpdate(blueprintId, mutable.Map[String, Long]())
          property.put(resultSet.getString("property"), resultSet.getLong("value"))
        }
        result.view.mapValues(_.toMap).toMap

      } finally {
        connection.close()
      }
    }

  }

  
  def saveAirportAssetBlueprints(blueprints : List[AirportAssetBlueprint]) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"INSERT INTO $AIRPORT_ASSET_BLUEPRINT_TABLE(airport, asset_type)  VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
      connection.setAutoCommit(false)

      blueprints.foreach { blueprint =>


        preparedStatement.setInt(1, blueprint.airport.id)
        preparedStatement.setString(2, blueprint.assetType.toString)
        preparedStatement.executeUpdate()

        val generatedKeys = preparedStatement.getGeneratedKeys
        if (generatedKeys.next()) {
          val generatedId = generatedKeys.getInt(1)
          blueprint.id = generatedId
        }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def updateAirportAsset(asset : AirportAsset) = {
    val connection = Meta.getConnection()
    try {
      var purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_BOOST_TABLE WHERE blueprint = ?")
      purgeStatement.executeUpdate()
      purgeStatement.close()

      purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_PROPERTY_TABLE WHERE blueprint = ?")
      purgeStatement.executeUpdate()
      purgeStatement.close()

      //var preparedStatement = connection.prepareStatement(s"UPDATE $AIRPORT_ASSET_TABLE SET airline = ?, name = ?, level = ?, completion_cycle = ?, revenue = ?, expense = ? WHERE id = ?")
      var preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRPORT_ASSET_TABLE (airline, name, level, completion_cycle, revenue, expense, id) VALUES(?,?,?,?,?,?,?)")

      preparedStatement.setInt(1, asset.airline.get.id)
      preparedStatement.setString(2, asset.name)
      preparedStatement.setInt(3, asset.level)
      preparedStatement.setInt(4, asset.completionCycle.get)
      preparedStatement.setLong(5, asset.revenue)
      preparedStatement.setLong(6, asset.expense)
      preparedStatement.setInt(7, asset.id)

      preparedStatement.executeUpdate()
      preparedStatement.close()


      asset.boosts.foreach { boost =>
        preparedStatement = connection.prepareStatement(s"INSERT INTO $AIRPORT_ASSET_BOOST_TABLE (blueprint, boost_type, value) VALUES(?,?,?)")
        preparedStatement.setInt(1, asset.id)
        preparedStatement.setString(2, boost.boostType.toString)
        preparedStatement.setLong(3, boost.value)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      }

      asset.properties.foreach {
        case (property, value) =>
          preparedStatement = connection.prepareStatement(s"INSERT INTO $AIRPORT_ASSET_PROPERTY_TABLE (blueprint, property, value) VALUES(?,?,?)")
          preparedStatement.setInt(1, asset.id)
          preparedStatement.setString(2, property)
          preparedStatement.setLong(3, value)
          preparedStatement.executeUpdate()
          preparedStatement.close()
      }


    } finally {
      connection.close()
    }
  }

  def deleteAirportAsset(id : Int) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_TABLE WHERE id = ?")

      preparedStatement.setInt(1, id)

      preparedStatement.executeUpdate()

      preparedStatement.close()
    } finally {
      connection.close()
    }
  }

  def deleteAllAirportAssetBlueprints() = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_BLUEPRINT_TABLE")
      preparedStatement.executeUpdate()

      preparedStatement.close()
    } finally {
      connection.close()
    }
  }
}