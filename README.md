[![Build Status](https://travis-ci.org/mozilla/moztelemetry.svg?branch=master)](https://travis-ci.org/mozilla/moztelemetry)
[![codecov.io](https://codecov.io/github/mozilla/moztelemetry/coverage.svg?branch=master)](https://codecov.io/github/mozilla/moztelemetry?branch=master)
[![CircleCi](https://circleci.com/gh/mozilla/moztelemetry.svg?style=shield&circle-token=3fff2168f7d8da61b47bd1481c4fcb984ec88ef5)](https://circleci.com/gh/mozilla/moztelemetry)

# moztelemetry
Mozilla's Telemetry API for Scala

## Using moztelemetry
In SBT:
```
resolvers += "S3 local maven snapshots" at "s3://net-mozaws-data-us-west-2-ops-mavenrepo/snapshots"
libraryDependencies += "com.mozilla.telemetry" %% "moztelemetry" % "1.0-SNAPSHOT"
```

## Testing
To run the tests you have to start a mock S3 service first with moto:

```
pip install moto
moto_server s3 -p 8001 &
AWS_ACCESS_KEY_ID=foo AWS_SECRET_ACCESS_KEY=foo sbt test
```

## Publishing snapshots
Snapshots will now be published to our local maven repo in s3 on every commit merged into master via a circleci build

