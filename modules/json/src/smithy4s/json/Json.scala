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
import smithy4s.schema.Schema
import com.github.plokhotnyuk.jsoniter_scala.core._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.PayloadWriter
import smithy4s.codecs.PayloadReader

object Json {

  val NullBlob = Blob("null")
  val EmptyObjectBlob = Blob("{}")

  /**
    * Reads an instance of `A` from a [[smithy4s.Blob]] holding a json payload.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def read[A: Schema](blob: Blob): Either[PayloadError, A] = {
    payloadReaders
      .fromSchema(Schema[A], payloadReadersGlobalCache)
      .decode(blob)
  }

  /**
    * Writes the json representation for an instance of `A` into a [[smithy4s.Blob]].
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    *
    * When writing interpreters, please prefer using the [[payloadCodecs]] object.
    */
  def writeBlob[A: Schema](a: A): Blob = {
    payloadWriters
      .fromSchema(Schema[A], payloadWritersGlobalCache)
      .encode(a)
  }

  /**
    * Writes the json representation for an instance of `A` into a 2-spaces-indented string.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    *
    * When writing interpreters, please prefer using the [[payloadCodecs]] object.
    */
  def writePrettyString[A: Schema](a: A): String = {
    payloadCodecs
      .withJsoniterWriterConfig(WriterConfig.withIndentionStep(2))
      .writers
      .fromSchema(Schema[A])
      .encode(a)
      .toUTF8String
  }

  /**
    * Parses a [[smithy4s.Document]] from a [[smithy4s.Blob]] containing a Json payload.
    */
  def readDocument(blob: Blob): Either[PayloadError, Document] = {
    documentReader.read(blob)
  }

  /**
    * Parses a [[smithy4s.Document]] from a [[String]] containing a Json payload.
    */
  def readDocument(string: String): Either[PayloadError, Document] = {
    documentReader.read(Blob(string))
  }

  /**
    * Parses a [[smithy4s.Document]] from a [[Array[Byte]]] containing a Json payload.
    */
  def readDocument(bytes: Array[Byte]): Either[PayloadError, Document] = {
    documentReader.read(Blob(bytes))
  }

  /**
    * Writes a [[smithy4s.Document]] into a binary Blob.
    */
  def writeDocumentAsBlob(document: Document): Blob = {
    documentWriter.encode(document)
  }

  /**
    * Writes a [[smithy4s.Document]] into a pretty string with a 2-spaces indentation
    */
  def writeDocumentAsPrettyString(document: Document): String = {
    prettyDocumentWriters.encode(document).toUTF8String
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

  private[smithy4s] val payloadWriters: CachedSchemaCompiler[PayloadWriter] =
    payloadCodecs.writers

  private[smithy4s] val payloadReaders: CachedSchemaCompiler[PayloadReader] =
    payloadCodecs.readers

  private val payloadWritersGlobalCache = payloadWriters.createCache()
  private val payloadReadersGlobalCache = payloadReaders.createCache()

  private val documentWriter: PayloadWriter[Document] =
    payloadWriters.fromSchema(Schema.document)

  private val documentReader: PayloadReader[Document] =
    payloadReaders.fromSchema(Schema.document)

  private val prettyDocumentWriters: PayloadWriter[Document] = {
    import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
    payloadCodecs
      .withJsoniterWriterConfig(WriterConfig.withIndentionStep(2))
      .writers
      .fromSchema(Schema.document)
  }

}
