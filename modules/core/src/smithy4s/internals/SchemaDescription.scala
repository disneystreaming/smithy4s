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
package internals

import smithy4s.schema.{
  Primitive,
  EnumValue,
  SchemaField,
  SchemaAlt,
  Alt,
  SchemaVisitor,
  CollectionTag
}
import smithy4s.schema.Primitive.PTimestamp

object SchemaDescription extends SchemaVisitor[SchemaDescription] {
  // format: off

  def of[A](value: String): SchemaDescription[A] = value

  override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): SchemaDescription[P] = {
    tag match {
      case PTimestamp =>
        val format = Primitive.timestampFormat(hints)
        Timestamp.showFormat(format)
      case other => Primitive.describe(other)
    }
  }
  override def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): SchemaDescription[C[A]] =
    SchemaDescription.of(tag.name)
  override def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): SchemaDescription[Map[K,V]] =
    SchemaDescription.of("Map")

  override def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): SchemaDescription[E] =
    SchemaDescription.of("Enumeration")

  override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): SchemaDescription[S] =
    SchemaDescription.of("Structure")

  override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: Alt.Dispatcher[Schema, U]): SchemaDescription[U] =
    SchemaDescription.of("Union")

  override def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))
  override def refine[A, B](schema: Schema[A], refinement: Refinement[A,B]): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))
  override def lazily[A](suspend: Lazy[Schema[A]]): SchemaDescription[A] =
    suspend.map(s => SchemaDescription.of(apply(s))).value
  // format: on
}
