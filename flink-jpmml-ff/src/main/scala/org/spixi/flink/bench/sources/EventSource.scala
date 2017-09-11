package org.spixi.flink.bench.sources

import java.util.Properties

import com.danielasfregola.randomdatagenerator.RandomDataGenerator._
import com.typesafe.config.ConfigFactory
import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}
import org.spixi.flink.generation.models.Pixel

class EventSource(properties: Properties) extends ParallelSourceFunction[Pixel] {

  private lazy val config = ConfigFactory.load()
  private lazy val rand = scala.util.Random

  private lazy val modelId: String = properties.getProperty("uuid") + "_" + properties.getProperty("version")

  override def cancel(): Unit = {}

  override def run(ctx: SourceFunction.SourceContext[Pixel]): Unit = {
    Thread.sleep(2000l)
    while (true) {
      val pixel = random[Pixel].copy(modelId = modelId)
      ctx.getCheckpointLock.synchronized {
        ctx.collect(pixel)
      }
    }
  }
}
