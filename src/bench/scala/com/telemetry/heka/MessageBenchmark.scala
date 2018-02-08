package com.mozilla.telemetry.heka

import org.scalameter.api._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalameter.picklers.noPickler._


object MessageBenchmarkFixture {
  // separate generation of fixtures from access of fixtures via this fixture
  val messages = Map[String, (Int) => RichMessage](
    "simple" -> MessageFixture.simpleMessage,
    "simple extracted" -> MessageFixture.extractedSimpleMessage
  )

  def generateMessages(messageNames: Gen[String], sizes: Gen[Int]): Gen[RichMessage] =
    for {
      messageName <- messageNames
      size <- sizes
    } yield messages(messageName)(size)
}

class MessageBenchmark extends Bench.ForkedTime {

  import MessageBenchmarkFixture._

  override def persistor = new SerializationPersistor

  val sizes = Gen.exponential("size")(1, 1000, 10)

  performance of "deserialization speed" in {
    val messageNames = Gen.enumeration[String]("message")("simple", "simple extracted")
    val input = generateMessages(messageNames, sizes)

    measure method "fieldsAsMap" in {
      using(input) in {
        _.fieldsAsMap
      }
    }
    measure method "toJValue" in {
      using(input) in {
        _.toJValue
      }
    }
  }

  performance of "extraction speed" in {
    performance of "simple extracted" in {
      val input = generateMessages(Gen.single("message")("simple extracted"), sizes)

      measure method "fieldsAsMap" in {
        using(input) in {
          m => {
            val payload = m.fieldsAsMap
            val submission = parse(payload("submission").asInstanceOf[String])
            (
              submission \\ "gamma",
              submission \\ "partiallyExtracted" \\ "alpha",
              parse(payload("partiallyExtracted.nested").asInstanceOf[String]) \\ "zeta",
              parse(payload("extracted.subfield").asInstanceOf[String]) \\ "delta"
            )
          }
        }
      }
      measure method "toJValue" in {
        using(input) in {
          m => {
            val submission = m.toJValue
            (
              submission \\ "gamma",
              submission \\ "partiallyExtracted" \\ "alpha",
              submission \\ "partiallyExtracted" \\ "nested" \\ "zeta",
              submission \\ "extracted" \\ "subfield" \\ "delta"
            )
          }
        }
      }
    }

    performance of "telemetry extracted" in {
      val input = for {name <- Gen.single("message")("telemetry extracted")} yield MessageFixture.extractedMessage

      measure method "fieldsAsMap" in {
        using(input) in {
          m => {
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
          }
        }
      }

      measure method "toJValue" in {
        using(input) in {
          m => {
            val submission = m.toJValue
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
          }
        }
      }
    }
  }
}

class MessageMemoryBenchmark extends Bench.ForkedTime {

  import MessageBenchmarkFixture._

  override def persistor = new SerializationPersistor

  override def measurer = new Executor.Measurer.MemoryFootprint

  val sizes = Gen.exponential("size")(1, 1000, 10)

  performance of "extraction memory footprint" in {
    performance of "simple extracted" in {
      val input = generateMessages(Gen.single("message")("simple extracted"), sizes)

      measure method "fieldsAsMap" in {
        using(input) in {
          m => {
            val payload = m.fieldsAsMap

            // every field is a json blob
            Seq(payload) ++ payload.map { case (_, v: String) => parse(v) }
          }
        }
        measure method "toJValue" in {
          using(input) in {
            _.toJValue
          }
        }
      }

      performance of "telemetry extracted" in {
        val input = for { name <- Gen.single("message")("telemetry extracted") } yield MessageFixture.extractedMessage

        measure method "fieldsAsMap" in {
          using(input) in {
            m => {
              val fields = m.fieldsAsMap

              Seq(
                fields,
                parse(fields.getOrElse("environment.addons", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("submission", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("environment.build", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("payload.info", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("environment.partner", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("environment.profile", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("environment.settings", "{}").asInstanceOf[String]),
                parse(fields.getOrElse("environment.system", "{}").asInstanceOf[String])
              )
            }
          }
        }

        measure method "toJValue" in {
          using(input) in {
            _.toJValue
          }
        }
      }
    }
  }
}
