package com.patson.data

import com.patson.data.Constants._
import com.patson.model.{AirlineAppeal, _}
import com.patson.util.{AirlineCache, AirportCache, AirportChampionInfo}

import java.sql.{Statement, Types}
import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

object AirportSource {
  private[this] val BASE_QUERY = "SELECT * FROM airport"
  def loadAllAirports(fullLoad : Boolean = false, loadFeatures : Boolean = false) = {
      loadAirportsByCriteria(List.empty, fullLoad, loadFeatures)
  }
  
  def loadAirportsByIds(ids : List[Int], fullLoad : Boolean = false) = {
    if (ids.isEmpty) {
      List.empty
    } else {
      val queryString = new StringBuilder(BASE_QUERY + " where id IN (");
      for (i <- 0 until ids.size - 1) {
            queryString.append("?,")
      }
      
      queryString.append("?)")
      loadAirportsByQueryString(queryString.toString(), ids, fullLoad)
    }
  }
  
  def loadAirportsByCriteria(criteria : List[(String, Any)], fullLoad : Boolean = false, loadFeatures : Boolean = false) = {
      var queryString = BASE_QUERY
      
      if (!criteria.isEmpty) {
        queryString += " WHERE "
        for (i <- 0 until criteria.size - 1) {
          queryString += criteria(i)._1 + " = ? AND "
        }
        queryString += criteria.last._1 + " = ?"
      }
      
      loadAirportsByQueryString(queryString, criteria.map(_._2), fullLoad, loadFeatures)
  }

  def getAirlineGlobalBonuses(): Map[Int, List[AirlineBonus]] = {
    AirlineSource.loadAirlineModifiers().filter(_._2.modifierType == AirlineModifierType.BANNER_LOYALTY_BOOST).map { //only 1 type for now
      case (airlineId, modifier) =>
        val airlineBonus = AirlineBonus(BonusType.BANNER,
          AirlineAppeal(loyalty = modifier.properties.getOrElse(AirlineModifierPropertyType.STRENGTH, 0L).toDouble), expirationCycle = modifier.expiryCycle)
        (airlineId, airlineBonus)
    }.groupMapReduce(_._1) {
      case(airlineId, airlineBonus) => List(airlineBonus)
    } {
      _:::_ //flatten everything by joining the sub-lists
    }
  }

  def getAirlineTitleBonuses(airport : Airport, countryAirlineTitleCache : mutable.HashMap[String, immutable.Map[Int, CountryAirlineTitle]]): Map[Int, List[AirlineBonus]] = {
    //get airport bonus //for now no db
    val airlineTitles: Map[Int, CountryAirlineTitle] = countryAirlineTitleCache.getOrElseUpdate(airport.countryCode, CountrySource.loadCountryAirlineTitlesByCountryCode(airport.countryCode).map(entry => (entry.airline.id, entry)).toMap)

    //map airline titles to bonus
    val bonusByAirlineId =  mutable.HashMap[Int, ListBuffer[AirlineBonus]]()

    airlineTitles.foreach {
      case(airlineId, countryAirlineTitle) =>
        val list = bonusByAirlineId.getOrElseUpdate(airlineId, ListBuffer())
        list.append(AirlineBonus(bonusType = CountryAirlineTitle.getBonusType(countryAirlineTitle.title), bonus = AirlineAppeal(countryAirlineTitle.loyaltyBonus), expirationCycle = None))
    }

    AirportSource.loadAirlineAppealBonusByAirport(airport.id).foreach {
      case (airline, bonusList) =>
        val list = bonusByAirlineId.getOrElseUpdate(airline.id, ListBuffer())
        list.appendAll(bonusList)
    }

    bonusByAirlineId.view.mapValues(_.toList).toMap
  }

  def getCampaignBonuses(airport : Airport, currentCycle : Int): Map[Int, List[AirlineBonus]] = {
    val campaigns = CampaignSource.loadCampaignsByAreaAirport(airport.id, true)


    val bonusByAirlineId = mutable.Map[Int, ListBuffer[AirlineBonus]]()

    DelegateSource.loadBusyDelegatesByCampaigns(campaigns).foreach {
      case(campaign, delegates) =>
        val bonus = campaign.getAirlineBonus(airport, delegates.map(_.assignedTask.asInstanceOf[CampaignDelegateTask]), currentCycle)
        bonusByAirlineId.getOrElseUpdate(campaign.airline.id, ListBuffer[AirlineBonus]()).append(bonus)
    }

    bonusByAirlineId.view.mapValues(_.toList).toMap
  }

