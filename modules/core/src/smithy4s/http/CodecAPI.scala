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

import internals.StringAndBlobCodecSchemaVisitor
import smithy4s.~>

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

  final def compileCodecK(cache: Cache): Schema ~> Codec =
    new (Schema ~> Codec) {
      def apply[A0](fa: Schema[A0]): Codec[A0] = compileCodec(fa, cache)
    }

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
    * Decodes data from a blob
    *
    * @param codec the implementation-specific codec type
    * @param bytes an byte array
    * @return either a PayloadError, or the data
    */
  def decode[A](codec: Codec[A], bytes: Blob): Either[PayloadError, A]

  /**
    * Writes data to a blob.
    *
    * @param codec the implementation-specific codec
    * @param value the value to encode
    */
  def encode[A](codec: Codec[A], value: A): Blob

}

object CodecAPI {

  trait Codec[A] { self =>
    def mediaType: HttpMediaType

    def decode(blob: Blob): Either[PayloadError, A]
    def encode(value: A): Blob

    def imap[B](to: A => B, from: B => A): Codec[B] = new Codec[B] {
      def mediaType: HttpMediaType = self.mediaType

      def decode(blob: Blob): Either[PayloadError, B] =
        self.decode(blob).map(to)

      def encode(value: B): Blob = self.encode(from(value))
    }

    def xmap[B](to: A => Either[ConstraintError, B], from: B => A): Codec[B] = {
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

    def decode[A](
        codec: Codec[A],
        blob: Blob
    ): Either[PayloadError, A] =
      codec.decode(blob)

    def encode[A](codec: Codec[A], value: A): Blob =
      codec.encode(value)

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
          case StringAndBlobCodecSchemaVisitor.BodyCodecResult(bodyCodec) =>
            bodyCodec
          case _ =>
            val underlyingCodec = underlying.compileCodec(schema, cache)
            new this.Codec[A] {
              def mediaType: HttpMediaType =
                underlying.mediaType(underlyingCodec)

              def decode(blob: Blob): Either[PayloadError, A] =
                underlying.decode(underlyingCodec, blob)

              def encode(value: A): Blob =
                underlying.encode(underlyingCodec, value)
            }
        }

      }
    }

}
