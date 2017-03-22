package org.spixi.flink.handson.model

trait TimeEvent[T] {
  def timestamp: Long
  def value: T
}
