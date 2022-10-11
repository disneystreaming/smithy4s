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

package smithy4s.schema

import smithy4s.Lazy

import smithy4s.Bijection
import smithy4s.Refinement

import smithy4s.{Hints, ShapeId}

private[schema] object HashVisitor {
  type Imperative[A] = Unit
}

private[schema] class HashVisitor()
    extends SchemaVisitor[HashVisitor.Imperative] {
  private var result: Int = 1
  private val prime: Int = 31
  private val visitedLazy: java.util.HashSet[ShapeId] =
    new java.util.HashSet[ShapeId]()

  def getResult: Int = result

  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): Unit = {
    result = result * prime + 1
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + tag.hashCode()
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Unit = {
    result = result * prime + 2
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + tag.hashCode()
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Unit = {
    result = result * prime + 3
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    this(key)
    this(value)
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Unit = {
    result = result * prime + 4
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    for (v <- values) {
      result = result * prime + v.hashCode()
    }
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Unit = {
    result = result * prime + 5
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + make.hashCode()
    def processField(field: SchemaField[S, _]): Unit = {
      result = result * prime + field.label.hashCode()
      result = result * prime + field.get.hashCode()
      this(field.instance)
    }
    fields.foreach(processField(_))
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Unit = {
    result = result * prime + 6
    result = result * prime + shapeId.hashCode()
    result = result * prime + hints.hashCode()
    result = result * prime + dispatch.hashCode()
    def processAlt(alt: SchemaAlt[U, _]): Unit = {
      result = result * prime + alt.label.hashCode()
      result = result * prime + alt.inject.hashCode()
      this(alt.instance)
    }
    alternatives.foreach(processAlt(_))
  }

  def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Unit = {
    result = result * prime + 7
    result = result * prime + bijection.hashCode()
    this(schema)
  }

  def refine[A, B](schema: Schema[A], refinement: Refinement[A, B]): Unit = {
    result = result * prime + 8
    result = result * prime + refinement.hashCode()
    this(schema)
  }

  def lazily[A](suspend: Lazy[Schema[A]]): Unit = {
    result = result * prime + 9
    val shapeId = suspend.value.shapeId
    if (!visitedLazy.contains(suspend.value.shapeId)) {
      visitedLazy.add(shapeId)
      this(suspend.value)
    }
  }

}
