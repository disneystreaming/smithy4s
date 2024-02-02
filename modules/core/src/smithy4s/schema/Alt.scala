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
package schema

import smithy4s.capability.EncoderK

import kinds._

/**
  * Represents a member of coproduct type (sealed trait)
  */
final case class Alt[U, A] private (
    label: String,
    schema: Schema[A],
    inject: A => U,
    project: PartialFunction[U, A]
) {

  @deprecated("use .schema instead", since = "0.18.0")
  def instance: Schema[A] = schema

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
  def apply[U, A](
      label: String,
      schema: Schema[A],
      inject: A => U,
      project: PartialFunction[U, A]
  ): Alt[U, A] = {
    new Alt(label, schema, inject, project)
  }

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

    def ordinal(u: U): Int

  }

  object Dispatcher {

    def fromUnion[U](union: Schema.UnionSchema[U]): Dispatcher[U] =
      apply(
        alts = union.alternatives,
        ordinal = union.ordinal
      )

    private[smithy4s] def apply[U](
        alts: Vector[Alt[U, _]],
        ordinal: U => Int
    ): Dispatcher[U] = new Impl[U](alts, ordinal)

    private[smithy4s] case class Impl[U](
        alts: Vector[Alt[U, _]],
        ord: U => Int
    ) extends Dispatcher[U] {
      def compile[F[_], Result](precompile: Precompiler[F])(implicit
          encoderK: EncoderK[F, Result]
      ): F[U] = {
        val compiler = precompile.toPolyFunction[U]
        val builder = scala.collection.mutable.ArrayBuffer[Any]()
        alts.foreach { alt =>
          builder += compiler(alt)
        }
        val precompiledAlts = builder.toArray

        encoderK.absorb[U] { u =>
          val ord = ordinal(u)
          encoderK(
            precompiledAlts(ord).asInstanceOf[F[Any]],
            alts(ord).project(u)
          )
        }
      }

      def ordinal(u: U): Int = ord(u)

    }
  }

}
