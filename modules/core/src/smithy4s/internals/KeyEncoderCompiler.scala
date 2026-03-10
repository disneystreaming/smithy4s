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

import smithy4s._
import smithy4s.schema.EnumValue
import smithy4s.schema.Primitive
import smithy4s.schema.Schema
import smithy4s.schema.EnumTag
import smithy4s.schema.Compilation
import smithy4s.schema.CollectionTag
import smithy4s.schema.Field
import smithy4s.schema.Alt
import Primitive._
import smithy.api.TimestampFormat
import smithy.api.TimestampFormat._

object KeyEncoderCompiler extends Compilation.Visitor[MaybeKeyEncoder] {

  private def forBigDecimal[A](f: A => BigDecimal): MaybeKeyEncoder[A] =
    Some(_.toString())
  private def fromToString[A]: MaybeKeyEncoder[A] = Some(_.toString())

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Compilation[MaybeKeyEncoder[P]] = leaf {
    tag match {
      case PBoolean    => fromToString
      case PBigDecimal => fromToString
      case PUUID       => fromToString
      case PString     => fromToString
      case PShort      => forBigDecimal { a => BigDecimal(a.toInt) }
      case PBigInt     => forBigDecimal { BigDecimal(_) }
      case PInt        => forBigDecimal { BigDecimal(_) }
      case PDouble     => forBigDecimal { BigDecimal(_) }
      case PLong       => forBigDecimal { BigDecimal(_) }
      case PByte       => forBigDecimal { a => BigDecimal(a.toInt) }
      case PFloat      => forBigDecimal { a => BigDecimal(a.toDouble) }
      case PBlob       => Some(_.toBase64String)
      case PTimestamp =>
        hints
          .get(TimestampFormat)
          .getOrElse(DATE_TIME) match {
          case DATE_TIME => Some { ts => ts.format(DATE_TIME) }
          case HTTP_DATE => Some { ts => ts.format(HTTP_DATE) }
          case EPOCH_SECONDS =>
            forBigDecimal { ts => BigDecimal(ts.epochSecond) }
        }
      case PDocument => None
    }
  }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Compilation[MaybeKeyEncoder[B]] =
    compile(schema).map(_.map(_.compose(bijection.from)))

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Compilation[MaybeKeyEncoder[B]] =
    compile(schema).map(_.map(_.compose(refinement.from)))

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Compilation[MaybeKeyEncoder[C[A]]] = leaf(None)

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Compilation[MaybeKeyEncoder[Map[K, V]]] = leaf(None)

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Compilation[MaybeKeyEncoder[E]] = tag match {
    case EnumTag.IntEnum() =>
      leaf(Some { a => total(a).intValue.toString })
    case _ =>
      leaf(Some { a => total(a).stringValue })
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): Compilation[MaybeKeyEncoder[S]] = leaf(None)

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      ordinal: U => Int
  ): Compilation[MaybeKeyEncoder[U]] = leaf(None)

  def lazily[A](suspend: Lazy[Schema[A]]): Compilation[MaybeKeyEncoder[A]] =
    leaf(None)
  def option[A](schema: Schema[A]): Compilation[MaybeKeyEncoder[Option[A]]] =
    leaf(None)

}
