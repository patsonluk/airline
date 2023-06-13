package com.patson

import com.patson.data.{AirlineSource, AirportAssetSource, DelegateSource}
import com.patson.model.Country.CountryCode
import com.patson.model._

import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode
import scala.collection.{MapView, mutable}

object AirportAssetSimulation {

  def computeAirportPaxStats(airportIds : Set[Int], linkRidershipDetails : Map[(PassengerGroup, Airport, Route), Int]) : Map[Int, PassengerStats] = {

    val arrivalGroupsByDestAirportIds = mutable.Map[Int, ListBuffer[(PassengerGroup, Int)]]() //key is arrival airport ID
    val departureGroupsByAirportIds = mutable.Map[Int, ListBuffer[(PassengerGroup, Int)]]() //key is departure airport ID
    val transitGroupsByAirportIds =  mutable.Map[Int, ListBuffer[(PassengerGroup, Int)]]() //key is transit airport ID

    //init with keys that we care
    airportIds.foreach { airportId =>
      arrivalGroupsByDestAirportIds.put(airportId, ListBuffer())
      departureGroupsByAirportIds.put(airportId, ListBuffer())
      transitGroupsByAirportIds.put(airportId, ListBuffer())
    }



    linkRidershipDetails.foreach {
      case((group, toAirport, route), passengerCount) =>
        arrivalGroupsByDestAirportIds.get(toAirport.id) match {
          case Some(groups) => groups.append((group, passengerCount))
          case None => //airports that we don't care (no assets), let's not build the list
        }

        val fromAirport = group.fromAirport
        departureGroupsByAirportIds.get(fromAirport.id) match {
          case Some(groups) => groups.append((group, passengerCount))
          case None => //airports that we don't care (no assets), let's not build the list
        }

        for (i <- 0 until route.links.size) {
          if (i > 0) {
            val airportId = route.links(i).from.id
            transitGroupsByAirportIds.get(airportId) match {
              case Some(groups) => groups.append((group, passengerCount))
              case None => //airports that we don't care (no assets), let's not build the list
            }
          }
        }


    }

    airportIds.map { airportId =>
      val stats = PassengerStats(
        transitGroupsByAirportIds(airportId).map(_._2).sum,
        arrivalGroupsByDestAirportIds(airportId).filter(_._1.passengerType != PassengerType.BUSINESS).map(_._2).sum,
        arrivalGroupsByDestAirportIds(airportId).filter(_._1.passengerType == PassengerType.BUSINESS).map(_._2).sum,
        departureGroupsByAirportIds(airportId).filter(_._1.passengerType != PassengerType.BUSINESS).map(_._2).sum,
        departureGroupsByAirportIds(airportId).filter(_._1.passengerType == PassengerType.BUSINESS).map(_._2).sum,
      )
      (airportId, stats)
    }.toMap
  }

  case class PaxAssetStats(countryStats :MapView[CountryCode, Int], transitCount: Int, totalCount : Int)
  def computeAssetPaxStats(linkRidershipDetails : Map[(PassengerGroup, Airport, Route), Int]) : Map[AirportAsset, PaxAssetStats] = {
    val flattenedList : List[(AirportAsset, (PassengerGroup, Int))] = linkRidershipDetails.toList.flatMap {
      case((paxGroup, airport, route), paxCount) => route.visitedAssets.map( asset => (asset, (paxGroup, paxCount)))
    }
    val paxGroupByAsset : MapView[AirportAsset, List[(PassengerGroup, Int)]] = flattenedList.groupBy(_._1).view.mapValues {
      list => list.map(_._2)
    }

    val countryPaxByAsset : MapView[AirportAsset, MapView[CountryCode, Int]] = paxGroupByAsset.mapValues { paxGroupList =>
      paxGroupList.map {
        case (paxGroup, paxCount) => (paxGroup.fromAirport.countryCode, paxCount)
      }.groupBy(_._1).view.mapValues { countryCodePaxCountList =>
         countryCodePaxCountList.map(_._2).sum
      }
    }

    val flattenIsDestinationList : List[(AirportAsset, (Boolean, Int))] = linkRidershipDetails.toList.flatMap {
      case ((paxGroup, destinationAirport, route), paxCount) => route.visitedAssets.map(asset => (asset, (asset.airport.id == destinationAirport.id, paxCount)))
    }

    val finalResult = mutable.HashMap[AirportAsset, PaxAssetStats]()

    val isDestinationPaxByAsset : MapView[AirportAsset, List[(Boolean, Int)]] = flattenIsDestinationList.groupBy(_._1).view.mapValues(list => list.map(_._2))
    isDestinationPaxByAsset.foreach {
      case(asset, isDestinationPaxList) =>
        var transitPax = 0
        var totalPax = 0
        isDestinationPaxList.foreach {
          case (isFinalDestination, paxCount) =>
            if (!isFinalDestination) {
              transitPax += paxCount
            }
            totalPax += paxCount
        }
        //combine the 2 map, both should have same keys
        val finalStats = PaxAssetStats(countryPaxByAsset(asset), transitPax, totalPax)
        finalResult.put(asset, finalStats)
    }
    finalResult.toMap
  }

