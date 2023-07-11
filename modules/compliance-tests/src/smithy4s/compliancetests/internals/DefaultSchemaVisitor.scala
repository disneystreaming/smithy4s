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

package smithy4s.compliancetests
package internals

import cats.Id

import java.util.UUID
import smithy4s.schema._
import smithy4s.Timestamp
import smithy4s.ByteArray
import smithy4s.schema.Primitive._
import smithy4s.{Bijection, Hints, Lazy, Refinement, ShapeId}
import smithy4s.Document.DNull

private[compliancetests] object DefaultSchemaVisitor extends SchemaVisitor[Id] {
  self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Id[P] = tag match {
    case PFloat      => 0: Float
    case PBigDecimal => 0: BigDecimal
    case PBigInt     => 0: BigInt
    case PBlob       => ByteArray(Array.emptyByteArray)
    case PDocument   => DNull
    case PByte       => 0: Byte
    case PInt        => 0
    case PShort      => 0: Short
    case PString     => ""
    case PLong       => 0: Long
    case PDouble     => 0: Double
    case PBoolean    => true
    case PTimestamp  => Timestamp(0L, 0)
    case PUUID       => new UUID(0, 0)
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Id[C[A]] = tag.empty

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Id[Map[K, V]] = Map.empty

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Id[E] = values.head.value

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): Id[S] = make(fields.map(_.schema.compile(self)))

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): Id[U] = {
    def processAlt[A](alt: Alt[U, A]) = alt.inject(apply(alt.schema))
    processAlt(alternatives.head)
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Id[B] = bijection(apply(schema))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Id[B] = refinement.unsafe(apply(schema))

  override def lazily[A](suspend: Lazy[Schema[A]]): Id[A] = {
    suspend.map(apply).value
  }

  override def nullable[A](schema: Schema[A]): Id[Option[A]] = None

}
