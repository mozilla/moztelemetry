# moztelemetry
Mozilla's Telemetry API for Scala

## Testing
To run the tests you have to start a mock S3 service first with moto:

```
pip install moto
moto_server s3 -p 8001 &
AWS_ACCESS_KEY_ID=foo AWS_SECRET_ACCESS_KEY=foo sbt test
```
