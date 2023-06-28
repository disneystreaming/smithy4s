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

import smithy4s.ByteArray
import smithy4s.PayloadPath
import smithy4s.Schema
import smithy4s.example._

class StringAndBlobSpec() extends munit.FunSuite {

  object Dummy
  def dummy: CodecAPI = new CodecAPI {
    type Codec[A] = Dummy.type
    type Cache = Dummy.type
    def createCache(): Dummy.type = Dummy
    def mediaType[A](codec: Codec[A]): HttpMediaType = HttpMediaType("foo/bar")
    def compileCodec[A](
        schema: Schema[A],
        cache: Cache
    ): Codec[A] = Dummy
    def decode[A](
        codec: Codec[A],
        blob: Blob
    ): Either[PayloadError, A] = Left(
      PayloadError(PayloadPath.root, "error", "error")
    )
    def encode[A](codec: Codec[A], value: A): Blob = Blob.empty
  }

  val stringsAndBlobs =
    CodecAPI.nativeStringsAndBlob(dummy)

  test("Strings") {
    val input = "hello"
    val codec = stringsAndBlobs.compileCodec(Schema.string)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect.same(result, Blob("hello"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("text/plain"), mediaType)
  }

  test("Strings (custom media-type)") {
    val input = CSV("hello")
    val codec = stringsAndBlobs.compileCodec(CSV.schema)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect.same(result, Blob("hello"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("text/csv"), mediaType)
  }
  test("String Enum") {
    val input = StringEnum.INTERESTING
    val codec = stringsAndBlobs.compileCodec(StringEnum.schema)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect.same(result, Blob("interesting"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("text/plain"), mediaType)
  }

  test("String Enum (custom media-type)") {
    val input = AudioEnum.BASS
    val codec = stringsAndBlobs.compileCodec(AudioEnum.schema)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result == Blob("bass"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("audio/mpeg3"), mediaType)
  }

  test("Blobs") {
    val input = ByteArray("hello".getBytes())
    val codec = stringsAndBlobs.compileCodec(Schema.blob)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect.same(result, Blob("hello"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("application/octet-stream"), mediaType)
  }

  test("Blobs (custom media-type)") {
    val input = PNG(ByteArray("hello".getBytes()))
    val codec = stringsAndBlobs.compileCodec(PNG.schema)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result == Blob("hello"))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType("image/png"), mediaType)
  }

  test("Delegates to some other codec when neither strings not bytes") {
    val input = 1
    val codec =
      stringsAndBlobs.compileCodec(Schema.int)
    val result = stringsAndBlobs.encode(codec, input)
    val roundTripped = stringsAndBlobs.decode(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.isEmpty)
    expect.same(
      Left(PayloadError(PayloadPath.root, "error", "error")),
      roundTripped
    )
    expect.same(HttpMediaType("foo/bar"), mediaType)
  }

}
