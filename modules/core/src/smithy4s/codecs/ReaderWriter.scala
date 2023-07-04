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

package smithy4s.codecs

import smithy4s.capability._
import smithy4s.kinds.PolyFunction
import smithy4s.Bijection

final case class ReaderWriter[F[_], G[_], A](reader: F[A], writer: G[A]) {
  def biject[B](bijection: Bijection[A, B])(implicit
      F: Covariant[F],
      G: Contravariant[G]
  ): ReaderWriter[F, G, B] =
    ReaderWriter(
      F.map(reader)(bijection.to),
      G.contramap(writer)(bijection.from)
    )
}

object ReaderWriter {

  def readerK[F[_], G[_]]: PolyFunction[ReaderWriter[F, G, *], F] =
    new PolyFunction[ReaderWriter[F, G, *], F] {
      def apply[A](tuple: ReaderWriter[F, G, A]): F[A] = tuple.reader
    }

  def writerK[F[_], G[_]]: PolyFunction[ReaderWriter[F, G, *], G] =
    new PolyFunction[ReaderWriter[F, G, *], G] {
      def apply[A](tuple: ReaderWriter[F, G, A]): G[A] = tuple.writer
    }

}