  def saveAirlineAppealBonus(airportId : Int, airlineId : Int, bonus : AirlineBonus) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE + "(airport, airline, bonus_type, loyalty_bonus, awareness_bonus, expiration_cycle) VALUES(?,?,?,?,?,?)")
      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(2, airlineId)
      preparedStatement.setInt(3, bonus.bonusType.id)
      preparedStatement.setDouble(4, bonus.bonus.loyalty)
      preparedStatement.setDouble(5, 0)
      bonus.expirationCycle match {
        case Some(cycle) =>
          preparedStatement.setInt(6, cycle)
        case None =>
          preparedStatement.setNull(6, java.sql.Types.INTEGER)
      }

      preparedStatement.executeUpdate()
      preparedStatement.close()

      AirportCache.invalidateAirport(airportId)

    } finally {
      connection.close()
    }
  }

  def loadAirlineAppealBonusByAirportAndAirline(airportId : Int, airlineId : Int) : List[AirlineBonus] = {
    val result = loadAirlineAppealBonusByCriteria(List(("airport", airportId), ("airline", airlineId)))
    if (result.isEmpty) {
      List.empty
    } else {
      result.last._2.last._2
    }
  }

  def loadAirlineAppealBonusByAirport(airportId : Int) : Map[Airline, List[AirlineBonus]] = {
    val result = loadAirlineAppealBonusByCriteria(List(("airport", airportId)))
    if (result.isEmpty) {
      Map.empty
    } else {
      result.last._2
    }
  }

  def loadAirlineAppealBonusByCriteria(criteria : List[(String, Any)]): Map[Airport, Map[Airline, List[AirlineBonus]]] = {
    val connection = Meta.getConnection()
    try {
      var queryString = "SELECT * FROM " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE

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


      val resultSet = preparedStatement.executeQuery()

      val result = mutable.HashMap[Airport, mutable.HashMap[Airline, ListBuffer[AirlineBonus]]]()


      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airport = AirportCache.getAirport(airportId, false).getOrElse(Airport.fromId(airportId))
        val airlineId = resultSet.getInt("airline")
        val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))

        val bonusList = result.getOrElseUpdate(airport, mutable.HashMap()).getOrElseUpdate(airline, ListBuffer())
        val expirationCycle = resultSet.getObject("expiration_cycle")
        bonusList.append(AirlineBonus(
          bonusType = BonusType(resultSet.getInt("bonus_type")),
          bonus = AirlineAppeal(loyalty = resultSet.getDouble("loyalty_bonus")),
          expirationCycle = if (expirationCycle == null) None else Some(expirationCycle.asInstanceOf[Int])))
      }
      resultSet.close()
      preparedStatement.close()

      result.view.mapValues { airlineAppealBonusMap =>
        airlineAppealBonusMap.view.mapValues(_.toList).toMap
      }.toMap
    } finally {
      connection.close()
    }
  }

  def deleteAirlineAppealBonus(airportId : Int, airlineId : Int, bonusType : BonusType.Value) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE + " where airport = ? AND airline = ? AND bonus_type = ?")

      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(2, airlineId)
      preparedStatement.setInt(3, bonusType.id)

      preparedStatement.executeUpdate()
      preparedStatement.close()

      AirportCache.invalidateAirport(airportId)
    } finally {
      connection.close()
    }
  }

  def purgeAirlineAppealBonus(atOrBeforeCycle : Int) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_AIRLINE_APPEAL_BONUS_TABLE + " where expiration_cycle IS NOT NULL AND expiration_cycle <= ?")

      preparedStatement.setInt(1, atOrBeforeCycle)

      preparedStatement.executeUpdate()
      preparedStatement.close()

    } finally {
      connection.close()
    }
  }



  def loadAirportsByQueryString(queryString : String, parameters : List[Any], fullLoad : Boolean = false, loadFeatures : Boolean = false) = {
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement(queryString)

      for (i <- 0 until parameters.size) {
        preparedStatement.setObject(i + 1, parameters(i))
      }


      val resultSet = preparedStatement.executeQuery()

      val airportData = new ListBuffer[Airport]()
      //val airlineMap : Map[Int, Airline] = AirlineSource.loadAllAirlines().foldLeft(Map[Int, Airline]())( (container, airline) => container + Tuple2(airline.id, airline))
      val countryAirlineTitleCache = mutable.HashMap[String, immutable.Map[Int, CountryAirlineTitle]]()
      val currentCycle = CycleSource.loadCycle()

      val airlineGlobalBonuses: Map[Int, List[AirlineBonus]] = //key is airline ID
        if (fullLoad) {
          getAirlineGlobalBonuses()
        } else {
          Map.empty
        }
      while (resultSet.next()) {
        val airport = Airport(
          resultSet.getString("iata"),
          resultSet.getString("icao"),
          resultSet.getString("name"),
          resultSet.getDouble("latitude"),
          resultSet.getDouble("longitude"),
          resultSet.getString("country_code"),
          resultSet.getString("city"),
          resultSet.getString("zone"),
          resultSet.getInt("airport_size"),
          resultSet.getInt("income"),
          resultSet.getLong("population"),
          runwayLength = resultSet.getInt("runway_length"))
        airport.id = resultSet.getInt("id")
        airportData += airport

        if (fullLoad || loadFeatures) {
          //load features first, as it might affect income level and pop, which both might affect later loading
          val featureStatement = connection.prepareStatement("SELECT * FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ?")
          featureStatement.setInt(1, airport.id)

          val featureResultSet = featureStatement.executeQuery()
          val features = ListBuffer[AirportFeature]()
          while (featureResultSet.next()) {
            val featureType = AirportFeatureType.withName(featureResultSet.getString("feature_type"))
            val strength = featureResultSet.getInt("strength")

            features += AirportFeature(featureType, strength)
          }
          featureStatement.close()
          airport.initFeatures(features.toList)
        }

        if (fullLoad) {
          //load assets
          airport.initAssets(AirportAssetSource.loadAirportAssetsByAirport(airport.id, Some(airport))) //pass the airport loaded so far to avoid cyclic load

          val loyaltyStatement = connection.prepareStatement("SELECT airline, loyalty FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ?")
          loyaltyStatement.setInt(1, airport.id)
          val loyaltyResultSet = loyaltyStatement.executeQuery()
          while (loyaltyResultSet.next()) {
            val airlineId = loyaltyResultSet.getInt("airline")
            //airlineAppeals.put(airlineId, AirlineAppeal(loyaltyResultSet.getDouble("loyalty"), loyaltyResultSet.getDouble("awareness")))
          }

          loyaltyResultSet.close()
          loyaltyStatement.close()

          val airlineBaseStatement = connection.prepareStatement("SELECT * FROM " + AIRLINE_BASE_TABLE + " WHERE airport = ?")
          airlineBaseStatement.setInt(1, airport.id)

          val airlineBaseResultSet = airlineBaseStatement.executeQuery()
          val airlineBases = ListBuffer[AirlineBase]()
          while (airlineBaseResultSet.next()) {
            val airlineId = airlineBaseResultSet.getInt("airline")
            val airline = AirlineCache.getAirline(airlineId).getOrElse(Airline.fromId(airlineId))
            val scale = airlineBaseResultSet.getInt("scale")
            val foundedCycle = airlineBaseResultSet.getInt("founded_cycle")
            val headquarter = airlineBaseResultSet.getBoolean("headquarter")
            val countryCode = airlineBaseResultSet.getString("country")

            airlineBases += AirlineBase(airline, airport, countryCode, scale, foundedCycle, headquarter)
          }
          airlineBaseStatement.close()
          airport.initAirlineBases(airlineBases.toList)


//          val airlineBonusesByAirlineIdBeforeFlatten : Map[Int, Seq[(Int, List[AirlineBonus])]] = (getAirlineTitleBonuses(airport, countryAirlineTitleCache).toSeq ++ getCampaignBonuses(airport, currentCycle).toSeq).groupBy(_._1)
//
//          val airlineBonuses : Map[Int, List[AirlineBonus]] = airlineBonusesByAirlineIdBeforeFlatten.view.mapValues { entry =>
//            entry.map {
//              case ((airlineId, bonusList)) => bonusList
//            }.flatten.toList
//          }.toMap
          //^^shorter but very unreadable...let's try something like below
          val titleBonuses = getAirlineTitleBonuses(airport, countryAirlineTitleCache)
          val campaignBonuses = getCampaignBonuses(airport, currentCycle)
          val airlineBonusesMutable = mutable.Map[Int, ListBuffer[AirlineBonus]]()
          (titleBonuses.toList ++ campaignBonuses.toList ++ airlineGlobalBonuses.toList).foreach {
            case((airlineId, bonuses)) =>
              val existingBonusesOfThisAirline = airlineBonusesMutable.getOrElseUpdate(airlineId, ListBuffer[AirlineBonus]())
              existingBonusesOfThisAirline.appendAll(bonuses)
          }
          val airlineBonuses = airlineBonusesMutable.view.mapValues(_.toList).toMap

          airport.initAirlineAppealsComputeLoyalty(airlineBonuses, LoyalistSource.loadLoyalistsByAirportId(airport.id))

//          val slotAssignments = mutable.Map[Int, Int]()
//
//          //val slotStatement = connection.prepareStatement("SELECT airline, SUM(frequency) as total_frequency FROM " + LINK_TABLE + " WHERE (from_airport = ? OR to_airport = ?) GROUP BY airline")
//          val slotStatement = connection.prepareStatement("SELECT airline, sum(a.frequency) as total_frequency FROM " + LINK_TABLE + " l INNER JOIN " + LINK_ASSIGNMENT_TABLE +  " a ON l.id = a.link AND (l.from_airport = ? OR l.to_airport = ?) GROUP BY airline")
//
//          slotStatement.setInt(1, airport.id)
//          slotStatement.setInt(2, airport.id)
//
//          val slotResultSet = slotStatement.executeQuery()
//          while (slotResultSet.next()) {
//            val airlineId = slotResultSet.getInt("airline")
//            slotAssignments.put(airlineId, slotResultSet.getInt("total_frequency"))
//          }
//          airport.initSlotAssignments(slotAssignments.toMap)
//          slotStatement.close()
          
          
          val lounges = AirlineSource.loadLoungesByAirport(airport)
          airport.initLounges(lounges)
          
          //load profile pics
           val imageStatement = connection.prepareStatement("SELECT * FROM " + AIRPORT_IMAGE_TABLE + " WHERE airport = ?")
          imageStatement.setInt(1, airport.id)
          
          val imageResultSet = imageStatement.executeQuery()
          if (imageResultSet.next()) {
            val airportUrl = imageResultSet.getString("airport_url")
            val cityUrl = imageResultSet.getString("city_url")
//            if (airportUrl != null) {
//              airport.setAirportImageUrl(airportUrl)
//            }
//            if (cityUrl != null) {
//              airport.setCityImageUrl(cityUrl)
//            }
          }
          imageStatement.close()

          //load runway
          val runwayStatement = connection.prepareStatement("SELECT * FROM " + AIRPORT_RUNWAY_TABLE + " WHERE airport = ?")
          runwayStatement.setInt(1, airport.id)

          val runwayResultSet = runwayStatement.executeQuery()
          val runways = ListBuffer[Runway]()

          while (runwayResultSet.next()) {
            val runwayType = RunwayType.withName(runwayResultSet.getString("runway_type"))
            val code = runwayResultSet.getString("code")
            val length = runwayResultSet.getInt("length")
            val lighted = runwayResultSet.getBoolean("lighted")
            runways += Runway(length, code, runwayType, lighted)
          }
          runwayStatement.close()
          airport.setRunways(runways.toList)

          airport.shouldLoadCities = true //set this flag so this airport can lazy load cities, which could be a lot of data
        }
      }
      
      resultSet.close()
      preparedStatement.close()
      airportData.toList
    } finally {
      connection.close()
    }
      
  }

  def updateAirportBaseSpecializations(airportId : Int, airlineId : Int, airlineBaseSpecializationTypes : List[AirlineBaseSpecialization.Value]) = {
    val connection = Meta.getConnection()
    try {
      val purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRLINE_BASE_SPECIALIZATION_TABLE WHERE airport = ? AND airline = ?")
      purgeStatement.setInt(1, airportId)
      purgeStatement.setInt(2, airlineId)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      var preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRLINE_BASE_SPECIALIZATION_TABLE  (airport, airline, specialization_type) VALUES(?,?,?)")
      airlineBaseSpecializationTypes.foreach { airlineBaseSpecializationType =>
        preparedStatement.setInt(1, airportId)
        preparedStatement.setInt(2, airlineId)
        preparedStatement.setString(3, airlineBaseSpecializationType.toString)
        preparedStatement.executeUpdate()
      }

      preparedStatement.close()

      preparedStatement = connection.prepareStatement(s"REPLACE INTO $AIRLINE_BASE_SPECIALIZATION_LAST_UPDATE_TABLE  (airport, airline, update_cycle) VALUES(?,?,?)")
      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(2, airlineId)
      preparedStatement.setInt(3, CycleSource.loadCycle())
      preparedStatement.executeUpdate()

      preparedStatement.close()

      AirportCache.invalidateAirport(airportId)
      AirlineCache.invalidateAirline(airlineId)
    } finally {
      connection.close()
    }
  }

  def loadAllAirportBaseSpecializations : List[(Airline, Airport, AirlineBaseSpecialization.Value)] = {
    val connection = Meta.getConnection()
    try {
      val queryString = s"SELECT * FROM $AIRLINE_BASE_SPECIALIZATION_TABLE"

      val preparedStatement = connection.prepareStatement(queryString)

      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[(Airline, Airport, AirlineBaseSpecialization.Value)]()

      while (resultSet.next()) {
        val specialization = AirlineBaseSpecialization.withName(resultSet.getString("specialization_type"))
        result.append((AirlineCache.getAirline(resultSet.getInt("airline")).get, AirportCache.getAirport(resultSet.getInt("airport")).get, specialization))


      }
      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportBaseSpecializations(airportId : Int, airlineId : Int) : List[AirlineBaseSpecialization.Value] = {
    val connection = Meta.getConnection()
    try {
      var queryString = s"SELECT * FROM $AIRLINE_BASE_SPECIALIZATION_TABLE WHERE airport = ? AND airline = ?"

      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(2, airlineId)


      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[AirlineBaseSpecialization.Value]()

      while (resultSet.next()) {
        val specialization = AirlineBaseSpecialization.withName(resultSet.getString("specialization_type"))
        result.append(specialization)
      }
      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadAirportBaseSpecializationsLastUpdate(airportId : Int, airlineId : Int) : Option[Int] = {
    val connection = Meta.getConnection()
    try {
      var queryString = s"SELECT * FROM $AIRLINE_BASE_SPECIALIZATION_LAST_UPDATE_TABLE WHERE airport = ? AND airline = ?"

      val preparedStatement = connection.prepareStatement(queryString)

      preparedStatement.setInt(1, airportId)
      preparedStatement.setInt(2, airlineId)


      val resultSet = preparedStatement.executeQuery()

      val result =
        if (resultSet.next()) {
          Some(resultSet.getInt("update_cycle"))
        } else {
          None
        }
      resultSet.close()
      preparedStatement.close()

      result
    } finally {
      connection.close()
    }
  }

  
  def loadAirportById(id : Int, fullLoad : Boolean = false) = {
      val result = loadAirportsByCriteria(List(("id", id)), fullLoad)
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  def loadAirportByIata(iata : String, fullLoad : Boolean = false) = {
      val result = loadAirportsByCriteria(List(("iata", iata)), fullLoad)
      if (result.isEmpty) {
        None
      } else {
        Some(result(0))
      }
  }
  def loadAirportsByCountry(countryCode : String) = {
    loadAirportsByCriteria(List(("country_code", countryCode)))
  }

  def updateAirlineAppeal(airportId : Int, airlineId : Int, airlineAppeal : AirlineAppeal) = {
   val connection = Meta.getConnection()
   try {  
     connection.setAutoCommit(false)
      if (airlineAppeal.loyalty == 0) {
        val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ? AND airline = ?")
        purgeStatement.setInt(1, airportId)
        purgeStatement.setInt(2, airlineId)
        purgeStatement.executeUpdate()
        purgeStatement.close()
      } else {
        val insertStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_APPEAL_TABLE + "(airport, airline, loyalty, awareness) VALUES (?,?,?,?)")
        insertStatement.setInt(1, airportId)
        insertStatement.setInt(2, airlineId)
        insertStatement.setDouble(3, airlineAppeal.loyalty)
        insertStatement.setDouble(4, 0)
        insertStatement.executeUpdate()
        insertStatement.close()
      }
     //AirportCache.invalidateAirport(airportId)
     connection.commit()
   } finally {
     connection.close()
   }
  }

  def deleteAirlineAppealsFromAllAirports(airlineId : Int) = {
    val connection = Meta.getConnection()
    try {
      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_APPEAL_TABLE + " WHERE airline = ?")
      purgeStatement.setInt(1, airlineId)
      purgeStatement.executeUpdate()
    } finally {
      connection.close()
    }
  }

  def updateAirlineAppeals(airportId : Int, airlineAppeals : Map[Int, AirlineAppeal]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRLINE_APPEAL_TABLE + " WHERE airport = ? AND airline = ?")
      val insertStatement = connection.prepareStatement("REPLACE INTO " + AIRLINE_APPEAL_TABLE + "(airport, airline, loyalty, awareness) VALUES (?,?,?,?)")
      airlineAppeals.foreach {
        case (airlineId, airlineAppeal) =>
          if (airlineAppeal.loyalty == 0) {
            purgeStatement.setInt(1, airportId)
            purgeStatement.setInt(2, airlineId)
            purgeStatement.addBatch()
          } else {
            insertStatement.setInt(1, airportId)
            insertStatement.setInt(2, airlineId)
            insertStatement.setDouble(3, airlineAppeal.loyalty)
            insertStatement.setDouble(4, 0)
            insertStatement.addBatch()
          }
      }
      purgeStatement.executeBatch()
      insertStatement.executeBatch()
      purgeStatement.close()
      insertStatement.close()
      //AirportCache.invalidateAirport(airportId)

      connection.commit()
    } finally {
      connection.close()
    }
  }

  
  def saveAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()
    try {
      val preparedStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_TABLE + "(iata, icao, name, latitude, longitude, country_code, city, zone, airport_size, income, population, runway_length)  VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
    
      connection.setAutoCommit(false)
      airports.foreach { 
        airport =>
          preparedStatement.setString(1, airport.iata)
          preparedStatement.setString(2, airport.icao)
          preparedStatement.setString(3, airport.name)
          preparedStatement.setDouble(4, airport.latitude)
          preparedStatement.setDouble(5, airport.longitude)
          preparedStatement.setString(6, airport.countryCode)
          preparedStatement.setString(7, airport.city)
          preparedStatement.setString(8, airport.zone)
          preparedStatement.setInt(9, airport.size)
          preparedStatement.setLong(10, airport.baseIncome)
          preparedStatement.setLong(11, airport.basePopulation)
          preparedStatement.setInt(12, airport.runwayLength)
          
          preparedStatement.executeUpdate()
          val generatedKeys = preparedStatement.getGeneratedKeys
          
          if (generatedKeys.next()) {
            val generatedId = generatedKeys.getInt(1)
            airport.id = generatedId
            
            //insert airline info too
            airport.citiesServed.foreach { 
              case (city, share) =>
              val infoStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_CITY_SHARE_TABLE + "(airport, city, share) VALUES(?,?,?)")
              infoStatement.setInt(1, airport.id)
              infoStatement.setInt(2, city.id)
              infoStatement.setDouble(3, share)
              infoStatement.executeUpdate()
              infoStatement.close()
            }
            //insert features
            airport.getFeatures().foreach { feature =>
              val featureStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_FEATURE_TABLE + "(airport, feature_type, strength) VALUES(?,?,?)")
              featureStatement.setInt(1, airport.id)
              featureStatement.setString(2, feature.featureType.toString())
              featureStatement.setInt(3, feature.strength)
              featureStatement.executeUpdate()
              featureStatement.close()
            }
            //insert runway
            airport.getRunways().foreach { runway =>
              val statement = connection.prepareStatement("INSERT INTO " + AIRPORT_RUNWAY_TABLE + "(airport, code, runway_type, length, lighted) VALUES(?,?,?,?,?)")
              statement.setInt(1, airport.id)
              statement.setString(2, runway.code)
              statement.setString(3, runway.runwayType.toString)
              statement.setInt(4, runway.length)
              statement.setBoolean(5, runway.lighted)
              statement.executeUpdate()
              statement.close()
            }
          }
      }
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def fullUpdateAirports(airports : List[Airport]) = {
            Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()

    try {
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPORT_TABLE + " SET airport_size = ?, income = ?, population = ?, name = ?, city = ?, runway_length = ?  WHERE id = ?")

      connection.setAutoCommit(false)


      airports.foreach {
        airport =>
          preparedStatement.setInt(1, airport.size)
          preparedStatement.setInt(2, airport.baseIncome)
          preparedStatement.setLong(3, airport.basePopulation)
          preparedStatement.setString(6, airport.name)
          preparedStatement.setString(7, airport.city)
          preparedStatement.setInt(8, airport.runwayLength)
          preparedStatement.setInt(9, airport.id)

          preparedStatement.addBatch()
          //preparedStatement.executeUpdate()


          val purgeCityShareStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_CITY_SHARE_TABLE + " WHERE airport = ?");
          purgeCityShareStatement.setInt(1, airport.id)
          purgeCityShareStatement.executeUpdate()
          purgeCityShareStatement.close()
          val purgeFeatureStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ?");
          purgeFeatureStatement.setInt(1, airport.id)
          purgeFeatureStatement.executeUpdate()
          purgeFeatureStatement.close()

          //update airline info too
          airport.citiesServed.foreach {
            case (city, share) =>
            val infoStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_CITY_SHARE_TABLE + "(airport, city, share) VALUES(?,?,?)")
            infoStatement.setInt(1, airport.id)
            infoStatement.setInt(2, city.id)
            infoStatement.setDouble(3, share)
            infoStatement.executeUpdate()
            infoStatement.close()
          }
          //insert features
          airport.getFeatures().foreach { feature =>
            val featureStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_FEATURE_TABLE + "(airport, feature_type, strength) VALUES(?,?,?)")
            featureStatement.setInt(1, airport.id)
            featureStatement.setString(2, feature.featureType.toString())
            featureStatement.setInt(3, feature.strength)
            featureStatement.executeUpdate()
            featureStatement.close()
          }

          val purgeRunwayStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_RUNWAY_TABLE + " WHERE airport = ?");
          purgeRunwayStatement.setInt(1, airport.id)
          purgeRunwayStatement.executeUpdate()
          purgeRunwayStatement.close()

          //insert runways
