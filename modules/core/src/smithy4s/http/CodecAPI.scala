/*
 *  Copyright 2021 Disney Streaming
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
import scala.collection.{Map => MMap}

import internals.StringAndBlobCodecSchematic

/**
  * An abstraction exposing serialisation functions to decode from bytes /
  * encode to bytes, based on a path-dependant Codec types.
  *
  * Only used in unary request/response patterns.
  */
trait CodecAPI {

  type Codec[A]

  def mediaType[A](codec: Codec[A]): HttpMediaType

  /**
    * Turns a Schema into this API's preferred representation.
    *
    * @param schema the value's schema
    * @return the codec associated to the A value.
    */
  def compileCodec[A](schema: Schema[A], hintMask: HintMask): Codec[A]

  /**
    * Decodes partial data from a byte array
    *
    * @param codec the implementation-specific codec type
    * @param bytes an byte array
    * @return either a PayloadError, or the partial data, which can be combined
    * with partial data coming from the metadata.
    */
  def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]]

  def decodeFromByteArray[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, A] =
    decodeFromByteArrayPartial(codec, bytes).map(_.complete(MMap.empty))

  /**
    * Decodes partial data from a byte buffer, returning a function that is able
    * to reconstruct the full data, provided a map resulting from the decoding
    * of the metadata.
    *
    * @param codec the implementation-specific codec
    * @param bytes a bytue buffer
    * @return either a PayloadError, or the partial data, which can be combined
    * with partial data coming from the metadata.
    */
  def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]]

  def decodeFromByteBuffer[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, A] =
    decodeFromByteBufferPartial(codec, bytes).map(_.complete(MMap.empty))

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

    def decodeFromByteArrayPartial(
        bytes: Array[Byte]
    ): Either[PayloadError, BodyPartial[A]]

    def decodeFromByteBufferPartial(
        bytes: ByteBuffer
    ): Either[PayloadError, BodyPartial[A]]

    def writeToArray(value: A): Array[Byte]

    def imap[B](to: A => B, from: B => A): Codec[B] = new Codec[B] {
      def mediaType: HttpMediaType = self.mediaType

      def decodeFromByteArrayPartial(
          bytes: Array[Byte]
      ): Either[PayloadError, BodyPartial[B]] =
        self.decodeFromByteArrayPartial(bytes).map(_.map(to))

      def decodeFromByteBufferPartial(
          bytes: ByteBuffer
      ): Either[PayloadError, BodyPartial[B]] =
        self.decodeFromByteBufferPartial(bytes).map(_.map(to))

      def writeToArray(value: B): Array[Byte] = self.writeToArray(from(value))
    }
  }

  abstract class DelegatingCodecAPI extends CodecAPI {
    type Codec[A] = CodecAPI.Codec[A]

    def mediaType[A](codec: Codec[A]): HttpMediaType = codec.mediaType

    def decodeFromByteArrayPartial[A](
        codec: Codec[A],
        bytes: Array[Byte]
    ): Either[PayloadError, BodyPartial[A]] =
      codec.decodeFromByteArrayPartial(bytes)

    def decodeFromByteBufferPartial[A](
        codec: Codec[A],
        bytes: ByteBuffer
    ): Either[PayloadError, BodyPartial[A]] =
      codec.decodeFromByteBufferPartial(bytes)

    def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
      codec.writeToArray(value)

  }

  /**
    * Creates special cases for String and Blobs so that they are encoded/decoded respectively
    * as plaintext or binary (as opposed to json strings and base64 encoded json strings,
    * for instance)
    */
  def nativeStringsAndBlob(
      underlying: CodecAPI,
      constraints: Constraints
  ): CodecAPI =
    new DelegatingCodecAPI {

      def compileCodec[A](
          schema: Schema[A],
          hintMask: HintMask
      ): this.Codec[A] = {
        val stringAndBlobResult = schema.compile(
          new internals.StringAndBlobCodecSchematic(constraints)
        )
        stringAndBlobResult.get match {
          case StringAndBlobCodecSchematic.BodyCodecResult(bodyCodec) =>
            bodyCodec
          case _ =>
            val underlyingCodec = underlying.compileCodec(schema, hintMask)
            new this.Codec[A] {
              def mediaType: HttpMediaType =
                underlying.mediaType(underlyingCodec)

              def decodeFromByteArrayPartial(
                  bytes: Array[Byte]
              ): Either[PayloadError, BodyPartial[A]] =
                underlying.decodeFromByteArrayPartial(underlyingCodec, bytes)

              def decodeFromByteBufferPartial(
                  bytes: ByteBuffer
              ): Either[PayloadError, BodyPartial[A]] =
                underlying.decodeFromByteBufferPartial(underlyingCodec, bytes)

              def writeToArray(value: A): Array[Byte] =
                underlying.writeToArray(underlyingCodec, value)
            }
        }

      }
    }

}
