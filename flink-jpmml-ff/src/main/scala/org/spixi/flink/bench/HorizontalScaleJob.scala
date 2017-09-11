package org.spixi.flink.bench

import java.util.{Properties, UUID}

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.bench.functions.HorizontalEnricher
import org.spixi.flink.bench.sources.{ControlSource, EventSource}
import org.spixi.flink.generation.models.Pixel

object HorizontalScaleJob {

  def main(args: Array[String]): Unit = {

    val see = StreamExecutionEnvironment.getExecutionEnvironment

    val uuid = UUID.randomUUID().toString
    val version = 1

    val properties = new Properties()

    properties.setProperty("uuid", uuid)
    properties.setProperty("version", version.toString)

    // Events source
    val eventStream: DataStream[Pixel] = see.addSource(new EventSource(properties))
    val controlSource: DataStream[AddMessage] = see.addSource(new ControlSource(properties))

    val toPredictStream = eventStream
      .connect(controlSource.broadcast)
      .flatMap(new HorizontalEnricher(uuid))

    val predictions = toPredictStream
      .withSupportStream(controlSource)
      .evaluate { (event, model) =>
        val vectorized = DenseVector(event.r, event.g)
        val prediction = model.predict(vectorized)
        (event, prediction)
      }

    predictions.print()

    see.execute("flpmml micro benchmark")
  }

}
