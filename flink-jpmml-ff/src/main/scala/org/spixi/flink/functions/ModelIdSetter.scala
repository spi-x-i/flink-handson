package org.spixi.flink.functions

import io.radicalbit.flink.pmml.scala.models.state.CheckpointType.MetadataCheckpoint
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector
import org.spixi.flink.generation.models.Pixel

import scala.util.{Failure, Success, Try}

import scala.collection.JavaConverters._

class ModelIdSetter(modelApp: String) extends RichCoFlatMapFunction[Pixel, Int, Pixel] with CheckpointedFunction {

  @transient
  private var modelVersion: ListState[Int] = _
  @transient
  private var currentVersion: List[Int] = _

  override def flatMap1(value: Pixel, out: Collector[Pixel]): Unit =
    currentVersion.foreach(version => out.collect(value.copy(modelId = modelApp + "_" + version)))

  override def flatMap2(value: Int, out: Collector[Pixel]): Unit =
    currentVersion = currentVersion :+ value

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    modelVersion.clear()
    currentVersion.foreach { version =>
      modelVersion.add(version)
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val typeInfo = TypeInformation.of(new TypeHint[Int]() {})
    val descriptor = new ListStateDescriptor[Int]("models-state", typeInfo)

    modelVersion = context.getOperatorStateStore.getUnionListState(descriptor)

    if (context.isRestored) {
      Try(modelVersion.get()) match {
        case Success(state) => currentVersion = currentVersion ++ state.asScala.toSet[Int].toList
        case Failure(_) => throw new RuntimeException("Not available state in ListState!")
      }
    }
  }

}
