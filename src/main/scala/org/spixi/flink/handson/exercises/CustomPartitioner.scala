package org.spixi.flink.handson.exercises

import org.apache.flink.api.common.functions.Partitioner

import scala.util.{Success, Try}

class CustomPartitioner extends Partitioner[String] {

  override def partition(key: String, numPartitions: Int): Int = {
    Try { (key.toInt, numPartitions == 2) } match {
      case Success((k, true)) => k.hashCode % 2
      case Success((k, false)) => k.hashCode
      case _ => throw new RuntimeException("Can't create the custom partitioner.")
    }
  }

}
