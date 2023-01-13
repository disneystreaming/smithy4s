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

import cats.effect.SyncIO
import cats.syntax.either._
import smithy4s.http.{BodyPartial, CodecAPI, HttpMediaType, PayloadError}
import smithy4s.schema.CompilationCache
import smithy4s.{PayloadPath, Schema}
import smithy4s.xml.internals.XmlDecoder
import smithy4s.xml.internals.XmlCursor
import smithy4s.xml.internals.XmlDecoderSchemaVisitor
import smithy4s.internals.InputOutput
import smithy4s.xml.XmlDocument
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.dom._

import java.nio.ByteBuffer

private[aws] class AwsQueryCodecAPI() extends CodecAPI {

  override type Codec[A] = Either[AwsQueryCodec[A], XmlDecoder[A]]
  override type Cache = CompilationCache[AwsQueryCodec]

  override def createCache(): Cache = CompilationCache.make[AwsQueryCodec]

  override def compileCodec[A](
      schema: Schema[A],
      cache: Cache
  ): Codec[A] =
    schema.hints match {
      case InputOutput.hint(InputOutput.Input) =>
        Left(schema.compile(new AwsSchemaVisitorAwsQueryCodec(cache)))
      case InputOutput.hint(InputOutput.Output) =>
        Right(schema.compile(XmlDecoderSchemaVisitor))
    }

  override def mediaType[A](codec: Codec[A]): HttpMediaType =
    HttpMediaType("application/x-www-form-urlencoded")

  override def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]] = codec match {
    case Left(_) =>
      Left(
        PayloadError(
          PayloadPath.root,
          "",
          "Invalid codec: got AWS Query encoder, expected XML decoder"
        )
      )
    case Right(xmlDecoder) =>
      Stream
        .emit[SyncIO, String](new String(bytes, "UTF-8"))
        .through(events[SyncIO, String]())
        .through(documents[SyncIO, XmlDocument])
        .map(doc => XmlCursor.fromDocument(doc))
        .map(cursor => xmlDecoder.decode(cursor))
        .rethrow
        .head
        .compile
        .lastOrError
        .map(a => BodyPartial(_ => a))
        .attempt
        .unsafeRunSync()
        .leftMap(e =>
          PayloadError(
            PayloadPath.root,
            "",
            s"Failed to decode the response, message: ${e.getMessage}"
          )
        )
  }

  override def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]] =
    throw new IllegalStateException("Must have not been called")

  override def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
    codec match {
      case Left(encoder) => encoder(value).render.getBytes("UTF-8")
      case Right(_) =>
        throw new IllegalStateException(
          "Invalid codec: got XML decoder, must be AWS query encoder"
        )
    }
}
