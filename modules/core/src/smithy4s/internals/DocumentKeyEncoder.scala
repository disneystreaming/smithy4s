/*
 *  Copyright 2021-2026 Disney Streaming
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
import smithy4s.schema.{
  AltF,
  CollectionTag,
  EnumTag,
  EnumValue,
  FieldF,
  MapTag,
  OptionalTag,
  Primitive,
  Schema,
  SchemaVisitor,
  VisitorF
}
import smithy4s.schema.Primitive._
import smithy4s.time.DurationOps._

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
            instance(_.toBase64String)
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
          case PDocument       => None
          case PLocalDate      => asString
          case PLocalTime      => asString
          case PDuration       => forBigDecimal { dur => dur.toBigDecimal }
          case POffsetDateTime => asString
        }
      }
      override def enumeration[E](
          shapeId: ShapeId,
          hints: Hints,
          tag: EnumTag[E],
          values: List[EnumValue[E]]
      ): OptDocumentKeyEncoder[E] = tag match {
        case EnumTag.IntEnum(value, _) =>
          Some { a => value(a).toString }
        case EnumTag.StringEnum(value, _) =>
          Some { value(_) }
      }

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

  val effectfulVisitor: VisitorF[OptDocumentKeyEncoder] =
    new VisitorF[OptDocumentKeyEncoder] {
      override def primitive[P](
          shapeId: ShapeId,
          hints: Hints,
          tag: Primitive[P]
      ): OptDocumentKeyEncoder[P] =
        trySchemaVisitor.primitive(shapeId, hints, tag)
      override def collection[C[_], A](
          shapeId: ShapeId,
          hints: Hints,
          tag: CollectionTag[C],
          member: Lazy[OptDocumentKeyEncoder[A]]
      ): OptDocumentKeyEncoder[C[A]] =
        None
      override def map[C[_, _], K, V](
          shapeId: ShapeId,
          hints: Hints,
          tag: MapTag[C],
          key: Lazy[OptDocumentKeyEncoder[K]],
          value: Lazy[OptDocumentKeyEncoder[V]]
      ): OptDocumentKeyEncoder[C[K, V]] =
        None
      override def enumeration[E](
          shapeId: ShapeId,
          hints: Hints,
          tag: EnumTag[E],
          values: List[EnumValue[E]]
      ): OptDocumentKeyEncoder[E] =
        trySchemaVisitor.enumeration(shapeId, hints, tag, values)
      override def struct[S](
          shapeId: ShapeId,
          hints: Hints,
          fields: Vector[FieldF[S, _, OptDocumentKeyEncoder[Any], Any]],
          make: IndexedSeq[Any] => S
      ): OptDocumentKeyEncoder[S] = None
      override def union[U](
          shapeId: ShapeId,
          hints: Hints,
          alternatives: Vector[AltF[U, _, OptDocumentKeyEncoder[Any], Any]],
          dispatch: U => Int
      ): OptDocumentKeyEncoder[U] = None
      override def biject[A, B](
          underlying: Lazy[OptDocumentKeyEncoder[A]],
          bijection: Bijection[A, B]
      ): OptDocumentKeyEncoder[B] =
        underlying.value.map(_.contramap(bijection.from))
      override def refine[A, B](
          underlying: Lazy[OptDocumentKeyEncoder[A]],
          refinement: Refinement[A, B]
      ): OptDocumentKeyEncoder[B] =
        underlying.value.map(_.contramap(refinement.from))
      override def lazily[A](
          suspend: Lazy[OptDocumentKeyEncoder[A]]
      ): OptDocumentKeyEncoder[A] = None
      override def option[C[_], A](
          tag: OptionalTag[C],
          member: Lazy[OptDocumentKeyEncoder[A]]
      ): OptDocumentKeyEncoder[C[A]] = None
    }
}
