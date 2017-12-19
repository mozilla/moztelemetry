/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.stats

import org.scalatest.{FlatSpec, Matchers}
import com.mozilla.telemetry.stats


class StatsTest extends FlatSpec with Matchers {

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
    stats.tieCorrect(Map(1 -> 1)) should equal (1.0)
    stats.tieCorrect(Map(1 -> 48)) should equal (0.0)
    stats.tieCorrect(
      Map(1 -> 10, 2 -> 10, 3 -> 10)) should equal (0.889877 +- epsilon)
  }

  "rank" can "be calculated" in {
    stats.rank(Map(1 -> 1)) should equal (Map(1 -> 1.0))
    stats.rank(
      Map(1 -> 5, 2 -> 4, 3 -> 3, 4 -> 2, 5 -> 1)) should equal (
        Map(1 -> 3.0, 2 -> 7.5, 3 -> 11.0, 4 -> 13.5, 5 -> 15.0))
  }

  "mwu with normal distribution" can "be calculated" in {
    val norm1 = Map(0 -> 42, 1 -> 60, 2 -> 92, 3 -> 100, 4 -> 109, 5 -> 120, 6 -> 128, 7 -> 92, 8 -> 76, 9 -> 42, 10 -> 50, 11 -> 11, 12 -> 11, 13 -> 1, 14 -> 3, 15 -> 3, 16 -> 2, -1 -> 35, -6 -> 1, -5 -> 1, -4 -> 2, -3 -> 4, -2 -> 15)
    val norm2 = Map(0 -> 6, 1 -> 23, 2 -> 39, 3 -> 91, 4 -> 119, 5 -> 142, 6 -> 157, 7 -> 139, 8 -> 122, 9 -> 80, 10 -> 45, 11 -> 20, 12 -> 10, 13 -> 4, 14 -> 1, -1 -> 2)
    var res = stats.mwu(norm1, norm2)
    res._1 should equal (381391.0)
    res._2 should equal (1.2950845524396078e-20 +- epsilon)
    res = stats.mwu(norm1, norm2, false)
    res._1 should equal (381391.0)
    res._2 should equal (1.2946137151181732e-20 +- epsilon)
  }

  "mwu with uniform distribution" can "be calculated" in {
    val uni1 = Map(1 -> 25, 2 -> 23, 3 -> 17, 4 -> 25, 5 -> 18, 6 -> 15, 7 -> 18, 8 -> 21, 9 -> 20, 10 -> 22, 11 -> 25, 12 -> 18, 13 -> 21, 14 -> 17, 15 -> 23, 16 -> 16, 17 -> 14, 18 -> 20, 19 -> 18, 20 -> 24, 21 -> 19, 22 -> 26, 23 -> 18, 24 -> 20, 25 -> 19, 26 -> 23, 27 -> 16, 28 -> 24, 29 -> 16, 30 -> 21, 31 -> 13, 32 -> 16, 33 -> 22, 34 -> 27, 35 -> 22, 36 -> 14, 37 -> 29, 38 -> 22, 39 -> 10, 40 -> 23, 41 -> 22, 42 -> 26, 43 -> 19, 44 -> 35, 45 -> 17, 46 -> 23, 47 -> 18, 48 -> 16, 49 -> 24)
    val uni2 = Map(10 -> 15, 11 -> 18, 12 -> 17, 13 -> 16, 14 -> 18, 15 -> 23, 16 -> 24, 17 -> 13, 18 -> 15, 19 -> 12, 20 -> 14, 21 -> 23, 22 -> 14, 23 -> 11, 24 -> 15, 25 -> 18, 26 -> 22, 27 -> 15, 28 -> 19, 29 -> 14, 30 -> 22, 31 -> 26, 32 -> 18, 33 -> 9, 34 -> 25, 35 -> 19, 36 -> 25, 37 -> 17, 38 -> 14, 39 -> 23, 40 -> 12, 41 -> 10, 42 -> 20, 43 -> 25, 44 -> 17, 45 -> 26, 46 -> 20, 47 -> 18, 48 -> 23, 49 -> 19, 50 -> 16, 51 -> 17, 52 -> 21, 53 -> 20, 54 -> 12, 55 -> 14, 56 -> 17, 57 -> 28, 58 -> 16, 59 -> 15)
    var res = stats.mwu(uni1, uni2)
    res._1 should equal (290777.0)
    res._2 should equal (7.03102430020267e-41 +- epsilon)
    res = stats.mwu(uni1, uni2, false)
    res._1 should equal (290777.0)
    res._2 should equal (7.027076082562216e-41 +- epsilon)
  }

  "mwu with skewed distribution" can "be calculated" in {
    val skew1 = Map(0 -> 383, 1 -> 465, 2 -> 137, 3 -> 14, 4 -> 1)
    val skew2 = Map(0 -> 362, 1 -> 416, 2 -> 107, 3 -> 14, 5 -> 1)
    var res = stats.mwu(skew1, skew2)
    res._1 should equal (438324.5)
    res._2 should equal (0.14272273391953955 +- epsilon)
    res = stats.mwu(skew1, skew2, false)
    res._1 should equal (438324.5)
    res._2 should equal (0.14271241840387777 +- epsilon)
  }

  "mwu with same distributions" can "be calculated" in {
    val norm1 = Map(1 -> 48)
    val norm2 = Map(1 -> 40)
    var res = stats.mwu(norm1, norm2)
    res._1 should equal (0.0)
    res._2 should equal (1.0)
    res = stats.mwu(norm1, norm2, false)
    res._1 should equal (0.0)
    res._2 should equal (1.0)
  }
}
