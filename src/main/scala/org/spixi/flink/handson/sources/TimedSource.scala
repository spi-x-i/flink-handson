package org.spixi.flink.handson.sources

import java.lang.Long
import java.util
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.watermark.Watermark
import org.spixi.flink.handson.model.{TimeEvent, TimedEvent}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class TimedSource(period: Long, slowDownFactor: Long) extends RichParallelSourceFunction[TimeEvent[Long]] {

  private var currentTimeMs: Long = 0l
  private var isRunning = new AtomicBoolean(true)

  override def open(parameters: Configuration) = {
    super.open(parameters)
    val now = System.currentTimeMillis()

    if (currentTimeMs == 0) {
      currentTimeMs = now - (now % 1000)
    }
  }

  override def cancel(): Unit = isRunning.set(false)

  override def run(ctx: SourceContext[TimeEvent[Long]]): Unit = {
    while (isRunning.get()) {
      ctx.getCheckpointLock.synchronized {
        ctx.collectWithTimestamp(TimedEvent[Long](currentTimeMs, 0l), currentTimeMs)
        ctx.emitWatermark(new Watermark(currentTimeMs))
        currentTimeMs += period
      }
      timeSync()
    }
  }

  private def timeSync(): Unit = {
    val delta = currentTimeMs - System.currentTimeMillis()

    val sleepTime = slowDownFactor match {
      case factor if factor != 1 => period * factor
      case _ => period + delta + randomJitter()
    }

    Thread.sleep(sleepTime)
  }

  private def randomJitter(random: Double = Math.random()): Long = {
    random match {
      case rand if rand > 0.5 => -(rand * period).toLong
      case rand => (rand * period).toLong
    }
  }

  // TODO -> implement with checkpointing
}