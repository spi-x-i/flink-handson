package org.spixi.flink.handson.sources

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}
import org.spixi.flink.handson.models.KeyedTimeEvent

import scala.util.Random

class EventStreamSource() extends ParallelSourceFunction[KeyedTimeEvent[String]] {

  private val isRunning: AtomicBoolean = new AtomicBoolean(true)

  private val rand: Random = new Random()

  private var count: Int = 0

  override def cancel(): Unit = isRunning.set(false)

  override def run(ctx: SourceFunction.SourceContext[KeyedTimeEvent[String]]): Unit = {

    while (isRunning.get() && count < 100) {
      val event = KeyedTimeEvent(System.currentTimeMillis(), "key", "event")
      Thread.sleep(rand.nextInt(300))
      ctx.getCheckpointLock.synchronized {
        ctx.collect(event)
      }
      count += 1
    }
  }
}
