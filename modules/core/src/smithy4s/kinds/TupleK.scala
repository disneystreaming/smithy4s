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

package smithy4s.kinds

import smithy4s.Bijection
import smithy4s.capability._

final case class TupleK[F[_], G[_], A](left: F[A], right: G[A]) {
  def biject[B](bijection: Bijection[A, B])(implicit
      F: Covariant[F],
      G: Contravariant[G]
  ): TupleK[F, G, B] =
    TupleK(F.map(left)(bijection.to), G.contramap(right)(bijection.from))
}

object TupleK {

  def leftK[F[_], G[_]]: PolyFunction[TupleK[F, G, *], F] =
    new PolyFunction[TupleK[F, G, *], F] {
      def apply[A](tuple: TupleK[F, G, A]): F[A] = tuple.left
    }

  def rightK[F[_], G[_]]: PolyFunction[TupleK[F, G, *], G] =
    new PolyFunction[TupleK[F, G, *], G] {
      def apply[A](tuple: TupleK[F, G, A]): G[A] = tuple.right
    }

}
