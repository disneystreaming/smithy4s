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

import java.nio.ByteBuffer

import internals.StringAndBlobCodecSchemaVisitor

/**
  * An abstraction exposing serialisation functions to decode from bytes /
  * encode to bytes, based on a path-dependant Codec types.
  *
  * Only used in unary request/response patterns.
  */
trait CodecAPI {

  type Codec[A]
  type Cache

  def createCache(): Cache

  def mediaType[A](codec: Codec[A]): HttpMediaType

  /**
    * Turns a Schema into this API's preferred representation.
    *
    * @param schema the value's schema
    * @return the codec associated to the A value.
    */
  final def compileCodec[A](schema: Schema[A]): Codec[A] =
    compileCodec(schema, createCache())

  /**
    * Turns a Schema into this API's preferred representation.
    *
    * @param schema the value's schema
    * @param cache a Cache that can be used to optimise the compilation of Schemas
    *   into Codecs.
    * @return the codec associated to the A value.
    */
  def compileCodec[A](schema: Schema[A], cache: Cache): Codec[A]

  /**
    * Decodes data from a byte array
    *
    * @param codec the implementation-specific codec type
    * @param bytes an byte array
    * @return either a PayloadError, or the data
    */
  def decodeFromByteArray[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, A]

  /**
    * Decodes data from a byte buffer
    *
    * @param codec the implementation-specific codec type
    * @param bytes an byte array
    * @return either a PayloadError, or the data
    */
  def decodeFromByteBuffer[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, A]

  /**
    * Writes data to a byte array. Field values bound
    * to http metadata (path/query/headers) must be eluded.
    *
    * @param codec the implementation-specific codec
    * @param value the value to encode
    */
  def writeToArray[A](codec: Codec[A], value: A): Array[Byte]

}

object CodecAPI {

  trait Codec[A] { self =>
    def mediaType: HttpMediaType

    def decodeFromByteArray(
        bytes: Array[Byte]
    ): Either[PayloadError, A]

    def decodeFromByteBuffer(
        bytes: ByteBuffer
    ): Either[PayloadError, A]

    def writeToArray(value: A): Array[Byte]

    def imap[B](to: A => B, from: B => A): Codec[B] = new Codec[B] {
      def mediaType: HttpMediaType = self.mediaType

      def decodeFromByteArray(
          bytes: Array[Byte]
      ): Either[PayloadError, B] =
        self.decodeFromByteArray(bytes).map(to)

      def decodeFromByteBuffer(
          bytes: ByteBuffer
      ): Either[PayloadError, B] =
        self.decodeFromByteBuffer(bytes).map(to)

      def writeToArray(value: B): Array[Byte] = self.writeToArray(from(value))
    }

    def xmap[B](to: A => Either[ConstraintError, B], from: B => A): Codec[B] = {
      // TODO, this is a hack that will be removed when we get around to rewriting the current
      // CodecAPI implementations using `SchemaVisitor` instead of `Schematic`
      def adapted(a: A): B = to(a) match {
        case Left(e)  => throw e
        case Right(b) => b
      }
      imap(adapted, from)
    }
  }

  abstract class DelegatingCodecAPI extends CodecAPI {
    type Codec[A] = CodecAPI.Codec[A]

    def mediaType[A](codec: Codec[A]): HttpMediaType = codec.mediaType

    def decodeFromByteArray[A](
        codec: Codec[A],
        bytes: Array[Byte]
    ): Either[PayloadError, A] =
      codec.decodeFromByteArray(bytes)

    def decodeFromByteBuffer[A](
        codec: Codec[A],
        bytes: ByteBuffer
    ): Either[PayloadError, A] =
      codec.decodeFromByteBuffer(bytes)

    def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
      codec.writeToArray(value)

  }

  /**
    * Creates special cases for String and Blobs so that they are encoded/decoded respectively
    * as plaintext or binary (as opposed to json strings and base64 encoded json strings,
    * for instance)
    */
  def nativeStringsAndBlob(
      underlying: CodecAPI
  ): CodecAPI =
    new DelegatingCodecAPI {

      type Cache = underlying.Cache
      def createCache(): Cache = underlying.createCache()

      def compileCodec[A](
          schema: Schema[A],
          cache: Cache
      ): this.Codec[A] = {
        val stringAndBlobResult =
          schema.compile(new internals.StringAndBlobCodecSchemaVisitor())
        stringAndBlobResult match {
          case StringAndBlobCodecSchemaVisitor.SimpleCodecResult(bodyCodec) =>
            bodyCodec
          case _ =>
            val underlyingCodec = underlying.compileCodec(schema, cache)
            new this.Codec[A] {
              def mediaType: HttpMediaType =
                underlying.mediaType(underlyingCodec)

              def decodeFromByteArray(
                  bytes: Array[Byte]
              ): Either[PayloadError, A] =
                underlying.decodeFromByteArray(underlyingCodec, bytes)

              def decodeFromByteBuffer(
                  bytes: ByteBuffer
              ): Either[PayloadError, A] =
                underlying.decodeFromByteBuffer(underlyingCodec, bytes)

              def writeToArray(value: A): Array[Byte] =
                underlying.writeToArray(underlyingCodec, value)
            }
        }

      }
    }

}
