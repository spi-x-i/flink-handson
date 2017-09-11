package org.spixi.flink.functions

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.util.Collector

import scala.util.{Failure, Success, Try}

import scala.collection.JavaConverters._

class ErrorFormatter extends RichFlatMapFunction[(String, Double), (Double, Double)] with CheckpointedFunction {

  @transient
  private var versionsCheckpoint: ListState[Int] = _
  @transient
  private var versions: Set[Int] = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    versions = Set.empty[Int]
  }

  override def flatMap(value: (String, Double), out: Collector[(Double, Double)]): Unit = {
    val version = value._1.split("_").last
    versions = versions + version.toInt

    if (version == versions.max.toString) out.collect((1 - value._2, computeAccuracy(version)))
  }

  private def computeAccuracy(version: String): Double =
    version.toInt match {
      case 1 => 0.48
      case 2 => 0.72
      case 3 => 0.90
      case _ => 0.0
    }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    versionsCheckpoint.clear()
    versions.foreach { version =>
      versionsCheckpoint.add(version)
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val typeInfo = TypeInformation.of(new TypeHint[Int]() {})
    val descriptor = new ListStateDescriptor[Int]("models-versions", typeInfo)

    versionsCheckpoint = context.getOperatorStateStore.getUnionListState(descriptor)

    if (context.isRestored) {
      Try(versionsCheckpoint.get()) match {
        case Success(state) => versions = versions ++ state.asScala.toSet[Int].toList
        case Failure(_) => throw new RuntimeException("Not available state in ListState!")
      }
    }
  }

}
