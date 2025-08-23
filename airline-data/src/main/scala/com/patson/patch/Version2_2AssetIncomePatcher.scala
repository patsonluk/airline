package com.patson.patch

import com.patson.data.AirportAssetSource
import com.patson.init.actorSystem
import com.patson.model.{AirportAssetBoostHistory, AirportBoostType, Computation, AirportBoost}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * patcher for v2.1
  */
object Version2_2AssetIncomePatcher extends App {
  mainFlow

  def mainFlow() {
    patchIncomeAsset()

    Await.result(actorSystem.terminate(), Duration.Inf)
  }


  def patchIncomeAsset(): Unit = {
    val allAssets = AirportAssetSource.loadAirportAssetsByAssetCriteria(List.empty)
    allAssets.foreach { asset =>
      val incomeBoost = asset.boosts.filter(_.boostType == AirportBoostType.INCOME)

      if (incomeBoost.length > 1) {
        println(s"!!!!!!!!!!Skipping more than one income boost on asset $asset")
      }


      if (incomeBoost.length == 1) {
        val correctVal : Option[Double] = asset.boostHistory.filter(_.boostType == AirportBoostType.INCOME).sortBy(_.cycle).lastOption.map(_.value)
        if (correctVal.isDefined && correctVal.get != incomeBoost.head.value) {
          val newBoosts : ListBuffer[AirportBoost] = ListBuffer.from(asset.boosts.filterNot(_ == incomeBoost.head))
          newBoosts.append(incomeBoost.head.copy(value = correctVal.get))
          println(s"Asset ${asset} Boost From ${asset.boosts} to ${newBoosts.toList}")
          asset.boosts = newBoosts.toList
          AirportAssetSource.updateAirportAsset(asset)
        }
      }
    }
  }
}