  def extractPropertiesFromStats(assetPaxStats : PaxAssetStats) : Map[String, String] = {
    AirportAsset.getPropertiesFromCountryStats(assetPaxStats.countryStats.toMap) ++ AirportAsset.getPropertiesFromPaxTypeStats(assetPaxStats.transitCount, assetPaxStats.totalCount)
  }

  def simulate(currentCycle : Int, linkRidershipDetails : Map[(PassengerGroup, Airport, Route), Int]) = {

    val allAssets = AirportAssetSource.loadAirportAssetsByAssetCriteria(List.empty)
    val allAssetPropertiesHistory = ListBuffer[AirportAssetPropertiesHistory]()

    val allAirportIds = allAssets.map(_.airport.id).toSet
    val paxStatsByAirportId = computeAirportPaxStats(allAirportIds, linkRidershipDetails) //currently use pax stats for profit computation
    val paxStatsByAsset = computeAssetPaxStats(linkRidershipDetails)//for visited assets, fun facts - top 3 countries, total pax, transit %; Otherwise, might have to rewrite a lot of profit logic...


    allAssets.foreach { asset =>
      //check for changes due to upgrade
      checkUpgradeCompletion(asset, currentCycle + 1) match { //simulate one cycle ahead, otherwise the asset would be stuck at COMPLETED w/o bonus
        case Some((newBoosts, newRoi, upgradeFactor)) =>
          println(s"$asset upgrading to $newBoosts with new Roi $newRoi upgrade factor $upgradeFactor")
          asset.boosts = newBoosts.map(_._1)
          //save the boost history
          val newBoostHistory = newBoosts.map {
            case (boost, gain) => AirportAssetBoostHistory(asset.id, asset.level, boost.boostType, boost.value, gain, upgradeFactor, currentCycle)
          }
          AirportAssetSource.saveAirportBoostHistory(newBoostHistory)

          asset.roi = newRoi
          asset.upgradeApplied = true
        case None => //do nothing
      }

      val result = simulateAssetBusiness(asset, paxStatsByAirportId(asset.airport.id))
      asset.revenue = result.revenue
      asset.expense = result.expense
      asset.properties = asset.properties ++ result.properties


      AirportAssetSource.updateAirportAsset(asset)

      var propertiesHistory = asset.properties.view.mapValues(_.toString).toMap + ("revenue" -> result.revenue.toString) + ("expense" -> result.expense.toString)
      paxStatsByAsset.get(asset).foreach { assetPaxStats => //just for fun fact, save to history only for now
        propertiesHistory = propertiesHistory ++ extractPropertiesFromStats(assetPaxStats)
      }

      allAssetPropertiesHistory.append(AirportAssetPropertiesHistory(asset.id, propertiesHistory , currentCycle))
    }

    AirportAssetSource.deleteAirportPropertiesHistory(currentCycle - 100)
    AirportAssetSource.saveAirportPropertiesHistory(allAssetPropertiesHistory.toList)

  }

  /**
    * Check whether there should be new boosts
    *
    * @return Some if there are new boosts. 2nd value in list tuple is boost gain
    */
  def checkUpgradeCompletion(asset : AirportAsset, cycle : Int) : Option[(List[(AirportBoost, Double)], Double, Double)] = { //(new boost, new roi, upgrade factor)
    //if (asset.status == AirportAssetStatus.COMPLETED && !asset.upgradeApplied) {
    if (asset.completionCycle.get <= cycle && !asset.upgradeApplied) {
      val history = asset.boostHistory
      if (history.isEmpty || history.map(_.level).max < asset.level) { //double check, the upgradeApplied flag is actually good enough
        val previousLevelBoosts =
          if (history.isEmpty) { //use basic as starting point
            asset.baseBoosts
          } else {
            asset.boosts
          }
        val upgradeFactor = generateUpgradeFactor(asset)
        Some(computeNewBoosts(asset, previousLevelBoosts, upgradeFactor), computeNewRoi(asset, upgradeFactor), upgradeFactor)
      } else {
        None
      }
    } else {
      None
    }
  }

