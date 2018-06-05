/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.pings.main

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class ProcessesTest extends FlatSpec with Matchers {
  val processClass = new ProcessesClass {
    override protected val getURL = (a: String, b: String) => Source.fromString(
      """
        |hello:
        |  gecko_enum: GeckoProcessType_Default
        |  description: This is a test process called 'hello'
        |world:
        |  description: This is a test process called 'world' without a gecko_enum field
      """.stripMargin)
  }

  "Process names" should "be returned as a seq" in {
    processClass.names should be(Seq("hello", "world"))
  }
}
