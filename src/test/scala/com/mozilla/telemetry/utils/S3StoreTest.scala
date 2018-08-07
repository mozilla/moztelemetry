/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.util.zip.GZIPOutputStream

import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.ObjectMetadata
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}


class S3StoreTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private val client = S3Store.s3
  private val bucketName = "test"

  override def beforeAll: Unit = {
    val motoHost: String = sys.env.getOrElse("MOTO_HOST", "localhost")
    client.setEndpoint(s"http://$motoHost:5000")
    client.setS3ClientOptions(S3ClientOptions
      .builder()
      .setPathStyleAccess(true)
      .disableChunkedEncoding
      .build)

    client.createBucket(bucketName)
    client.putObject(bucketName, "a/b/c", "foo")
    client.putObject(bucketName, "a/b/d", "foo")
    client.putObject(bucketName, "a/e/f", "bar")
  }

  override def afterAll: Unit = {
    val bucket = client.bucket(bucketName).get
    while (client.keys(bucket).nonEmpty) {
      client.keys(bucket).foreach(x => client.deleteObject(bucketName, x))
    }
    client.deleteBucket(bucketName)
  }

  "S3Store" can "get the value of a key" in {
    IOUtils.toString(S3Store.getKey(bucketName, "a/b/c")) should be ("foo")
  }

  it can "list keys" in {
    S3Store.listKeys(bucketName, "a/b/").toSet should be (Set(ObjectSummary("a/b/c", 3), ObjectSummary("a/b/d", 3)))
  }

  it can "list folders" in {
    S3Store.listFolders(bucketName, "a/").toSet should be (Set("a/b/", "a/e/"))
    S3Store.listFolders(bucketName, "a/b/").toSet should be (Set())
  }

  it can "upload files" in {
    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      val p = new java.io.PrintWriter(f)
      try { op(p) } finally { p.close() }
    }

    val tempFile = File.createTempFile("test", ".txt")
    tempFile.deleteOnExit()
    printToFile(tempFile) (p => p.print("foo"))

    S3Store.uploadFile(tempFile, bucketName, "prefix", "id")
    IOUtils.toString(S3Store.getKey(bucketName, "prefix/id")) should be ("foo")
  }

  it can "delete keys" in {
    S3Store.isPrefixEmpty(bucketName, "a/e/") should be (false)
    S3Store.deleteKey(bucketName, "a/e/f")
    a[Exception] should be thrownBy {
      S3Store.getKey(bucketName, "a/e/f")
    }
    S3Store.isPrefixEmpty(bucketName, "a/e/") should be (true)
  }

  it can "read gzipped files" in {
    val obj = new ByteArrayOutputStream()
    val gzip = new GZIPOutputStream(obj)
    gzip.write("foo".getBytes())
    gzip.close()

    val stream = new ByteArrayInputStream(obj.toByteArray())
    val metadata = new ObjectMetadata()
    metadata.setContentEncoding("gzip")
    client.putObject(bucketName, "gzipped", stream, metadata)
    IOUtils.toString(S3Store.getKey(bucketName, "gzipped")) should be ("foo")
  }

  it can "read multiple batches" in {
    for (i <- 1 to 1042) {
      client.putObject(bucketName, s"batch/${i}/foo", "bar")
    }
    S3Store.listFolders(bucketName, "batch/").size shouldBe (1042)
  }
}
