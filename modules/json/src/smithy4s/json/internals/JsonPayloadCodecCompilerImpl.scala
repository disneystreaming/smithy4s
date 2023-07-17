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

  type Cache = jsoniterCodecCompiler.Cache
  def createCache(): Cache = jsoniterCodecCompiler.createCache()

  def fromSchema[A](schema: Schema[A], cache: Cache): PayloadCodec[A] = {
    val jcodec = jsoniterCodecCompiler.fromSchema(schema, cache)
    val reader: PayloadReader[A] = new JsonPayloadReader(jcodec)
    val writer: PayloadWriter[A] =
      schema.hints.get(smithy.api.HttpPayload) match {
        case Some(_) =>
          Writer.encodeBy { (value: A) =>
            val intermediate = Blob(
              writeToArray(value, jsoniterWriterConfig)(jcodec)
            )
            if (intermediate.sameBytesAs(Blob("null"))) Blob("")
            else intermediate
          }
        case None =>
          Writer.encodeBy { (value: A) =>
            val intermediate = Blob(
              writeToArray(value, jsoniterWriterConfig)(jcodec)
            )
            if (intermediate.sameBytesAs(Blob("null"))) Blob("{}")
            else intermediate
          }
      }
    ReaderWriter(reader, writer)
  }

  def fromSchema[A](schema: Schema[A]): PayloadCodec[A] =
    fromSchema(schema, createCache())

  private class JsonPayloadReader[A](jcodec: JsonCodec[A])
      extends PayloadReader[A] {
    def read(blob: Blob): Either[PayloadError, A] = {
      val nonEmpty =
        if (blob.isEmpty) "{}".getBytes
        else blob.toArray
      try {
        Right {
          readFromArray(nonEmpty, jsoniterReaderConfig)(jcodec)
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
