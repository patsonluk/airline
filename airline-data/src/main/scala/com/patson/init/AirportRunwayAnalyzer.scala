package com.patson.init

import com.patson.data.AirportSource
import com.patson.model.{Runway, RunwayType}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * Use to analyze user input from https://docs.google.com/spreadsheets/d/1D6xeBwF6CnX-EOFZMNWOcR4UNc1ISbX_IddORahn0Gs/edit#gid=629953473
  * The data is coped to runway-proposal.csv
  * And then it is compared to runways-imported-2022-dec.csv
  *
  * Runways with exact match AND has lighting will be all accepted
  * Runways with exact match BUT has no lighting will be all rejected
  * Then rest of the runways (that is not found in import) with pop > 100k were gone through one by one.
  * Only those with decent air traffic (and looks like lighted) will be manually selected
  *
  * The final patch file is runway-patch-2022-dec.csv
  *
  */
object AirportRunwayAnalyzer extends App {

  //implicit val materializer = FlowMaterializer()

  mainFlow

  def mainFlow() {
    val airportByIcao = AirportSource.loadAllAirports(false).map( airport => (airport.icao, airport)).toMap
    val proposals = loadRunwayProposals()
    val latestRunways = loadRunway("runways-imported-2022-dec.csv")

    proposals.foreach {
      case(icao, proposedRunways) =>
        airportByIcao.get(icao).foreach { airport =>
          latestRunways.get(icao) match {
            case Some(latestRunwaysOfThis) =>
              if (proposedRunways.equals(latestRunwaysOfThis)) {
                //match the ourairportdata and they are lighted
                latestRunwaysOfThis.foreach { runway =>
//                  println(s"$icao,${runway.length},${runway.runwayType.toString},${runway.code}")
                }
              } //else do not consider it, if it's mismatch or not lighted
            case None => //manually go through them...
              if (airport.population > 100_000) {
                proposedRunways.foreach { runway =>
                    println(s"$icao,${runway.length},${runway.runwayType.toString},${runway.code}")
                }
              }
          }
        }

    }

    Await.result(actorSystem.terminate(), Duration.Inf)

  }

  def loadRunwayProposals() : Map[String, Set[Runway]] = {
    val result = scala.collection.mutable.HashMap[String, collection.mutable.ListBuffer[Runway]]()
    val asphaltPattern = "(asp.*)".r
    val concretePattern = "(con.*|pem.*)".r
    val gravelPattern = "(gvl.*|.*gravel.*)".r
    for (line : String <- Source.fromFile("runway-proposal.csv").getLines) {
      val info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map { token =>
        if (token.startsWith("\"") && token.endsWith("\"")) {
          token.substring(1, token.length() - 1)
        } else {
          token
        }
      }

      try {
        val length = info(1).toInt

        val icao = info(0)
        val code = info(3)

        val lighted = true
        val runwayOption =
          info(2).toLowerCase() match {
            case asphaltPattern(_) =>
              Some(Runway(length, code, RunwayType.Asphalt, lighted))
            case concretePattern(_) => Some(Runway(length, code, RunwayType.Concrete, lighted))
            case gravelPattern(_) => Some(Runway(length, code, RunwayType.Gravel, lighted))
            case _ => Some(Runway(length, code, RunwayType.Unknown, lighted))
          }
        runwayOption.foreach {
          case (runway) =>
            val list = result.getOrElseUpdate(icao, ListBuffer[Runway]())
            list += runway
        }
      } catch {
        case _ : NumberFormatException => None
      }
    }
    result.view.mapValues(_.toSet).toMap
  }


  def loadRunway(file : String) : Map[String, Set[Runway]] = {
      val result = scala.collection.mutable.HashMap[String, collection.mutable.ListBuffer[Runway]]()
      val asphaltPattern = "(asp.*)".r
      val concretePattern = "(con.*|pem.*)".r
      val gravelPattern = "(gvl.*|.*gravel.*)".r
      for (line : String <- Source.fromFile(file).getLines) {
        val info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map { token =>
          if (token.startsWith("\"") && token.endsWith("\"")) {
            token.substring(1, token.length() - 1)
          } else {
            token
          }
        }


        val lighted = info(6) == "1"

        try {
          var length = (info(3).toInt * 0.3048).toInt
          if (length % 10 == 9) { //somehow the data is off my 1 meter
            length += 1
          }
          val icao = info(2)
          var codeTokens = ListBuffer[String](info(8).trim, info(14).trim)
          codeTokens = codeTokens.filterNot(token => "XX".equals(token) || "".equals(token))
          val code = codeTokens.mkString("/")

          val runwayOption =
            info(5).toLowerCase() match {
              case asphaltPattern(_) =>
                Some(Runway(length, code, RunwayType.Asphalt, lighted))
              case concretePattern(_) => Some(Runway(length, code, RunwayType.Concrete, lighted))
              case gravelPattern(_) => Some(Runway(length, code, RunwayType.Gravel, lighted))
              case _ => Some(Runway(length, code, RunwayType.Unknown, lighted))
            }
          runwayOption.foreach {
            case (runway) =>
              val list = result.getOrElseUpdate(icao, ListBuffer[Runway]())
              list += runway
          }
        } catch {
          case _ : NumberFormatException => None
        }

        //
        //        if (infoArray(6) == "P" && isCity(infoArray(7), infoArray(8)) && infoArray(14).toInt > 0) { //then a valid target
        //          if (incomeInfo.get(infoArray(8)).isEmpty) {
        //            println(infoArray(8) + " has no income info")
        //          }
        //          result += new City(infoArray(1), infoArray(4).toDouble, infoArray(5).toDouble, infoArray(8), infoArray(14).toInt, incomeInfo.get(infoArray(8)).getOrElse(Country.DEFAULT_UNKNOWN_INCOME)) //1, 4, 5, 8 - country code, 14
        //        }
      }
      result.view.mapValues(_.toSet).toMap
    }
}