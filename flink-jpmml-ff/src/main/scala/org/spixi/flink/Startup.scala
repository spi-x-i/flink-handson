package org.spixi.flink

import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.functions.ModelIdSetter
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

    val normalizedOutput =
      predicted
        .filter(_._2.value.getOrElse(0.0) != 0.0)
        .map { pred =>
          if (pred._2.value.get > 0.0) (pred._1, 1.0) else (pred._1, -1.0)
        }

    val accuracyStream = normalizedOutput.keyBy(_._1.modelId).map(new AccuracyComputation())

    versionStream.print()
    accuracyStream.print()
    predicted.print()
    see.execute("FF-Job")
  }

}
