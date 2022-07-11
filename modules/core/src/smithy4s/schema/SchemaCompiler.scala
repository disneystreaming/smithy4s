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

// format: off
// A generalised compiler that takes Schema expressions written in `F` and folds them in a `G` structure
trait SchemaCompiler[F[_], G[_]] extends (F ~> G) { self =>
  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]) : G[P]
  def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: F[A]): G[C[A]]
  def map[K, V](shapeId: ShapeId, hints: Hints, key: F[K], value: F[V]): G[Map[K, V]]
  def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]) : G[E]
  def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[F, S, _]], make: IndexedSeq[Any] => S) : G[S]
  def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[F, U, _]], dispatcher: Alt.Dispatcher[F, U]) : G[U]
  def biject[A, B](original: F[A], to: A => B, from: B => A) : G[B]
  def surject[A, B](original: F[A], to: Refinement[A, B], from: B => A) : G[B]
  def lazily[A](suspend: Lazy[Schema[A]]) : G[A]
}

object SchemaCompiler {

  abstract class Default[F[_], G[_]] extends SchemaCompiler[F, G]{
    def default[A]: G[A]
    override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): G[P] = default
    override def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: F[A]): G[C[A]] = default
    override def map[K, V](shapeId: ShapeId, hints: Hints, key: F[K], value: F[V]): G[Map[K,V]] = default
    override def enumeration[E](shapeId: ShapeId, hints: Hints, values: List[EnumValue[E]], total: E => EnumValue[E]): G[E] = default
    override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[F, S, _]], make: IndexedSeq[Any] => S) : G[S] = default
    override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[F, U, _]], dispatch: Alt.Dispatcher[F, U]) : G[U] = default
    override def biject[A, B](schema: F[A], to: A => B, from: B => A): G[B] = default
    override def surject[A, B](schema: F[A], to: Refinement[A,B], from: B => A): G[B] = default
    override def lazily[A](suspend: Lazy[Schema[A]]): G[A] = default
  }

}
