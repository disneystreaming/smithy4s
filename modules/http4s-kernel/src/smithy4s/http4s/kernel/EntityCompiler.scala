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

import cats.effect.Concurrent
import cats.syntax.all._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s._
import org.http4s.headers.`Content-Type`
import smithy4s.http.BodyPartial
import smithy4s.http.CodecAPI
import smithy4s.http.Metadata
import smithy4s.schema.Schema

trait EntityCompiler[F[_]] {

  type Cache

  def createCache(): Cache

  /**
    * Turns a Schema into an http4s EntityEncoder
    *
    * @param schema the value's schema
    * @param cache a cache that can be used to avoid recompiling encoders
    * @return the entity encoder associated to the A value.
    */
  def compileEntityEncoder[A](
      schema: Schema[A],
      cache: Cache
  ): EntityEncoder[F, A]

  /**
    * Turns a Schema into an http4s EntityDecoder
    *
    * @param schema the value's schema
    * @param cache a cache that can be used to avoid recompiling encoders
    * @return the entity decoder associated to the A value.
    */
  def compileEntityDecoder[A](
      schema: Schema[A],
      cache: Cache
  ): EntityDecoder[F, A]

  /**
    * Turns a Schema into an http4s EntityDecoder that only partially
    * decodes the data, expecting for decoded metadata to be provided
    * to complete the data.
    *
    * @param schema the value's schema
    * @return the entity encoder associated to the A value.
    */
  def compilePartialEntityDecoder[A](
      schema: Schema[A],
      cache: Cache
  ): EntityDecoder[F, BodyPartial[A]]

}

object EntityCompiler {

  def fromCodecAPI[F[_]](
      codecAPI: CodecAPI
  )(implicit F: Concurrent[F]): EntityCompiler[F] =
    new EntityCompiler[F] {
      type Cache = codecAPI.Cache
      def createCache(): Cache = codecAPI.createCache()

      def compileEntityEncoder[A](
          schema: Schema[A],
          cache: Cache
      ): EntityEncoder[F, A] = {
        val codecA: codecAPI.Codec[A] = codecAPI.compileCodec(schema, cache)
        val mediaType = MediaType.unsafeParse(codecAPI.mediaType(codecA).value)
        val expectBody = Metadata.PartialDecoder
          .fromSchema(schema)
          .total
          .isEmpty // expect body if metadata decoder is not total
        if (expectBody) {
          EntityEncoder
            .byteArrayEncoder[F]
            .withContentType(
              `Content-Type`(mediaType)
            )
            .contramap[A]((a: A) => codecAPI.writeToArray(codecA, a))
        } else {
          EntityEncoder.emptyEncoder
        }
      }

      def compileEntityDecoder[A](
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

      // def compileMessageDecoder[A](
      //     schema: Schema[A],
      //     cache: Cache
      // ): EntityDecoder[F, A] = {
      //   val codecA: codecAPI.Codec[A] = codecAPI.compileCodec(schema, cache)
      //   val mediaType = MediaType.unsafeParse(codecAPI.mediaType(codecA).value)
      //   val partialDecoder = Metadata.PartialDecoder.fromSchema(schema)
      //   partialDecoder.total match {
      //     case None =>
      //     case Some(value) =>
      //       new EntityDecoder[F, A] {
      //         def decode(m: Media[F], strict: Boolean): DecodeResult[F, A] = {
      //         }
      //       }
      //   }
      // }

      def compilePartialEntityDecoder[A](
          schema: Schema[A],
          cache: Cache
      ): EntityDecoder[F, BodyPartial[A]] = {
        val codecA: codecAPI.Codec[A] = codecAPI.compileCodec(schema, cache)
        val mediaType = MediaType.unsafeParse(codecAPI.mediaType(codecA).value)
        EntityDecoder
          .decodeBy(mediaType)(EntityDecoder.collectBinary[F])
          .flatMapR(chunk =>
            codecAPI
              .decodeFromByteArrayPartial(codecA, chunk.toArray)
              .leftWiden[Throwable]
              .liftTo[DecodeResult[F, *]]
          )
      }

    }

}
