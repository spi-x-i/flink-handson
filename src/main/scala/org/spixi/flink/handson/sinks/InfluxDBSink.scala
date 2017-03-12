package org.spixi.flink.handson.sinks

import com.paulgoldbaum.influxdbclient.{InfluxDB, Point}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction


class InfluxDBSink[T](measurement: String) extends RichSinkFunction[T] {

  @transient
  private var influxDB: InfluxDB = _
  private val databaseName = "sineWave"
  private val fieldName = "value"

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    influxDB = InfluxDB.connect(
      host = "localhost",
      port = 8086,
      username = "admin",
      password = "admin"
    )
    influxDB.selectDatabase(databaseName)
  }

  override def close(): Unit = super.close()

  override def invoke(value: T): Unit = {
    val builder = Point(measurement).
  }
}
