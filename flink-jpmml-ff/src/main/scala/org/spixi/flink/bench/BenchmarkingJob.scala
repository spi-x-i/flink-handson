package org.spixi.flink.bench

import java.util.{Properties, UUID}

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
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

    val supportStream = see.fromCollection(Seq(
      AddMessage(uuid,
        version.toString,
        getClass.getResourceAsStream("/Users/aspina/Workspace/Radical/Talks/JugMilano/flink-handson/flink-jpmml-ff/src/main/resources/svm_model_1.pmml"))))

    see.execute("flpmml micro benchmark")
  }

}
