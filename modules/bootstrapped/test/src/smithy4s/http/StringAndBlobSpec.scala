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
  object DummyReaderCompiler extends CachedSchemaCompiler.Impl[HttpBodyReader] {
    def fromSchema[A](schema: Schema[A], cache: Cache): HttpBodyReader[A] =
      HttpMediaTyped(
        HttpMediaType("application/binary"),
        Reader.decodeStatic(Left(error): Either[PayloadError, A])
      )
  }

  object DummyWriterCompiler extends CachedSchemaCompiler.Impl[HttpBodyWriter] {
    def fromSchema[A](schema: Schema[A], cache: Cache): HttpBodyWriter[A] =
      HttpMediaTyped(
        HttpMediaType("application/binary"),
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
    val readerMediaType = writer.mediaType
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

  // test("String Enum (custom media-type)") {
  //   val input = AudioEnum.BASS
  //   val codec = stringsAndBlobs.compileCodec(AudioEnum.schema)
  //   val result = stringsAndBlobs.encode(codec, input)
  //   val roundTripped = stringsAndBlobs.decode(codec, result)
  //   val mediaType = stringsAndBlobs.mediaType(codec)
  //   expect(result == Blob("bass"))
  //   expect.same(Right(input), roundTripped)
  //   expect.same(HttpMediaType("audio/mpeg3"), mediaType)
  // }

  // test("Blobs") {
  //   val input = ByteArray("hello".getBytes())
  //   val codec = stringsAndBlobs.compileCodec(Schema.blob)
  //   val result = stringsAndBlobs.encode(codec, input)
  //   val roundTripped = stringsAndBlobs.decode(codec, result)
  //   val mediaType = stringsAndBlobs.mediaType(codec)
  //   expect.same(result, Blob("hello"))
  //   expect.same(Right(input), roundTripped)
  //   expect.same(HttpMediaType("application/octet-stream"), mediaType)
  // }

  // test("Blobs (custom media-type)") {
  //   val input = PNG(ByteArray("hello".getBytes()))
  //   val codec = stringsAndBlobs.compileCodec(PNG.schema)
  //   val result = stringsAndBlobs.encode(codec, input)
  //   val roundTripped = stringsAndBlobs.decode(codec, result)
  //   val mediaType = stringsAndBlobs.mediaType(codec)
  //   expect(result == Blob("hello"))
  //   expect.same(Right(input), roundTripped)
  //   expect.same(HttpMediaType("image/png"), mediaType)
  // }

  // test("Delegates to some other codec when neither strings not bytes") {
  //   val input = 1
  //   val codec =
  //     stringsAndBlobs.compileCodec(Schema.int)
  //   val result = stringsAndBlobs.encode(codec, input)
  //   val roundTripped = stringsAndBlobs.decode(codec, result)
  //   val mediaType = stringsAndBlobs.mediaType(codec)
  //   expect(result.isEmpty)
  //   expect.same(
  //     Left(PayloadError(PayloadPath.root, "error", "error")),
  //     roundTripped
  //   )
  //   expect.same(HttpMediaType("foo/bar"), mediaType)
  // }

}
