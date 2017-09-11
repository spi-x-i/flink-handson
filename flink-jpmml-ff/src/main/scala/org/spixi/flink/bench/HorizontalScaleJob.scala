package org.spixi.flink.bench

import java.util.{Properties, UUID}

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import io.radicalbit.flink.pmml.scala.models.prediction.Prediction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.RichAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.spixi.flink.bench.sources.EventSource
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
    val eventStream = see.addSource(new EventSource(properties))

    val supportStream = see.fromCollection(
      Seq(
        AddMessage(
          uuid,
          version.toLong,
          "/Users/aspina/Workspace/Radical/Talks/JugMilano/flink-handson/flink-jpmml-ff/src/main/resources/svm_model_1.pmml",
          System.currentTimeMillis()
        )))

    val predictions = eventStream
      .withSupportStream(supportStream)
      .evaluate { (event, model) =>
        val vectorized = DenseVector(event.r, event.g)
        val prediction = model.predict(vectorized)
        (event, prediction)
      }



    see.execute("flpmml micro benchmark")

  }

}
