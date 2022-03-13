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

package smithy4s.http

import smithy4s.ByteArray
import smithy4s.Schema
import smithy4s.Constraints
import smithy4s.PayloadPath
import smithy4s.example._

import java.nio.ByteBuffer

object StringAndBlobSpec extends weaver.FunSuite {

  object Dummy
  def dummy: CodecAPI = new CodecAPI {
    type Codec[A] = Dummy.type
    def mediaType[A](codec: Codec[A]): HttpMediaType = HttpMediaType("foo/bar")
    def compileCodec[A](
        schema: Schema[A]
    ): Codec[A] = Dummy
    def decodeFromByteArrayPartial[A](
        codec: Codec[A],
        bytes: Array[Byte]
    ): Either[PayloadError, BodyPartial[A]] = Left(
      PayloadError(PayloadPath.root, "error", "error")
    )
    def decodeFromByteBufferPartial[A](
        codec: Codec[A],
        bytes: ByteBuffer
    ): Either[PayloadError, BodyPartial[A]] = Left(
      PayloadError(PayloadPath.root, "error", "error")
    )
    def writeToArray[A](codec: Codec[A], value: A): Array[Byte] = Array.empty
  }

  val stringsAndBlobs =
    CodecAPI.nativeStringsAndBlob(dummy, Constraints.defaultConstraints)

  test("Strings") {
    val input = StringBody("hello")
    val codec = stringsAndBlobs.compileCodec(StringBody.schema)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.sameElements("hello".getBytes())) &&
    expect.same(Right(input), roundTripped) &&
    expect.same(HttpMediaType("text/plain"), mediaType)
  }

  test("Strings (custom media-type)") {
    val input = CSVBody(CSV("hello"))
    val codec = stringsAndBlobs.compileCodec(CSVBody.schema)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.sameElements("hello".getBytes())) &&
    expect.same(Right(input), roundTripped) &&
    expect.same(HttpMediaType("text/csv"), mediaType)
  }

  test("Blobs") {
    val input = BlobBody(ByteArray("hello".getBytes()))
    val codec = stringsAndBlobs.compileCodec(BlobBody.schema)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.sameElements("hello".getBytes())) &&
    expect.same(Right(input), roundTripped) &&
    expect.same(HttpMediaType("application/octet-stream"), mediaType)
  }

  test("Blobs (custom media-type)") {
    val input = PNGBody(PNG(ByteArray("hello".getBytes())))
    val codec = stringsAndBlobs.compileCodec(PNGBody.schema)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.sameElements("hello".getBytes())) &&
    expect.same(Right(input), roundTripped) &&
    expect.same(HttpMediaType("image/png"), mediaType)
  }

  test("Delegates to some other codec when neither strings not bytes") {
    val input = 1
    val codec =
      stringsAndBlobs.compileCodec(smithy4s.schema.syntax.int)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.isEmpty) &&
    expect.same(
      Left(PayloadError(PayloadPath.root, "error", "error")),
      roundTripped
    ) &&
    expect.same(HttpMediaType("foo/bar"), mediaType)
  }

}
