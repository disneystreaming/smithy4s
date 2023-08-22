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

package smithy4s.http

import smithy4s.capability._
import smithy4s.kinds._

/**
  * A class representing some kind of codec accompanied with a media type.
  */
final case class HttpMediaTyped[F[_], A](
    mediaType: HttpMediaType,
    instance: F[A]
) {
  def mapInstance[G[_], B](f: F[A] => G[B]): HttpMediaTyped[G, B] =
    HttpMediaTyped(mediaType, f(instance))

  def map[B](f: A => B)(implicit C: Covariant[F]): HttpMediaTyped[F, B] =
    HttpMediaTyped(mediaType, C.map(instance)(f))

  def contramap[B](f: B => A)(implicit
      C: Contravariant[F]
  ): HttpMediaTyped[F, B] = HttpMediaTyped(mediaType, C.contramap(instance)(f))

  def mapK[G[_]](fk: PolyFunction[F, G]): HttpMediaTyped[G, A] =
    HttpMediaTyped(mediaType, fk(instance))
}

object HttpMediaTyped {

  def liftPolyFunction[F[_], G[_]](
      fk: PolyFunction[F, G]
  ): PolyFunction[HttpMediaTyped[F, *], HttpMediaTyped[G, *]] =
    new PolyFunction[HttpMediaTyped[F, *], HttpMediaTyped[G, *]] {
      def apply[A](fa: HttpMediaTyped[F, A]): HttpMediaTyped[G, A] =
        fa.copy(instance = fk(fa.instance))
    }

  def mediaTypeK[F[_]](
      mediaType: HttpMediaType
  ): PolyFunction[F, HttpMediaTyped[F, *]] =
    new PolyFunction[F, HttpMediaTyped[F, *]] {
      def apply[A](fa: F[A]): HttpMediaTyped[F, A] =
        HttpMediaTyped(mediaType, fa)
    }

  def unwrappedK[F[_]]: PolyFunction[HttpMediaTyped[F, *], F] =
    new PolyFunction[HttpMediaTyped[F, *], F] {
      def apply[A](fa: HttpMediaTyped[F, A]): F[A] = fa.instance
    }
}
