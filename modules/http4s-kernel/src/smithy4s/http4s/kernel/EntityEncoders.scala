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
import smithy4s.http.CodecAPI
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

object EntityEncoders {

  def fromCodecAPI[F[_]](
      codecAPI: CodecAPI
  ): CachedSchemaCompiler[EntityEncoder[F, *]] =
    new CachedSchemaCompiler[EntityEncoder[F, *]] {
      type Cache = codecAPI.Cache
      def createCache(): Cache = codecAPI.createCache()
      def fromSchema[A](schema: Schema[A]): EntityEncoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          schema: Schema[A],
          cache: Cache
      ): EntityEncoder[F, A] = {
        val codecA: codecAPI.Codec[A] = codecAPI.compileCodec(schema, cache)
        val mediaType = MediaType.unsafeParse(codecAPI.mediaType(codecA).value)
        EntityEncoder
          .byteArrayEncoder[F]
          .withContentType(`Content-Type`(mediaType))
          .contramap[A]((a: A) => codecAPI.encode(codecA, a).toArray)
      }
    }

}
