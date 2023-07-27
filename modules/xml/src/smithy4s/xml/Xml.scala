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
import cats.effect.SyncIO
import cats.syntax.all._

object Xml {

  private val decoderCacheGlobal = XmlDocument.Decoder.createCache()
  private val encoderCacheGlobal = XmlDocument.Encoder.createCache()

  private def deriveXmlDecoder[A: Schema]: XmlDocument.Decoder[A] =
    XmlDocument.Decoder.fromSchema(Schema[A], decoderCacheGlobal)

  private def deriveXmlEncoder[A: Schema]: XmlDocument.Encoder[A] =
    XmlDocument.Encoder.fromSchema(Schema[A], encoderCacheGlobal)

  /**
    * Parses an [[XmlDocument]] from a [[Blob]] inside of an
    * fs2.Stream.
    */
  def readToDocumentStream[F[_]: RaiseThrowable](
      blob: Blob
  ): Stream[F, XmlDocument] =
    Stream
      .emit(blob.toUTF8String)
      .through(events[F, String]())
      .through(documents[F, XmlDocument])

  /**
    * Reads an instance of `A` from a [[Blob]] inside of an
    * fs2.Stream.
    * 
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def readToStream[F[_]: RaiseThrowable, A: Schema](
      blob: Blob
  ): Stream[F, A] = {
    val decoder = deriveXmlDecoder[A]
    readToDocumentStream[F](blob).flatMap(document =>
      Stream.fromEither(decoder.decode(document))
    )
  }

  /**
    * Reads an instance of `A` from a [[smithy4s.Blob]] holding an XML payload.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def read[A: Schema](blob: Blob): Either[XmlDecodeError, A] = {
    readToStream[SyncIO, A](blob)
      .take(1)
      .compile
      .last
      .flatMap(
        SyncIO.fromOption(_)(
          new XmlDecodeError(XPath(List.empty), "Unable to decode XML input")
        )
      )
      .attempt
      .unsafeRunSync()
      .leftMap {
        case x: XmlDecodeError => x
        case other => new XmlDecodeError(XPath(List.empty), other.getMessage)
      }
  }

  /**
    * Writes the XML representation for an instance of `A` into an [[fs2.Stream]] of String.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def writeToString[A: Schema](a: A): Stream[fs2.Pure, String] = {
    val xmlDocument = deriveXmlEncoder[A].encode(a)

    XmlDocument.documentEventifier
      .eventify(xmlDocument)
      .through(render(collapseEmpty = false))
  }

  /**
    * Writes the XML representation for an instance of `A` into an [[fs2.Stream]] of Byte.
    *
    * Beware : using this method with a non-static schema (for instance, dynamically generated) may
    * result in memory leaks.
    */
  def writeToBytes[A: Schema](a: A): Stream[fs2.Pure, Byte] =
    writeToString[A](a).through(fs2.text.utf8.encode[fs2.Pure])

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

}