//          "airport INTEGER," +
//            "code VARCHAR(16)," +
//            "type SMALLINT," +
//            "length SMALLINT," +
          airport.getRunways().foreach { runway =>
            val statement = connection.prepareStatement("INSERT INTO " + AIRPORT_RUNWAY_TABLE + "(airport, code, runway_type, length, lighted) VALUES(?,?,?,?,?)")
            statement.setInt(1, airport.id)
            statement.setString(2, runway.code)
            statement.setString(3, runway.runwayType.toString)
            statement.setInt(4, runway.length)
            statement.setBoolean(5, runway.lighted)
            statement.executeUpdate()
            statement.close()
          }


          AirportCache.invalidateAirport(airport.id)
      }
      preparedStatement.executeBatch()
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def updateAirports(airports : List[Airport]) = {
    Class.forName(DB_DRIVER);
    val connection = Meta.getConnection()

    try {
      val preparedStatement = connection.prepareStatement("UPDATE " + AIRPORT_TABLE + " SET airport_size = ?, income = ?, population = ?, runway_length = ?  WHERE id = ?")

      connection.setAutoCommit(false)


      airports.foreach {
        airport =>
          if (airport.id != 0) {
            preparedStatement.setInt(1, airport.size)
            preparedStatement.setLong(2, airport.baseIncome)
            preparedStatement.setLong(3, airport.basePopulation)
            preparedStatement.setInt(4, airport.runwayLength)
            preparedStatement.setInt(5, airport.id)

            preparedStatement.addBatch()
            //preparedStatement.executeUpdate()


            val purgeCityShareStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_CITY_SHARE_TABLE + " WHERE airport = ?");
            purgeCityShareStatement.setInt(1, airport.id)
            purgeCityShareStatement.executeUpdate()
            purgeCityShareStatement.close()
            val purgeFeatureStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ?");
            purgeFeatureStatement.setInt(1, airport.id)
            purgeFeatureStatement.executeUpdate()
            purgeFeatureStatement.close()

            println(s"updating airport $airport")

            //update airline info too
            airport.citiesServed.foreach {
              case (city, share) =>
                val infoStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_CITY_SHARE_TABLE + "(airport, city, share) VALUES(?,?,?)")
                infoStatement.setInt(1, airport.id)
                infoStatement.setInt(2, city.id)
                infoStatement.setDouble(3, share)
                infoStatement.executeUpdate()
                infoStatement.close()
            }
            //insert features
            airport.getFeatures().foreach { feature =>
              val featureStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_FEATURE_TABLE + "(airport, feature_type, strength) VALUES(?,?,?)")
              featureStatement.setInt(1, airport.id)
              featureStatement.setString(2, feature.featureType.toString())
              featureStatement.setInt(3, feature.strength)
              featureStatement.executeUpdate()
              featureStatement.close()
            }

            val purgeRunwayStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_RUNWAY_TABLE + " WHERE airport = ?");
            purgeRunwayStatement.setInt(1, airport.id)
            purgeRunwayStatement.executeUpdate()
            purgeRunwayStatement.close()

            //insert runways
            //          "airport INTEGER," +
            //            "code VARCHAR(16)," +
            //            "type SMALLINT," +
            //            "length SMALLINT," +
            airport.getRunways().foreach { runway =>
              val statement = connection.prepareStatement("INSERT INTO " + AIRPORT_RUNWAY_TABLE + "(airport, code, runway_type, length, lighted) VALUES(?,?,?,?,?)")
              statement.setInt(1, airport.id)
              statement.setString(2, runway.code)
              statement.setString(3, runway.runwayType.toString)
              statement.setInt(4, runway.length)
              statement.setBoolean(5, runway.lighted)
              statement.executeUpdate()
              statement.close()
            }

            AirportCache.invalidateAirport(airport.id)
          }
      }
      preparedStatement.executeBatch()
      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def updateAirportFeatures(airportId : Int, features : List[AirportFeature]) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ?")
      purgeStatement.setInt(1, airportId)
      purgeStatement.executeUpdate()
      purgeStatement.close()

      val featureStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_FEATURE_TABLE + "(airport, feature_type, strength) VALUES(?,?,?)")
      features.foreach { feature =>
        featureStatement.setInt(1, airportId)
        featureStatement.setString(2, feature.featureType.toString())
        featureStatement.setInt(3, feature.strength)
        featureStatement.executeUpdate()
      }
      featureStatement.close()
      AirportCache.invalidateAirport(airportId)

      connection.commit()
    } finally {
      connection.close()
    }
  }


  def saveAirportFeature(airportId : Int, feature : AirportFeature) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val featureStatement = connection.prepareStatement("REPLACE INTO " + AIRPORT_FEATURE_TABLE + "(airport, feature_type, strength) VALUES(?,?,?)")
      featureStatement.setInt(1, airportId)
      featureStatement.setString(2, feature.featureType.toString())
      featureStatement.setInt(3, feature.strength)
      featureStatement.executeUpdate()

      featureStatement.close()
      AirportCache.invalidateAirport(airportId)
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def deleteAirportFeature(airportId : Int, featureType : AirportFeatureType.Value) = {
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val featureStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ? AND feature_type = ?")
      featureStatement.setInt(1, airportId)
      featureStatement.setString(2, featureType.toString())
      featureStatement.executeUpdate()

      featureStatement.close()
      AirportCache.invalidateAirport(airportId)
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def loadAirportFeatures(airportId : Int) = {
    val connection = Meta.getConnection()
    try {
      val featureStatement = connection.prepareStatement("SELECT * FROM " + AIRPORT_FEATURE_TABLE + " WHERE airport = ?")
      featureStatement.setInt(1, airportId)

      val featureResultSet = featureStatement.executeQuery()
      val features = ListBuffer[AirportFeature]()
      while (featureResultSet.next()) {
        val featureType = AirportFeatureType.withName(featureResultSet.getString("feature_type"))
        val strength = featureResultSet.getInt("strength")

        features += AirportFeature(featureType, strength)
      }
      featureResultSet.close()
      featureStatement.close()
      features.toList
    } finally {
      connection.close()
    }
  }


//  def updateAirportImages(airports : List[Airport]) = {
//    val connection = Meta.getConnection()
//    try {
//      connection.setAutoCommit(false)
//      airports.foreach { airport =>
//        val purgeStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_IMAGE_TABLE + " WHERE airport = ?")
//        purgeStatement.setInt(1, airport.id)
//        purgeStatement.executeUpdate()
//        purgeStatement.close()
//
//        val featureStatement = connection.prepareStatement("INSERT INTO " + AIRPORT_IMAGE_TABLE + "(airport, city_url, airport_url) VALUES(?,?,?)")
//        featureStatement.setInt(1, airport.id)
//        featureStatement.setString(2, airport.getCityImageUrl().getOrElse(null))
//        featureStatement.setString(3, airport.getAirportImageUrl().getOrElse(null))
//        featureStatement.executeUpdate()
//
//        featureStatement.close()
//
//        //AirportCache.invalidateAirport(airport.id)
//      }
//      connection.commit()
//    } finally {
//      connection.close()
//    }
//  }


  def deleteAirports(airportIds : List[Int]) = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)

      val preparedStatement = connection.prepareStatement("DELETE FROM " + AIRPORT_TABLE + " WHERE id = ?")
      connection.setAutoCommit(false)
      airportIds.foreach { airportId =>
          preparedStatement.setInt(1, airportId)

          preparedStatement.executeUpdate()
          AirportCache.invalidateAirport(airportId)
      }

      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }
  
  def deleteAllAirports() = {
    //open the hsqldb
    val connection = Meta.getConnection()
    try {
      var queryString = "DELETE FROM " + AIRPORT_TABLE
      
      val preparedStatement = connection.prepareStatement(queryString)
      val deletedCount = preparedStatement.executeUpdate()
      
      preparedStatement.close()
      println("Deleted " + deletedCount + " airport records")
      deletedCount
    } finally {
      connection.close()
    }
  }


  def loadAirportSharesOnCity(cityId : Int) : List[(Airport, Double)] = {
    CitySource.loadCityById(cityId) match {
      case Some(city) =>
          //open the hsqldb
        val connection = Meta.getConnection()
        try {  
          //var queryString = "SELECT * FROM " + AIRPORT_CITY_SHARE_TABLE + " c LEFT JOIN " + AIRPORT_TABLE + " a ON c.airport = a.id WHERE c.city = ?"
          val queryString = "SELECT * FROM " + AIRPORT_CITY_SHARE_TABLE + " WHERE city = ?"
          
          val preparedStatement = connection.prepareStatement(queryString)
          
          preparedStatement.setInt(1, cityId)
          
          val resultSet = preparedStatement.executeQuery()
          
          val airportShareList = new ListBuffer[(Airport, Double)]()
          
          while (resultSet.next()) {
            AirportCache.getAirport(resultSet.getInt("airport")).foreach { airport =>
              airportShareList.append((airport, resultSet.getDouble("share")))
            } 
          }
          
          resultSet.close()
          preparedStatement.close()
          airportShareList.toList
        } finally {
          connection.close()
        }        
      case None => List.empty 
    }
  }

  def updateChampionInfo(info : List[AirportChampionInfo]) = {

    val connection = Meta.getConnection()
    try {
      connection.setAutoCommit(false)
      val purgeStatement = connection.prepareStatement(s"DELETE FROM $AIRPORT_CHAMPION_TABLE")
      purgeStatement.executeUpdate()
      purgeStatement.close()

      val preparedStatement = connection.prepareStatement(s"INSERT INTO $AIRPORT_CHAMPION_TABLE(airport, airline, loyalist, ranking, reputation_boost) VALUES(?,?,?,?,?)")

      info.foreach {
        entry =>
          preparedStatement.setInt(1, entry.loyalist.airport.id)
          preparedStatement.setInt(2, entry.loyalist.airline.id)
          preparedStatement.setInt(3, entry.loyalist.amount)
          preparedStatement.setInt(4, entry.ranking)
          preparedStatement.setDouble(5, entry.reputationBoost)

          preparedStatement.executeUpdate()
      }

      preparedStatement.close()
      connection.commit()
    } finally {
      connection.close()
    }
  }

  def loadChampionInfoByCriteria(criteria : List[(String, Any)]) = {
    val connection = Meta.getConnection()
    try {
      var queryString = "SELECT * FROM " + AIRPORT_CHAMPION_TABLE

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


      val resultSet = preparedStatement.executeQuery()

      val result = ListBuffer[AirportChampionInfo]()


      while (resultSet.next()) {
        val airportId = resultSet.getInt("airport")
        val airlineId = resultSet.getInt("airline")
        val loyalist = Loyalist(AirportCache.getAirport(airportId).get, AirlineCache.getAirline(airlineId).get, resultSet.getInt("loyalist"))
        result += AirportChampionInfo(loyalist, ranking = resultSet.getInt("ranking"), reputationBoost = resultSet.getDouble("reputation_boost"))
      }
      resultSet.close()
      preparedStatement.close()

      result.toList
    } finally {
      connection.close()
    }
  }

  def loadCitiesServed(airportId : Int) : List[(City,Double)] = {
    val connection = Meta.getConnection()
    try {
      val cityStatement = connection.prepareStatement("SELECT a.*, c.* FROM " + AIRPORT_CITY_SHARE_TABLE + " a LEFT JOIN " + CITY_TABLE + " c ON a.city = c.id WHERE airport = ?")
      cityStatement.setInt(1, airportId)
      val result = ListBuffer[(City, Double)]()
      val cityResultSet = cityStatement.executeQuery()
      while (cityResultSet.next()) {
        val city = City(
          cityResultSet.getString("name"),
          cityResultSet.getDouble("latitude"),
          cityResultSet.getDouble("longitude"),
          cityResultSet.getString("country_code"),
          cityResultSet.getInt("population"),
          cityResultSet.getInt("income"),
          cityResultSet.getInt("city"))
        result.append((city, cityResultSet.getDouble("share")))
      }
      cityResultSet.close()
      cityStatement.close()
      result.toList
    } finally {
      connection.close()
    }
  }
}
