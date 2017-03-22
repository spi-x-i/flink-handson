package org.spixi.flink.handson.model

trait TimeEvent[T <: Numeric[T]] {
  def timestamp: Long
  def value: T

}
