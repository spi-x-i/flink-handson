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
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.scala._
import org.spixi.flink.handson.sources.TimedSource

object HandsOn {

  def main(args: Array[String]) {
    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment


    // execute program
    env.execute("Flink Scala API Skeleton")
  }

  private def generateSensorData(env: StreamExecutionEnvironment) = {
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(1000, 1000))
    env.setParallelism(1)
    env.disableOperatorChaining

    val SLOWDOWN_FACTOR = 1
    val PERIOD_MS = 100

    val timestampSource = env.addSource(new TimedSource(PERIOD_MS, SLOWDOWN_FACTOR)).name("test data")

  }

  /*
  private def generateSensorData(env: StreamExecutionEnvironment) = {

    // Transform into sawtooth pattern
    val sawtoothStream = timestampSource.map(new SawtoothFunction(10)).name("sawTooth")
    // Simulate temp sensor
    val tempStream = sawtoothStream.map(new AssignKeyFunction("temp")).name("assignKey(temp)")
    // Make sine wave and use for pressure sensor
    val pressureStream = sawtoothStream.map(new SineWaveFunction).name("sineWave").map(new AssignKeyFunction("pressure")).name("assignKey(pressure")
    // Make square wave and use for door sensor
    val doorStream = sawtoothStream.map(new SquareWaveFunction).name("squareWave").map(new AssignKeyFunction("door")).name("assignKey(door)")
    // Combine all the streams into one and write it to Kafka
    val sensorStream = tempStream.union(pressureStream).union(doorStream)
    sensorStream
  }
 */

}
