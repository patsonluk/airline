package com.patson.patch

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.patson.data.Constants.{DATABASE_CONNECTION, DATABASE_PASSWORD, DATABASE_USER, DB_DRIVER}
import com.patson.data.{AirportAssetSource, AirportSource, Meta}
import com.patson.init.{AirplaneModelPatcher, AirportGeoPatcher, AssetBlueprintGenerator, actorSystem}
import com.patson.model.{AirportAsset, AirportAssetBoostHistory, AirportBoostType, Computation}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v2.1
  */
object Version2_2Patcher extends App {
  mainFlow

  def mainFlow() {
    patchAssetIncomeBoosts()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def patchAssetIncomeBoosts(): Unit = {
    val allAssets = AirportAssetSource.loadAirportAssetsByAssetCriteria(List.empty)
    allAssets.foreach { asset =>
      var changed = false
      val newBoosts = asset.boosts.map { boost =>
        if (boost.boostType == AirportBoostType.INCOME) { //the boost was on income level
          val incomeBoost = Computation.computeIncomeBoostFromLevel(asset.airport.baseIncome, boost.value)
          changed = true
          boost.copy(value = incomeBoost)
        } else {
          boost
        }
      }
      if (changed) {
        asset.boosts = newBoosts
        AirportAssetSource.updateAirportAsset(asset)

        var incomeWalker = Computation.fromIncomeLevel(asset.airport.baseIncomeLevel + asset.blueprint.assetType.baseBoosts.find(_.boostType == AirportBoostType.INCOME).get.value)
        //now patch history too

        val newHistoryEntries : List[AirportAssetBoostHistory] = asset.boostHistory.sortBy(_.cycle).map { history =>
          if (history.boostType == AirportBoostType.INCOME) {
            val incomeAtThisLevel = Computation.fromIncomeLevel(asset.airport.baseIncomeLevel + history.value)
            val incomeGain = incomeAtThisLevel - incomeWalker
            val incomeBoost = incomeAtThisLevel - asset.airport.baseIncome
            incomeWalker = incomeAtThisLevel
            history.copy(value = incomeBoost, gain = incomeGain)
          } else {
            history
          }
        }
        AirportAssetSource.saveAirportBoostHistory(newHistoryEntries)
      }
    }
  }
}