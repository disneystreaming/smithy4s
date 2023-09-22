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
package json
package internals

import smithy4s.schema.CachedSchemaCompiler
import com.github.plokhotnyuk.jsoniter_scala.core.{
  ReaderConfig => JsoniterReaderConfig
}
import com.github.plokhotnyuk.jsoniter_scala.core.{
  WriterConfig => JsoniterWriterConfig
}
import com.github.plokhotnyuk.jsoniter_scala.core._

import smithy4s.codecs._

private[json] case class JsonPayloadCodecCompilerImpl(
    jsoniterCodecCompiler: JsoniterCodecCompiler,
    jsoniterReaderConfig: JsoniterReaderConfig,
    jsoniterWriterConfig: JsoniterWriterConfig
) extends JsonPayloadCodecCompiler {

  def withJsoniterCodecCompiler(
      jsoniterCodecCompiler: JsoniterCodecCompiler
  ): JsonPayloadCodecCompiler =
    copy(jsoniterCodecCompiler = jsoniterCodecCompiler)
  def withJsoniterReaderConfig(
      jsoniterReaderConfig: JsoniterReaderConfig
  ): JsonPayloadCodecCompiler =
    copy(jsoniterReaderConfig = jsoniterReaderConfig)
  def withJsoniterWriterConfig(
      jsoniterWriterConfig: JsoniterWriterConfig
  ): JsonPayloadCodecCompiler =
    copy(jsoniterWriterConfig = jsoniterWriterConfig)

  def writers: CachedSchemaCompiler[PayloadEncoder] =
    new CachedSchemaCompiler[PayloadEncoder] {
      type Cache = jsoniterCodecCompiler.Cache
      def createCache(): Cache = jsoniterCodecCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): PayloadEncoder[A] = {
        val jcodec = jsoniterCodecCompiler.fromSchema(schema, cache)
        (value: A) =>
          Blob(
            writeToArray(value, jsoniterWriterConfig)(jcodec)
          )
      }
      def fromSchema[A](schema: Schema[A]): PayloadEncoder[A] =
        fromSchema(schema, createCache())
    }

  def decoders: CachedSchemaCompiler[PayloadDecoder] =
    new CachedSchemaCompiler[PayloadDecoder] {
      type Cache = jsoniterCodecCompiler.Cache
      def createCache(): Cache = jsoniterCodecCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): PayloadDecoder[A] = {
        val jcodec = jsoniterCodecCompiler.fromSchema(schema, cache)
        new JsonPayloadDecoder(jcodec)
      }

      def fromSchema[A](schema: Schema[A]): PayloadDecoder[A] =
        fromSchema(schema, createCache())
    }

  private class JsonPayloadDecoder[A](jcodec: JsonCodec[A])
      extends PayloadDecoder[A] {
    def decode(blob: Blob): Either[PayloadError, A] = {
      try {
        Right {
          if (blob.isEmpty) readFromString("{}", jsoniterReaderConfig)(jcodec)
          else
            blob match {
              case b: Blob.ArraySliceBlob =>
                readFromSubArray(
                  b.arr,
                  b.offset,
                  b.offset + b.size,
                  jsoniterReaderConfig
                )(jcodec)
              case b: Blob.ByteBufferBlob =>
                readFromByteBuffer(b.buf, jsoniterReaderConfig)(jcodec)
              case other =>
                readFromArray(other.toArray, jsoniterReaderConfig)(jcodec)
            }
        }
      } catch {
        case e: PayloadError => Left(e)
      }
    }
  }

}

private[smithy4s] object JsonPayloadCodecCompilerImpl {

  private val defaultJsoniterReaderConfig: JsoniterReaderConfig =
    JsoniterReaderConfig
      .withAppendHexDumpToParseException(true)
      .withCheckForEndOfInput(false)

  val defaultJsonPayloadCodecCompiler = JsonPayloadCodecCompilerImpl(
    jsoniterCodecCompiler =
      JsoniterCodecCompilerImpl.defaultJsoniterCodecCompiler,
    jsoniterReaderConfig = defaultJsoniterReaderConfig,
    jsoniterWriterConfig = JsoniterWriterConfig
  )

}
