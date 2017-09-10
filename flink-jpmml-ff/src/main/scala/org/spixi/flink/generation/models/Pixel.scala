package org.spixi.flink.generation.models

import io.radicalbit.flink.pmml.scala.models.input.BaseEvent

final case class Pixel(r: Int, g: Int, trueClazz: Int, modelId: String, occurredOn: Long) extends BaseEvent
