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

package smithy4s.xml

import smithy4s.Blob
import smithy4s.schema.Schema
import fs2._
import fs2.data.xml._
import fs2.data.xml.dom._
import cats.syntax.all._
import cats.effect.Concurrent
import smithy4s.codecs._

object Xml {

  /**
    * Reads an instance of `A` from a [[smithy4s.Blob]] holding an XML payload.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def read[A: Schema](blob: Blob): Either[XmlDecodeError, A] =
    readToStream[fs2.Fallible, A](blob).compile.onlyOrError
      .leftMap {
        case x: XmlDecodeError => x
        case other => new XmlDecodeError(XPath(List.empty), other.getMessage)
      }

  /**
    * Writes the XML representation for an instance of `A` into a [[smithy4s.Blob]].
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def write[A: Schema](a: A): Blob = {
    val result = writeToBytes[A](a).compile
      .to(Collector.supportsArray(Array))

    Blob(result)
  }

  /**
    * Writes the XML representation for an instance of `A` into a String.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def writeToString[A: Schema](a: A): Option[String] =
    writeToStringStream[A](a).compile.last

  /**
    * Byte Stream Encoder Compiler made accessible to reduce repetition of Xml handling
    * in interpreters.
    */
  def xmlByteStreamEncoders[F[_]]: XmlByteStreamEncoderCompiler[F] =
    new smithy4s.xml.internals.XmlByteStreamEncoderCompilerImpl[F]()

  /**
    * Byte Stream Decoder Compiler made accessible to reduce repetition of Xml handling
    * in interpreters.
    */
  def xmlByteStreamDecoders[F[_]: Concurrent]: XmlByteStreamDecoderCompiler[F] =
    new smithy4s.xml.internals.XmlByteStreamDecoderCompilerImpl[F]()

  // TODO move the implementation of this
  val readers: BlobDecoder.Compiler = new BlobDecoder.Compiler {
    type Cache = XmlDocument.Decoder.Cache
    def createCache(): Cache = XmlDocument.Decoder.createCache()
    def fromSchema[A](schema: Schema[A], cache: Cache): BlobDecoder[A] = {
      val xmlDocumentDecoder = XmlDocument.Decoder.fromSchema(schema, cache)
      new BlobDecoder[A] {
        def read(blob: Blob): Either[PayloadError, A] =
          readToDocumentStream[fs2.Fallible](blob).compile.onlyOrError
            .leftMap { error =>
              new XmlDecodeError(XPath(List.empty), error.getMessage)
            }
            .flatMap(xmlDocumentDecoder.decode(_))
            .leftMap { case XmlDecodeError(xPath, message) =>
              PayloadError(xPath.toPayloadPath, "", message)
            }
      }
    }
    def fromSchema[A](schema: Schema[A]): BlobDecoder[A] =
      fromSchema(schema, createCache())
  }

  val writers: BlobEncoder.Compiler =
    Xml.xmlByteStreamEncoders[fs2.Pure].mapK {
      smithy4s.codecs.Writer
        .addingTo[Any]
        .andThenK { (stream: Stream[Pure, Byte]) =>
          val bytes = stream.compile.to(fs2.Chunk)
          Blob(bytes.toArray)
        }
    }

  private val decoderCacheGlobal = XmlDocument.Decoder.createCache()
  private val encoderCacheGlobal = XmlDocument.Encoder.createCache()

  private def deriveXmlDecoder[A: Schema]: XmlDocument.Decoder[A] =
    XmlDocument.Decoder.fromSchema(Schema[A], decoderCacheGlobal)

  private def deriveXmlEncoder[A: Schema]: XmlDocument.Encoder[A] =
    XmlDocument.Encoder.fromSchema(Schema[A], encoderCacheGlobal)

  private def readToDocumentStream[F[_]: RaiseThrowable](
      blob: Blob
  ): Stream[F, XmlDocument] =
    Stream
      .emit(blob.toUTF8String)
      .through(events[F, String]())
      .through(documents[F, XmlDocument])

  private def readToStream[F[_]: RaiseThrowable, A: Schema](
      blob: Blob
  ): Stream[F, A] = {
    val decoder = deriveXmlDecoder[A]
    readToDocumentStream[F](blob).flatMap(document =>
      Stream.fromEither(decoder.decode(document))
    )
  }

  private def writeToStringStream[A: Schema](a: A): Stream[fs2.Pure, String] = {
    val xmlDocument = deriveXmlEncoder[A].encode(a)

    XmlDocument.documentEventifier
      .eventify(xmlDocument)
      .through(render(collapseEmpty = false))
  }

  private def writeToBytes[A: Schema](a: A): Stream[fs2.Pure, Byte] =
    writeToStringStream[A](a).through(fs2.text.utf8.encode[fs2.Pure])

}
