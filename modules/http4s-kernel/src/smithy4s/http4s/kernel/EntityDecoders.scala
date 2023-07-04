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
package http4s
package kernel

import smithy4s.http.HttpBodyReader
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import org.http4s.EntityDecoder
import org.http4s.MediaType
import org.http4s._
import smithy4s.kinds.PolyFunction

object EntityDecoders {

  def fromHttpBodyReader[F[_]: Concurrent, A](
      httpBodyReader: HttpBodyReader[A]
  ): EntityDecoder[F, A] = {
    val mediaType = MediaType.unsafeParse(httpBodyReader.mediaType.value)
    EntityDecoder
      .decodeBy(mediaType)(EntityDecoder.collectBinary[F])
      .flatMapR(chunk =>
        httpBodyReader.instance
          .read(Blob(chunk.toArray))
          .leftWiden[Throwable]
          .liftTo[DecodeResult[F, *]]
      )
  }

  def fromHttpBodyReaderK[F[_]: Concurrent]
      : PolyFunction[HttpBodyReader, EntityDecoder[F, *]] =
    new PolyFunction[HttpBodyReader, EntityDecoder[F, *]] {
      def apply[A](httpBodyReader: HttpBodyReader[A]): EntityDecoder[F, A] =
        fromHttpBodyReader[F, A](httpBodyReader)
    }

}
