package org.spixi.flink.handson

/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.handson.models.{KeyedTimeEvent, SupportEvent}
import org.spixi.flink.handson.sources.{ControlStreamSource, EventStreamSource}

object HandsOn {

  def main(args: Array[String]) {
    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val mainInput: DataStream[KeyedTimeEvent[String]] = env.addSource(new EventStreamSource)

    val supportInput: DataStream[SupportEvent[Int]] =
      env.addSource(new ControlStreamSource("Goofie", "Donald", "Mikey"))

    val result = mainInput.connect(supportInput.broadcast).process(new SupportFunction)

    // execute program
    env.execute("Flink Scala API Skeleton")
  }

}
