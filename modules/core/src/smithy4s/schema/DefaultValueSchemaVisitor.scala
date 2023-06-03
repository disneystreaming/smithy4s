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

package smithy4s
package schema

import smithy4s.schema.Primitive._

private[schema] object DefaultValueSchemaVisitor extends SchemaVisitor[Option] {

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Option[P] =
    tag match {
      case PShort      => Some(0: Short)
      case PString     => Some("")
      case PFloat      => Some(0f)
      case PDouble     => Some(0d)
      case PInt        => Some(0)
      case PLong       => Some(0L)
      case PBoolean    => Some(false)
      case PTimestamp  => Some(Timestamp.epoch)
      case PBlob       => Some(ByteArray.empty)
      case PBigInt     => Some(BigInt(0))
      case PBigDecimal => Some(BigDecimal(0))
      case PDocument   => Some(Document.DNull)
      case PUUID       => None
      case PByte       => None
      case PUnit       => None
    }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Option[C[A]] = Some(tag.empty)

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Option[Map[K, V]] = Some(Map.empty)

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Option[E] = None

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Option[S] = None

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Option[U] = None

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Option[B] = {
    schema.compile(this).map(bijection.to)
  }

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Option[B] = None

  def lazily[A](
      shapeId: ShapeId,
      hints: Hints,
      suspend: Lazy[Schema[A]]
  ): Option[A] =
    suspend.map(_.compile(this)).value

  def nullable[A](
      shapeId: ShapeId,
      hints: Hints,
      schema: Schema[A]
  ): Option[Option[A]] = Some(None)
}
