[![Build Status](https://travis-ci.org/mozilla/moztelemetry.svg?branch=master)](https://travis-ci.org/mozilla/moztelemetry)
[![codecov.io](https://codecov.io/github/mozilla/moztelemetry/coverage.svg?branch=master)](https://codecov.io/github/mozilla/moztelemetry?branch=master)

# moztelemetry
Mozilla's Telemetry API for Scala

## Using moztelemetry
In SBT:
```
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += "com.mozilla.telemetry" %% "moztelemetry" % "1.0-SNAPSHOT"
```

## Testing
To run the tests you have to start a mock S3 service first with moto:

```
pip install moto
moto_server s3 -p 8001 &
AWS_ACCESS_KEY_ID=foo AWS_SECRET_ACCESS_KEY=foo sbt test
```
