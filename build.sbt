name := "moztelemetry"

version := "1.0"

scalaVersion := "2.11.8"

spName := "mozilla/moztelemetry"

spIncludeMaven := false

sparkVersion := "2.1.0"

sparkComponents ++= Seq("core")

resolvers += Resolver.bintrayRepo("findify", "maven")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.apache.commons" % "commons-io" % "1.3.2" % "test",
  "com.github.seratch" %% "awscala" % "0.5.+",
  "com.amazonaws" % "aws-java-sdk" % "1.11.83",
  "com.google.protobuf" % "protobuf-java" % "2.5.0"
)
/*
 The HBase client requires protobuf-java 2.5.0 but scalapb uses protobuf-java 3.x
 so we have to force the dependency here. This should be fine as we are using only
 version 2 of the protobuf spec.
*/
dependencyOverrides += "com.google.protobuf" % "protobuf-java" % "2.5.0"

// Compile proto files
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// Exclude generated classes from the coverage
coverageExcludedPackages := "com\\.mozilla\\.telemetry\\.heka\\.(Field|Message|Header)"

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")

licenses += "Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0")
