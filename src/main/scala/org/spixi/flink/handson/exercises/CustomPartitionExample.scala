package org.spixi.flink.handson.exercises

import org.apache.flink.streaming.api.scala._
import org.spixi.flink.handson.model.KeyedTimedEvent
import org.spixi.flink.handson.sources.TimedKeyValueSource


object CustomPartitionExample {

  private lazy val PERIOD_MS = 1000

  def execute(env: StreamExecutionEnvironment): Unit = {

    val source = env.addSource(new TimedKeyValueSource(PERIOD_MS)).name("Simple timed key-value Source")


  }

}
