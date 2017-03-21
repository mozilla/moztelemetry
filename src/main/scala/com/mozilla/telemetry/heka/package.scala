/*
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.mozilla.telemetry

import com.google.protobuf.ByteString

package object heka {
  class RichMessage(m: Message) {
    def fieldsAsMap: Map[String, Any] = {
      val fields = m.fields
      Map(fields.map(_.name).zip(fields.map(field)): _*)
    }
  }

  private def field(f: Field): Any = {
    // I am assuming there is only one value
    f.getValueType match {
      case Field.ValueType.BYTES => {
        val bytes = f.valueBytes(0)
        // Our JSON bytes fields sometimes contain non-UTF8 strings that can
        // still be parsed as JSON. For now, we attempt to coerce all bytes
        // fields to UTF8 strings, as we only have JSON bytes fields. See
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1339421
        // for details.
        bytes.toStringUtf8
      }
      case Field.ValueType.STRING => f.valueString(0)
      case Field.ValueType.BOOL => f.valueBool(0)
      case Field.ValueType.DOUBLE => f.valueDouble(0)
      case Field.ValueType.INTEGER => f.valueInteger(0)
      case _ => assert(false)
    }
  }

  implicit def messageToRichMessage(m: Message): RichMessage = new RichMessage(m)

  object RichMessage {
    def apply (uuid: String, fieldsMap: Map[String, Any], payload: Option[String]): Message = {
      val fields = fieldsMap.toList.map{
        case (k: String, v: ByteString) => {
          Field(k, Some(Field.ValueType.BYTES), valueBytes=Seq(v))
        }
        case (k: String, v: String) => {
          Field(k, Some(Field.ValueType.STRING), valueString=Seq(v))
        }
        case (k: String, v: Boolean) => {
          Field(k, Some(Field.ValueType.BOOL), valueBool=Seq(v))
        }
        case (k: String, v: Double) => {
          Field(k, Some(Field.ValueType.DOUBLE), valueDouble=Seq(v))
        }
        case (k: String, v: Long) => {
          Field(k, Some(Field.ValueType.INTEGER), valueInteger=Seq(v))
        }
      }.toSeq
      Message(ByteString.copyFromUtf8(uuid), 0, payload=payload, fields=fields)
    }
  }
}
