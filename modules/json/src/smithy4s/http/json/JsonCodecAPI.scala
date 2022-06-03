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
package http
package json

import com.github.plokhotnyuk.jsoniter_scala.core.ReaderConfig
import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig

import java.nio.ByteBuffer

import smithy4s.schema.SchemaVisitor

abstract class JsonCodecAPI(
    schemaVisitorJCodec: SchemaVisitor[JCodec],
    readerConfig: ReaderConfig = JsonCodecAPI.defaultReaderConfig,
    writerConfig: WriterConfig = WriterConfig
) extends CodecAPI {

  type Codec[A] = JCodec[A]

  def compileCodec[A](schema: Schema[A]): JCodec[A] =
    schema.compile(schemaVisitorJCodec)

  def mediaType[A](codec: JCodec[A]): HttpMediaType.Type =
    HttpMediaType("application/json")

  override def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]] =
    try {
      Right {
        BodyPartial(
          com.github.plokhotnyuk.jsoniter_scala.core
            .readFromArray(bytes, readerConfig)(codec.messageCodec)
        )
      }
    } catch {
      case e: PayloadError => Left(e)
    }

  override def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]] = {
    try {
      Right {
        BodyPartial(
          com.github.plokhotnyuk.jsoniter_scala.core
            .readFromByteBuffer(bytes, readerConfig)(codec.messageCodec)
        )
      }
    } catch {
      case e: PayloadError => Left(e)
    }
  }

  override def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
    com.github.plokhotnyuk.jsoniter_scala.core.writeToArray(value)(codec)

}

object JsonCodecAPI {

  private val defaultReaderConfig: ReaderConfig = ReaderConfig
    .withAppendHexDumpToParseException(true)
    .withCheckForEndOfInput(false)
  // .withThrowReaderExceptionWithStackTrace(true)

}
