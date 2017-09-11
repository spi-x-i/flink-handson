package org.spixi.flink.bench.sources

import java.util.Properties

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.typesafe.config.ConfigFactory
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, RichParallelSourceFunction, SourceFunction}
import org.spixi.flink.generation.models.Pixel

class ControlSource(properties: Properties) extends RichParallelSourceFunction[Pixel] {

  private lazy val state: ValueState[Set[Int]] = {
    val descriptor =
      new ValueStateDescriptor[Set[Int]](
        s"model_versions",
        TypeInformation.of(new TypeHint[Set[Int]]() {})
      )
    getRuntimeContext.getState[Set[Int]](descriptor)
  }

  def versions: Set[Int] = Option(state.value()).getOrElse(Set.empty[Int])

  private lazy val config = ConfigFactory.load()
  private lazy val rand = scala.util.Random

  private lazy val modelApp: String = properties.getProperty("uuid")

  override def cancel(): Unit = {}

  override def run(ctx: SourceFunction.SourceContext[Pixel]): Unit = {
    Thread.sleep(2000l)
    while (true) {
      versions foreach { version =>

      }
      val pixel = random[Pixel].copy(modelId = modelId)
      ctx.getCheckpointLock.synchronized {
        ctx.collect(pixel)
      }
    }
  }
}
