package com.patson.init

import play.api.libs.json.{JsObject, JsValue, Json}

import scala.io.Source
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import com.patson.model._


object AirportWeatherData {
  val weatherDatasByIata = init()

  def init() : Map[String, WeatherData] = {
    val dataOfIata = ListBuffer[WeatherData]()
    val result = mutable.Map[String, WeatherData]()
    for (line <- Source.fromFile("airport-weather.csv").getLines) {
      val tuple = parseLine(line)
      val currentIata = tuple._1
      val season = tuple._2
      val data = tuple._3
      dataOfIata.append(data)
      if (dataOfIata.length == 2) {
        flushWeatherData(currentIata, result, dataOfIata)
      }
    }

    result.toMap
  }

  def flushWeatherData(iata : String, result : mutable.Map[String, WeatherData], dataOfIata : ListBuffer[WeatherData]) = {
    val combinedWeatherData = dataOfIata.fold(dataOfIata.head)((a, b) => a.combine(b))
    println(s"$iata => $combinedWeatherData")
    result.put(iata, combinedWeatherData)
    dataOfIata.clear()
  }

  def parseLine(line : String) : (String, String, WeatherData) = {
    val iata = line.substring(0, line.indexOf(","))
    var holder = line.substring(line.indexOf(",") + 1)
    val season = holder.substring(0, holder.indexOf(","))
    holder = holder.substring(holder.indexOf(",") + 1) //the rest should be the json
    val json = Json.parse(holder).asInstanceOf[JsObject]
    //{...,"mintemp":-2,"maxtemp":1,"avgtemp":0,"totalsnow":0.3,"sunhour":3.7,"uv_index":2}
    val allWeatherData = json.value.values.map { jsonPerDay =>
      val perDay = jsonPerDay.asInstanceOf[JsObject]
      val isSunny = perDay("sunhour").as[Double] >= 8
      WeatherData(perDay("mintemp").as[Int], perDay("maxtemp").as[Int], 1, if (isSunny) 100 else 0, perDay("sunhour").as[Double], perDay("totalsnow").as[Double])
    }
    (iata, season, allWeatherData.fold(allWeatherData.head)((a, b) => a.combine(b)))
  }

  def getAirportWeatherData(airport : Airport) : Option[WeatherData] = {
    weatherDatasByIata.get(airport.iata)
  }
}

case class WeatherData(minTemperature : Double, maxTemperature : Double, dayCount : Int, sunnyDayPercentage : Double, sunHourPerDay : Double, snowPerDay : Double) {
  def combine(that : WeatherData): WeatherData = {
    WeatherData(
      Math.min(this.minTemperature, that.minTemperature),
      Math.max(this.maxTemperature, that.maxTemperature),
      this.dayCount + that.dayCount,
      ((this.dayCount * this.sunnyDayPercentage) + (that.dayCount * that.sunnyDayPercentage)) / (this.dayCount + that.dayCount),
      ((this.dayCount * this.sunHourPerDay) + (that.dayCount * that.sunHourPerDay)) / (this.dayCount + that.dayCount),
      ((this.dayCount * this.snowPerDay) + (that.dayCount * that.snowPerDay)) / (this.dayCount + that.dayCount)
    )
  }
}