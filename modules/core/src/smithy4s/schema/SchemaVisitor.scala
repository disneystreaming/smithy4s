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
trait SchemaVisitor[F[_]] extends (Schema ~> F) { self =>
  def primitive[P](schema: Schema[P], shapeId: ShapeId, hints: Hints, tag: Primitive[P]): F[P]
  def collection[C[_], A](schema: Schema[C[A]], shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): F[C[A]]
  def map[K, V](schema: Schema[Map[K,V]], shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K, V]]
  def enumeration[E](schema: Schema[E], shapeId: ShapeId, hints: Hints, tag: EnumTag, values: List[EnumValue[E]], total: E => EnumValue[E]): F[E]
  def struct[S](schema: Schema[S], shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): F[S]
  def union[U](schema: Schema[U], shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: Alt.Dispatcher[Schema, U]): F[U]
  def biject[A, B](schema: Schema[A], bijectionSchema: Schema[B], bijection: Bijection[B, A]): F[A]
  def refine[A, B](schema: Schema[A], refinementSchema: Schema[B], refinement: Refinement[B, A]): F[A]
  def lazily[A](schema: Schema[A], suspend: Lazy[Schema[A]]): F[A]

  def apply[A](schema: Schema[A]): F[A] = schema match {
    case PrimitiveSchema(shapeId, hints, tag) => primitive(schema, shapeId, hints, tag)
    case s: CollectionSchema[c, a] => collection[c,a](schema, s.shapeId, s.hints, s.tag, s.member)
    case schema @ MapSchema(shapeId, hints, key, value) => map(schema, shapeId, hints, key, value)
    case EnumerationSchema(shapeId, hints, tag, values, total) => enumeration(schema, shapeId, hints, tag, values, total)
    case StructSchema(shapeId, hints, fields, make) => struct(schema, shapeId, hints, fields, make)
    case u@UnionSchema(shapeId, hints, alts, _) => union(schema, shapeId, hints, alts, Alt.Dispatcher.fromUnion(u))
    case BijectionSchema(bijectionSchema, bijection) => biject(schema, bijectionSchema, bijection)
    case RefinementSchema(refinementSchema, refinement) => refine(schema, refinementSchema, refinement)
    case LazySchema(make) => lazily(schema, make)
  }

}



object SchemaVisitor {

  trait Default[F[_]] extends SchemaVisitor[F]{
    def default[A](schema: Schema[A]): F[A]
    override def primitive[P](schema: Schema[P], shapeId: ShapeId, hints: Hints, tag: Primitive[P]): F[P] = default(schema)    
    override def collection[C[_], M](schema: Schema[C[M]], shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[M]): F[C[M]] =
      default(schema)
    override def map[K, V](schema: Schema[Map[K,V]], shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K,V]] = default(schema)
    override def enumeration[E](schema: Schema[E], shapeId: ShapeId, hints: Hints, tag: EnumTag, values: List[EnumValue[E]], total: E => EnumValue[E]): F[E] = default(schema)
    override def struct[S](schema: Schema[S], shapeId: ShapeId, hints: Hints, fields: Vector[SchemaField[S, _]], make: IndexedSeq[Any] => S): F[S] = default(schema)
    override def union[U](schema: Schema[U], shapeId: ShapeId, hints: Hints, alternatives: Vector[SchemaAlt[U, _]], dispatch: Alt.Dispatcher[Schema, U]): F[U] = default(schema)
    override def biject[A, B](schema: Schema[A], bijectionSchema: Schema[B], bijection: Bijection[B, A]): F[A] = default(schema)
    override def refine[A, B](schema: Schema[A], refinementSchema: Schema[B], refinement: Refinement[B, A]): F[A] = default(schema)
    override def lazily[A](schema: Schema[A], suspend: Lazy[Schema[A]]): F[A] = default(schema)
  }

  abstract class Cached[F[_]] extends SchemaVisitor[F] {
    protected val cache: CompilationCache[F]

    override def apply[A](schema: Schema[A]): F[A] = {
      cache.getOrElseUpdate(schema, super.apply(_: Schema[A]))
    }
  }

}
