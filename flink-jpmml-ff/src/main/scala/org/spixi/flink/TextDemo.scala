package org.spixi.flink

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.radicalbit.flink.pmml.scala.models.control.ServingMessage
import io.radicalbit.flink.pmml.scala.models.prediction.{Prediction, Score}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
import org.spixi.flink.functions.ModelIdSetter
import org.spixi.flink.generation.EventGenerator
import org.spixi.flink.generation.function.{AccuracyComputation, ControlStreamer}
import org.spixi.flink.generation.models.Pixel
import org.spixi.flink._

object TextDemo {

  private lazy val config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val outputPath = params.get("outputPath") match {
      case path if path.endsWith("/") && path.length > 1 => path.dropRight(1)
      case otherPaths => otherPaths
    }

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

    enrichedStream.writeAsText(s"$outputPath/main-stream.out", WriteMode.OVERWRITE).setParallelism(1)

    // creating the control stream
    val controlStream: DataStream[ServingMessage] = modelPath.map(new ControlStreamer(modelApplication))

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
        .flatMapWith{
          case (pixel, Prediction(Score(value))) =>
            if (value > 0) Seq((pixel, 1.0)) else Seq((pixel, -1.0))
          case _ => Seq.empty
        }

    val accuracyStream = normalizedOutput.keyBy(_._1.modelId).map(new AccuracyComputation())

    accuracyStream.writeAsText(s"$outputPath/accuracy-stream.out", WriteMode.OVERWRITE).setParallelism(1)
    predicted.writeAsText(s"$outputPath/predicted.out", WriteMode.OVERWRITE).setParallelism(1)

    see.execute("FF-Job")
  }

}
