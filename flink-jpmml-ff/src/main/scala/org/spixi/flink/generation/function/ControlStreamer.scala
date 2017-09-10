package org.spixi.flink.generation.function

import io.radicalbit.flink.pmml.scala.models.control.{AddMessage, ServingMessage}
import org.apache.flink.api.common.functions.MapFunction

class ControlStreamer(modelApp: String) extends MapFunction[String, ServingMessage] {

  override def map(value: String): ServingMessage = {
    val version = value.split("\\.").head.split("\\_").last.toInt
    val message = AddMessage(modelApp, version, value, System.currentTimeMillis())
    message
  }

}
