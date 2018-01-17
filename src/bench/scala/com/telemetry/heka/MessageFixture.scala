package com.mozilla.telemetry.heka

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

  def message = generateMessage(extract = false)

  def extractedMessage = generateMessage(extract = true)

  def extractedSimpleMessage = {
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
        "extracted.nested.subfield" -> """{"epsilon": "5"}""",
        "partiallyExtracted.nested" -> """{"zeta": "6"}"""
      ),
      None
    )
  }

  def simpleMessage = {
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
    MessageFixture.message.fieldsAsMap.keys should contain("submission")
  }

  it should "contain the same values" in {
    def removeMeta(json: JValue): JValue = {
      json removeField {
        case JField("meta", _) => true
        case _ => false
      }
    }

    val original = removeMeta(MessageFixture.message.toJValue)
    val extracted = removeMeta(MessageFixture.extractedMessage.toJValue)

    val Diff(changed, _, _) = original diff extracted
    changed should be(JNothing)
  }

  it should "fill a list with separate instances of RichMessages" in {
    val instantiatedMessage = MessageFixture.simpleMessage
    val referencedMessages = List.fill(2)(instantiatedMessage)

    // instantiated copies contain the same object, but fieldsAsMap are separate
    assert(referencedMessages(0) eq referencedMessages(1))
    assert(referencedMessages(0).fieldsAsMap ne referencedMessages(1).fieldsAsMap)

    // check that each message is different too
    val list = List.fill(2)(MessageFixture.simpleMessage)
    assert(list(0) ne list(1))
    assert(list(0).toJValue ne list(1).toJValue)
  }
}