  /**
    * how successful the upgrade is, from 0 to 1
    */
  def generateUpgradeFactor(asset : AirportAsset) : Double = {
    //get performance factor up to last 10 weeks
    val historyEntries = AirportAssetSource.loadAirportPropertyHistoryByAssetId(asset.id).sortBy(_.cycle).takeRight(10)

    val performances : Seq[Double] = historyEntries.map { entry =>
      entry.properties.get("performance").map(_.toLong).getOrElse(0L) / 100.0
    }

    if (performances.length == 0 || asset.level == 1) {  //all rng for level 1
      Math.random()
    } else {
      val finalPerformance =  performances.sum / performances.length //from 0 to 1
      Math.random() * 0.5 + finalPerformance * 0.5 //50% performance 50% luck
    }


  }

  def computeNewBoosts(asset : AirportAsset, previousLevelBoosts : List[AirportBoost], upgradeFactor : Double) : List[(AirportBoost, Double)] = { //2nd value is gain
    previousLevelBoosts.map { previousBoost =>
      val maxGain = asset.baseBoosts.find(_.boostType == previousBoost.boostType).get.value * 0.2
      var gain = maxGain * (0.2 + upgradeFactor * 0.8)
      if (AirportBoostType.getValueType(previousBoost.boostType) == classOf[Long]) {
        gain = gain.toLong
      } else {
        gain = BigDecimal(gain).setScale(3, RoundingMode.HALF_UP).toDouble
      }
      var newValue = previousBoost.value + gain
      if (AirportBoostType.getValueType(previousBoost.boostType) == classOf[Long]) {
        newValue = newValue.toLong
      }
      (AirportBoost(previousBoost.boostType, newValue), gain)
    }
  }

  def computeNewRoi(asset : AirportAsset, upgradeFactor : Double) = {
    val maxGrowth = (asset.assetType.maxRoi - asset.assetType.initRoi) / AirportAsset.MAX_LEVEL
    val delta = { // 0 -> -(max growth), 0.7 -> neutral, at 1 -> max growth
      if (upgradeFactor >= 0.7) {
        (upgradeFactor - 0.7) / 0.3 * maxGrowth
      } else {
        (upgradeFactor - 0.7) / 0.7 * maxGrowth
      }
    }
    Math.max(asset.assetType.minRoi, asset.roi + delta)
  }

  /**
    *
    * @param asset
    */
  def simulateAssetBusiness(asset : AirportAsset, paxStats : PassengerStats) : AssetSimulationResult = {
    import com.patson.model.AirportAssetType._
    if (!asset.isOperational()) {
      AssetSimulationResult(0, 0, Map.empty)
    } else {
      val result : AssetSimulationResult = asset.assetType match {
        case AIRPORT_HOTEL | GRAND_HOTEL_TOURIST | GRAND_HOTEL_BUSINESS | BEACH_RESORT | SKI_RESORT | INN | HOTEL | LUXURIOUS_HOTEL =>
          simulateHotelAssetPerformance(asset.asInstanceOf[HotelAsset], paxStats)
        case AMUSEMENT_PARK | STADIUM | MUSEUM | LANDMARK | SPORT_ARENA | CINEMA | GOLF_COURSE =>
          simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
        case OFFICE_BUILDING_1 | OFFICE_BUILDING_2 | OFFICE_BUILDING_3 | OFFICE_BUILDING_4 | RESIDENTIAL_COMPLEX | SCIENCE_PARK | SHOPPING_MALL =>
          simulateRentalAssetPerformance(asset.asInstanceOf[RentalAsset], paxStats)
        case CITY_TRANSIT | SUBWAY | CONVENTION_CENTER | SOLAR_POWER_PLANT | TRAVEL_AGENCY | GAME_ARCADE | RESTAURANT =>
          simulateGenericAssetPerformance(asset, paxStats)
        case _ =>
          println(s"Missing business sim for ${asset.assetType}")
          AssetSimulationResult(0, 0, Map.empty)
      }
      result
    }
  }

