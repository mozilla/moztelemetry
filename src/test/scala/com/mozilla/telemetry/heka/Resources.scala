/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry.heka

import java.io.ByteArrayOutputStream

import com.google.protobuf._

object Resources {
  val message = {
    val fields = List(
      Field("bytes", Some(Field.ValueType.BYTES), valueBytes = Seq(ByteString.copyFromUtf8("foo"))),
      Field("string", Some(Field.ValueType.STRING), valueString = Seq("foo")),
      Field("bool", Some(Field.ValueType.BOOL), valueBool = Seq(true)),
      Field("double", Some(Field.ValueType.DOUBLE), valueDouble = Seq(4.2)),
      Field("integer", Some(Field.ValueType.INTEGER), valueInteger = Seq(42)))

    Message(ByteString.copyFromUtf8("1234"), 0, payload = Some("payload"), fields = fields)
  }

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
