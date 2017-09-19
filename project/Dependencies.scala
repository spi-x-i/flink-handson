import sbt._

object Dependencies {

  private object flink {
    lazy val namespace = "org.apache.flink"
    lazy val version = "1.3.2"
    lazy val core = namespace %% "flink-scala" % version
    lazy val streaming = namespace %% "flink-streaming-scala" % version
    lazy val clients = namespace %% "flink-clients" % version
  }

  private object influxdb {
    lazy val namespace = "com.paulgoldbaum"
    lazy val version = "0.5.2"
    lazy val scala = namespace %% "scala-influxdb-client" % version
  }

  private object jpmml {
    lazy val version = "1.1.4-DEV"
    lazy val namespace = "io.radicalbit"
    lazy val core = namespace %% "flink-jpmml-scala" % version
  }

  private object logging {
    lazy val namespace = "org.slf4j"
    lazy val version = "1.7.7"
    lazy val slf4j = namespace % "slf4j-api" % version
  }

  lazy val simpleDependencies = Seq(
    flink.core % Provided,
    flink.streaming % Provided,
    flink.clients % Provided,
    influxdb.scala
  )

  lazy val jpmmlDependencies = Seq(
    flink.core % Provided,
    flink.streaming % Provided,
    flink.clients % Provided,
    jpmml.core,
    logging.slf4j
  )

}