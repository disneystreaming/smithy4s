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

import smithy4s.schema.SchemaVisitor
import smithy4s.schema.CompilationCache

import java.nio.ByteBuffer

abstract class JsonCodecAPI(
    makeVisitor: CompilationCache[JCodec] => SchemaVisitor[JCodec],
    hintMask: Option[HintMask],
    readerConfig: ReaderConfig = JsonCodecAPI.defaultReaderConfig,
    writerConfig: WriterConfig = WriterConfig
) extends CodecAPI {

  def this(
      schemaVisitorJCodec: SchemaVisitor[JCodec],
      hintMask: Option[HintMask],
      readerConfig: ReaderConfig,
      writerConfig: WriterConfig
  ) = this(
    (_: CompilationCache[JCodec]) => schemaVisitorJCodec,
    hintMask,
    readerConfig,
    writerConfig
  )

  type Cache = CompilationCache[JCodec]
  type Codec[A] = JCodec[A]

  def createCache(): Cache = CompilationCache.make[JCodec]

  def compileCodec[A](schema0: Schema[A], cache: Cache): JCodec[A] = {
    val schema =
      hintMask
        .map(mask => schema0.transformHintsTransitively(mask.apply))
        .getOrElse(schema0)
    schema.compile(makeVisitor(cache))
  }

  def mediaType[A](codec: JCodec[A]): HttpMediaType.Type =
    HttpMediaType("application/json")

  override def decodeFromByteArray[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, A] =
    try {
      Right {
        com.github.plokhotnyuk.jsoniter_scala.core
          .readFromArray(bytes, readerConfig)(codec)
      }
    } catch {
      case e: PayloadError => Left(e)
    }

  override def decodeFromByteBuffer[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, A] = {
    try {
      Right {
        com.github.plokhotnyuk.jsoniter_scala.core
          .readFromByteBuffer(bytes, readerConfig)(codec)
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
