/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
}
