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

  def loadAirportAssetByAssetId(id : Int) : Option[AirportAsset] = {
    loadAirportAssetsByBlueprintCriteria(List(("id", id))).headOption //do not use loadAirportAssetsByIds, as it expects those IDs is built already
  }

  def loadBuiltAirportAssetsByIds(ids : List[Int]) = {
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

  def loadAirportAssetsByAssetCriteria(criteria : List[(String, Any)]) = {
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
        val roi = resultSet.getDouble("roi")
        val upgradeApplied = resultSet.getBoolean("upgrade_applied")

        assets += AirportAsset.getAirportAsset(id, airport, assetType, airline, name, level, Some(completionCycle), idToBoost.getOrElse(id, List.empty), revenue, expense, roi, upgradeApplied, idToProperties.getOrElse(id, Map.empty), currentCycle)
      }

      resultSet.close()
      preparedStatement.close()

      assets.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportAssetsByAirport(airportId : Int, loadedAirport : Option[Airport] = None) = {
    loadAirportAssetsByBlueprintCriteria(List(("airport", airportId)), loadedAirport)
  }


  private[this] def loadAirportAssetsByBlueprintCriteria(criteria : List[(String, Any)], loadedAirport : Option[Airport] = None) = {
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
        val airportId = resultSet.getInt("airport")
        val airport =
          loadedAirport match {
            case None => AirportCache.getAirport(airportId, false).get
            case Some(loadedAirport) => loadedAirport
          }

        idToAssetBlueprint.put(id, AirportAssetBlueprint(airport, assetType, id))
      }

      val idToOwnedAssets = loadBuiltAirportAssetsByIds(idToAssetBlueprint.keys.toList).map(entry => (entry.id, entry)).toMap

      val result = idToAssetBlueprint.map {
        case (id, blueprint) =>
          idToOwnedAssets.getOrElse(id, AirportAsset.getAirportAsset(blueprint, airline = None, name = "", level = 0, completionCycle = None,  boosts = List.empty, revenue = 0, expense = 0, roi = blueprint.assetType.initRoi, upgradeApplied = false, properties = Map.empty, currentCycle))
      }

      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportBoostsByAssetIds(ids : List[Int]) : Map[Int, List[AirportBoost]] = {
    if (ids.isEmpty) {
      Map.empty
    } else {
      val queryString = new StringBuilder(s"SELECT * FROM $AIRPORT_ASSET_BOOST_TABLE where asset IN (");
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
          val boost = AirportBoost(boostType, resultSet.getDouble("value"))
          val assetId = resultSet.getInt("asset")
          val boosts = result.getOrElseUpdate(assetId, ListBuffer[AirportBoost]())
          boosts.append(boost)
        }
        resultSet.close()
        preparedStatement.close()
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
      val queryString = new StringBuilder(s"SELECT * FROM $AIRPORT_ASSET_PROPERTY_TABLE where asset IN (");
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
          val assetId = resultSet.getInt("asset")
          val property = result.getOrElseUpdate(assetId, mutable.Map[String, Long]())
          property.put(resultSet.getString("property"), resultSet.getLong("value"))
        }
        resultSet.close()
        preparedStatement.close()
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
      var purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_BOOST_TABLE WHERE asset = ?")
      purgeStatement.setInt(1, asset.id)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_PROPERTY_TABLE WHERE asset = ?")
      purgeStatement.setInt(1, asset.id)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      //var preparedStatement = connection.prepareStatement(s"UPDATE $AIRPORT_ASSET_TABLE SET airline = ?, name = ?, level = ?, completion_cycle = ?, revenue = ?, expense = ? WHERE id = ?")
      var preparedStatement = connection.prepareStatement(s"INSERT INTO $AIRPORT_ASSET_TABLE (airline, airport, asset_type, name, level, completion_cycle, revenue, expense, roi, upgrade_applied, id) VALUES(?,?,?,?,?,?,?,?,?,?,?) " +
      " ON DUPLICATE KEY UPDATE name=VALUES(name), level=VALUES(level), completion_cycle=VALUES(completion_cycle), revenue=VALUES(revenue), expense=VALUES(expense), roi=VALUES(roi), upgrade_applied=VALUES(upgrade_applied) ")


      preparedStatement.setInt(1, asset.airline.get.id)
      preparedStatement.setInt(2, asset.blueprint.airport.id)
      preparedStatement.setString(3, asset.assetType.toString)
      preparedStatement.setString(4, asset.name)
      preparedStatement.setInt(5, asset.level)
      preparedStatement.setInt(6, asset.completionCycle.get)
      preparedStatement.setLong(7, asset.revenue)
      preparedStatement.setLong(8, asset.expense)
      preparedStatement.setDouble(9, asset.roi)
      preparedStatement.setBoolean(10, asset.upgradeApplied)
      preparedStatement.setInt(11, asset.id)

      preparedStatement.executeUpdate()
      preparedStatement.close()


      asset.boosts.foreach { boost =>
        preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRPORT_ASSET_BOOST_TABLE (asset, boost_type, value) VALUES(?,?,?)")
        preparedStatement.setInt(1, asset.id)
        preparedStatement.setString(2, boost.boostType.toString)
        preparedStatement.setDouble(3, boost.value)
        preparedStatement.executeUpdate()
        preparedStatement.close()
      }

      asset.properties.foreach {
        case (property, value) =>
          preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRPORT_ASSET_PROPERTY_TABLE (asset, property, value) VALUES(?,?,?)")
          preparedStatement.setInt(1, asset.id)
          preparedStatement.setString(2, property)
          preparedStatement.setLong(3, value)
          preparedStatement.executeUpdate()
          preparedStatement.close()
      }

      AirportCache.invalidateAirport(asset.blueprint.airport.id)
    } finally {
      connection.close()
    }
  }

  def deleteAirportAsset(id : Int) = {
    val connection = Meta.getConnection()
    val airportId = loadAirportAssetByAssetId(id).get.blueprint.airport.id
    try {
      val preparedStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_ASSET_TABLE WHERE id = ?")

      preparedStatement.setInt(1, id)

      preparedStatement.executeUpdate()

      preparedStatement.close()

      AirportCache.invalidateAirport(airportId)
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

  def loadAirportBoostHistoryByAssetId(assetId : Int) : List[AirportAssetBoostHistory] = {
    val queryString = s"SELECT * FROM $AIRPORT_ASSET_BOOST_HISTORY_TABLE where asset = ?";

    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setObject(1, assetId)

      val resultSet = preparedStatement.executeQuery()
      val result = ListBuffer[AirportAssetBoostHistory]()
      while (resultSet.next()) {
        val boostType = AirportBoostType.withName(resultSet.getString("boost_type"))
        val assetId = resultSet.getInt("asset")
        val level = resultSet.getInt("level")
        val value = resultSet.getDouble("value")
        val gain = resultSet.getDouble("gain")
        val upgradeFactor = resultSet.getDouble("upgrade_factor")
        val cycle = resultSet.getInt("cycle")
        result.append(AirportAssetBoostHistory(assetId, level, boostType, value, gain, upgradeFactor, cycle))
      }

      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportPropertyHistoryByAssetId(assetId : Int) = {
    loadAirportPropertyHistoryByCriteria(List(("asset", assetId)))
  }

  def loadAirportPropertyHistoryByAssetIdAndCycle(assetId : Int, cycle : Int) = {
    val result = loadAirportPropertyHistoryByCriteria(List(("asset", assetId), ("cycle", cycle)))
    if (result.length == 1) {
      Some(result(0))
    } else if (result.length > 1) { //unexpected
      println(s"loadAirportPropertyHistoryByAssetIdAndCycle expects 1 result but found ${result.length}")
      Some(result(0))
    } else {
      None
    }
  }

  def loadAirportPropertyHistoryByCriteria(criteria : List[(String, Any)]) : List[AirportAssetPropertiesHistory] = {
    var queryString = s"SELECT * FROM $AIRPORT_ASSET_PROPERTY_HISTORY_TABLE";

    if (!criteria.isEmpty) {
      queryString += " WHERE "
      for (i <- 0 until criteria.size - 1) {
        queryString += criteria(i)._1 + " = ? AND "
      }
      queryString += criteria.last._1 + " = ?"
    }


    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until criteria.size) {
        preparedStatement.setObject(i + 1, criteria(i)._2)
      }

      val resultSet = preparedStatement.executeQuery()
      val result = mutable.Map[(Int, Int), mutable.Map[String, String]]() //key (assetId, cycle)
      while (resultSet.next()) {
        val assetId = resultSet.getInt("asset")
        val propertyKey = resultSet.getString("property")
        val value = resultSet.getString("value")
        val cycle = resultSet.getInt("cycle")
        val cycleProperties = result.getOrElseUpdate((assetId, cycle), mutable.Map[String, String]())
        cycleProperties.put(propertyKey, value)
      }

      resultSet.close()
      preparedStatement.close()

      result.map {
        case((assetId, cycle), properties) => AirportAssetPropertiesHistory(assetId, properties.toMap, cycle)
      }.toList
    } finally {
      connection.close()
    }
  }



  def saveAirportBoostHistory(entries : List[AirportAssetBoostHistory]) = {
    val queryString = s"REPLACE INTO $AIRPORT_ASSET_BOOST_HISTORY_TABLE (asset, boost_type, level, cycle, value, gain, upgrade_factor) VALUES(?,?,?,?,?,?,?)"
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      entries.foreach { entry =>
        preparedStatement.setInt(1, entry.assetId)
        preparedStatement.setString(2, entry.boostType.toString)
        preparedStatement.setInt(3, entry.level)
        preparedStatement.setInt(4, entry.cycle)
        preparedStatement.setDouble(5, entry.value)
        preparedStatement.setDouble(6, entry.gain)
        preparedStatement.setDouble(7, entry.upgradeFactor)

        preparedStatement.executeUpdate()
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def deleteAirportBoostHistoryByLevel(assetId : Int, level : Int) = {
    val queryString = s"DELETE FROM $AIRPORT_ASSET_BOOST_HISTORY_TABLE WHERE asset = ? AND level = ?"
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)
      preparedStatement.setInt(1, assetId)
      preparedStatement.setInt(2, level)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }


  def saveAirportPropertiesHistory(entries : List[AirportAssetPropertiesHistory]) = {
    val queryString = s"REPLACE INTO $AIRPORT_ASSET_PROPERTY_HISTORY_TABLE (asset, property, cycle, value) VALUES(?,?,?,?)";
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val preparedStatement = connection.prepareStatement(queryString)
      entries.foreach { entry =>
        entry.properties.foreach { case(property, value) =>
          preparedStatement.setInt(1, entry.assetId)
          preparedStatement.setString(2, property)
          preparedStatement.setInt(3, entry.cycle)
          preparedStatement.setString(4, value)
          preparedStatement.executeUpdate()
        }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def deleteAirportPropertiesHistory(cycleCutoff : Int) = { //anything before the cutoff will be purged
    val queryString = s"DELETE FROM $AIRPORT_ASSET_PROPERTY_HISTORY_TABLE WHERE cycle < ?";
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString.toString)
      preparedStatement.setInt(1, cycleCutoff)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } finally {
      connection.close()
    }
  }


}