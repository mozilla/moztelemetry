/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.mozilla.telemetry

import com.google.protobuf.ByteString
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.language.implicitConversions
import scala.util.Try

package object heka {
  class RichMessage(m: Message) {

    /** Deserialize the Heka message as a native Scala Map. */
    def fieldsAsMap: Map[String, Any] = {
      val fields = m.fields
      Map(fields.map(_.name).zip(fields.map(field)): _*)
    }

    /** Reconstructs a telemetry Heka message as a json4s JValue.
      *
      * The message is parsed and serialized into JSON. Keys that have been extracted
      * into the toplevel document are inserted into to the proper place in the document.
      *
      * This method will be None if there is an error parsing the payload or submission.
      *
      * @return wrapped reconstructed JValue
      */
    def toJValue: Option[JValue] = {
      val fields = m.fields.map(f => (f.name, fieldAsJValue(f))).toMap

      // Parse the document payload. All pings using v4 of the telemetry infrastructure will use
      // the "submission" field instead of the message payload. This kept for backwards compatibility
      // with tests and older telemetry data (est. cut-over is February 2017).
      val payload = m.payload match {
        case Some(payload: String) => Try(parse(payload)).toOption
        case _ => {
          fields.get("submission") match {
            case Some(submission) => submission match {
              // submission field must be a json object if it exists
              case JObject(_) => Some(submission)
              case _ => None
            }
            // continue parsing the message if there is no payload or
            // submission, effectively treating all fields as metadata
            case None => Some(JNothing)
          }
        }
      }

      val meta = ("Timestamp" -> m.timestamp) ~ ("Type" -> m.dtype) ~ ("Hostname" -> m.hostname)

      payload.map(doc => rebuildDocument(doc, meta, fields, Seq("submission")))
    }
  }

  /**
    * Reconstruct the original document structure based on namespacing delimited by periods.
    *
    * @param payload      document to merge extracted fields with
    * @param meta         initial set of meta fields to seed the structure with
    * @param fields       heka fields that have been deserialized as json values
    * @param skipFields   fields to skip during reconstruction
    * @return             reconstructed document
    */
  private def rebuildDocument(payload: JValue, meta: JValue, fields: Map[String, JValue], skipFields: Seq[String]): JValue = {
    val (extractedKeys, metaKeys) =
      fields
        .keys
        .filter(k => !skipFields.contains(k))
        .partition(k => k.contains("."))

    val metaFields = meta merge render(fields.filterKeys(metaKeys.toSet))

    val payloadFields =
      fields
        .filterKeys(extractedKeys.toSet)
        .map { case (k, v) => applyNestedField(k.split("\\."), v) }
        .foldLeft(JNothing.asInstanceOf[JValue]) (_ merge _)

    payload merge payloadFields merge render(("meta" -> metaFields))
  }

  private def applyNestedField(keys: Seq[String], value: JValue): JValue = {
    def helper(keys: Seq[String], value: JValue): JValue = {
      if(keys.isEmpty) { value }
      else { helper(keys.tail, render(Map(keys.head -> value))) }
    }
    // this should be applied from right to left e.g foo.bar.baz -> {key: value}
    helper(keys.reverse, value)
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

  private def fieldAsJValue(f: Field): JValue = {
    f.getValueType match {
      case Field.ValueTypeEnum.BYTES => {
        val string = f.valueBytes(0).toStringUtf8
        Try(parse(string)).getOrElse(JString(string))
      }
      case Field.ValueTypeEnum.STRING => {
        // Test whether the string is a valid json object. If it's not, then
        // return it as a string type to avoid implicit casting.
        val json = Try(parse(f.valueString(0))).getOrElse(JNothing)
        json match {
          case JObject(_) => json
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
    def apply (uuid: String, fieldsMap: Map[String, Any], payload: Option[String],
               timestamp: Long=0, dtype: Option[String]=None, hostname: Option[String]=None): Message = {
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
      Message(ByteString.copyFromUtf8(uuid), timestamp, payload=payload, fields=fields, dtype=dtype, hostname=hostname)
    }
  }
}
