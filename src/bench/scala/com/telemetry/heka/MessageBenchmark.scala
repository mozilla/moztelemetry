package com.mozilla.telemetry.heka

import org.scalameter.api._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalameter.picklers.noPickler._

object MessageBenchmark extends Bench.LocalTime {
  override def persistor = new SerializationPersistor

  val messages = Map[String, RichMessage](
    "simple" -> MessageFixture.simpleMessage,
    "simple extracted" -> MessageFixture.extractedSimpleMessage,
    "telemetry" -> MessageFixture.message,
    "telemetry extracted" -> MessageFixture.extractedMessage
  )

  performance of "Message" in {
    val input = for {
      size <- Gen.exponential("size")(10, 1000, 10)
      messageName <- Gen.enumeration[String]("message")(messages.keys.toList :_*)
    } yield List.fill(size)(messages(messageName))


    measure method "fieldsAsMap" in {
      using(input) in { m => m.foreach(_.fieldsAsMap) }
    }
    measure method "asJson" in {
      using(input) in { m => m.foreach(_.asJson) }
    }
  }

  val sizes = Gen.exponential("size")(10, 1000, 10)
  def generateMessages(messageName: String, sizes: Gen[Int]): Gen[List[RichMessage]] = {
    for { size <- sizes } yield List.fill(size)(messages(messageName))
  }

  performance of "simple extracted" in {
    val input = generateMessages("simple extracted", sizes)

    measure method "fieldsAsMap" in {
      using(input) in {
        m => m.foreach(m => {
          val payload = m.fieldsAsMap
          val submission = parse(payload("submission").asInstanceOf[String])
          (
            submission \\ "gamma",
            submission \\ "partiallyExtracted" \\ "alpha",
            parse(payload("partiallyExtracted.nested").asInstanceOf[String]) \\ "zeta",
            parse(payload("extracted.subfield").asInstanceOf[String]) \\ "delta"
          )
        })
      }
    }
    measure method "asJson" in {
      using(input) in {
        m => m.foreach(m => {
          val submission = m.asJson
          (
            submission \\ "gamma",
            submission \\ "partiallyExtracted" \\ "alpha",
            submission \\ "partiallyExtracted" \\ "nested" \\ "zeta",
            submission \\ "extracted" \\ "subfield" \\ "delta"
          )
        })
      }
    }
  }

  performance of "telemetry extracted" in {
    val input = generateMessages("telemetry extracted", sizes)

    measure method "fieldsAsMap" in {
      using(input) in {
        m =>
          m.foreach(m => {
            val fields = m.fieldsAsMap

            lazy val addons = parse(fields.getOrElse("environment.addons", "{}").asInstanceOf[String])
            lazy val payload = parse(fields.getOrElse("submission", "{}").asInstanceOf[String])
            lazy val application = payload \ "application"
            lazy val build = parse(fields.getOrElse("environment.build", "{}").asInstanceOf[String])
            lazy val info = parse(fields.getOrElse("payload.info", "{}").asInstanceOf[String])
            lazy val partner = parse(fields.getOrElse("environment.partner", "{}").asInstanceOf[String])
            lazy val profile = parse(fields.getOrElse("environment.profile", "{}").asInstanceOf[String])
            lazy val settings = parse(fields.getOrElse("environment.settings", "{}").asInstanceOf[String])
            lazy val system = parse(fields.getOrElse("environment.system", "{}").asInstanceOf[String])

            // fields chosen arbitrarily to access the above fields
            (
              addons \\ "activeExperiment" \\ "id",
              application \\ "buildId",
              build \\ "buildId",
              info \\ "subsessionCounter",
              partner \\ "distributionId",
              payload \\ "creationDate",
              profile \\ "creationDate",
              settings \\ "locale",
              system \\ "os" \\ "name"
            )

          })
      }
    }

    measure method "asJson" in {
      using(input) in {
        m =>
          m.foreach(m => {
            val submission = m.asJson
            (
              submission \\ "environment" \\ "addons" \\ "activeExperiment" \\ "id",
              submission \\ "application" \\ "buildId",
              submission \\ "environment" \\ "build" \\ "buildId",
              submission \\ "payload" \\ "info" \\ "subsessionCounter",
              submission \\ "environment" \\ "partner" \\ "distributionId",
              submission \\ "creationDate",
              submission \\ "environment" \\ "profile" \\ "creationDate",
              submission \\ "environment" \\ "settings" \\ "locale",
              submission \\ "environment" \\ "system" \\ "os" \\ "name"
            )
          })
      }
    }
  }
}
