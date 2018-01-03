/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.heka

import org.scalatest.{FlatSpec, Matchers}
import org.json4s._
import org.json4s.jackson.JsonMethods.parse

class MessageTest extends FlatSpec with Matchers {
  "Fields" can "be parsed" in {
    val fields = Resources.message.fieldsAsMap
    fields("bytes").asInstanceOf[String] should be ("foo")
    fields("string").asInstanceOf[String] should be ("foo")
    fields("bool").asInstanceOf[Boolean] should be (true)
    fields("double").asInstanceOf[Double] should be (4.2)
    fields("integer").asInstanceOf[Long] should be (42)
  }

  "Message" can "be represented as valid JSON" in {
    val json = Resources.message.asJson
    val doc = parse(json)

    // check that the field has been casted correctly
    doc \\ "meta" \\ "integer" should be (JInt(42))

    doc \\ "gamma" should be (JString("3"))

    // single level of unnesting
    doc \\ "subfield" \\ "delta" should be (JString("4"))

    // multiple level of unnnesting
    doc \\ "nested" \\ "subfield" \\ "epsilon" should be (JString("5"))

    // count the number of keys in partiallyExtracted
    (doc \\ "partiallyExtracted").values.asInstanceOf[Map[String, Any]].keys.size should be (3)

    // partial extraction
    doc \\ "partiallyExtracted" \\ "nested" \\ "zeta" should be (JString("6"))
  }

  it can "handle documents without extracted fields" in {
    val message = RichMessage("uuid", Resources.message.fieldsAsMap.filterKeys(k => !k.contains(".")), None)
    parse(message.asJson)
  }
}
