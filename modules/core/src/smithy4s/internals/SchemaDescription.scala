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

package smithy4s
package internals

import smithy4s.schema.Primitive.PTimestamp
import smithy4s.schema._
import smithy4s.time._

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

  override def map[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Schema[K], value: Schema[V]): SchemaDescription[C[K,V]] =
    SchemaDescription.of("Map")

  override def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]]): SchemaDescription[E] =
    SchemaDescription.of("Enumeration")

  override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S): SchemaDescription[S] =
    SchemaDescription.of("Structure")

  override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], dispatch: Alt.Dispatcher[U]): SchemaDescription[U] =
    SchemaDescription.of("Union")

  override def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))
  override def refine[A, B](schema: Schema[A], refinement: Refinement[A,B]): SchemaDescription[B] =
    SchemaDescription.of(apply(schema))

  override def option[C[_], A](tag: OptionalTag[C], schema: Schema[A]): SchemaDescription[C[A]] =
    SchemaDescription.of(tag.name)

  override def lazily[A](suspend: Lazy[Schema[A]]): SchemaDescription[A] =
    suspend.map(s => SchemaDescription.of(apply(s))).value
  // format: on
}
