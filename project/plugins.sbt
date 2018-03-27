resolvers += "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15")

addSbtPlugin("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.14.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"
