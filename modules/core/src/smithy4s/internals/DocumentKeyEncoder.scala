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

import smithy.api.TimestampFormat
import smithy.api.TimestampFormat._
import smithy4s._
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Primitive._
import smithy4s.schema.Schema
import smithy4s.schema.SchemaVisitor

import java.util.Base64

trait DocumentKeyEncoder[A] { self =>
  def apply(a: A): String

  def contramap[B](f: B => A): DocumentKeyEncoder[B] =
    new DocumentKeyEncoder[B] {
      def apply(b: B): String = self(f(b))
    }
}

object DocumentKeyEncoder {
  type OptDocumentKeyEncoder[A] = Option[DocumentKeyEncoder[A]]
  val trySchemaVisitor: SchemaVisitor[OptDocumentKeyEncoder] =
    new SchemaVisitor.Default[OptDocumentKeyEncoder] {
      private def instance[A](f: A => String): OptDocumentKeyEncoder[A] = Some {
        a =>
          a.toString()
      }
      private def forBigDecimal[A](
          f: A => BigDecimal
      ): OptDocumentKeyEncoder[A] =
        Some { a =>
          a.toString()
        }
      private def asString[A]: OptDocumentKeyEncoder[A] = instance {
        _.toString()
      }
      def default[A]: OptDocumentKeyEncoder[A] = None

      override def primitive[P](
          shapeId: ShapeId,
          hints: Hints,
          tag: Primitive[P]
      ): OptDocumentKeyEncoder[P] = {
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
      ): OptDocumentKeyEncoder[E] = Some { a => total(a).stringValue }

      override def biject[A, B](
          schema: Schema[A],
          bijection: Bijection[A, B]
      ): OptDocumentKeyEncoder[B] =
        apply(schema).map(_.contramap(bijection.from))

      override def refine[A, B](
          schema: Schema[A],
          refinement: Refinement[A, B]
      ): OptDocumentKeyEncoder[B] =
        apply(schema).map(_.contramap(refinement.from))
    }
}
