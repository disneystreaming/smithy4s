/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.xml
package internals

import fs2.Collector
import smithy4s.Blob
import smithy4s.codecs.BlobEncoder
import smithy4s.schema.Schema

private[xml] class XmlPayloadEncoderCompilerImpl(escapeAttributes: Boolean)
    extends XmlPayloadEncoderCompiler {
  type Cache = XmlDocument.Encoder.Cache
  def createCache(): Cache = XmlDocument.Encoder.createCache()
  def fromSchema[A](schema: Schema[A], cache: Cache): BlobEncoder[A] = {
    val xmlDocumentEncoder = XmlDocument.Encoder.fromSchema(schema, cache)
    val eventifier = XmlDocument.makeDocumentEventifier(escapeAttributes)
    (a: A) =>
      Blob {
        eventifier
          .eventify(xmlDocumentEncoder.encode(a))
          .through(fs2.data.xml.render.raw(collapseEmpty = false))
          .through(fs2.text.utf8.encode[fs2.Pure])
          .compile
          .to(Collector.supportsArray(Array))
      }
  }
  def fromSchema[A](schema: Schema[A]): BlobEncoder[A] =
    fromSchema(schema, createCache())

  def withEscapeAttributes(
      escapeAttributes: Boolean
  ): XmlPayloadEncoderCompiler =
    new XmlPayloadEncoderCompilerImpl(escapeAttributes)
}
