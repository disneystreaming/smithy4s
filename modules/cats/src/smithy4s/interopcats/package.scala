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

import scala.util.hashing.MurmurHash3
import smithy4s.capability.MonadThrowLike
import cats.MonadThrow
import cats.syntax.all._
import cats.kernel.Monoid

package object interopcats {

  implicit def monadThrowShim[F[_]: MonadThrow]: MonadThrowLike[F] =
    new MonadThrowLike[F] {
      def pure[A](a: A): F[A] = MonadThrow[F].pure(a)
      def zipMapAll[A](seq: IndexedSeq[F[Any]])(f: IndexedSeq[Any] => A): F[A] =
        seq.toVector.asInstanceOf[Vector[F[Any]]].sequence.map(f)
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        MonadThrow[F].flatMap(fa)(f)
      def raiseError[A](e: Throwable): F[A] = MonadThrow[F].raiseError(e)
      def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] =
        MonadThrow[F].handleErrorWith(fa)(f)
    }

  implicit def monoidEndpointMiddleware[Construct]
      : Monoid[Endpoint.Middleware[Construct]] =
    new Monoid[Endpoint.Middleware[Construct]] {
      def combine(
          a: Endpoint.Middleware[Construct],
          b: Endpoint.Middleware[Construct]
      ): Endpoint.Middleware[Construct] =
        a.andThen(b)

      val empty = Endpoint.Middleware.noop
    }

  private[interopcats] def combineHash(start: Int, hashes: Int*): Int = {
    var hashResult = start
    hashes.foreach(hash => hashResult = MurmurHash3.mix(hashResult, hash))
    MurmurHash3.finalizeHash(hashResult, hashes.length)
  }

}
