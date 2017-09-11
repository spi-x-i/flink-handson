package org.spixi.flink.generation.function

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.spixi.flink.generation.models.Pixel

final case class Accuracy(modelId: String, success: Int, events: Int)

class AccuracyComputation extends RichMapFunction[(Pixel, Double), (String, Double)] {

  protected lazy val accuracy: ValueState[Accuracy] = {
    val typeInfo = TypeInformation.of(new TypeHint[Accuracy]() {})
    val descriptor = new ValueStateDescriptor[Accuracy](
      "random-id",
      typeInfo
    )
    getRuntimeContext.getState(descriptor)
  }

  def state: Accuracy = Option(accuracy.value()).getOrElse(Accuracy("", 0, 0))

  override def map(value: (Pixel, Double)): (String, Double) = {
    val isSuccess = if (value._1.trueClazz == value._2) 1 else 0
    accuracy.update(
      state.copy(modelId = value._1.modelId, success = state.success + isSuccess, events = state.events + 1))

    (state.modelId, state.success / state.events.toDouble)
  }

}
