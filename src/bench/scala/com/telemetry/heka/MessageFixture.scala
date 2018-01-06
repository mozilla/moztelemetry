package com.mozilla.telemetry.heka

import com.google.protobuf.ByteString
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{FlatSpec, Matchers}

object MessageFixture {
  val extractPayloadKeys = Set(
    "addonDetails",
    "addonHistograms",
    "childPayloads",
    "chromeHangs",
    "fileIOReports",
    "histograms",
    "info",
    "keyedHistograms",
    "lateWrites",
    "log",
    "simpleMeasurements",
    "slowSQL",
    "slowSQLstartup",
    "threadHangStats",
    "UIMeasurements",
    "gc"
  )

  val extractEnvironmentKeys = Set(
    "addons",
    "build",
    "experiments",
    "partner",
    "profile",
    "settings",
    "system"
  )

  private def generateMessage(extract: Boolean = false): RichMessage = {
    val jsonResource = getClass.getResourceAsStream("/test_telemetry_snappy.heka.json")
    val json = parse(jsonResource)

    val payload = if (extract) {
      // create a tuple (namespace, key)
      val keysToExtract =
        extractPayloadKeys.map(k => ("payload", k)) ++ extractEnvironmentKeys.map(k => ("environment", k))

      val extracted =
        keysToExtract
          .map { case (ns, k) => render(s"$ns.$k" -> compact(json \\ ns \\ k)) }
          .reduce(_ merge _)

      // note: if there are multiple keys globally, this will remove all instances of them
      val filteredJson = render(
        "submission" -> compact(
          keysToExtract.foldLeft(json) {
            case (json, (_, k)) => json removeField {
              case JField(k, _) => true
              case _ => false
            }
          })
      )
      render(filteredJson merge extracted)
    }
    else {
      render(Map("submission" -> compact(json)))
    }
    val keys = payload.values.asInstanceOf[Map[String, Any]].keys
    val formattedPayload = keys.map(k => (k, compact(payload \ k))).toMap
    RichMessage("uuid", formattedPayload, None)
  }

  val message = generateMessage(extract = false)
  val extractedMessage = generateMessage(extract = true)

  val extractedSimpleMessage = {
    RichMessage(
      "simpleMessage",
      Map(
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
      None
    )
  }
  val simpleMessage = {
    RichMessage(
      "simpleMessage",
      Map(
        "submission" ->
          """
            | {
            |   "partiallyExtracted" : {
            |     "alpha" : "1",
            |     "beta" : "2",
            |     "zeta" : "6"
            |   },
            |   "gamma": "3",
            |   "extracted": {
            |     "subfield" : {
            |       "delta": "4"
            |     },
            |     "nested": {
            |       "subfield": {
            |         "epsilon": "5"
            |        }
            |     }
            |   }
            | }
          """.stripMargin
      ),
      None
    )
  }
}


class MessageFixtureTest extends FlatSpec with Matchers {
  "MessageFixture" should "contain a submission field" in {
    MessageFixture.message.fieldsAsMap.keys should contain ("submission")
  }

  it should "contain the same values" in {
    def removeMeta(json: JValue): JValue = {
      json removeField {
        case JField("meta", _) => true
        case _ => false
      }
    }

    val original = removeMeta(parse(MessageFixture.message.asJson))
    val extracted = removeMeta(parse(MessageFixture.extractedMessage.asJson))

    val Diff(changed, _, _) = original diff extracted
    changed should be (JNothing)
  }
}