package org.spixi.flink.handson.models

/**
  * Simple trait abstracting a simple event with a epoch Linux timestamp and a value
  * @tparam T value type
  */
trait TimeEvent[T] {
  def timestamp: Long
  def value: T

}
