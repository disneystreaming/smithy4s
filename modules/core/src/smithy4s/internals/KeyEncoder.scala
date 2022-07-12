/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.internals

import java.util.Base64

import smithy.api.TimestampFormat
import smithy.api.TimestampFormat._
import smithy4s._
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Primitive._
import smithy4s.schema.SchemaVisitor

trait KeyEncoder[A] {
  def apply(a: A): String
}

object KeyEncoder {
  type OptKeyEncoder[A] = Option[KeyEncoder[A]]
  val trySchemaVisitor: SchemaVisitor[OptKeyEncoder] =
    new SchemaVisitor.Default[OptKeyEncoder] {
      private def instance[A](f: A => String): OptKeyEncoder[A] = Some { a =>
        a.toString()
      }
      private def forBigDecimal[A](f: A => BigDecimal): OptKeyEncoder[A] =
        Some { a =>
          a.toString()
        }
      private def asString[A]: OptKeyEncoder[A] = instance { _.toString() }
      def default[A]: OptKeyEncoder[A] = None

      override def primitive[P](
          shapeId: ShapeId,
          hints: Hints,
          tag: Primitive[P]
      ): OptKeyEncoder[P] = {
        tag match {
          case PBoolean    => asString
          case PBigDecimal => asString
          case PUUID       => asString
          case PString     => asString
          case PShort      => forBigDecimal { a => BigDecimal(a.toInt) }
          case PBigInt     => forBigDecimal { BigDecimal(_) }
          case PInt        => forBigDecimal { BigDecimal(_) }
          case PDouble     => forBigDecimal { BigDecimal(_) }
          case PLong       => forBigDecimal { BigDecimal(_) }
          case PByte       => forBigDecimal { a => BigDecimal(a.toInt) }
          case PFloat      => forBigDecimal { a => BigDecimal(a.toDouble) }
          case PBlob =>
            instance(bytes => Base64.getEncoder().encodeToString(bytes.array))
          case PUnit => None
          case PTimestamp =>
            hints
              .get(TimestampFormat)
              .getOrElse(DATE_TIME) match {
              case DATE_TIME =>
                instance { ts => ts.format(DATE_TIME) }
              case HTTP_DATE =>
                instance { ts => ts.format(HTTP_DATE) }
              case EPOCH_SECONDS =>
                forBigDecimal { ts => BigDecimal(ts.epochSecond) }
            }
          case PDocument => None
        }
      }
      override def enumeration[E](
          shapeId: ShapeId,
          hints: Hints,
          values: List[EnumValue[E]],
          total: E => EnumValue[E]
      ): OptKeyEncoder[E] = Some { a => total(a).stringValue }
    }
}
