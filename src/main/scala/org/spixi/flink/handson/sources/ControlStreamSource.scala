package org.spixi.flink.handson.sources

import java.util.concurrent.atomic.AtomicBoolean

import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}
import org.spixi.flink.handson.models.SupportEvent

class ControlStreamSource(message: String*) extends ParallelSourceFunction[SupportEvent[Int]] {

  private val rand = new scala.util.Random

  private val isRunning: AtomicBoolean = new AtomicBoolean(true)

  private var count = 0

  private var versions: Map[String, Int] = message.map(_ -> 1).toMap

  override def cancel(): Unit = isRunning.set(false)

  override def run(ctx: SourceFunction.SourceContext[SupportEvent[Int]]): Unit = {
    while (isRunning.get() && count < 10) {

      // I take a message name randomly and related version (I need to update the next version accordingly)
      val supportMessage = rand.shuffle(message).head
      val version = {
        val value = versions(supportMessage)
        versions = versions.updated(supportMessage, value + 1)
        value
      }
      val event = SupportEvent(
        System.currentTimeMillis,
        count,
        Map("name" -> supportMessage, "version" -> version.toString)
      )
      // Wait a little bit
      Thread.sleep(2000)

      ctx.getCheckpointLock.synchronized {
        ctx.collect(event)
      }

      println("CONTROL EMITTED " + event)

      count += 1
    }
  }

}
