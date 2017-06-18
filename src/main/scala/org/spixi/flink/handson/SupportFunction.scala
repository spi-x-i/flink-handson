package org.spixi.flink.handson

import java.util
import java.util.Collections

import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.functions.co.{CoProcessFunction, RichCoProcessFunction}
import org.apache.flink.util.Collector
import org.spixi.flink.handson.SupportFunction.{SupportBody, SupportRecord, SupportType}
import org.spixi.flink.handson.models.{KeyedTimeEvent, SupportEvent}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

import scala.collection.JavaConverters._

object SupportFunction {

  type SupportType = String
  type SupportBody = String

  case class SupportRecord(desc: String, version: Int)

}

class SupportFunction
    extends RichCoProcessFunction[KeyedTimeEvent[String], SupportEvent[Int], String]
    with ListCheckpointed[util.Map[SupportType, SupportRecord]]
    with LazyLogging {

  private val rand = new scala.util.Random

  private var supportHistory = Map.empty[SupportType, SupportRecord]

  @transient
  private var supports = mutable.Map.empty[SupportType, SupportBody]

  override def processElement1(event: KeyedTimeEvent[String],
                               ctx: CoProcessFunction.Context,
                               out: Collector[String]): Unit = {
    // verify the message body has been already loaded
    val supportBody = supports.getOrElseUpdate(event.key, rand.nextString(32))
    // compute output abiding by the prediction
    out.collect(s"${event.key} -> $supportBody")
  }

  override def processElement2(message: SupportEvent[Int],
                               ctx: CoProcessFunction.Context,
                               out: Collector[String]): Unit = {
    message match {
      case SupportEvent(_, descriptionCurrent, infoCurrent) =>
        // check if the message is at its latest version or not
        if (supportHistory(infoCurrent("name")).version < infoCurrent("version").toInt) {
          // Updating the message record and related version
          supportHistory = supportHistory.updated(
            infoCurrent("name"),
            SupportRecord(descriptionCurrent.toString, infoCurrent("version").toInt))
          // Updating the message to current version if already loaded
          if (supports.contains(infoCurrent("name")))
            supports =
              supports.updated(infoCurrent("name"), s"${infoCurrent("name")} on version ${infoCurrent("version")}")
          else
            supports(infoCurrent("name")) = s"${infoCurrent("name")} on version ${infoCurrent("version")}"
        }
      case _ => logger.warn(s"Process function was not able to interpret control event message $message .")
    }
  }

  override def restoreState(state: util.List[util.Map[SupportType, SupportRecord]]): Unit = {
    supportHistory = state.asScala.map(_.asScala).reduceLeft(_ ++ _).toMap
  }

  override def snapshotState(checkpointId: Long, timestamp: Long): util.List[util.Map[SupportType, SupportRecord]] = {
    Collections.singletonList(supportHistory.asJava)
  }

  override def onTimer(timestamp: Long, ctx: CoProcessFunction.OnTimerContext, out: Collector[String]): Unit = {}

}
