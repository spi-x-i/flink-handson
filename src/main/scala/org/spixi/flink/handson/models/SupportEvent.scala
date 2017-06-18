package org.spixi.flink.handson.models

case class SupportEvent[T](timestamp: Long, value: T, supportInfo: Map[String, String]) extends TimeEvent[T] {

  override def toString: String = Seq(timestamp, value.toString, supportInfo.toString) mkString ","

}
