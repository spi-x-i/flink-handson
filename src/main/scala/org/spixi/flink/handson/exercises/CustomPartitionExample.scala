package org.spixi.flink.handson.exercises

import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.handson.sources.TimedKeyValueSource

object CustomPartitionExample {

  private lazy val PERIOD_MS = 1000

  def execute(env: StreamExecutionEnvironment, destinationPath: String): Unit = {

    env.setParallelism(2)

    val source = env.addSource(new TimedKeyValueSource(PERIOD_MS)).name("Simple timed key-value Source")

    source
      .partitionCustom(new CustomPartitioner(), event => event.key)
      .writeAsText(destinationPath, WriteMode.OVERWRITE)

  }

}
