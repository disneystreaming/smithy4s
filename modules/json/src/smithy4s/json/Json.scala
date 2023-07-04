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

package smithy4s.json

import smithy4s.Document
import smithy4s.Blob
import smithy4s.codecs.PayloadError
import smithy4s.codecs.PayloadCodec
import smithy4s.schema.Schema
import com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec

object Json {

  /**
    * Parses a [[smithy4s.Document]] from a [[smithy4s.Blob]] containing a Json payload.
    */
  def readDocument(blob: Blob): Either[PayloadError, Document] = {
    documentCodecs.reader.read(blob)
  }

  /**
    * Parses a [[smithy4s.Document]] from a [[String]] containing a Json payload.
    */
  def readDocument(string: String): Either[PayloadError, Document] = {
    documentCodecs.reader.read(Blob(string))
  }

  /**
    * Parses a [[smithy4s.Document]] from a [[Array[Byte]]] containing a Json payload.
    */
  def readDocument(bytes: Array[Byte]): Either[PayloadError, Document] = {
    documentCodecs.reader.read(Blob(bytes))
  }

  /**
    * Writes a [[smithy4s.Document]] into a binary Blob.
    */
  def writeDocumentAsBlob(document: Document): Blob = {
    documentCodecs.writer.encode(document)
  }

  /**
    * Writes a [[smithy4s.Document]] into a pretty string with a 2-spaces indentation
    */
  def writeDocumentAsPrettyString(document: Document): String = {
    prettyDocumentCodecs.writer.encode(document).toUTF8String
  }

  /**
    * Default jsoniter codec compiler, which can produce instances of [[com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec]]
    * from instances of [[smithy4s.schema.Schema]] (which are generated for all smithy data types)
    */
  val jsoniter: JsoniterCodecCompiler =
    internals.JsoniterCodecCompilerImpl.defaultJsoniterCodecCompiler

  private val jsoniterCodecGlobalCache = jsoniter.createCache()

  implicit def deriveJsonCodec[A: Schema]: JsonCodec[A] =
    jsoniter.fromSchema(implicitly[Schema[A]], jsoniterCodecGlobalCache)

  /**
    * Default payload codec compiler, which can produce instances of [[smithy4s.codec.PayloadCodec]]
    * from instances of [[smithy4s.schema.Schema]] (which are generated for all smithy data types). PayloadCodecs
    * can be used, for instance, in http-interpreters.
    */
  val payloadCodecs: JsonPayloadCodecCompiler =
    internals.JsonPayloadCodecCompilerImpl.defaultJsonPayloadCodecCompiler

  private val documentCodecs: PayloadCodec[Document] =
    payloadCodecs.fromSchema(Schema.document)

  private val prettyDocumentCodecs: PayloadCodec[Document] = {
    import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
    payloadCodecs
      .withJsoniterWriterConfig(WriterConfig.withIndentionStep(2))
      .fromSchema(Schema.document)
  }

}
