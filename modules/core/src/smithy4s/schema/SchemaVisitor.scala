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
package schema

import smithy4s.kinds.OptionK

// format: off
trait SchemaVisitor[F[_]] extends (Schema ~> F) with SchemaVisitorBase[F] {

  def apply[A](schema: Schema[A]): F[A] = SchemaVisitorBase.run(schema, this)

}



object SchemaVisitor { outer =>

  trait Default[F[_]] extends SchemaVisitor[F]{
    def default[A]: F[A]
    override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): F[P] = default
    override def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): F[C[A]] = default
    override def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): F[Map[K,V]] = default
    override def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]], total: E => EnumValue[E]): F[E] = default
    override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S): F[S] = default
    override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], dispatch: Alt.Dispatcher[U]): F[U] = default
    override def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): F[B] = default
    override def refine[A, B](schema: Schema[A], refinement: Refinement[A, B]): F[B] = default
    override def lazily[A](suspend: Lazy[Schema[A]]): F[A] = default
    override def option[A](schema: Schema[A]): F[Option[A]] = default
  }

  trait Optional[F[_]] extends Default[OptionK[F, *]]{
    def default[A]: Option[F[A]] = None
  }


  abstract class Cached[F[_]] extends SchemaVisitor[F] {
    protected val cache: CompilationCache[F]

    override def apply[A](schema: Schema[A]): F[A] = {
      cache.getOrElseUpdate(schema, super.apply(_: Schema[A]))
    }
  }

}
