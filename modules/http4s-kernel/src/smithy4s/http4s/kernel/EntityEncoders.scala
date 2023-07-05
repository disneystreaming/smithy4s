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

import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import smithy4s.codecs._
import smithy4s.http._
import smithy4s.kinds.PolyFunction

object EntityEncoders {

  def fromHttpMediaWriter[F[_], A](
      httpBodyWriter: HttpMediaWriter[A]
  ): EntityEncoder[F, A] = {
    val mediaType = MediaType.unsafeParse(httpBodyWriter.mediaType.value)
    EntityEncoder
      .byteArrayEncoder[F]
      .withContentType(`Content-Type`(mediaType))
      .contramap[A]((a: A) => httpBodyWriter.instance.encode(a).toArray)
  }

  def fromHttpMediaWriterK[F[_]]
      : PolyFunction[HttpMediaWriter, EntityEncoder[F, *]] =
    new PolyFunction[HttpMediaWriter, EntityEncoder[F, *]] {
      def apply[A](httpBodyWriter: HttpMediaWriter[A]): EntityEncoder[F, A] =
        fromHttpMediaWriter[F, A](httpBodyWriter)
    }

  def fromPayloadCodecK[F[_]](
      mediaType: HttpMediaType
  ): PolyFunction[PayloadCodec, EntityEncoder[F, *]] = {
    PayloadCodec.writerK
      .andThen[HttpMediaWriter](HttpMediaTyped.mediaTypeK(mediaType))
      .andThen[EntityEncoder[F, *]](EntityEncoders.fromHttpMediaWriterK)
  }

}
