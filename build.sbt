resolvers in ThisBuild ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal)

name := "Flink Hands-On"

version := "0.1-SNAPSHOT"

organization := "org.spixi"

scalaVersion in ThisBuild := "2.11.7"

lazy val root = (project in file(".")).settings(
  libraryDependencies ++= Dependencies.addons
)

onLoad in Global := (Command.process("scalafmt", _: State)) compose (onLoad in Global).value

mainClass in assembly := Some("org.spixi.Job")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _ *) => MergeStrategy.discard
  case x => MergeStrategy.first
}

// make run command include the provided dependencies
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
