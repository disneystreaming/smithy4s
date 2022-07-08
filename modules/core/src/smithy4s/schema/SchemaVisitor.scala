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

import Schema._

// format: off
trait SchemaVisitor[F[_]] extends (Schema ~> F) {
  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]) : F[P]
  def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): F[C[A]]
  def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K, V]]
  def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]) : F[E]
  def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S) : F[S]
  def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]) : F[U]
  def biject[A, B](schema: Schema[A], to: A => B, from: B => A) : F[B]
  def surject[A, B](schema: Schema[A], to: Refinement[A, B], from: B => A) : F[B]
  def lazily[A](suspend: Lazy[Schema[A]]) : F[A]

  final def apply[A](schema: Schema[A]) : F[A] = schema match {
    case PrimitiveSchema(shapeId, hints, tag) => primitive(shapeId, hints, tag)
    case s: CollectionSchema[c, a] => collection[c,a](s.shapeId, s.hints, s.tag, s.member)
    case MapSchema(shapeId, hints, key, value) => map(shapeId, hints, key, value)
    case EnumerationSchema(shapeId, hints, values, total) => enumeration(shapeId, hints, values, total)
    case StructSchema(shapeId, hints, fields, make) => struct(shapeId, hints, fields, make)
    case UnionSchema(shapeId, hints, alts, dispatch) => union(shapeId, hints, alts, dispatch)
    case BijectionSchema(schema, to, from) => biject(schema, to, from)
    case SurjectionSchema(schema, to, from) => surject(schema, to, from)
    case LazySchema(make) => lazily(make)
  }
}

object SchemaVisitor {

  abstract class Default[F[_]] extends SchemaVisitor[F]{
    def default[A]: F[A]
    def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): F[P] = default
    def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): F[C[A]] = default
    def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K,V]] = default
    def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): F[E] = default
    def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S) : F[S] = default
    def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: U => Alt.SchemaAndValue[U, _]) : F[U] = default
    def biject[A, B](schema: Schema[A], to: A => B, from: B => A): F[B] = default
    def surject[A, B](schema: Schema[A], to: Refinement[A,B], from: B => A): F[B] = default
    def lazily[A](suspend: Lazy[Schema[A]]): F[A] = default

  }

}
