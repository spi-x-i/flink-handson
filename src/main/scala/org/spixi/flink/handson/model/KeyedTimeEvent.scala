package org.spixi.flink.handson.model

case class TimedEvent[T](timestamp: Long, value: T) extends TimeEvent[T] {

  override def toString: String = {
    timestamp + "," + value
  }

}

case class KeyedTimedEvent[T](timestamp: Long, key: String, value: T) extends TimeEvent[T] {

  override def toString: String = {
    timestamp + "," + key + "," + value
  }

}

case class StringTimedEvent[T](timestamp: Long, key: String, value: T) extends TimeEvent[T]
