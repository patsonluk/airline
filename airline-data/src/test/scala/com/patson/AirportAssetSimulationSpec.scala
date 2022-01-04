package com.patson

import com.patson.AirportAssetSimulation.{AssetSimulationResult, PassengerStats}
import com.patson.model._
import org.scalatest.{Matchers, WordSpecLike}

class AirportAssetSimulationSpec extends WordSpecLike with Matchers {
  val highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 1000000) //NA city with 1m pop, no charm
  val airline = Airline.fromId(1)
  "simulateHotelPerformance".must {
    "compute reasonable room rate on no/low/mid/high pax (Airport Hotel)".in {
      var assetType = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      var paxStats = PassengerStats(0, 0, 0)
      var result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert(result.revenue - result.expense < 0)
      assert(result.properties("occupancy") == 0)
      assert(result.properties("rate") > 0)

      var transferPax = asset.capacity
      var arrivalPax = asset.capacity
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      val lowRoomRate = result.properties("rate")
      val lowOccupancy = result.properties("occupancy")
      val lowProfit = result.revenue - result.expense
      assert(lowProfit < 0)
      assert(lowOccupancy > 0)
      assert(lowRoomRate > 50 && lowRoomRate < 100)

      transferPax = asset.capacity * 5
      arrivalPax = asset.capacity * 5
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      val midRoomRate = result.properties("rate")
      val midOccupancy = result.properties("occupancy")
      val midProfit = result.revenue - result.expense
      assert(midProfit > 0)
      assert(midOccupancy > 0)
      assert(midRoomRate < 300)

      transferPax = asset.capacity * 100
      arrivalPax = asset.capacity * 100
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      val highRoomRate = result.properties("rate")
      val highOccupancy = result.properties("occupancy")
      val highProfit = result.revenue - result.expense
      assert(highProfit > 0)
      assert(highOccupancy > 0)
      assert(highRoomRate < 500)

      assert(lowRoomRate < midRoomRate)
      assert(midRoomRate < highRoomRate)
    }
    "compute reasonable room rate on all Hotel types (level 1), profit = roi".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      val paxStats = PassengerStats(200000, 200000, 200000)
      var result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      var rate = result.properties("rate")
      assert(rate > 100 && rate < 200)

      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 40 && rate < 200)


      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 200)


      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 30 && rate < 60)

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 50 && rate < 100)

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 150 && rate < 300)
    }
    "compute correct room rate on all Hotel types (level 10), max ROI".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      val paxStats = PassengerStats(200000, 200000, 200000)
      var result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      var rate = result.properties("rate")
      assert(rate > 300 && rate < 600)

      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 300)

      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 200 && rate < 400)

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 300)


      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 250 && rate < 500)


      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 50 && rate < 150)

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 70 && rate < 150)

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 300 && rate < 500)
    }

    "reach max profit when demand is huge".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      val paxStats = PassengerStats(300000, 300000, 300000)
      var result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateHotelAssetPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )
    }
  }


  "simulateAdmissionPerformance".must {
    "compute reasonable rate on no/low/mid/high pax (Amusement Park)".in {
      var highIncomeAirport = Airport.fromId(1).copy(baseIncome = 40000, basePopulation = 100000) //less powerful
      var assetType = AirportAssetType.AMUSEMENT_PARK
      var asset = AmusementParkAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      var paxStats = PassengerStats(0, 0, 0)
      var result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset, paxStats)

      val worstAdmissionRate = result.properties("rate")
      val worstOccupancy = result.properties("visitors")
      val worstProfit = result.revenue - result.expense
      assert(worstProfit < 0)
      assert(worstOccupancy > 500 && worstOccupancy < 3000)
      assert(worstAdmissionRate > 20 && worstAdmissionRate < 60)

      var transferPax = asset.capacity
      var arrivalPax = asset.capacity
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset, paxStats)

      val lowAdmissionRate = result.properties("rate")
      val lowOccupancy = result.properties("visitors")
      val lowProfit = result.revenue - result.expense
      assert(lowProfit < 0)
      assert(lowOccupancy > 0)
      assert(lowAdmissionRate > 30 && lowAdmissionRate < 70)

      transferPax = asset.capacity * 5
      arrivalPax = asset.capacity * 5
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset, paxStats)
      val midAdmissionRate = result.properties("rate")
      val midOccupancy = result.properties("visitors")
      val midProfit = result.revenue - result.expense
      assert(midProfit > 0)
      assert(midOccupancy > 0)
      assert(midAdmissionRate > 50 && midAdmissionRate < 100)

      transferPax = asset.capacity * 15
      arrivalPax = asset.capacity * 15
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset, paxStats)
      val highAdmissionRate = result.properties("rate")
      val highOccupancy = result.properties("visitors")
      val highProfit = result.revenue - result.expense
      assert(highProfit > 0)
      assert(highOccupancy > 0)
      assert(highAdmissionRate > 50 && highAdmissionRate < 100)

      assert(worstOccupancy < lowOccupancy)
      assert(lowOccupancy < midOccupancy)
      assert(midOccupancy < highOccupancy)

      assert(worstProfit < lowProfit)
      assert(lowProfit < midProfit)
      assert(midProfit < highProfit)
    }
    "compute reasonable  rate on all asset types (level 1), profit = roi, maxed out performance at level 1".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AMUSEMENT_PARK
      var asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      val paxStats = PassengerStats(500000, 300000, 300000)
      var result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      var rate = result.properties("rate")
      assert(rate > 100 && rate < 300)

      assetType = AirportAssetType.STADIUM
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.MUSEUM
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.LANDMARK
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 40 && rate < 200)


      assetType = AirportAssetType.SPORT_ARENA
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 50 && rate < 100)


      assetType = AirportAssetType.CINEMA
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 10 && rate < 25)

      assetType = AirportAssetType.GOLF_COURSE
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), List.empty, 0, 0, roi = assetType.initRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 3000 && rate < 5000)
    }

    "compute reasonable rate on all asset types (level 10), profit = maxRoi".in {
      var highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 10000000)
      var assetType : AirportAssetType.Value = AirportAssetType.AMUSEMENT_PARK
      var asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      val paxStats = PassengerStats(500000, 500000, 500000)
      var result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      var rate = result.properties("rate")
      assert(rate > 100 && rate < 1000)

      assetType = AirportAssetType.STADIUM
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 300 && rate < 1000)

      assetType = AirportAssetType.MUSEUM
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 300 && rate < 1000)

      assetType = AirportAssetType.LANDMARK
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 300 && rate < 1000)


      assetType = AirportAssetType.SPORT_ARENA
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 400)


      assetType = AirportAssetType.CINEMA
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 30 && rate < 60)

      assetType = AirportAssetType.GOLF_COURSE
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      rate = result.properties("rate")
      assert(rate > 10000 && rate < 20000)


    }
    "reach max profit when demand is huge".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AMUSEMENT_PARK
      var highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 8000000)
      var asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      var paxStats = PassengerStats(300000, 500000, 300000)
      var result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.STADIUM
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 6000000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(200000, 300000, 300000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.MUSEUM
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 6000000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(300000, 500000, 500000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.LANDMARK
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 8000000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(300000, 500000, 500000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.SPORT_ARENA
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 1500000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(50000, 100000, 100000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.CINEMA
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 1000000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(50000, 50000, 50000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.GOLF_COURSE
      highIncomeAirport = Airport.fromId(1).copy(baseIncome = 50000, basePopulation = 8000000)
      asset = AirportAsset.getAirportAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), List.empty, 0, 0, roi = assetType.maxRoi, false, Map.empty, 0 + assetType.constructionDuration)
      paxStats = PassengerStats(300000, 500000, 300000)
      result = AirportAssetSimulation.simulateAdmissionAssetPerformance(asset.asInstanceOf[AdmissionAsset], paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )
    }
  }



}
