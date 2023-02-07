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

trait GetDefaultValue[A] { self =>
  def getDefault: Option[Document]
}

object GetDefaultValue {
  def none[A]: GetDefaultValue[A] = new GetDefaultValue[A] {
    def getDefault: Option[Document] = None
  }
}

object DefaultValueSchemaVisitor extends SchemaVisitor[GetDefaultValue] {

  private def instance[A](maybeDefault: Option[Document]): GetDefaultValue[A] =
    new GetDefaultValue[A] {
      def getDefault: Option[Document] = maybeDefault
    }

  private def from[A](
      hints: Hints,
      onNullDefault: => Option[Document]
  ): GetDefaultValue[A] = {
    hints.get(smithy.api.Default).map(_.value) match {
      case Some(d) if d == Document.DNull => instance(onNullDefault)
      case d                              => instance(d)
    }
  }

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): GetDefaultValue[P] = from(
    hints,
    tag match {
      case PShort      => Some(Document.fromInt(0))
      case PString     => Some(Document.fromString(""))
      case PFloat      => Some(Document.fromDouble(0))
      case PDouble     => Some(Document.fromDouble(0))
      case PTimestamp  => None
      case PBlob       => Some(Document.fromString(""))
      case PBigInt     => Some(Document.fromBigDecimal(BigDecimal(0)))
      case PUUID       => None
      case PInt        => Some(Document.fromInt(0))
      case PBigDecimal => Some(Document.fromBigDecimal(BigDecimal(0)))
      case PBoolean    => Some(Document.fromBoolean(false))
      case PLong       => Some(Document.fromLong(0))
      case PByte       => None
      case PUnit       => None
      case PDocument   => Some(Document.DNull)
    }
  )

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): GetDefaultValue[C[A]] = from(hints, Some(Document.array()))

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): GetDefaultValue[Map[K, V]] = from(hints, Some(Document.obj()))

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): GetDefaultValue[E] = from(hints, None)

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): GetDefaultValue[S] = from(hints, None)

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): GetDefaultValue[U] = from(hints, None)

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): GetDefaultValue[B] = {
    val res = apply(schema)
    new GetDefaultValue[B] {
      def getDefault: Option[Document] = res.getDefault
    }
  }

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): GetDefaultValue[B] = {
    val res = apply(schema)
    new GetDefaultValue[B] {
      def getDefault: Option[Document] = res.getDefault
    }
  }

  def lazily[A](suspend: Lazy[Schema[A]]): GetDefaultValue[A] = {
    lazy val underlying = apply(suspend.value)
    new GetDefaultValue[A] {
      def getDefault: Option[Document] = underlying.getDefault
    }
  }
}
