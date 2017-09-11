package org.spixi.flink.functions

import io.radicalbit.nsdb.api.scala.Bit

object SinkFunction {
  def converter(namespace: String, dims: List[String])(metric: String)(in: (Double, Double, Double)): Bit = {
    val zipping = dims
      .zip(List(in._1, in._2))
      .map(item => (item._1 -> item._2.asInstanceOf[java.io.Serializable]))

    Bit(namespace = namespace, metric = metric, dimensions = zipping).value(in._3)
  }
  def converterCouples(namespace: String, dims: List[String])(metric: String)(in: (Double, Double)): Bit = {
    val zipping = dims
      .zip(List(in._1))
      .map(item => (item._1 -> item._2.asInstanceOf[java.io.Serializable]))

    Bit(namespace = namespace, metric = metric, dimensions = zipping).value(in._2)
  }

  def converterToSeries(namespace: String)(metric: String)(in: Double): Bit =
    Bit(namespace = namespace, metric = metric, dimensions = List.empty).value(in)

}
