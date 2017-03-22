package org.spixi.flink.handson.sinks

import java.util.concurrent.TimeUnit

import com.paulgoldbaum.influxdbclient.{InfluxDB, Point}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.spixi.flink.handson.model.{KeyedTimedEvent, TimeEvent}
import org.influxdb.{InfluxDB => Influx}
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.{Point => JPoint}

class InfluxDBSink[T <: TimeEvent[_]](measurement: String) extends RichSinkFunction[T] {
  /*
  @transient
  private var influxDB: InfluxDB = _

  private lazy val databaseName = "sineWave"
  private lazy val fieldName = "value"
   */

  @transient
  private var influxDB: Influx = _

  private lazy val databaseName = "sineWave"
  private lazy val fieldName = "value"

  /*
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
   */

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)

    influxDB = InfluxDBFactory.connect("http://localhost:8086", "admin", "admin")
    influxDB.createDatabase(databaseName)
    influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS)
  }

  override def close(): Unit = super.close()

  /*
  override def invoke(value: T): Unit = {
    val builder = Point(key = measurement, timestamp = value.timestamp)

    value match {
      case v: String => builder.addField(fieldName, v)
      case v: Long => builder.addField(fieldName, v)
      case v: Int => builder.addField(fieldName, v)
      case v: Boolean => builder.addField(fieldName, v)
    }
  }
   */

  override def invoke(value: T): Unit = {
    val builder = JPoint
      .measurement(measurement)
      .time(value.timestamp, TimeUnit.MILLISECONDS)
      .addField(fieldName, value.value.asInstanceOf[Number])

    value match {
      case v if v.isInstanceOf[KeyedTimedEvent[_]] => builder.tag("key", value.asInstanceOf[KeyedTimedEvent[_]].key)
      case _ =>
    }

    val p = builder.build
    influxDB.write(databaseName, "autogen", builder.build)
  }

}
