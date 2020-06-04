import sbt._
import Keys._

lazy val resolutionRepos = Seq(
  // For Snowplow
  "Snowplow Analytics Maven releases repo" at "http://maven.snplow.com/releases/",
  // For ua-parser
  "user-agent-parser repo"                 at "https://clojars.org/repo/",
  // For Snowplow libraries
  "Snowplow Bintray" at "https://snowplow.bintray.com/snowplow-maven/"
)

// we fork a JVM per test in order to not reuse enrichment registries
import Tests._
{
  def oneJVMPerTest(tests: Seq[TestDefinition]) =
    tests.map(t => new Group(t.name, Seq(t), SubProcess(ForkOptions()))).toSeq
  testGrouping in Test := oneJVMPerTest((definedTests in Test).value)
}

lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization  := "com.snowplowanalytics",
  version       := "1.2.2",
  scalaVersion  := "2.12.10",
  resolvers     ++= resolutionRepos
)

lazy val paradiseDependency =
  "org.scalamacros" % "paradise" % scalaMacrosVersion cross CrossVersion.full
lazy val macroSettings = Seq(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  addCompilerPlugin(paradiseDependency)
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

import com.typesafe.sbt.packager.docker._
packageName in Docker := "snowplow/beam-enrich"
maintainer in Docker := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>"
dockerBaseImage := "snowplow-docker-registry.bintray.io/snowplow/k8s-dataflow:0.1.1"
daemonUser in Docker := "snowplow"
dockerUpdateLatest := true
dockerCommands := dockerCommands.value.map{
  case ExecCmd("ENTRYPOINT", args) => ExecCmd("ENTRYPOINT", "docker-entrypoint.sh", args)
  case e => e
}

lazy val scioVersion = "0.8.1"
lazy val beamVersion = "2.18.0"
lazy val sceVersion = "1.1.2"
lazy val scalaMacrosVersion = "2.1.1"
lazy val slf4jVersion = "1.7.25"
lazy val circeVersion = "0.11.1"
lazy val scalatestVersion = "3.0.8"
lazy val sentryVersion = "1.7.30"

lazy val root: Project = Project(
  "beam-enrich",
  file(".")
).settings(
  commonSettings ++ macroSettings ++ noPublishSettings,
  description := "Streaming enrich job written using SCIO",
  buildInfoKeys := Seq[BuildInfoKey](organization, name, version, "sceVersion" -> sceVersion),
  buildInfoPackage := "com.snowplowanalytics.snowplow.enrich.beam.generated",
  scalafmtConfig := file(".scalafmt.conf"),
  scalafmtOnCompile := true,
  libraryDependencies ++= Seq(
    "com.spotify" %% "scio-core" % scioVersion,
    "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion,
    "com.snowplowanalytics" %% "snowplow-common-enrich" % sceVersion,
    "org.slf4j" % "slf4j-simple" % slf4jVersion,
    "io.sentry" % "sentry" % sentryVersion
  ) ++ Seq(
    "com.spotify" %% "scio-test" % scioVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion,
    "io.circe" %% "circe-literal" % circeVersion
  ).map(_ % "test")
).enablePlugins(JavaAppPackaging, DockerPlugin, BuildInfoPlugin)

lazy val repl: Project = Project(
  "repl",
  file(".repl")
).settings(
  commonSettings ++ macroSettings ++ noPublishSettings,
  description := "Scio REPL for beam-enrich",
  libraryDependencies ++= Seq(
    "com.spotify" %% "scio-repl" % scioVersion
  ),
  mainClass in Compile := Some("com.spotify.scio.repl.ScioShell")
).dependsOn(
  root
)
