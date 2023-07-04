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

package smithy4s.http4s.kernel

import cats.MonadThrow
import org.http4s.EntityDecoder
import org.http4s.Media
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

object MediaDecoder {

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): MediaDecoder[F, A] = new MediaDecoder[F, A] {
    def read(response: Media[F]): F[A] = response.as[A]
  }

  def fromEntityDecoderK[F[_]: MonadThrow]
      : PolyFunction[EntityDecoder[F, *], MediaDecoder[F, *]] =
    new PolyFunction[EntityDecoder[F, *], MediaDecoder[F, *]] {
      def apply[A](fa: EntityDecoder[F, A]): MediaDecoder[F, A] =
        fromEntityDecoder(MonadThrow[F], fa)
    }

  type CachedCompiler[F[_], Message] =
    CachedSchemaCompiler[MediaDecoder[F, *]]

}
