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

/**
  * Represents a member of coproduct type (sealed trait)
  */
final case class Alt[F[_], U, A](
    label: String,
    instance: F[A],
    inject: A => U,
    hints: Hints
) {
  def apply(value: A): Alt.WithValue[F, U, A] =
    Alt.WithValue(this, value)

  def transformHintsLocally(f: Hints => Hints): Alt[F, U, A] =
    Alt(label, instance, inject, f(hints))

  def addHints(newHints: Hint*) = transformHintsLocally(
    _ ++ Hints(newHints: _*)
  )

  def mapK[G[_]](fk: PolyFunction[F, G]): Alt[G, U, A] =
    Alt(label, fk(instance), inject, hints)

}
object Alt {

  final case class WithValue[F[_], U, A](alt: Alt[F, U, A], value: A) {
    def mapK[G[_]](fk: PolyFunction[F, G]): WithValue[G, U, A] =
      WithValue(alt.mapK(fk), value)

    /**
     * Helper function to identify this `WithValue` as an instance of another `Alt`.
     */
    def matchAs[B](another: Alt[F, U, B]): Option[WithValue[F, U, B]] =
      if (another eq alt)
        Some(this.asInstanceOf[WithValue[F, U, B]])
      else None
  }

  def liftK[F[_], G[_], U](
      fk: PolyFunction[F, G]
  ): PolyFunction[Alt[F, U, *], Alt[G, U, *]] =
    new PolyFunction[Alt[F, U, *], Alt[G, U, *]] {
      def apply[A](fa: Alt[F, U, A]): Alt[G, U, A] = fa.mapK(fk)
    }

  def shiftHintsK[U]: PolyFunction[Alt[Schema, U, *], Alt[Schema, U, *]] =
    new PolyFunction[Alt[Schema, U, *], Alt[Schema, U, *]] {
      def apply[A](fa: Alt[Schema, U, A]): Alt[Schema, U, A] =
        Alt(
          fa.label,
          fa.instance.transformHintsLocally(_ ++ fa.hints),
          fa.inject,
          Hints.empty
        )
    }

  implicit class SchemaAltOps[U, A](private val alt: SchemaAlt[U, A])
      extends AnyVal {
    def validated[C](implicit
        constraint: Validator.Simple[C, A]
    ): SchemaAlt[U, A] =
      Alt(
        alt.label,
        alt.instance.validatedAgainstHints(alt.hints),
        alt.inject,
        alt.hints
      )
  }

  type SchemaAndValue[S, A] = WithValue[Schema, S, A]
}
