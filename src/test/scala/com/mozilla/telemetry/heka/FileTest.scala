/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.heka

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import org.scalatest.{FlatSpec, Matchers}
import org.xerial.snappy.Snappy

private class CloseableByteArrayInputStream(buf: Array[Byte]) extends ByteArrayInputStream(buf) {
  var isClosed = false

  override def close: Unit = {
    isClosed = true
    super.close()
  }
}

class FileTest extends FlatSpec with Matchers {
  "A Heka file" can "be parsed" in {
    val hekaFile = Resources.hekaFile(42)
    val messages = File.parse(new ByteArrayInputStream(hekaFile)).toList
    messages.size should be (42)
  }

  it should "contain properly framed messages" in {
    val emptyFile = new ByteArrayInputStream("foo".getBytes)
    assertThrows[Exception] {
      File.parse(emptyFile).toList
    }

    val fileWithMissingUnitSeparator = {
      val baos = new ByteArrayOutputStream
      val header = Resources.header.toByteArray

      // see https://hekad.readthedocs.org/en/latest/message/index.html
      baos.write(0x1E)
      baos.write(header.size)
      baos.write(header, 0, header.size)
      baos.toByteArray
    }
    assertThrows[Exception] {
      File.parse(new ByteArrayInputStream(fileWithMissingUnitSeparator)).toList
    }
  }

  it can "contain Snappy compressed messages" in {
    val framedMessage = {
      val baos = new ByteArrayOutputStream
      val message = Resources.message.toByteArray
      val compressedMessage = Snappy.compress(message)
      val header = Header(compressedMessage.length).toByteArray

      // see https://hekad.readthedocs.org/en/latest/message/index.html
      baos.write(0x1E)
      baos.write(header.size)
      baos.write(header, 0, header.size)
      baos.write(0x1F)
      baos.write(compressedMessage, 0, compressedMessage.size)
      baos.toByteArray
    }

    val compressedHekaFile = Resources.hekaFile(42, framedMessage)
    File.parse(new ByteArrayInputStream(compressedHekaFile)).size should be (42)
  }

  "Parsing" should "be retried in case of failure" in {
    var count = 0
    def wackyStream: InputStream = {
      count += 1
      if (count == 3) {
        new ByteArrayInputStream(Resources.hekaFile(42))
      } else {
        new ByteArrayInputStream(Resources.hekaFile(21) ++ "foo".getBytes)
      }
    }

    File.parse(wackyStream).size should be (42)
  }

  it should "fail if skipping fails" in {
    var count = 0
    def wackyStream: InputStream = {
      count += 1
      if (count == 3) {
        new ByteArrayInputStream(Resources.hekaFile(20))
      } else {
        new ByteArrayInputStream(Resources.hekaFile(21) ++ "foo".getBytes)
      }
    }

    assertThrows[Exception] {
      File.parse(wackyStream).size
    }
  }

  it should "close streams" in {
    val hekaFile = new CloseableByteArrayInputStream(Resources.hekaFile(42))
    File.parse(hekaFile).size
    hekaFile.isClosed should be (true)
  }
}
