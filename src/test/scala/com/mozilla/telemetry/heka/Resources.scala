/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.heka

import java.io.ByteArrayOutputStream

import com.google.protobuf._

object Resources {
  val message = RichMessage(
    "1234",
    Map(
      "bytes" -> ByteString.copyFromUtf8("foo"),
      "string" -> "foo",
      "bool" -> true,
      "double" -> 4.2,
      "integer" -> 42L,
      "submission" ->
        """
          | {
          |   "partiallyExtracted" : {
          |     "alpha" : "1",
          |     "beta" : "2"
          |   },
          |   "gamma": "3"
          | }
        """.stripMargin,
      "extracted.subfield" -> """{"delta": "4"}""",
      "extracted.nested.subfield"-> """{"epsilon": "5"}""",
      "partiallyExtracted.nested" -> """{"zeta": "6"}"""
  ),
    Some("payload")
  )

  val header = Header(message.toByteArray.length)

  private val framedMessage = {
    val baos = new ByteArrayOutputStream
    val bHeader = header.toByteArray
    val bMessage = message.toByteArray

    // see https://hekad.readthedocs.org/en/latest/message/index.html
    baos.write(0x1E)
    baos.write(bHeader.size)
    baos.write(bHeader, 0, bHeader.size)
    baos.write(0x1F)
    baos.write(bMessage, 0, bMessage.size)
    baos.toByteArray
  }

  def hekaFile(numRecords: Integer = 42, framedMessage: Array[Byte] = framedMessage): Array[Byte] = {
    val ba = new Array[Byte](numRecords*framedMessage.size)
    for (i <- 0 until numRecords) {
      System.arraycopy(framedMessage, 0, ba, i*framedMessage.size, framedMessage.size)
    }
    ba
  }
}
