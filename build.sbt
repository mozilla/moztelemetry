name := "moztelemetry"

version := "1.1-SNAPSHOT"

organization := "com.mozilla.telemetry"

scalaVersion := "2.11.8"

val sparkVersion = "2.2.0"

resolvers += Resolver.bintrayRepo("findify", "maven")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test, bench",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "commons-io" % "commons-io" % "1.3.2" % "test",
  "com.storm-enroute" %% "scalameter" % "0.8.2" % "bench",
  "com.github.seratch" %% "awscala" % "0.5.+",
  "com.amazonaws" % "aws-java-sdk" % "1.11.83",
  "org.yaml" % "snakeyaml" % "1.21"
)

// Compile proto files
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// Exclude generated classes from the coverage
coverageExcludedPackages := "com\\.mozilla\\.telemetry\\.heka\\.(Field|Message|Header)"

// Include the benchmarking framework
lazy val Benchmark = config("bench") extend Test
lazy val root =
  Project("root", file("."))
  .configs(Benchmark)
  .settings(inConfig(Benchmark)(Defaults.testSettings): _*)
  .settings(
    resolvers += "Sonatype OSS Snapshots" at
      "https://oss.sonatype.org/content/repositories/snapshots",
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false,
    parallelExecution in Benchmark := false
)
publishArtifact in Benchmark := false

scalacOptions ++= Seq(
  "-feature",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Xfatal-warnings"
)

publishMavenStyle := true

publishTo := {
  val localMaven = "s3://net-mozaws-data-us-west-2-ops-mavenrepo/"
  if (isSnapshot.value)
    Some("snapshots" at localMaven + "snapshots")
  else
    Some("releases"  at localMaven + "releases")
}

// Make sure we don't include benchmark dependencies in the pom
// https://github.com/sbt/sbt/issues/1380#issuecomment-388057539
makePomConfiguration := makePomConfiguration.value.withConfigurations(Configurations.defaultMavenConfigurations)

pomExtra := (
  <url>https://github.com/mozilla/moztelemetry</url>
    <licenses>
      <license>
        <name>MPL 2.0</name>
        <url>https://www.mozilla.org/en-US/MPL/2.0/</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com/mozilla/moztelemetry.git</url>
      <connection>scm:git:git@github.com/mozilla/moztelemetry.git</connection>
    </scm>
    <developers>
      <developer>
        <id>vitillo</id>
        <name>Roberto Agostino Vitillo</name>
        <url>https://robertovitillo.com</url>
      </developer>
    </developers>)
