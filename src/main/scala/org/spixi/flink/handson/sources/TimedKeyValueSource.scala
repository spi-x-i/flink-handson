package org.spixi.flink.handson.sources

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.watermark.Watermark
import org.spixi.flink.handson.model.{StringTimedEvent}

object TimedKeyValueSource {

  object ElementProducer {
    def apply(key: Int) = {

      val value =
        if (key % 2 == 0) {
          "I'm a even number."
        } else {
          "I'm a odd number."
        }

      new ElementProducer(key.toString, value)
    }
  }
  protected case class ElementProducer(key: String, value: String)

  private lazy val rnd = scala.util.Random

  private lazy val keys = Seq(1, 2, 3, 4, 5, 6)

}
class TimedKeyValueSource(period: Long) extends RichParallelSourceFunction[StringTimedEvent[String]] {

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

  override def run(ctx: SourceContext[StringTimedEvent[String]]): Unit = {
    while (isRunning.get()) {
      ctx.getCheckpointLock.synchronized {
        val elem = ElementProducer(rnd.shuffle(keys).head)
        ctx.collectWithTimestamp(
          StringTimedEvent[String](currentTimeMs, elem.key, elem.value),
          currentTimeMs
        )
        ctx.emitWatermark(new Watermark(currentTimeMs))
        currentTimeMs += period
      }
      Thread.sleep(rnd.nextInt(1000).toLong)
    }
  }

}
