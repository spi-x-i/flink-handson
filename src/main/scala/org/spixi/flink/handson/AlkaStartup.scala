package org.spixi.flink.handson

import org.apache.flink.api.common.functions.{Partitioner, RichMapFunction}
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.streaming.api.scala._

import scala.util.Random

case class Record(id: String, field: String)
object CustomerPartitioner {

  val random = new Random()
  def element = Record(random.nextInt(1000).toString, random.nextString(5))

  val partitioner = new Partitioner[Record] {
    override def partition(key: Record, numPartitions: Int): Int = {
      key.id.hashCode % numPartitions
    }
  }

  val debugMapper = new RichMapFunction[Record, String] {
    override def map(value: Record): String = {
      value.id
    }
  }

  def main(args: Array[String]): Unit = {

    val see = StreamExecutionEnvironment.getExecutionEnvironment
    see.setParallelism(2)
    val iterator = Seq.fill(50)(element)
    val stream = see.fromCollection(iterator).partitionCustom(partitioner, record => record)

    stream.map(debugMapper).writeAsText("/Users/aspina/Desktop/partition", WriteMode.OVERWRITE)

    see.execute()
  }
}