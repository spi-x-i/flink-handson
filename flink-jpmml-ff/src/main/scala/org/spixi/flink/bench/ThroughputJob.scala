package org.spixi.flink.bench

import java.util.{Properties, UUID}

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import io.radicalbit.flink.pmml.scala.models.prediction.Prediction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.RichAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.spixi.flink.bench.sources.EventSource
import org.spixi.flink.generation.models.Pixel

object ThroughputJob {

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

    val throughput = predictions
        .assignAscendingTimestamps(_._1.occurredOn)
        .timeWindowAll(Time.seconds(10l))
        .apply(new RichAllWindowFunction[(Pixel, Prediction), Int, TimeWindow] {
          private lazy val state: ValueState[Int] = {
            val descriptor =
              new ValueStateDescriptor[Int](
                s"throughput_values",
                TypeInformation.of(new TypeHint[Int]() {})
              )
            getRuntimeContext.getState[Int](descriptor)
          }

          def greaterT: Int = Option(state.value()).getOrElse(0)

          override def apply(window: TimeWindow, input: Iterable[(Pixel, Prediction)],out: Collector[Int]): Unit = {
            val currentT = input.size
            if (currentT > greaterT) state.update(currentT)
            out.collect(greaterT)
          }
       })

    throughput.print()

    see.execute("flpmml micro benchmark")
  }

}
