package com.patson.util

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.typesafe.config.ConfigFactory
import java.time.Instant
import scala.jdk.CollectionConverters._

object MetricsExport {
  private val config = ConfigFactory.load()
  private val enabled = config.hasPath("influxdb.url")
  
  private lazy val client = if (enabled) {
    try {
      val url = config.getString("influxdb.url")
      val token = config.getString("influxdb.token")
      val org = config.getString("influxdb.org")
      val bucket = config.getString("influxdb.bucket")
      Some(InfluxDBClientFactory.create(url, token.toCharArray, org, bucket))
    } catch {
      case e: Exception =>
        println(s"Failed to initialize InfluxDB client: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  } else {
    None
  }

  private lazy val writeApi = client.map(_.makeWriteApi())

  def write(measurement: String, tags: Map[String, String], fields: Map[String, Any]) = {
    writeApi.foreach { api =>
      try {
        val point = Point.measurement(measurement)
          .time(Instant.now(), WritePrecision.MS)
        
        tags.foreach { case (k, v) => point.addTag(k, v) }
        fields.foreach { 
          case (k, v: Int) => point.addField(k, v)
          case (k, v: Long) => point.addField(k, v)
          case (k, v: Double) => point.addField(k, v)
          case (k, v: Float) => point.addField(k, v)
          case (k, v: Boolean) => point.addField(k, v)
          case (k, v: String) => point.addField(k, v)
          case (k, v) => point.addField(k, v.toString)
        }
        
        api.writePoint(point)
      } catch {
        case e: Exception => 
          println(s"Error writing metric $measurement: ${e.getMessage}")
      }
    }
  }
  
  def close() = {
    client.foreach(_.close())
  }
}
