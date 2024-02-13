/*
 *  Copyright 2021-2023 Disney Streaming
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
package codecs

import smithy4s.Schema
import smithy4s.example._
import smithy4s.schema.CachedSchemaCompiler

class StringAndBlobSpec() extends munit.FunSuite {

  val error = PayloadError(PayloadPath.root, "error", "error")
  object DummyDecoderCompiler
      extends CachedSchemaCompiler.Impl[PayloadDecoder] {
    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): PayloadDecoder[A] =
      Decoder.static(Left(error): Either[PayloadError, A])
  }

  object DummyWriterCompiler extends CachedSchemaCompiler.Impl[PayloadEncoder] {
    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): PayloadEncoder[A] =
      Encoder.static(Blob.empty): Encoder[Blob, A]

  }

  val stringsAndBlobsDecoders = CachedSchemaCompiler.getOrElse(
    StringAndBlobCodecs.decoders,
    DummyDecoderCompiler
  )
  val stringsAndBlobsEncoders = CachedSchemaCompiler.getOrElse(
    StringAndBlobCodecs.encoders,
    DummyWriterCompiler
  )

  def check[A](
      schema: Schema[A],
      data: A,
      expectedEncoded: Blob
  ): Unit = {
    val writer = stringsAndBlobsEncoders.fromSchema(schema)
    val decoder = stringsAndBlobsDecoders.fromSchema(schema)
    val result = writer.encode(data)
    val roundTripped = decoder.decode(result)
    expect.same(result, expectedEncoded)
    expect.same(Right(data), roundTripped)
  }

  test("Strings") {
    check(Schema.string, "hello", Blob("hello"))
  }

  test("Strings (custom media-type)") {
    check(CSV.schema, CSV("hello"), Blob("hello"))
  }

  test("String Enum") {
    check(
      StringEnum.schema,
      StringEnum.INTERESTING,
      Blob("interesting")
    )
  }

  test("String Enum (custom media-type)") {
    check(
      AudioEnum.schema,
      AudioEnum.BASS,
      Blob("bass")
    )
  }

  test("Blobs") {
    check(
      Schema.blob,
      Blob("hello"),
      Blob("hello")
    )
  }

  test("Blobs (custom media-type)") {
    check(
      PNG.schema,
      PNG(Blob("hello")),
      Blob("hello")
    )
  }

  test("Delegates to some other codec when neither strings not bytes") {
    val writer = stringsAndBlobsEncoders.fromSchema(Schema.int)
    val decoder = stringsAndBlobsDecoders.fromSchema(Schema.int)
    val result = writer.encode(1)
    val roundTripped = decoder.decode(result)
    val expectedRoundTripped =
      Left(PayloadError(PayloadPath.root, "error", "error"))
    expect.same(result, Blob.empty)
    expect.same(roundTripped, expectedRoundTripped)
  }

}
