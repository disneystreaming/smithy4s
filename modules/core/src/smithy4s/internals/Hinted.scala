/*
 *  Copyright 2021 Disney Streaming
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
package internals

import smithy4s.capability._

import schema.Field.Wrapped

case class Hinted[F[_], A](hints: Hints, make: Hints => F[A]) {
  def get: F[A] = make(hints)

  def addHints(h: Hints): Hinted[F, A] = copy(hints = hints ++ h)

  def map[B](f: A => B)(implicit C: Covariant[F]): Hinted[F, B] =
    Hinted(hints, h => C.map(make(h))(f))

  def imap[B](to: A => B, from: B => A)(implicit
      I: Invariant[F]
  ): Hinted[F, B] =
    Hinted(hints, h => I.imap(make(h))(to, from))

  def xmap[B](to: A => Either[ConstraintError, B], from: B => A)(implicit
      I: Invariant[F]
  ): Hinted[F, B] =
    Hinted(hints, h => I.xmap(make(h))(to, from))

  def mapK[G[_]](polyFunction: PolyFunction[F, G]): Hinted[G, A] =
    Hinted(hints, h => polyFunction(make(h)))

  def contramap[B](f: B => A)(implicit C: Contravariant[F]): Hinted[F, B] =
    Hinted(hints, h => C.contramap(make(h))(f))

  // hints do not get propagated through transforms
  def transform[B](f: F[A] => F[B]) =
    transformWithHints { (fa, _) => f(fa) }

  def transformWithHints[B](f: (F[A], Hints) => F[B]) =
    Hinted(Hints(), (h: Hints) => f(make(hints ++ h), h))

  def productTransform[A2, B](other: Hinted[F, A2])(
      f: (F[A], F[A2]) => F[B]
  ): Hinted[F, B] =
    Hinted(
      Hints(),
      (h: Hints) => f(make(hints ++ h), other.make(other.hints ++ h))
    )

  def emap[B](
      f: A => Either[ConstraintError, B]
  )(implicit C: Covariant[F]): Hinted[F, B] =
    Hinted(hints, h => C.emap(make(h))(f))

}

object Hinted {

  def liftK[F[_], G[_]](
      polyFunction: PolyFunction[F, G]
  ): PolyFunction[Hinted[F, *], Hinted[G, *]] =
    new PolyFunction[Hinted[F, *], Hinted[G, *]] {
      def apply[A](fa: Hinted[F, A]): Hinted[G, A] = fa.mapK(polyFunction)
    }

  def wrapK[F[_], G[_]](
      polyFunction: PolyFunction[F, Wrapped[F, G, *]]
  ): PolyFunction[Hinted[F, *], Wrapped[Hinted[F, *], G, *]] =
    new PolyFunction[Hinted[F, *], Wrapped[Hinted[F, *], G, *]] {
      def apply[A](fa: Hinted[F, A]): Hinted[F, G[A]] =
        Hinted(fa.hints, hints => polyFunction(fa.make(hints)))
    }

  def static[F[_], A](fa: F[A]): Hinted[F, A] =
    Hinted(Hints(), (_: Hints) => fa)

  def apply[F[_]]: PartiallyAppliedHinted[F] = new PartiallyAppliedHinted[F]

  class PartiallyAppliedHinted[F[_]] {
    def from[A](f: Hints => F[A]): Hinted[F, A] = Hinted(Hints(), f(_))
    def onHintOpt[H: ShapeTag, A](f: Option[H] => F[A]): Hinted[F, A] =
      Hinted(Hints(), (h: Hints) => f(h.get[H]))
    def onHintsOpt[H1: ShapeTag, H2: ShapeTag, A](
        f: (Option[H1], Option[H2]) => F[A]
    ): Hinted[F, A] =
      Hinted(Hints(), (h: Hints) => f(h.get[H1], h.get[H2]))
    def onHint[H: ShapeTag, A](default: H)(f: H => F[A]): Hinted[F, A] =
      Hinted(Hints(), (h: Hints) => f(h.get[H].getOrElse(default)))
    def static[A](fa: F[A]): Hinted[F, A] =
      Hinted(Hints(), (_: Hints) => fa)
  }

}
