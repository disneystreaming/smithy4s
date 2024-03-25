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

abstract class NewtypeValidated[A] extends HasId { self =>
  opaque type Type = A

  def apply(a: A): Either[String, Type]

  def unsafeApply(a: A): Type = a

  extension (orig: Type) def value: A = orig

  def unapply(orig: Type): Some[A] = Some(orig.value)

  def schema: Schema[Type]

  implicit val tag: ShapeTag[Type] = new ShapeTag[Type] {
    def id: ShapeId = self.id
    def schema: Schema[Type] = self.schema
  }

  given refinementProvider[C](using
      ev: RefinementProvider.Simple[C, A]
  ): RefinementProvider.Simple[C, Type] with {

    val tag: ShapeTag[C] = ev.tag

    override def make(c: C): Refinement.Aux[C, Type, Type] =
      ev.make(c).imapFull[Type, Type](asBijectionUnsafe, asBijectionUnsafe)
  }

  protected val validators: List[A => Either[String, A]]

  protected def validateInternal[C](c: C)(value: A)(using
      ev: RefinementProvider.Simple[C, A]
  ): Either[String, A] =
    ev.make(c).apply(value)

  protected val asBijectionUnsafe: Bijection[A, Type] =
    new NewtypeValidated.Make[A, Type] {
      def to(a: A): Type = self.unsafeApply(a)
      def from(t: Type): A = value(t)
    }

  object hint {
    def unapply(h: Hints): Option[Type] = h.get(tag)
  }
}

object NewtypeValidated {
  private[smithy4s] trait Make[A, B] extends Bijection[A, B]
}
