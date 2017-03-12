import sbt._

object Dependencies {

  object flink {
    lazy val namespace = "org.apache.flink"
    lazy val version = "1.2.0"
    lazy val core = namespace %% "flink-scala" % version
    lazy val streaming = namespace %% "flink-streaming-scala" % version
  }

  lazy val addons = Seq(
    flink.core % Provided,
    flink.streaming % Provided
  )

}