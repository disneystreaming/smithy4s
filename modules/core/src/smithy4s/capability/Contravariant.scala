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

package smithy4s.capability

trait Contravariant[F[_]] {
  def contramap[A, B](fa: F[A])(f: B => A): F[B]
}

object Contravariant {

  def apply[F[_]](implicit instance: Contravariant[F]): Contravariant[F] =
    instance

  type Wrapped[F[_], G[_], A] = F[G[A]]

  implicit def contravariantOfCovariantInstance[F[_], G[_]](implicit
      F: Covariant[F],
      G: Contravariant[G]
  ): Contravariant[Wrapped[F, G, *]] = new Contravariant[Wrapped[F, G, *]] {
    def contramap[A, B](fa: F[G[A]])(f: B => A): F[G[B]] =
      F.map(fa)(ga => G.contramap(ga)(f))
  }

}
