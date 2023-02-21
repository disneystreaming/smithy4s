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

import cats.syntax.all._
import org.http4s.EntityDecoder
import org.http4s.MediaType
import org.http4s._
import smithy4s.http.CodecAPI
import smithy4s.schema.Schema
import cats.effect.kernel.Async
import smithy4s.schema.CachedSchemaCompiler

object EntityDecoders {

  def fromCodecAPICompiler[F[_]](
      codecAPI: CodecAPI
  )(implicit F: Async[F]): CachedSchemaCompiler[EntityDecoder[F, *]] =
    new CachedSchemaCompiler[EntityDecoder[F, *]] {
      type Cache = codecAPI.Cache
      def createCache(): Cache = codecAPI.createCache()
      def fromSchema[A](schema: Schema[A]): EntityDecoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          schema: Schema[A],
          cache: Cache
      ): EntityDecoder[F, A] = {
        val codecA: codecAPI.Codec[A] = codecAPI.compileCodec(schema, cache)
        val mediaType = MediaType.unsafeParse(codecAPI.mediaType(codecA).value)
        EntityDecoder
          .decodeBy(mediaType)(EntityDecoder.collectBinary[F])
          .flatMapR(chunk =>
            codecAPI
              .decodeFromByteArray(codecA, chunk.toArray)
              .leftWiden[Throwable]
              .liftTo[DecodeResult[F, *]]
          )
      }

    }

}
