/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry.pings.main

import scala.io.Source
import org.yaml.snakeyaml.Yaml
import collection.JavaConversions._
import java.util

class ProcessesClass {
  protected case class Process(name: String, geckoEnum: Option[String], description: Option[String])
  protected val getURL: (String, String) => scala.io.Source = Source.fromURL

  protected lazy val parsedProcesses: Map[String, Seq[Process]] = {
    val uris = Map("release" -> "https://hg.mozilla.org/releases/mozilla-release/raw-file/tip/toolkit/components/telemetry/Processes.yaml",
      "beta" -> "https://hg.mozilla.org/releases/mozilla-beta/raw-file/tip/toolkit/components/telemetry/Processes.yaml",
      "nightly" -> "https://hg.mozilla.org/mozilla-central/raw-file/tip/toolkit/components/telemetry/Processes.yaml")
    val sources = uris.map { case (key, value) => key -> getURL(value, "UTF8") }
    sources.map { case (key, source) => {
      val parsed = (new Yaml().load(source.mkString)).asInstanceOf[util.LinkedHashMap[String, util.LinkedHashMap[String, String]]]
      val processes = parsed
        .map {case (name, paramsJavaMap) => {
          val params = mapAsScalaMap(paramsJavaMap)
          Process(name, params.get("gecko_enum"), params.get("description"))
        }}
        .toSeq
      key -> processes
    }
    }
  }

  lazy val names: Seq[String] =
    parsedProcesses.flatMap { case (_, processes) =>
      processes.map(_.name)
    }.toSeq.distinct
}

object Processes extends ProcessesClass
