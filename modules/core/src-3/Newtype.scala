/*
 *  Copyright 2021-2024 Disney Streaming
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

  extension (orig: Type) def value: A = orig

  def unapply(orig: Type): Some[A] = Some(orig.value)

  def schema: Schema[Type]

  implicit val tag: ShapeTag[Type] = new ShapeTag[Type] {
    def id: ShapeId = self.id
    def schema: Schema[Type] = self.schema
  }

  implicit val asBijection: Bijection[A, Type] = new Newtype.Make[A, Type] {
    def to(a: A): Type = self.apply(a)
    def from(t: Type): A = value(t)
  }

  object hint {
    def unapply(h: Hints): Option[Type] = h.get(tag)
  }
}

object Newtype {
  private[smithy4s] trait Make[A, B] extends Bijection[A, B]
}
