import sbt.Keys._
import sbt._

object Commons {

  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.11",
    organization := "org.spixi",
    resolvers in ThisBuild ++= Seq(
      "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
      "Radicalbit Snapshots" at "https://tools.radicalbit.io/maven/repository/snapshots/",
      "Radicalbit Releases" at "https://tools.radicalbit.io/maven/repository/internal/",
      Resolver.sonatypeRepo("releases"),
      Resolver.mavenLocal
    ),
    parallelExecution in Test := false,
    run in Compile := Defaults
      .runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
      .evaluated
  )
}