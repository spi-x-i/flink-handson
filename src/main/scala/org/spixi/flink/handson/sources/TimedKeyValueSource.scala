package org.spixi.flink.handson.sources

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.watermark.Watermark
import org.spixi.flink.handson.model.KeyedTimedEvent

import scala.util.Random


object TimedKeyValueSource {

  private lazy val rnd = scala.util.Random

  private lazy val keys = Seq(
    "key1",
    "key2",
    "key3",
    "key4"
  )
}
class TimedKeyValueSource(period: Long) extends RichParallelSourceFunction[KeyedTimedEvent[String]] {

  import TimedKeyValueSource._

  private var currentTimeMs: Long = 0l
  private var isRunning = new AtomicBoolean(true)

  override def cancel(): Unit = ???

  override def open(parameters: Configuration) = {
    super.open(parameters)
    val now = System.currentTimeMillis()

    if (currentTimeMs == 0) {
      currentTimeMs = now
    }

  }

  override def run(ctx: SourceContext[KeyedTimedEvent[String]]): Unit = {
    while(isRunning.get()) {
      ctx.getCheckpointLock.synchronized {
        ctx.collectWithTimestamp(
          KeyedTimedEvent[String](
            currentTimeMs,
            rnd.shuffle(keys).head,
            rnd.nextString(5)),
          currentTimeMs
        )
        ctx.emitWatermark(new Watermark(currentTimeMs))
        currentTimeMs += period
      }
      Thread.sleep(rnd.nextInt(100000).toLong)
    }
  }

}
