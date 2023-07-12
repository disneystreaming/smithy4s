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

import smithy4s.capability.EncoderK
import kinds._

/**
  * Represents a member of coproduct type (sealed trait)
  */
final case class Alt[U, A](
    label: String,
    schema: Schema[A],
    inject: A => U
) {

  @deprecated("use .schema instead", since = "0.18.0")
  def instance: Schema[A] = schema

  def apply(value: A): Alt.WithValue[U, A] =
    Alt.WithValue(this, value)

  def hints: Hints = schema.hints
  def memberHints: Hints = schema.hints.memberHints

  def addHints(newHints: Hints): Alt[U, A] =
    copy(schema = schema.addMemberHints(newHints))

  def addHints(newHints: Hint*): Alt[U, A] =
    addHints(Hints(newHints: _*))

  def transformHintsLocally(f: Hints => Hints): Alt[U, A] =
    copy(schema = schema.transformHintsLocally(f))

  def transformHintsTransitively(f: Hints => Hints): Alt[U, A] =
    copy(schema = schema.transformHintsTransitively(f))

  def validated[C](c: C)(implicit
      constraint: RefinementProvider.Simple[C, A]
  ): Alt[U, A] =
    copy(schema = schema.validated(c)(constraint))

}
object Alt {

  final case class WithValue[U, A](
      private[Alt] val alt: Alt[U, A],
      value: A
  )

  /**
    * Precompiles an Alt to produce an instance of `G`
    */
  trait Precompiler[G[_]] { self =>
    def apply[A](label: String, schema: Schema[A]): G[A]
    def toPolyFunction[U]: PolyFunction[Alt[U, *], G] =
      new PolyFunction[Alt[U, *], G] {
        def apply[A](fa: Alt[U, A]): G[A] = self.apply(fa.label, fa.schema)
      }
  }

  /**
    * Construct that does the heavily lifting for encoding union values, by
    * memoising the compilation of the alternatives, dispatching the union
    * instance to the correct pre-compiled encoder, and lift the resulting
    * function into an encoder that works on the union.
    */
  trait Dispatcher[U] {

    def compile[G[_], Result](precompile: Precompiler[G])(implicit
        encoderK: EncoderK[G, Result]
    ): G[U]

    def projector[A](alt: Alt[U, A]): U => Option[A]
  }

  object Dispatcher {

    def fromUnion[U](union: Schema.UnionSchema[U]): Dispatcher[U] =
      apply(
        alts = union.alternatives,
        dispatchF = union.dispatch
      )

    private[smithy4s] def apply[U](
        alts: Vector[Alt[U, _]],
        dispatchF: U => Alt.WithValue[U, _]
    ): Dispatcher[U] = new Impl[U](alts, dispatchF)

    private[smithy4s] case class Impl[U](
        alts: Vector[Alt[U, _]],
        underlying: U => Alt.WithValue[U, _]
    ) extends Dispatcher[U] {
      def compile[G[_], Result](precompile: Precompiler[G])(implicit
          encoderK: EncoderK[G, Result]
      ): G[U] = {
        val precompiledAlts =
          precompile.toPolyFunction
            .unsafeCacheBy[String](
              alts.map(Kind1.existential(_)),
              (alt: Kind1.Existential[Alt[U, *]]) =>
                alt.asInstanceOf[Alt[U, _]].label
            )

        encoderK.absorb[U] { u =>
          underlying(u) match {
            case awv: Alt.WithValue[U, a] =>
              encoderK(precompiledAlts(awv.alt), awv.value)
          }
        }
      }

      def projector[A](alt: Alt[U, A]): U => Option[A] = { u =>
        val under = underlying(u)
        if (under.alt.label == alt.label)
          Some(under.value.asInstanceOf[A])
        else None
      }
    }
  }

}
