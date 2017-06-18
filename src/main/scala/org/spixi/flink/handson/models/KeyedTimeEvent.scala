package org.spixi.flink.handson.models

/**
  * An event extending a time event enriched by a string key.
  * @param timestamp The epoch timestamp.
  * @param key The event key as String.
  * @param value The value as T type.
  * @tparam T value type
  */
case class KeyedTimeEvent[T](timestamp: Long, key: String, value: T) extends TimeEvent[T] {

  override def toString: String = {
    timestamp + "," + key + "," + value
  }

}
