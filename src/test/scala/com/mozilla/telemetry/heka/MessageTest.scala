/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.heka

import org.scalatest.{FlatSpec, Matchers}

class MessageTest extends FlatSpec with Matchers {
  "Fields" can "be parsed" in {
    val fields = Resources.message.fieldsAsMap
    fields("bytes").asInstanceOf[String] should be ("foo")
    fields("string").asInstanceOf[String] should be ("foo")
    fields("bool").asInstanceOf[Boolean] should be (true)
    fields("double").asInstanceOf[Double] should be (4.2)
    fields("integer").asInstanceOf[Long] should be (42)
  }
}
