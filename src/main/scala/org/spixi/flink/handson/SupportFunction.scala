package org.spixi.flink.handson

import java.util
import java.util.Collections

import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.util.Collector
import org.spixi.flink.handson.SupportFunction.{SupportBody, SupportRecord, SupportType}
import org.spixi.flink.handson.models.{KeyedTimeEvent, SupportEvent}
import com.typesafe.scalalogging.LazyLogging
import org.apache.flink.api.common.state.MapState
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.co.CoProcessFunction

import scala.collection.mutable
import scala.collection.JavaConverters._

object SupportFunction {

  type SupportType = String
  type SupportBody = String

  type Context = CoProcessFunction[KeyedTimeEvent[String], SupportEvent[Int], String]#Context

  case class SupportRecord(desc: String, version: Int)

}

class SupportFunction
    extends CoProcessFunction[KeyedTimeEvent[String], SupportEvent[Int], String]
    with ListCheckpointed[util.HashMap[SupportType, SupportRecord]]
    with LazyLogging {

  private val rand = new scala.util.Random

  private var supportHistory = Map.empty[String, SupportRecord]

  @transient
  private var supports: mutable.Map[SupportType, SupportBody]  = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)

    supports = mutable.Map.empty[SupportType, SupportBody]
  }

  override def processElement1(event: KeyedTimeEvent[String],
                               ctx: SupportFunction.Context,
                               out: Collector[String]): Unit = {
    // verify the message body has been already loaded, skip otherwise
    val supportBody = supports.getOrElse(event.key, "NO MESSAGE AVAILABLE")
    // compute output abiding by the prediction
    out.collect(s"${event.key} -> $supportBody")
  }

  override def processElement2(message: SupportEvent[Int],
                               ctx: SupportFunction.Context,
                               out: Collector[String]): Unit = {
    message match {
      case SupportEvent(_, descriptionCurrent, infoCurrent) =>
        // check if the history has already received this control
        if (supportHistory.contains(infoCurrent("name"))) {
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
        } else {
          // Inserting brand-new message entry
          supports(infoCurrent("name")) = s"${infoCurrent("name")} on version ${infoCurrent("version")}"
        }
      case _ => logger.warn(s"Process function was not able to interpret control event message $message .")
    }
  }

  override def restoreState(state: util.List[util.HashMap[SupportType, SupportRecord]]): Unit = {
    supportHistory = state.asScala
      .map { hashMap: util.HashMap[SupportType, SupportRecord] =>
        hashMap.asScala
      }
      .reduceLeft(_ ++ _)
      .toMap
  }

  override def snapshotState(checkpointId: Long,
                             timestamp: Long): util.List[util.HashMap[SupportType, SupportRecord]] = {
    Collections.singletonList(new util.HashMap[SupportType, SupportRecord](supportHistory.asJava))
  }

}
