package com.patson

import com.patson.AirportAssetSimulation.{AssetSimulationResult, PassengerStats}
import com.patson.model._
import org.scalatest.{Matchers, WordSpecLike}

class AirportAssetSimulationSpec extends WordSpecLike with Matchers {
  val highIncomeAirport = Airport.fromId(1).copy(baseIncome = 40000, basePopulation = 1000000)
  val airline = Airline.fromId(1)
  "simulateHotelPerformance".must {
    "compute reasonable room rate on no/low/mid/high pax (Airport Hotel)".in {
      var assetType = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      var paxStats = PassengerStats(0, 0, 0)
      var result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert(result.revenue - result.expense < 0)
      assert(result.properties("occupancy") == 0)
      assert(result.properties("rate") > 0)

      var transferPax = asset.capacity
      var arrivalPax = asset.capacity
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      val lowRoomRate = result.properties("rate")
      val lowOccupancy = result.properties("occupancy")
      val lowProfit = result.revenue - result.expense
      assert(lowProfit < 0)
      assert(lowOccupancy > 0)
      assert(lowRoomRate > 50 && lowRoomRate < 100)

      transferPax = asset.capacity * 5
      arrivalPax = asset.capacity * 5
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      val midRoomRate = result.properties("rate")
      val midOccupancy = result.properties("occupancy")
      val midProfit = result.revenue - result.expense
      assert(midProfit > 0)
      assert(midOccupancy > 0)
      assert(midRoomRate < 300)

      transferPax = asset.capacity * 100
      arrivalPax = asset.capacity * 100
      paxStats = PassengerStats(transferPax, arrivalPax, 0)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
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
      var result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      var rate = result.properties("rate")
      assert(rate > 100 && rate < 200)

      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 250)

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 40 && rate < 200)


      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 200)


      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 30 && rate < 60)

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 50 && rate < 100)

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 1, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.initRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 150 && rate < 300)
    }
    "compute correct room rate on all Hotel types (level 10), max ROI".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      val paxStats = PassengerStats(200000, 200000, 200000)
      var result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      var rate = result.properties("rate")
      assert(rate > 300 && rate < 600)

      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 300)

      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 200 && rate < 400)

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 100 && rate < 300)


      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 250 && rate < 500)


      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 50 && rate < 150)

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 70 && rate < 150)

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      rate = result.properties("rate")
      assert(rate > 300 && rate < 500)
    }

    "reach max profit when demand is huge".in {
      var assetType : AirportAssetType.Value = AirportAssetType.AIRPORT_HOTEL
      var asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      val paxStats = PassengerStats(300000, 300000, 300000)
      var result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.GRAND_HOTEL_TOURIST
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )


      assetType = AirportAssetType.GRAND_HOTEL_BUSINESS
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.BEACH_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.SKI_RESORT
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.INN
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )

      assetType = AirportAssetType.LUXURIOUS_HOTEL
      asset = AirportHotelAsset(AirportAssetBlueprint(highIncomeAirport, assetType), Some(airline), "", 10, Some(0), AirportAssetStatus.COMPLETED, List.empty, 0, 0, roi = assetType.maxRoi, Map.empty)
      result = AirportAssetSimulation.simulateAirportHotelPerformance(asset, paxStats)
      assert((result.revenue - result.expense).toDouble / asset.value * 52 > assetType.maxRoi * 0.99 )
      assert((result.revenue - result.expense).toDouble / asset.value * 52 < assetType.maxRoi * 1.01 )
    }
  }



}
