package org.spixi.flink.bench.sources

import java.util.Properties

import com.typesafe.config.ConfigFactory
import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}

import scala.annotation.tailrec

class ControlSource(properties: Properties) extends RichParallelSourceFunction[AddMessage] {

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
  private lazy val modelApp: String = properties.getProperty("uuid")

  override def cancel(): Unit = {}

  val message = AddMessage(modelApp, 0, config.getString("bench.model.path"), System.currentTimeMillis())

  override def run(ctx: SourceFunction.SourceContext[AddMessage]): Unit =
    computeMessages(ctx, 1)

  @tailrec
  final private def computeMessages(ctx: SourceFunction.SourceContext[AddMessage], version: Int): Unit = {
    ctx.getCheckpointLock.synchronized {
      ctx.collect(message.copy(version = version))
    }
    println(s"\n Running with $version different models.")
    Thread.sleep(5000l)
    computeMessages(ctx, version + 1)
  }

}
