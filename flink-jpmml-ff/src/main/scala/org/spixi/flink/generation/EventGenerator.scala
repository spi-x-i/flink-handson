package org.spixi.flink.generation

import com.typesafe.config.ConfigFactory
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.spixi.flink.generation.models.Pixel

class EventGenerator extends SourceFunction[Pixel] {

  private lazy val config = ConfigFactory.load()
  private lazy val rand = scala.util.Random

  override def cancel(): Unit = {}

  override def run(ctx: SourceFunction.SourceContext[Pixel]): Unit = {
    val elems = EventsReader.fromSource(config.getString("test.events.path"))

    Thread.sleep(2500l)

    while (true) {
      elems foreach { event =>
        ctx.getCheckpointLock.synchronized {
          ctx.collect(event)
          Thread.sleep(200l)
        }
      }
    }

  }
}
