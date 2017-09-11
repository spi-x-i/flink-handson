package org.spixi.flink.bench.functions

import io.radicalbit.flink.pmml.scala.models.control.AddMessage
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector
import org.spixi.flink.generation.models.Pixel

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

class HorizontalEnricher(modelApplication: String)
    extends RichCoFlatMapFunction[Pixel, AddMessage, Pixel]
    with CheckpointedFunction {

  @transient
  private var modelVersion: ListState[Int] = _
  @transient
  private var currentVersion: Set[Int] = _

  override def flatMap1(value: Pixel, out: Collector[Pixel]): Unit = {
    val version = scala.util.Random.shuffle(currentVersion).headOption.getOrElse(-1)
    out.collect(value.copy(modelId = modelApplication + "_" + version))
  }

  override def flatMap2(value: AddMessage, out: Collector[Pixel]): Unit = {
    currentVersion = currentVersion + value.modelId.version.toInt
  }

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    currentVersion = Set.empty[Int]
  }

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
