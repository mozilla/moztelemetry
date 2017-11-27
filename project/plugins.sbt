resolvers += "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")

addSbtPlugin("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.12.0")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
