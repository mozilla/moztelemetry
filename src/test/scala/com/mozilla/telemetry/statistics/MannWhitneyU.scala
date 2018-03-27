/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.statistics

import org.scalatest.{FlatSpec, Matchers}
import com.mozilla.telemetry.{statistics => stats}


class StatisticsTest extends FlatSpec with Matchers {

  val epsilon = 0.000001

  "erf" can "be calculated" in {
    stats.erf(-3)  should equal (-0.9999779 +- epsilon)
    stats.erf(-1)  should equal (-0.8427008 +- epsilon)
    stats.erf(0)   should equal ( 0.0       +- epsilon)
    stats.erf(0.5) should equal ( 0.5204999 +- epsilon)
    stats.erf(2.1) should equal ( 0.9970205 +- epsilon)
  }

  "ndtr" can "be calculated" in {
    stats.ndtr(0) should equal (0.5      +- epsilon)
    stats.ndtr(1) should equal (0.841345 +- epsilon)
    stats.ndtr(2) should equal (0.977250 +- epsilon)
    stats.ndtr(3) should equal (0.998650 +- epsilon)
  }

  "tieCorrect" can "be calculated" in {
    stats.tieCorrect(Map(1L -> 1L)) should equal (1.0)
    stats.tieCorrect(Map(1L -> 48L)) should equal (0.0)
    stats.tieCorrect(
      Map(1L -> 10L, 2L -> 10L, 3L -> 10L)) should equal (0.889877 +- epsilon)
  }

  "rank" can "be calculated" in {
    stats.rank(Map(1L -> 1L)) should equal (Map(1L -> 1.0))
    stats.rank(
      Map(1L -> 5L, 2L -> 4L, 3L -> 3L, 4L -> 2L, 5L -> 1L)) should equal (
        Map(1L -> 3.0, 2L -> 7.5, 3L -> 11.0, 4L -> 13.5, 5L -> 15.0))
  }

  "mwu with normal distribution" can "be calculated" in {
    val norm1 = Map(0L -> 42L, 1L -> 60L, 2L -> 92L, 3L -> 100L, 4L -> 109L,
      5L -> 120L, 6L -> 128L, 7L -> 92L, 8L -> 76L, 9L -> 42L, 10L -> 50L,
      11L -> 11L, 12L -> 11L, 13L -> 1L, 14L -> 3L, 15L -> 3L, 16L -> 2L,
      -1L -> 35L, -6L -> 1L, -5L -> 1L, -4L -> 2L, -3L -> 4L, -2L -> 15L)
    val norm2 = Map(0L -> 6L, 1L -> 23L, 2L -> 39L, 3L -> 91L, 4L -> 119L,
      5L -> 142L, 6L -> 157L, 7L -> 139L, 8L -> 122L, 9L -> 80L, 10L -> 45L,
      11L -> 20L, 12L -> 10L, 13L -> 4L, 14L -> 1L, -1L -> 2L)
    var res = stats.mwu(norm1, norm2)
    res._1 should equal (381391.0)
    res._2 should equal (1.2950845524396078e-20 +- epsilon)
    res = stats.mwu(norm1, norm2, false)
    res._1 should equal (381391.0)
    res._2 should equal (1.2946137151181732e-20 +- epsilon)
  }

  "mwu with uniform distribution" can "be calculated" in {
    val uni1 = Map(1L -> 25L, 2L -> 23L, 3L -> 17L, 4L -> 25L, 5L -> 18L, 6L ->
      15L, 7L -> 18L, 8L -> 21L, 9L -> 20L, 10L -> 22L, 11L -> 25L, 12L -> 18L,
      13L -> 21L, 14L -> 17L, 15L -> 23L, 16L -> 16L, 17L -> 14L, 18L -> 20L,
      19L -> 18L, 20L -> 24L, 21L -> 19L, 22L -> 26L, 23L -> 18L, 24L -> 20L,
      25L -> 19L, 26L -> 23L, 27L -> 16L, 28L -> 24L, 29L -> 16L, 30L -> 21L,
      31L -> 13L, 32L -> 16L, 33L -> 22L, 34L -> 27L, 35L -> 22L, 36L -> 14L,
      37L -> 29L, 38L -> 22L, 39L -> 10L, 40L -> 23L, 41L -> 22L, 42L -> 26L,
      43L -> 19L, 44L -> 35L, 45L -> 17L, 46L -> 23L, 47L -> 18L, 48L -> 16L,
      49L -> 24L)
    val uni2 = Map(10L -> 15L, 11L -> 18L, 12L -> 17L, 13L -> 16L, 14L -> 18L,
      15L -> 23L, 16L -> 24L, 17L -> 13L, 18L -> 15L, 19L -> 12L, 20L -> 14L,
      21L -> 23L, 22L -> 14L, 23L -> 11L, 24L -> 15L, 25L -> 18L, 26L -> 22L,
      27L -> 15L, 28L -> 19L, 29L -> 14L, 30L -> 22L, 31L -> 26L, 32L -> 18L,
      33L -> 9L,  34L -> 25L, 35L -> 19L, 36L -> 25L, 37L -> 17L, 38L -> 14L,
      39L -> 23L, 40L -> 12L, 41L -> 10L, 42L -> 20L, 43L -> 25L, 44L -> 17L,
      45L -> 26L, 46L -> 20L, 47L -> 18L, 48L -> 23L, 49L -> 19L, 50L -> 16L,
      51L -> 17L, 52L -> 21L, 53L -> 20L, 54L -> 12L, 55L -> 14L, 56L -> 17L,
      57L -> 28L, 58L -> 16L, 59L -> 15L)
    var res = stats.mwu(uni1, uni2)
    res._1 should equal (290777.0)
    res._2 should equal (7.03102430020267e-41 +- epsilon)
    res = stats.mwu(uni1, uni2, false)
    res._1 should equal (290777.0)
    res._2 should equal (7.027076082562216e-41 +- epsilon)
  }

  "mwu with skewed distribution" can "be calculated" in {
    val skew1 = Map(0L -> 383L, 1L -> 465L, 2L -> 137L, 3L -> 14L, 4L -> 1L)
    val skew2 = Map(0L -> 362L, 1L -> 416L, 2L -> 107L, 3L -> 14L, 5L -> 1L)
    var res = stats.mwu(skew1, skew2)
    res._1 should equal (438324.5)
    res._2 should equal (0.14272273391953955 +- epsilon)
    res = stats.mwu(skew1, skew2, false)
    res._1 should equal (438324.5)
    res._2 should equal (0.14271241840387777 +- epsilon)
  }

  "mwu with same distributions" can "be calculated" in {
    val norm1 = Map(1L -> 48L)
    val norm2 = Map(1L -> 40L)
    var res = stats.mwu(norm1, norm2)
    res._1 should equal (0.0)
    res._2 should equal (1.0)
    res = stats.mwu(norm1, norm2, false)
    res._1 should equal (0.0)
    res._2 should equal (1.0)
  }
}
