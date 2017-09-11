package org.spixi.flink.bench

import java.util.{Properties, UUID}

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.bench.sources.EventSource

object BenchmarkingJob {

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
