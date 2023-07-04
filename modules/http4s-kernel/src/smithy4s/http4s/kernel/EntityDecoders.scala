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

import cats.effect.kernel.Concurrent
import cats.syntax.all._
import org.http4s.EntityDecoder
import org.http4s.MediaType
import org.http4s._
import smithy4s.codecs._
import smithy4s.http._
import smithy4s.kinds.PolyFunction
import smithy4s.kinds.PolyFunctions

object EntityDecoders {

  def fromHttpMediaReader[F[_]: Concurrent, A](
      httpBodyReader: HttpMediaReader[A]
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

  def fromHttpMediaReaderK[F[_]: Concurrent]
      : PolyFunction[HttpMediaReader, EntityDecoder[F, *]] =
    new PolyFunction[HttpMediaReader, EntityDecoder[F, *]] {
      def apply[A](httpBodyReader: HttpMediaReader[A]): EntityDecoder[F, A] =
        fromHttpMediaReader[F, A](httpBodyReader)
    }

  def fromPayloadCodecK[F[_]: Concurrent](
      mediaType: HttpMediaType
  ): PolyFunction[PayloadCodec, EntityDecoder[F, *]] = {
    // scalafmt: {maxColumn = 120}
    PayloadCodec.readerK
      .andThen[HttpPayloadReader](Reader.liftPolyFunction(PolyFunctions.mapErrorK(HttpPayloadError(_))))
      .andThen[HttpMediaReader](HttpMediaTyped.mediaTypeK(mediaType))
      .andThen[EntityDecoder[F, *]](EntityDecoders.fromHttpMediaReaderK)
  }

}