  case class PassengerStats(transferPax : Long, arrivalTourist : Long, arrivalBusiness : Long, departureTourist : Long, departureBusiness : Long) {
    val arrivalPax = arrivalTourist + arrivalBusiness
    val departurePax = departureTourist + departureBusiness
  }

  def simulateHotelAssetPerformance(asset : HotelAsset, paxStats : PassengerStats): AssetSimulationResult = {
    var potentialGuests : Int = asset.assetType match {
      case com.patson.model.AirportAssetType.AIRPORT_HOTEL =>
      //assume 30% transfer pax will use airport hotel, 5% arrival will use this
        (paxStats.transferPax * 0.3 + paxStats.arrivalPax * 0.05).toInt
      case com.patson.model.AirportAssetType.GRAND_HOTEL_TOURIST =>
        (paxStats.arrivalTourist * 0.35).toInt
      case com.patson.model.AirportAssetType.GRAND_HOTEL_BUSINESS =>
        (paxStats.arrivalBusiness * 0.3).toInt + (paxStats.arrivalBusiness * 0.05).toInt
      case com.patson.model.AirportAssetType.BEACH_RESORT =>
        (paxStats.arrivalTourist * 0.5).toInt
      case com.patson.model.AirportAssetType.SKI_RESORT =>
        (paxStats.arrivalTourist * 0.5).toInt
      case com.patson.model.AirportAssetType.INN =>
        (paxStats.arrivalPax * 0.2).toInt
      case com.patson.model.AirportAssetType.HOTEL =>
        (paxStats.arrivalPax * 0.2).toInt
      case com.patson.model.AirportAssetType.LUXURIOUS_HOTEL =>
        (paxStats.arrivalTourist * 0.1 + paxStats.arrivalBusiness * 0.2).toInt
      case _ => println(s"Unknown hotel type for performance computation!! ${asset.assetType}")
        0
    }


    potentialGuests = (potentialGuests * Util.getBellRandom(1)).toInt

    val potentialToCapRatio = potentialGuests.toDouble / asset.capacity
    //potentialGuests has to be
    // 10 times of capacity => 100% performance
    // 1 time of full capacity => 50% performance
    val performanceFactor = if (potentialToCapRatio < 1) 0.5 * potentialToCapRatio else 0.5 + 0.5 * Math.min(1, potentialToCapRatio / 10)

    val neutralProfitFactor = 0.25 //start losing money < 0.25 performance

    val weeklyProfit = asset.value * asset.roi / 52 * (performanceFactor - neutralProfitFactor) / (1 - neutralProfitFactor)
    //from profit, deduce expense by considering revenue = 0 at performanceFactor = 0.
    val baseExpense = asset.value * asset.roi / 52 * (0 - neutralProfitFactor) / (1 - neutralProfitFactor) * -1

    //very easy to get to 80%, then harder
    val occupancy : Int =
      ((if (performanceFactor <= 0.5) { //80% max
        0.8 * performanceFactor / 0.5
      } else {
        0.8 + (performanceFactor - 0.5) * 0.2 / 0.5
      }) * asset.capacity).toInt

    //expense increase slightly per occupancy
    val costPerGuestPerNight = asset.assetType match {
      case com.patson.model.AirportAssetType.AIRPORT_HOTEL => 25
      case com.patson.model.AirportAssetType.GRAND_HOTEL_TOURIST => 100
      case com.patson.model.AirportAssetType.GRAND_HOTEL_BUSINESS => 100
      case com.patson.model.AirportAssetType.BEACH_RESORT => 50
      case com.patson.model.AirportAssetType.SKI_RESORT => 50
      case com.patson.model.AirportAssetType.INN => 35
      case com.patson.model.AirportAssetType.HOTEL => 40
      case com.patson.model.AirportAssetType.LUXURIOUS_HOTEL => 150
      case _ =>
        println(s"Unknown hotel type for costPerGuest!! ${asset.assetType}")
        0
    }


    //finally from revenue deduce room rate
    //expense increase slightly per occupancy
    val nightsPerGuest = asset.assetType match {
      case com.patson.model.AirportAssetType.AIRPORT_HOTEL => 2
      case com.patson.model.AirportAssetType.GRAND_HOTEL_TOURIST => 5
      case com.patson.model.AirportAssetType.GRAND_HOTEL_BUSINESS => 3
      case com.patson.model.AirportAssetType.BEACH_RESORT => 7
      case com.patson.model.AirportAssetType.SKI_RESORT => 5
      case com.patson.model.AirportAssetType.INN => 3
      case com.patson.model.AirportAssetType.HOTEL => 5
      case com.patson.model.AirportAssetType.LUXURIOUS_HOTEL => 5
      case _ =>
        println(s"Unknown hotel type for costPerGuest!! ${asset.assetType}")
        1
    }

    val costPerGuest = costPerGuestPerNight * nightsPerGuest
    val expense = baseExpense + occupancy * costPerGuest
    val revenue = expense + weeklyProfit

    val roomRate =
      if (revenue > 0) {
        (revenue / occupancy / nightsPerGuest * 0.8).toInt //*0.8, assume 20% income from something else
      } else {
        (costPerGuest * 1.5).toInt
      }

    val properties : Map[String, Long] = Map("occupancy" -> occupancy, "rate" -> roomRate, "performance" -> (performanceFactor * 100).toLong)
    AssetSimulationResult(revenue.toLong, expense.toLong, properties)
  }

