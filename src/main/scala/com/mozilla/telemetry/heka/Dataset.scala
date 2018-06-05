/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.heka

import com.mozilla.telemetry.utils.{ObjectSummary, S3Store}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source
import scala.language.implicitConversions

private case class Schema(dimensions: List[Dimension])
private case class Dimension(fieldName: String)

class Dataset private (bucket: String, schema: Schema, prefix: String,
                       clauses: Map[String, PartialFunction[String, Boolean]]) extends java.io.Serializable {
  private object Logger extends Serializable {
    @transient lazy val log = org.apache.log4j.Logger.getLogger(Dataset.getClass.getName)
  }

  def where(dimension: String)(clause: PartialFunction[String, Boolean]): Dataset = {
    if (clauses.contains(dimension)) {
      throw new Exception(s"There should be only one clause for $dimension")
    }

    if (!schema.dimensions.contains(Dimension(dimension))) {
      throw new Exception(s"The dimension $dimension doesn't exist")
    }

    new Dataset(bucket, schema, prefix, clauses + (dimension -> clause))
  }

  def summaries(fileLimit: Option[Int] = None): Seq[ObjectSummary] = {
    import scala.collection.GenSeq

    def scan(dimensions: List[Dimension], prefixes: GenSeq[String]): GenSeq[String] = {
      if (dimensions.isEmpty) {
        prefixes
      } else {
        val dimension = dimensions.head
        val clause = clauses.getOrElse(dimension.fieldName, {case x => true}: PartialFunction[String, Boolean])
        val matched = prefixes
          .flatMap(S3Store.listFolders(bucket, _))
          .filter{ prefix =>
            val value = prefix.split("/").last
            clause.isDefinedAt(value) && clause(value)
          }
        scan(dimensions.tail, matched)
      }
    }

    fileLimit match {
      case Some(x) =>
        scan(schema.dimensions, Stream(s"$prefix/"))
          .flatMap(S3Store.listKeys(bucket, _))
          .take(x)
          .seq
      case _ =>
        scan(schema.dimensions, Stream(s"$prefix/").par)
          .flatMap(S3Store.listKeys(bucket, _))
          .seq
    }
  }

  def records(fileLimit: Option[Int] = None, minPartitions: Option[Int] = None)(implicit sc: SparkContext): RDD[Message] = {
    val summarized = summaries(fileLimit).toIterator
    val groups = minPartitions match {
      case Some(x) =>
        ObjectSummary.equallySizedGroups(summarized, x)
      case None =>
        // Partition the files into groups of approximately-equal size
        ObjectSummary.groupBySize(summarized)
    }
    sc.parallelize(groups, groups.size).flatMap(x => x).flatMap(o => {
      File.parse(S3Store.getKey(bucket, o.key), ex => Logger.log.warn(s"Failure to read file ${o.key}: ${ex.getMessage}"))
    })
  }
}

object Dataset {
  def apply(dataset: String): Dataset = {
    implicit val formats = DefaultFormats

    val metaBucket = "net-mozaws-prod-us-west-2-pipeline-metadata"
    val metaSources = parse(Source.fromInputStream(S3Store.getKey(metaBucket, "sources.json")).mkString)
    val JString(prefix) = metaSources \\ dataset \\ "prefix"
    val JString(metadataPrefix) = metaSources \\ dataset \\ "metadata_prefix"
    val JString(bucketName) = metaSources \\ dataset \\ "bucket"
    val schema = parse(Source.fromInputStream(S3Store.getKey(metaBucket, s"$metadataPrefix/schema.json")).mkString)
      .camelizeKeys
      .extract[Schema]

    new Dataset(bucketName, schema, prefix, Map())
  }

  implicit def datasetToRDD(dataset: Dataset)(implicit sc: SparkContext): RDD[Message] = {
    dataset.records()
  }
}
