/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.utils

import org.scalatest.{FlatSpec, Matchers}

class ObjectSummaryTest extends FlatSpec with Matchers {
  "ObjectSummarys" can "be grouped by size" in {
    val totalBytes = 1000
    val threshold = 12
    val summaries = for (i <- 1 to totalBytes) yield ObjectSummary("foo", 1)
    val groups = ObjectSummary.groupBySize(summaries.toIterator, threshold)
    groups.size should be (Math.ceil(totalBytes.toFloat/(threshold - 1)).toInt)
    groups.flatMap(x => x).map(_.size).sum should be (totalBytes)
  }

  "ObjectSummarys" can "be grouped into a specific number of groups" in {
    val testSummaries = List(5, 1, 2, 3, 9, 8, 7, 1, 12, 8).map(x => ObjectSummary(x.toString, x))
    val groups = ObjectSummary.equallySizedGroups(testSummaries.toIterator, 5)
    val expectedGroups = List(
      List(1, 9),
      List(3, 8),
      List(5, 7),
      List(12),
      List(1, 2, 8)
    ).map(g => g.map(x => ObjectSummary(x.toString, x)))
    // This is overly strict, but it's unclear to me the best way to guarantee "as close to equal as possible"
    // short of hardcoding an example check.
    groups should be(expectedGroups)
  }

  "ObjectSummarys" should "respect the threshold parameter over group count when a number of groups is requested" in {
    val testSummaries = List(5, 5, 5, 5, 5).map(x => ObjectSummary(x.toString, x))
    val groups = ObjectSummary.equallySizedGroups(testSummaries.toIterator, 2, 11)
    groups.size should be(3)
    groups.flatten.map(_.size).sum should be(25)
    groups.map(l => l.map(_.size).sum).max should be(10)
  }

}