  def simulateRentalAssetPerformance(asset : RentalAsset, paxStats : PassengerStats): AssetSimulationResult = {
    val airport = asset.airport
    var potentialLeases : Double = asset.assetType match {
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_1 => //depends heavily if the renter can travel to other places
        airport.population * airport.incomeLevel / 50000 / 40 +
        paxStats.departureBusiness.toDouble / 50
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_2 =>
        airport.population * airport.incomeLevel / 70000 / 40 +
        paxStats.departureBusiness.toDouble / 80
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_3 =>
        airport.population * airport.incomeLevel / 90000 / 40 +
        paxStats.departureBusiness.toDouble / 140
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_4 =>
        airport.population * airport.incomeLevel / 130000 / 40 +
        paxStats.departureBusiness.toDouble / 275
      case com.patson.model.AirportAssetType.RESIDENTIAL_COMPLEX =>
        airport.population * airport.incomeLevel / 250 / 30 +
        paxStats.departurePax.toDouble * 10
      case com.patson.model.AirportAssetType.SCIENCE_PARK =>
        airport.population * airport.incomeLevel / 40000 / 40 +
        paxStats.departureBusiness.toDouble / 450 +
        paxStats.arrivalBusiness.toDouble / 450

      case com.patson.model.AirportAssetType.SHOPPING_MALL =>
        airport.population * airport.incomeLevel / 65000 / 40 +
        paxStats.departurePax.toDouble / 2000 +
        paxStats.arrivalPax.toDouble / 800

      case _ => println(s"Unknown rental type for performance computation!! ${asset.assetType}")
        0
    }


    val potentialDemandSpace =  potentialLeases * asset.spacePerLease * Util.getBellRandom(1, 0.1)

    val potentialToCapRatio = potentialDemandSpace / asset.space
    //potentialGuests has to be
    // 2 times of capacity => 100% performance
    // 1 time of full capacity => 50% performance
    val performanceFactor = Math.min(1, potentialToCapRatio / 2)

    val neutralProfitFactor = 0.2 //start losing money < 0.2 performance

    //from profit, deduce expense by considering revenue = 0 at performanceFactor = 0.
    val baseExpense = asset.value * asset.roi / 52 * (0 - neutralProfitFactor) / (1 - neutralProfitFactor) * -1

    //very easy to get to 70%, then harder
    val occupancyRate : Double =
      if (performanceFactor <= 0.5) { //70% max
        0.7 * performanceFactor / 0.5
      } else {
        0.7 + (performanceFactor - 0.5) * 0.3 / 0.5
      }

    val leasesSigned = (asset.maxLeaseCount * occupancyRate).toInt

    //expense increase slightly per lease per week
    val costPerLeasePerWeek = asset.assetType match {
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_1 => 50
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_2 => 100
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_3 => 150
      case com.patson.model.AirportAssetType.OFFICE_BUILDING_4 => 200
      case com.patson.model.AirportAssetType.RESIDENTIAL_COMPLEX => 50
      case com.patson.model.AirportAssetType.SCIENCE_PARK => 2000
      case com.patson.model.AirportAssetType.SHOPPING_MALL => 400
      case _ =>
        println(s"Unknown rental type for costPerLeasePerWeek!! ${asset.assetType}")
        0
    }

    val expense = baseExpense + leasesSigned * costPerLeasePerWeek
    val weeklyProfit = if (leasesSigned == 0) -1 * expense else asset.value * asset.roi / 52 * (performanceFactor - neutralProfitFactor) / (1 - neutralProfitFactor)

    val revenue = expense + weeklyProfit

    val leaseRate = //per sq ft per month (4 weeks)!
      if (revenue > 0) {
        (revenue * 4) / (leasesSigned * asset.spacePerLease)
      } else {
        (costPerLeasePerWeek * 4 * asset.maxLeaseCount).toDouble / asset.space * 1.5
      }

    val properties : Map[String, Long] = Map("leasedSpace" -> leasesSigned * asset.spacePerLease, "rate100Point" -> (leaseRate * 100).toLong, "performance" -> (performanceFactor * 100).toLong) //leaseRate * 100 to keep last 2 digits here
    AssetSimulationResult(revenue.toLong, expense.toLong, properties)
  }

