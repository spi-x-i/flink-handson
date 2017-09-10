package org.spixi.flink.handson.model

case class KeyedTimeEvent[T](timestamp: Long, key: String, value: T) extends TimeEvent[T] {

  override def toString: String = {
    timestamp + "," + key + "," + value
  }

}
