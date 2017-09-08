import sbt._
import sbt.Keys._

lazy val root = (project in file("."))
  .settings(Commons.settings: _*)
  .settings(
    name := "flink-hands-on",
    publish := {},
    publishLocal := {}
  )
  .aggregate(
    `simple-handson`,
    `flink-jpmml-ff`
  )

lazy val `simple-handson` = (project in file("simple-handson"))
  .settings(Commons.settings: _*)
  .settings(
    name := "simple-handson",
    libraryDependencies ++= Dependencies.simpleDependencies
  )

lazy val `flink-jpmml-ff` = (project in file("flink-jpmml-ff"))
  .settings(Commons.settings: _*)
  .settings(
    name := "flink-jpmml-ff",
    libraryDependencies ++= Dependencies.jpmmlDependencies
  )

scalafmtOnCompile in ThisBuild := true

// make run command include the provided dependencies
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