  def simulateAdmissionAssetPerformance(asset : AdmissionAsset, paxStats : PassengerStats): AssetSimulationResult = {
    var potentialGuests = asset.assetType match {
      case com.patson.model.AirportAssetType.AMUSEMENT_PARK =>
        (paxStats.arrivalTourist * 0.4 +
          (if (asset.airport.incomeLevel >= 30) {
            1
          } else {
            asset.airport.incomeLevel / 50
          }) * asset.airport.population / 52 / 2).toInt //assuming income level 30, local pop visit it once every 2 years
      case com.patson.model.AirportAssetType.STADIUM =>
        (paxStats.arrivalPax * 0.1 +
          (if (asset.airport.incomeLevel >= 20) {
            1
          } else {
            asset.airport.incomeLevel / 25
          }) * asset.airport.population / 52).toInt
      case com.patson.model.AirportAssetType.MUSEUM =>
        (paxStats.arrivalPax * 0.25 +
          (if (asset.airport.incomeLevel >= 40) {
            1
          } else {
            asset.airport.incomeLevel / 80
          }) * asset.airport.population / 52 / 1.5).toInt
      case com.patson.model.AirportAssetType.LANDMARK =>
        (paxStats.arrivalTourist * 0.5 + paxStats.arrivalBusiness * 0.2 +
          (if (asset.airport.incomeLevel >= 25) {
            1
          } else {
            asset.airport.incomeLevel / 30
          }) * asset.airport.population / 52 / 10).toInt
      case com.patson.model.AirportAssetType.SPORT_ARENA =>
        (paxStats.arrivalPax * 0.15 +
          (if (asset.airport.incomeLevel >= 25) {
            1
          } else {
            asset.airport.incomeLevel / 30
          }) * asset.airport.population / 52 * 4).toInt
      case com.patson.model.AirportAssetType.CINEMA =>
        (paxStats.arrivalPax * 0.15 +
          (if (asset.airport.incomeLevel >= 20) {
            1
          } else {
            asset.airport.incomeLevel / 25
          }) * asset.airport.population / 52 * 12).toInt
      case com.patson.model.AirportAssetType.GOLF_COURSE =>
        (paxStats.arrivalPax * 0.03 +
          (if (asset.airport.incomeLevel >= 45) {
            1
          } else {
            asset.airport.incomeLevel / 120
          }) * asset.airport.population / 52 / 100).toInt
      case _ =>
        println(s"Unknown admission type ${asset.assetType}")
        0
    }

    potentialGuests = (potentialGuests * Util.getBellRandom(1, 0.2)).toInt

    val potentialToCapRatio = potentialGuests.toDouble / asset.capacity
    //potentialGuests has to be 5 times of capacity for 100% performance, otherwise at 70% for full capacity
    val performanceFactor = if (potentialToCapRatio < 1) 0.7 * potentialToCapRatio else 0.7 + 0.3 * Math.min(1, potentialToCapRatio / 5)

    val neutralProfitFactor = 0.6 //start losing money < 0.6 performance

    val weeklyProfit = asset.value * asset.roi / 52 * (performanceFactor - neutralProfitFactor) / (1 - neutralProfitFactor)
    val expense = asset.value * asset.roi / 52 * (0 - neutralProfitFactor) / (1 - neutralProfitFactor) * -1
    val visitorsApprox = Math.min(potentialGuests, asset.capacity)
    val revenue = expense + weeklyProfit

    //a little bit different here, use ticket price step
    val ticketStep = asset.assetType match {
      case com.patson.model.AirportAssetType.AMUSEMENT_PARK =>
        10
      case com.patson.model.AirportAssetType.STADIUM =>
        10
      case com.patson.model.AirportAssetType.MUSEUM =>
        5
      case com.patson.model.AirportAssetType.LANDMARK =>
        5
      case com.patson.model.AirportAssetType.SPORT_ARENA =>
        5
      case com.patson.model.AirportAssetType.CINEMA =>
        2
      case com.patson.model.AirportAssetType.GOLF_COURSE =>
        20
      case _ =>
        println(s"Unknown admission type ${asset.assetType}")
        1
    }
    var ticketPriceApprox =
      if (visitorsApprox == 0) {
        0
      } else {
        revenue.toDouble / visitorsApprox
      }
    val ticketPrice = ((ticketPriceApprox / ticketStep).toInt + 1) * ticketStep
    val visitors = (revenue / ticketPrice).toInt //adjust visitors


    val properties : Map[String, Long] = Map("visitors" -> visitors, "rate" -> ticketPrice, "performance" -> (performanceFactor * 100).toLong)
    AssetSimulationResult(revenue.toLong, expense.toLong, properties)
  }


