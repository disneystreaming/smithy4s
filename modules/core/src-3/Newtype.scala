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

abstract class Newtype[A] extends HasId { self =>
  opaque type Type = A

  def apply(a: A): Type = a
  final val make: Newtype.Make[A, Type] = apply(_: A)

  extension (orig: Type) def value: A = orig

  def unapply(orig: Type): Some[A] = Some(orig.value)

  def schema: Schema[Type]

  implicit val tag: ShapeTag[Type] = new ShapeTag[Type] {
    def id: ShapeId = self.id
    def schema: Schema[Type] = self.schema
  }

  implicit val isomorphismInstance: capability.Isomorphism[A, Type] =
    new capability.Isomorphism[A, Type] {
      inline def from(b: Type): A = b.value
      inline def to(a: A): Type = apply(a)
    }

  object hint {
    def unapply(h: Hints): Option[Type] = h.get(tag)
  }
}

object Newtype {
  trait Make[A, B] extends Function[A, B]
}
