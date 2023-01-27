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
final case class Alt[F[_], U, A](
    label: String,
    instance: F[A],
    inject: A => U
) {
  def apply(value: A): Alt.WithValue[F, U, A] =
    Alt.WithValue(this, value)

  def mapK[G[_]](fk: PolyFunction[F, G]): Alt[G, U, A] =
    Alt(label, fk(instance), inject)

}
object Alt {

  final case class WithValue[F[_], U, A](
      private[Alt] val alt: Alt[F, U, A],
      value: A
  ) {
    def mapK[G[_]](fk: PolyFunction[F, G]): WithValue[G, U, A] =
      WithValue(alt.mapK(fk), value)
  }

  /**
    * Precompiles an Alt to produce an instance of `G`
    */
  trait Precompiler[F[_], G[_]] { self =>
    def apply[A](label: String, instance: F[A]): G[A]
    def toPolyFunction[U]: PolyFunction[Alt[F, U, *], G] =
      new PolyFunction[Alt[F, U, *], G] {
        def apply[A](fa: Alt[F, U, A]): G[A] = self.apply(fa.label, fa.instance)
      }
  }

  /**
    * Construct that does the heavily lifting for encoding union values, by
    * memoising the compilation of the alternatives, dispatching the union
    * instance to the correct pre-compiled encoder, and lift the resulting
    * function into an encoder that works on the union.
    */
  trait Dispatcher[F[_], U] {

    def compile[G[_], Result](precompile: Precompiler[F, G])(implicit
        encoderK: EncoderK[G, Result]
    ): G[U]

    def projector[A](alt: Alt[F, U, A]): U => Option[A]
  }

  object Dispatcher {

    private[smithy4s] def apply[F[_], U](
        alts: Vector[Alt[F, U, _]],
        dispatchF: U => Alt.WithValue[F, U, _]
    ): Dispatcher[F, U] = new Impl[F, U](alts, dispatchF)

    private[smithy4s] case class Impl[F[_], U](
        alts: Vector[Alt[F, U, _]],
        underlying: U => Alt.WithValue[F, U, _]
    ) extends Dispatcher[F, U] {
      def compile[G[_], Result](precompile: Precompiler[F, G])(implicit
          encoderK: EncoderK[G, Result]
      ): G[U] = {
        val precompiledAlts =
          precompile.toPolyFunction
            .unsafeCacheBy[String](
              alts.map(Kind1.existential(_)),
              (alt: Kind1.Existential[Alt[F, U, *]]) =>
                alt.asInstanceOf[Alt[F, U, _]].label
            )

        encoderK.absorb[U] { u =>
          underlying(u) match {
            case awv: Alt.WithValue[F, U, a] =>
              encoderK(precompiledAlts(awv.alt), awv.value)
          }
        }
      }

      def projector[A](alt: Alt[F, U, A]): U => Option[A] = { u =>
        val under = underlying(u)
        if (under.alt.label == alt.label)
          Some(under.value.asInstanceOf[A])
        else None
      }
    }
  }

  def liftK[F[_], G[_], U](
      fk: PolyFunction[F, G]
  ): PolyFunction[Alt[F, U, *], Alt[G, U, *]] =
    new PolyFunction[Alt[F, U, *], Alt[G, U, *]] {
      def apply[A](fa: Alt[F, U, A]): Alt[G, U, A] = fa.mapK(fk)
    }

  implicit class SchemaAltOps[U, A](private val alt: SchemaAlt[U, A])
      extends AnyVal {

    def hints: Hints = alt.instance.hints

    def addHints(newHints: Hint*) =
      Alt(alt.label, alt.instance.addHints(newHints: _*), alt.inject)

    def addHints(newHints: Hints) =
      Alt(alt.label, alt.instance.addHints(newHints), alt.inject)

    def validated[C](c: C)(implicit
        constraint: RefinementProvider.Simple[C, A]
    ): SchemaAlt[U, A] =
      Alt(
        alt.label,
        alt.instance.validated(c)(constraint),
        alt.inject
      )
  }

  type SchemaAndValue[S, A] = WithValue[Schema, S, A]
}
