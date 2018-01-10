/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry

import com.google.protobuf.ByteString
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import scala.util.Try

package object heka {
  class RichMessage(m: Message) {

    /** Deserialize the Heka message as a native Scala Map. */
    def fieldsAsMap: Map[String, Any] = {
      val fields = m.fields
      Map(fields.map(_.name).zip(fields.map(field)): _*)
    }

    /** Reconstructs a Heka message as a json4s JValue.
      *
      * The message is parsed and serialized into JSON. Keys that have been extracted
      * into the toplevel document are inserted into to the proper place in the document.
      *
      * @return reconstructed JValue
      */
    def asJson: JValue = {
      val fields = Map(m.fields.map(_.name).zip(m.fields.map(fieldAsJValue)): _*)

      lazy val submission = fields.getOrElse("submission", JNothing)
      val payload = m.payload match {
        case Some(payload: String) => {
          val json = Try(parse(payload)).getOrElse(JNothing)
          json match {
            case JObject(x) => json
            case _ => submission
          }
        }
        case _ => submission
      }

      val (extractedKeys, metaKeys) =
        fields
          .keys
          .filter(k => k != "submission")
          .partition(k => k.contains("."))

      val initialMetaFields = render(
        Map(
          "Timestamp" -> m.timestamp
        )
      )
      val metaFields = initialMetaFields merge render(fields.filterKeys(metaKeys.toSet))

      val payloadFields =
        fields
          .filterKeys(extractedKeys.toSet)
          .map { case (k, v) => applyNestedField(k.split("\\."), v) }
          .foldLeft(JNothing.asInstanceOf[JValue]) ((acc, field) =>  acc merge field)

      payload merge payloadFields merge render(("meta" -> metaFields))
    }
  }

  private def field(f: Field): Any = {
    // I am assuming there is only one value
    f.getValueType match {
      case Field.ValueTypeEnum.BYTES => {
        val bytes = f.valueBytes(0)
        // Our JSON bytes fields sometimes contain non-UTF8 strings that can
        // still be parsed as JSON. For now, we attempt to coerce all bytes
        // fields to UTF8 strings, as we only have JSON bytes fields. See
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1339421
        // for details.
        bytes.toStringUtf8
      }
      case Field.ValueTypeEnum.STRING => f.valueString(0)
      case Field.ValueTypeEnum.BOOL => f.valueBool(0)
      case Field.ValueTypeEnum.DOUBLE => f.valueDouble(0)
      case Field.ValueTypeEnum.INTEGER => f.valueInteger(0)
      case _ => assert(false)
    }
  }

  private def applyNestedField(keys: Seq[String], value: JValue): JValue = {
    def helper(keys: Seq[String], value: JValue): JValue = {
      if(keys.isEmpty) { value }
      else { helper(keys.tail, render(Map(keys.head -> value))) }
    }
    // this should be applied from right to left e.g foo.bar.baz -> {key: value}
    helper(keys.reverse, value)
  }

  private def fieldAsJValue(f: Field): JValue = {
    f.getValueType match {
      case Field.ValueTypeEnum.BYTES => {
        val bytes = f.valueBytes(0).toStringUtf8
        Try(parse(bytes)).getOrElse(bytes)
      }
      case Field.ValueTypeEnum.STRING => {
        val json = Try(parse(f.valueString(0))).getOrElse(JNothing)
        json match {
          case JObject(x) => json
          case _ => JString(f.valueString(0))
        }
      }
      case Field.ValueTypeEnum.BOOL => JBool(f.valueBool(0))
      case Field.ValueTypeEnum.DOUBLE => JDouble(f.valueDouble(0))
      case Field.ValueTypeEnum.INTEGER => JInt(f.valueInteger(0))
      case _ => JNull
    }
  }

  implicit def messageToRichMessage(m: Message): RichMessage = new RichMessage(m)

  object RichMessage {
    def apply (uuid: String, fieldsMap: Map[String, Any], payload: Option[String], timestamp: Long=0): Message = {
      val fields = fieldsMap.toList.map{
        case (k: String, v: ByteString) => {
          Field(k, Some(Field.ValueTypeEnum.BYTES), valueBytes=Seq(v))
        }
        case (k: String, v: String) => {
          Field(k, Some(Field.ValueTypeEnum.STRING), valueString=Seq(v))
        }
        case (k: String, v: Boolean) => {
          Field(k, Some(Field.ValueTypeEnum.BOOL), valueBool=Seq(v))
        }
        case (k: String, v: Double) => {
          Field(k, Some(Field.ValueTypeEnum.DOUBLE), valueDouble=Seq(v))
        }
        case (k: String, v: Long) => {
          Field(k, Some(Field.ValueTypeEnum.INTEGER), valueInteger=Seq(v))
        }
      }.toSeq
      Message(ByteString.copyFromUtf8(uuid), timestamp, payload=payload, fields=fields)
    }
  }
}
