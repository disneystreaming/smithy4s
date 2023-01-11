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

package smithy4s.aws.query

import smithy4s.http.{BodyPartial, CodecAPI, HttpMediaType, PayloadError}
import smithy4s.schema.CompilationCache
import smithy4s.{PayloadPath, Schema}

import java.nio.ByteBuffer

private[aws] class AwsQueryCodecAPI() extends CodecAPI {
  private val decodingError = Left(
    PayloadError(
      PayloadPath.root,
      "decoding is unsupported, encoding only",
      "TODO: integrate with XML decoders"
    )
  )

  override type Codec[A] = AwsQueryCodec[A]
  override type Cache = CompilationCache[AwsQueryCodec]

  override def createCache(): Cache = CompilationCache.make[Codec]

  override def mediaType[A](codec: Codec[A]): HttpMediaType =
    HttpMediaType("application/x-www-form-urlencoded")

  override def compileCodec[A](
      schema: Schema[A],
      cache: Cache
  ): Codec[A] = schema.compile(new AwsSchemaVisitorAwsQueryCodec(cache))

  override def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]] = decodingError

  override def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]] = decodingError

  override def writeToArray[A](
      codec: Codec[A],
      value: A
  ): Array[Byte] =
    codec(value).render.getBytes("UTF-8")
}
