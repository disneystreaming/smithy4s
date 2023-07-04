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

import smithy4s.codecs._
import smithy4s.Schema
import smithy4s.example._
import smithy4s.http.StringAndBlobCodecs
import smithy4s.schema.CachedSchemaCompiler

class StringAndBlobSpec() extends munit.FunSuite {

  val error =
    PayloadError(PayloadPath.root, "error", "error")
  object DummyReaderCompiler
      extends CachedSchemaCompiler.Impl[HttpMediaReader] {
    def fromSchema[A](schema: Schema[A], cache: Cache): HttpMediaReader[A] =
      HttpMediaTyped(
        HttpMediaType("foo/bar"),
        Reader.decodeStatic(Left(error): Either[PayloadError, A])
      )
  }

  object DummyWriterCompiler
      extends CachedSchemaCompiler.Impl[HttpMediaWriter] {
    def fromSchema[A](schema: Schema[A], cache: Cache): HttpMediaWriter[A] =
      HttpMediaTyped(
        HttpMediaType("foo/bar"),
        Writer.encodeStatic(Blob.empty)
      )

  }

  val stringsAndBlobsReaders = StringAndBlobCodecs.readerOr(DummyReaderCompiler)
  val stringsAndBlobsWriters = StringAndBlobCodecs.writerOr(DummyWriterCompiler)

  def check[A](
      schema: Schema[A],
      data: A,
      expectedEncoded: Blob,
      expectedMediaType: String
  ): Unit = {
    val writer = stringsAndBlobsWriters.fromSchema(schema)
    val reader = stringsAndBlobsReaders.fromSchema(schema)
    val result = writer.instance.encode(data)
    val roundTripped = reader.instance.read(result)
    val writerMediaType = writer.mediaType
    val readerMediaType = reader.mediaType
    expect.same(result, expectedEncoded)
    expect.same(Right(data), roundTripped)
    expect.same(writerMediaType, HttpMediaType(expectedMediaType))
    expect.same(readerMediaType, HttpMediaType(expectedMediaType))
  }

  test("Strings") {
    check(Schema.string, "hello", Blob("hello"), "text/plain")
  }

  test("Strings (custom media-type)") {
    check(CSV.schema, CSV("hello"), Blob("hello"), "text/csv")
  }

  test("String Enum") {
    check(
      StringEnum.schema,
      StringEnum.INTERESTING,
      Blob("interesting"),
      "text/plain"
    )
  }

  test("String Enum (custom media-type)") {
    check(
      AudioEnum.schema,
      AudioEnum.BASS,
      Blob("bass"),
      "audio/mpeg3"
    )
  }

  test("Blobs") {
    check(
      Schema.bytes,
      ByteArray("hello".getBytes()),
      Blob("hello"),
      "application/octet-stream"
    )
  }

  test("Blobs (custom media-type)") {
    check(
      PNG.schema,
      PNG(ByteArray("hello".getBytes())),
      Blob("hello"),
      "image/png"
    )
  }

  test("Delegates to some other codec when neither strings not bytes") {
    val writer = stringsAndBlobsWriters.fromSchema(Schema.int)
    val reader = stringsAndBlobsReaders.fromSchema(Schema.int)
    val result = writer.instance.encode(1)
    val roundTripped = reader.instance.read(result)
    val readerMediaType = reader.mediaType
    val writerMediaType = writer.mediaType
    val expectedRoundTripped =
      Left(PayloadError(PayloadPath.root, "error", "error"))
    expect.same(result, Blob.empty)
    expect.same(roundTripped, expectedRoundTripped)
    expect.same(writerMediaType, HttpMediaType("foo/bar"))
    expect.same(readerMediaType, HttpMediaType("foo/bar"))
  }

}
