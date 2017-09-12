package org.spixi.flink

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.radicalbit.flink.pmml.scala.models.prediction.{Prediction, Score}
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
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

    // defining the model application - it has to be uuid compliant
    val modelApplication = UUID.randomUUID().toString

    val see = StreamExecutionEnvironment.getExecutionEnvironment

    // taking from socket the model path for easy playing with
    // just before running open a socket nc -l 9999 - it will be used to send model paths
    val modelPath: DataStream[String] =
      see.socketTextStream(config.getString("socket.host"), config.getInt("socket.port"))

    // extracting the model versions from path contained in socket stream
    val versionStream: DataStream[Int] = modelPath.map { path =>
      path.split("\\.").head.split("\\_").last.toInt
    }

    // event generation from source testset file
    val eventSource = see.addSource(new EventGenerator())

    // modeling events in order to evaluate them all against all the models uploaded
    val enrichedStream =
      eventSource
        .connect(versionStream.broadcast)
        .flatMap(new ModelIdSetter(modelApplication))

    enrichedStream.writeAsText("/Users/aspina/Desktop/main-stream.out", WriteMode.OVERWRITE).setParallelism(1)

    // creating the control stream
    val controlStream = modelPath.map(new ControlStreamer(modelApplication))

    // flink-jpmml core evaluation
    val predicted =
      enrichedStream
      .withSupportStream(controlStream)
      .evaluate { (event, model) =>
        val vector = event.toVector
        val prediction = model.predict(vector)
        (event, prediction)
      }

    // normalizing model evaluation output
    val normalizedOutput: DataStream[(Pixel, Double)] =
      predicted
        .flatMapWith {
          case (pixel, Prediction(Score(value))) =>
            if (value > 0) Seq((pixel, 1.0)) else Seq((pixel, -1.0))
          case _ => Seq.empty
        }

    val accuracyStream = normalizedOutput.keyBy(_._1.modelId).map(new AccuracyComputation())

    accuracyStream.writeAsText("/Users/aspina/Desktop/accuracy-stream.out", WriteMode.OVERWRITE).setParallelism(1)
    predicted.writeAsText("/Users/aspina/Desktop/predicted.out", WriteMode.OVERWRITE).setParallelism(1)

    see.execute("FF-Job")
  }

}
