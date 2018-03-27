/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry

import scala.collection.Map


package object statistics {

  def erf(x: Double): Double =  {
    /*
     * Returns error function at x.
     *
     * See: https://en.wikipedia.org/wiki/Error_function#Approximation_with_elementary_functions
     */

    // Constants.
    val a1 = 0.254829592
    val a2 = -0.284496736
    val a3 = 1.421413741
    val a4 = -1.453152027
    val a5 = 1.061405429
    val p = 0.3275911

    val sign = if (x < 0) -1 else 1
    val t = 1.0 / (1.0 + p * math.abs(x))
    sign * (1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * math.exp(-x * x))
  }

  def erfc(x: Double): Double = 1.0 - erf(x)

  def ndtr(a: Double): Double = {
    /*
     * Returns the area under the Gaussian probability density function,
     * integrated from minus infinity to x.
     *
     * See: https://docs.scipy.org/doc/scipy/reference/generated/scipy.special.ndtr.html#scipy.special.ndtr
     */
    val sqrth = math.sqrt(2) / 2
    val x = a * sqrth
    val z = math.abs(x)

    (x, z) match {
      case (_, b) if b < sqrth => 0.5 + 0.5 * erf(x)
      case (a, _) if a > 0 => 1.0 - 0.5 * erfc(z)
      case _ => 0.5 * erfc(z)
    }
  }

  def tieCorrect(sample: Map[Long, Long]): Double = {
    val n = sample.foldLeft[Long](0L)(_ + _._2)

    (n) match {
      case a if a < 2 => 1.0
      case _ => 1 - sample.foldLeft[Double](0.0)((sum, i) => {
        sum + math.pow(i._2, 3) - i._2
      }) / (math.pow(n, 3) - n)
    }
  }

  def rank(sample: Map[Long, Long]): Map[Long, Double] = {
    val sorted = scala.collection.immutable.SortedMap[Long, Long]() ++ sample

    sorted.foldLeft(1L, Map.empty[Long, Double]){
      case ((sum, ranks), (k, v)) => {
        (sum + v, ranks + (k -> (sum + (v.toDouble - 1) / 2)))
      }
    }._2
  }

  def mwu(sample1: Map[Long, Long], sample2: Map[Long, Long],
          useContinuity: Boolean = true): (Double, Double) = {
    // Merge maps, adding values if keys match.
    val sample = sample1 ++ sample2.map {
      case (k, v) => {
        k -> (v + sample1.getOrElse(k, 0L))
      }
    }

    val ranks = rank(sample)
    val sumOfRanks = sample1.foldLeft[Double](0.0) {
      (sum, i) => sum + (i._2 * ranks(i._1))
    }
    val n1 = sample1.foldLeft[Long](0L)(_ + _._2)
    val n2 = sample2.foldLeft[Long](0L)(_ + _._2)

    // Calculate Mann-Whitney U for both samples.
    val u1 = sumOfRanks - (n1 * (n1 + 1)) / 2
    val u2 = n1 * n2 - u1

    val tc = tieCorrect(sample)
    (tc) match {
      case a if a == 0 => 0.0 -> 1.0
      case _ => {
        val sd = math.sqrt(tc * n1 * n2 * (n1 + n2 + 1) / 12.0)
        val meanRank = n1 * n2 / 2.0 + 0.5 * (if (useContinuity) 1 else 0)
        val z = math.abs((math.max(u1, u2) - meanRank) / sd)
        math.min(u1, u2) -> ndtr(-z)
      }
    }
  }

}
