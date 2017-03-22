import sbt._

object Dependencies {

  object flink {
    lazy val namespace = "org.apache.flink"
    lazy val version = "1.2.0"
    lazy val core = namespace %% "flink-scala" % version
    lazy val streaming = namespace %% "flink-streaming-scala" % version
    lazy val clients = namespace %% "flink-clients" % version
  }

  object influxdb {
    lazy val namespace = "com.paulgoldbaum"
    lazy val version = "0.5.2"
    lazy val scala = namespace %% "scala-influxdb-client" % version
  }

  object influx {
    lazy val namespace = "org.influxdb"
    lazy val version = "2.3"
    lazy val db = namespace % "influxdb-java" % version
  }

  lazy val addons = Seq(
    flink.core % Provided,
    flink.streaming % Provided,
    flink.clients % Provided,
    influxdb.scala,
    influx.db
  )

}