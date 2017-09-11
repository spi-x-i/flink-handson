package org.spixi.flink.functions

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.util.Collector
import org.spixi.flink.generation.models.Pixel

class EvaluationSplitter(side2: OutputTag[(Double, Double, Double)], side3: OutputTag[(Double, Double, Double)])
  extends ProcessFunction[(Pixel, Double), (Double, Double, Double)] {

  override def processElement(value: (Pixel, Double),
                              ctx: ProcessFunction[(Pixel, Double), (Double, Double, Double)]#Context,
                              out: Collector[(Double, Double, Double)]): Unit = {

    val version = value._1.modelId.takeRight(1)
    val outcome = (value._1.r.toDouble, value._1.g.toDouble, value._2)

    version match {
      case "5" => out.collect(outcome)
      case "1" => ctx.output(side2, outcome)
      case "3" => ctx.output(side3, outcome)
      case _   =>
    }

  }

}
