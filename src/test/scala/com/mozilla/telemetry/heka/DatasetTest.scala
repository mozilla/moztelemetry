/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.heka

import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.ObjectMetadata
import com.mozilla.telemetry.utils.{ObjectSummary, S3Store}
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class DatasetTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private val client = S3Store.s3
  private val metaBucketName = "net-mozaws-prod-us-west-2-pipeline-metadata"
  private val dataBucketName = "data"
  private val hekaFile = Resources.hekaFile()
  private def hekaStream: InputStream = new ByteArrayInputStream(hekaFile)

  override def beforeAll: Unit = {
    client.setEndpoint("http://127.0.0.1:8001")
    client.setS3ClientOptions(S3ClientOptions
      .builder()
      .setPathStyleAccess(true)
      .disableChunkedEncoding
      .build)

    client.createBucket(metaBucketName)
    client.createBucket(dataBucketName)

    val sources = s"""
                     |{
                     |  "telemetry": {
                     |    "prefix": "telemetry",
                     |    "metadata_prefix": "telemetry",
                     |    "bucket": "$dataBucketName"
                     |  }
                     |}
                  """.stripMargin
    client.putObject(metaBucketName, "sources.json", sources)

    val schema = """
                   |{
                   |  "dimensions": [
                   |    { "field_name": "submissionDate" },
                   |    { "field_name": "docType" },
                   |    { "field_name": "appName" }
                   |  ]
                   |}
                 """.stripMargin

    client.putObject(metaBucketName, "telemetry/schema.json", schema)

    def putHekaObject(key: String) = {
      client.putObject(dataBucketName, key, hekaStream, new ObjectMetadata())
    }

    putHekaObject("telemetry/20160606/main/Firefox/x")
    putHekaObject("telemetry/20160606/main/Fennec/x")
    putHekaObject("telemetry/20160607/main/Firefox/x")
    putHekaObject("telemetry/20160606/crash/Fennec/x")
    client.putObject(dataBucketName, "telemetry/20160606/error/Firefox/x", new ByteArrayInputStream("foo".getBytes), new ObjectMetadata())
  }

  override def afterAll(): Unit = {
    val metaBucket = client.bucket(metaBucketName).get
    val dataBucket = client.bucket(dataBucketName).get

    client.keys(metaBucket).foreach(x => client.deleteObject(metaBucketName, x))
    client.keys(dataBucket).foreach(x => client.deleteObject(dataBucketName, x))
    client.deleteBucket(metaBucket)
    client.deleteBucket(dataBucket)
  }

  "A partition" can "be filtered" in {
    val files = Dataset("telemetry")
      .where("submissionDate") {
        case date if date.endsWith("06") => true
      }.where("docType") {
        case "main" => true
      }.where("appName") {
        case "Firefox" => true
      }.summaries().toList

    files should be (List(ObjectSummary("telemetry/20160606/main/Firefox/x", hekaFile.size)))
  }

  it should "have at most one clause" in {
    a[Exception] should be thrownBy {
      Dataset("telemetry")
        .where("submissionDate") {
          case date if date.endsWith("06") => true
        }.where("docType") {
          case "main" => true
        }.where("docType") {
          case "crash" => true
        }
    }
  }

  it should "exist" in {
    a[Exception] should be thrownBy {
      Dataset("telemetry").where("foobar") {
        case date if date.endsWith("06") => true
      }
    }
  }

  "Files" can "be limited" in {
    val files = Dataset("telemetry")
      .where("submissionDate") {
        case "20160606" => true
      }.where("docType") {
        case "main" => true
      }.summaries(Some(1)).toList

    files should be (List(ObjectSummary("telemetry/20160606/main/Fennec/x", hekaFile.size)))
  }

  "Records" should "not be missing" in {
    val spark = SparkSession.builder().master("local[1]").getOrCreate()
    implicit val sc = spark.sparkContext

    try {
      val records = Dataset("telemetry")
        .where("submissionDate") {
          case "20160606" => true
        }.where("docType") {
          case "main" => true
        }.where("appName") {
          case "Firefox" => true
        }

      records.count() should be (42)
    } finally {
      sc.stop()
    }
  }

  "Reads from S3" should "fail silently" in {
    val spark = SparkSession.builder().master("local[1]").getOrCreate()
    implicit val sc = spark.sparkContext

    try {
      val records = Dataset("telemetry")
        .where("submissionDate") {
          case "20160606" => true
        }.where("docType") {
        case "error" => true
      }

      records.count() should be (0)
    } finally {
      sc.stop()
    }
  }
}
