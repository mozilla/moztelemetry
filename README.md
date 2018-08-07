[![Build Status](https://travis-ci.org/mozilla/moztelemetry.svg?branch=master)](https://travis-ci.org/mozilla/moztelemetry)
[![codecov.io](https://codecov.io/github/mozilla/moztelemetry/coverage.svg?branch=master)](https://codecov.io/github/mozilla/moztelemetry?branch=master)
[![CircleCi](https://circleci.com/gh/mozilla/moztelemetry.svg?style=shield&circle-token=3fff2168f7d8da61b47bd1481c4fcb984ec88ef5)](https://circleci.com/gh/mozilla/moztelemetry)

# moztelemetry

Mozilla's Telemetry API for Scala

## Using moztelemetry

In SBT:
```
resolvers += "S3 local maven snapshots" at "s3://net-mozaws-data-us-west-2-ops-mavenrepo/snapshots"
libraryDependencies += "com.mozilla.telemetry" %% "moztelemetry" % "1.1-SNAPSHOT"
```

## Testing

The only dependency you'll need to have installed is `docker-compose`
which should be included with the 
[official Docker distribution for your system](https://www.docker.com/get-started).

To run the tests, use the `./bin/sbt` wrapper:

```
# Will spin up moto and sbt via docker-compose, and run all the CI tests.
./bin/sbt
```

For a faster, more targeted test, you might want to avoid spinning up the
S3 mock server and specify a specific suite to run:

```
DOCKER_COMPOSE_ARGS='--no-deps' ./bin/sbt 'testOnly *StatisticsTest'
```

### Running benchmarks

Benchmarks of the deserialization process can be run using the testing script:
```
# via docker
./bin/sbt bench:test

# directly
sbt bench:test
```

## Publishing snapshots

Snapshots will be published to our local maven repo in S3 on every commit merged into master via a CircleCI job.

## Inspecting the Generated Protoc Case Classes

We use protoc to generate case classes which decode (and encode) the protobuf byte arrays.

You can inspect the functions in the generated Field class:
```
sbt compile
cd target/scala-2.11/classes/com/mozilla/telemetry/heka/
javap -c Field\$.class
```

There are more components of the field class, which can be seen by simply running `ls`. For example:
```
Field$ValueTypeEnum$.class
Field$ValueTypeEnum$BOOL$.class
Field$ValueTypeEnum$BYTES$.class
Field$ValueTypeEnum$DOUBLE$.class
Field$ValueTypeEnum$INTEGER$.class
Field$ValueTypeEnum$STRING$.class
Field$ValueTypeEnum$Unrecognized$.class
Field$ValueTypeEnum$Unrecognized.class
Field$ValueTypeEnum$class.class
Field$ValueTypeEnum.class
```
