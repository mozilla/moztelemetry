/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.utils

import scala.collection.mutable

// S3ObjectSummary can't be serialized
case class ObjectSummary(key: String, size: Long)

private case class SummaryGroup(totalSize: Long, summaries: List[ObjectSummary])

object ObjectSummary {
  def groupBySize(keys: Iterator[ObjectSummary], threshold: Long = 1L << 31): List[List[ObjectSummary]] = {
    keys.foldRight((0L, List[List[ObjectSummary]]()))(
      (x, acc) => {
        acc match {
          case (size, head :: tail) if size + x.size < threshold =>
            (size + x.size, (x :: head) :: tail)
          case (size, res) if size + x.size < threshold =>
            (size + x.size, List(x) :: res)
          case (_, res) =>
            (x.size, List(x) :: res)
        }
      })._2
  }
  def equallySizedGroups(keys: Iterator[ObjectSummary], minGroups: Int, threshold: Long = 1L << 31): List[List[ObjectSummary]] = {
    if (minGroups <= 1) {
      groupBySize(keys, threshold)
    } else {
      // We order by -group.totalSize so that dequeue returns the minimum element.
      val pq = mutable.PriorityQueue(List.fill(minGroups)(SummaryGroup(0, List.empty)) : _*)(Ordering.by(-_.totalSize))
      // Sort the objects first so that they pack more optimally.
      for (v <- keys.toList.sortBy(_.size).reverseIterator) {
        val best = pq.dequeue()
        if (best.totalSize + v.size > threshold) {
          pq.enqueue(best, SummaryGroup(v.size, List(v)))
        } else {
          pq.enqueue(SummaryGroup(best.totalSize + v.size, v :: best.summaries))
        }
      }
      // If we had fewer items in `keys` than minGroups, then we could have empty lists in the result
      pq.iterator.map(x => x.summaries).filter(_.nonEmpty).toList
    }
  }
}
