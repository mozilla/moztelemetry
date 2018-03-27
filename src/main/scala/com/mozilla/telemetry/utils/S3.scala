/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.utils

import java.io.{File, InputStream}
import java.util.UUID
import java.util.zip.GZIPInputStream
import awscala.s3.{Bucket, S3}
import org.apache.log4j.Logger
import scala.collection.JavaConverters._

object S3Store {
  @transient private[telemetry] implicit lazy val s3: S3 = S3()
  @transient private lazy val logger = Logger.getLogger("S3Store")

  def getKey(bucket: String, key: String): InputStream = {
    val s3Object = Bucket(bucket).getObject(key).getOrElse(throw new Exception(s"File missing on S3: $key"))
    val encoding = s3Object.getObjectMetadata.getContentEncoding

    encoding match {
      case "gzip" => new GZIPInputStream(s3Object.getObjectContent)
      case _ => s3Object.getObjectContent
    }
  }

  def listKeys(bucket: String, prefix: String): Stream[ObjectSummary] = {
    s3.objectSummaries(Bucket(bucket), prefix)
      .map(summary => ObjectSummary(summary.getKey, summary.getSize))
  }

  def listFolders(bucket: String, prefix: String, delimiter: String = "/"): Stream[String] = {
    import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}

    val request = new ListObjectsRequest().
      withBucketName(bucket).
      withPrefix(prefix).
      withDelimiter(delimiter)
    val firstListing = s3.listObjects(request)

    def completeStream(listing: ObjectListing): Stream[String] = {
      val prefixes = listing.getCommonPrefixes.asScala.toStream
      prefixes #:::
        (if (listing.isTruncated) {
          completeStream(s3.listNextBatchOfObjects(listing))
        } else {
          Stream.empty
        })
    }

    completeStream(firstListing)
  }

  def uploadFile(file: File, bucket: String, prefix: String, name: String = UUID.randomUUID.toString) {
    val key = s"$prefix/$name"
    logger.info(s"Uploading file to $bucket/$key")
    s3.putObject(bucket, key, file)
  }

  def deleteKey(bucket: String, key: String) {
    logger.info(s"Deleting file s3://$bucket/$key")
    s3.deleteObject(bucket, key)
  }

  def isPrefixEmpty(bucket: String, prefix: String): Boolean = {
    s3.objectSummaries(Bucket(bucket), prefix).isEmpty
  }
}
