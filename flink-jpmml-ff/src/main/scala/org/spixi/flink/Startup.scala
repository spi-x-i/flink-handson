package org.spixi.flink

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.radicalbit.nsdb.api.scala.Bit
import io.radicalbit.nsdb.connector.flink.sink.NSDBSink
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
import org.spixi.flink.functions.{ModelIdSetter, SinkFunction}
import org.spixi.flink.generation.EventGenerator
import org.spixi.flink.generation.function.{AccuracyComputation, ControlStreamer}
import org.spixi.flink.generation.models.Pixel

object Startup {

  private implicit class VectorConverter(pixel: Pixel) {
    def toVector = DenseVector(pixel.g, pixel.r)
  }

  private lazy val config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    val modelApplication = UUID.randomUUID().toString

    val see = StreamExecutionEnvironment.getExecutionEnvironment

    val modelPath: DataStream[String] =
      see.socketTextStream(config.getString("socket.host"), config.getInt("socket.port"))

    val versionStream: DataStream[Int] = modelPath.map { path =>
      path.split("\\.").head.split("\\_").last.toInt
    }

    val eventSource = see.addSource(new EventGenerator())

    val enrichedStream =
      eventSource
        .connect(versionStream.broadcast)
        .flatMap(new ModelIdSetter(modelApplication))

    val controlStream = modelPath.map(new ControlStreamer(modelApplication))

    val predicted =
      enrichedStream
        .withSupportStream(controlStream)
        .evaluate { (event, model) =>
          val prediction = model.predict(event.toVector)
          (event, prediction)
        }

    val normalizedOutput: DataStream[(Pixel, Double)] =
      predicted
        .filter(_._2.value.getOrElse(0.0) != 0.0)
        .map { pred =>
          if (pred._2.value.get > 1.0) (pred._1, 1.0) else (pred._1, -1.0)
        }

    val sideModel2Tag = OutputTag[(Double, Double, Double)]("sideModelTwo")

    val sideModel3Tag = OutputTag[(Double, Double, Double)]("sideModelThree")

    val sideModel1: DataStream[(Double, Double, Double)] = normalizedOutput
      .keyBy(_._1.modelId)
      .process(new ProcessFunction[(Pixel, Double), (Double, Double, Double)] {
        override def processElement(value: (Pixel, Double),
                                    ctx: ProcessFunction[(Pixel, Double), (Double, Double, Double)]#Context,
                                    out: Collector[(Double, Double, Double)]) = {
          if (value._1.modelId.endsWith("5")) {
            out.collect((value._1.r.toDouble, value._1.g.toDouble, value._2))
          } else if (value._1.modelId.endsWith("1")) {
            ctx.output(sideModel2Tag, (value._1.r.toDouble, value._1.g.toDouble, value._2))
          } else if (value._1.modelId.endsWith("3")) {
            ctx.output(sideModel3Tag, (value._1.r.toDouble, value._1.g.toDouble, value._2))
          }
        }

      })

    val sideModel2 = sideModel1.getSideOutput(sideModel2Tag)
    val sideModel3 = sideModel1.getSideOutput(sideModel3Tag)

    val namespace = "flink-jpmml"
    val dimKeys = List("x", "y")

    val host = "localhost"
    val port = 7817

    val fSink = SinkFunction.converter(namespace, dimKeys) _

    sideModel1.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_1")))
    sideModel2.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_2")))
    sideModel3.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_3")))

//    sideModel2.addSink(new NSDBSink[(Double, Double, Double)](host, port)(SinkFunction(namespace, "evaluation_2", dimKeys)))
//    sideModel3.addSink(new NSDBSink[(Double, Double, Double)](host, port)(SinkFunction(namespace, "evaluation_3", dimKeys)))

    val accuracyStream = normalizedOutput.keyBy(_._1.modelId).map(new AccuracyComputation())

    versionStream.print()
    accuracyStream.print()
    predicted.print()

    see.execute("FF-Job")
  }

}
