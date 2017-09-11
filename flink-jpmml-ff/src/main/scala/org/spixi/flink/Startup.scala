package org.spixi.flink

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.radicalbit.flink.pmml.scala.models.prediction.{Prediction, Score}
import io.radicalbit.nsdb.connector.flink.sink.NSDBSink
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala.extensions._
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.functions.{ErrorFormatter, EvaluationSplitter, ModelIdSetter, SinkFunction}
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

    // creating the control stream
    val controlStream = modelPath.map(new ControlStreamer(modelApplication))

    // flink-jpmml core evaluation
    val predicted =
      enrichedStream
        .withSupportStream(controlStream)
        .evaluate { (event, model) =>
          val prediction = model.predict(event.toVector)
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

    // separating evaluation stream by process function
    val sideModel2Tag = OutputTag[(Double, Double, Double)]("sideModelTwo")
    val sideModel3Tag = OutputTag[(Double, Double, Double)]("sideModelThree")

    val sideModel1: DataStream[(Double, Double, Double)] = normalizedOutput
      .keyBy(_._1.modelId)
      .process(new EvaluationSplitter(sideModel2Tag, sideModel3Tag))

    val sideModel2 = sideModel1.getSideOutput(sideModel2Tag)
    val sideModel3 = sideModel1.getSideOutput(sideModel3Tag)

    val accuracyStream = normalizedOutput.keyBy(_._1.modelId).map(new AccuracyComputation())

    val errorOut: DataStream[(Double, Double)] =
      accuracyStream.connect(versionStream.broadcast).flatMap(new ErrorFormatter())

    val accuracyOut = versionStream.mapWith {
      case 1 => 0.48
      case 2 => 0.72
      case 3 => 0.90
      case _ => 0.0
    }

    val namespace = config.getString("nsdb.namespace")
    val host = config.getString("nsdb.host")
    val port = config.getInt("nsdb.port")

    val fSink = SinkFunction.converter(namespace, List("x", "y")) _
    val sSink = SinkFunction.converterToSeries(namespace) _
    val cSink = SinkFunction.converterCouples(namespace, List("error")) _

    sideModel1.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_1")))
    sideModel2.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_2")))
    sideModel3.addSink(new NSDBSink[(Double, Double, Double)](host, port)(fSink("evaluation_3")))

    errorOut.addSink(new NSDBSink[(Double, Double)](host, port)(cSink("error_accuracy_stream")))
    // accuracyOut.addSink(new NSDBSink[Double](host, port)(sSink("accuracy_stream")))

    accuracyStream.print()
    predicted.print()

    see.execute("FF-Job")
  }

}
