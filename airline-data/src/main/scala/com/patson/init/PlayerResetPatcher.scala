package com.patson.init

import com.patson.data.airplane.ModelSource
import com.patson.data._
import com.patson.model._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Regenerate ALL airport data (pops, runway, power etc) without wiping the existing airport DB
  *
  * It will attempt to update the airport if it's already existed and insert airport otherwise
  *
  * it will NOT purge airports that no longer in the CSV file tho
  *
  */
object PlayerResetPatcher extends App {
  //implicit val materializer = FlowMaterializer()

  mainFlow


  def mainFlow() {
    val airlines = AirlineSource.loadAllAirlines()
    airlines.foreach { airline =>
      Airline.resetAirline(airline.id, 0, true)

    }
    LoyalistSource.deleteLoyalists(LoyalistSource.loadLoyalistsByCriteria(List.empty))
    val nextCycle = CycleSource.loadCycle() + 1
    LoyalistSource.deleteLoyalistHistoryBeforeCycle(nextCycle)
    AllianceSource.deleteAllianceByCriteria(List.empty)
    LinkStatisticsSource.deleteLinkStatisticsBeforeCycle(nextCycle)
    ConsumptionHistorySource.deleteAllConsumptions()
    IncomeSource.deleteIncomesBefore(nextCycle, Period.MONTHLY)
    IncomeSource.deleteIncomesBefore(nextCycle, Period.WEEKLY)
    IncomeSource.deleteIncomesBefore(nextCycle, Period.YEARLY)
    LinkConsumptionHistory
    AirlineSource.deleteCashFlowItems(nextCycle)
    AirlineSource.deleteTransactions(nextCycle)
    ModelSource.deleteAllAirlineDiscounts()
    ModelSource.deleteAllFavoriteModelIds()


    Await.result(actorSystem.terminate(), Duration.Inf)
  }



}