  def simulateGenericAssetPerformance(asset : AirportAsset, paxStats : PassengerStats): AssetSimulationResult = {
    val airport = asset.airport


    //the boundary # that the asset will reach max performance at level 1 (on its own per figure), neutralProfit Factor - at what performance 0 - 1 will it be profit = 0
    val (boundaryPop, boundaryIncomeLevel, boundaryPaxStats, neutralProfitFactor) = asset.assetType match {
      case com.patson.model.AirportAssetType.SUBWAY =>
        (5000000, 35, PassengerStats(400000, 200000, 200000, 150000, 150000), 0.8)
      case com.patson.model.AirportAssetType.CITY_TRANSIT =>
        (5000000, 35, PassengerStats(100000, 200000, 200000, 50000, 50000), 0.8)
      case com.patson.model.AirportAssetType.CONVENTION_CENTER =>
        (4000000, 40, PassengerStats(200000, 200000, 200000, 400000, 400000), 0.5)
      case com.patson.model.AirportAssetType.SOLAR_POWER_PLANT =>
        (200000, 40, PassengerStats(20000, 20000, 20000, 5000, 5000), 0.8)
      case com.patson.model.AirportAssetType.TRAVEL_AGENCY =>
        (500000, 40, PassengerStats(Long.MaxValue, Long.MaxValue, Long.MaxValue, 10000, 10000), 0.4)
      case com.patson.model.AirportAssetType.GAME_ARCADE =>
        (150000, 40, PassengerStats(10000, 5000, 5000, 5000, 5000), 0.4)
      case com.patson.model.AirportAssetType.RESTAURANT =>
        (50000, 40, PassengerStats(5000, 2000, 2000, 2000, 2000), 0.6)

      case _ => println(s"Unknown generic asset type for performance computation!! ${asset.assetType}")
        (4000000, 40, PassengerStats(150000, 100000, 100000, 200000, 200000), 0.5)
    }

    var performanceFactor =
      ((airport.population * airport.incomeLevel) / (boundaryPop * boundaryIncomeLevel) + //pop factor
      paxStats.transferPax.toDouble / boundaryPaxStats.transferPax +
      paxStats.arrivalTourist.toDouble / boundaryPaxStats.arrivalTourist +
      paxStats.arrivalBusiness.toDouble / boundaryPaxStats.arrivalBusiness +
      paxStats.departureTourist.toDouble / boundaryPaxStats.departureTourist +
      paxStats.departureBusiness.toDouble / boundaryPaxStats.departureBusiness) / asset.level

    performanceFactor = Math.min(1, performanceFactor) * Util.getBellRandom(1, 0.1)

    //from profit, deduce expense by considering revenue = 0 at performanceFactor = 0.
    val expense = asset.value * asset.roi / 52 * (0 - neutralProfitFactor) / (1 - neutralProfitFactor) * -1
    val weeklyProfit = asset.value * asset.roi / 52 * (performanceFactor - neutralProfitFactor) / (1 - neutralProfitFactor)

    val revenue = expense + weeklyProfit


    val properties : Map[String, Long] = Map("performance" -> (performanceFactor * 100).toLong)
    AssetSimulationResult(revenue.toLong, expense.toLong, properties)
  }

  case class AssetSimulationResult(revenue : Long, expense : Long, properties : Map[String, Long])
}
