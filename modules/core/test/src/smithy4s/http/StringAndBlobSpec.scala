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

import java.nio.ByteBuffer

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
    def decodeFromByteArray[A](
        codec: Codec[A],
        bytes: Array[Byte]
    ): Either[PayloadError, A] = Left(
      PayloadError(PayloadPath.root, "error", "error")
    )
    def decodeFromByteBuffer[A](
        codec: Codec[A],
        bytes: ByteBuffer
    ): Either[PayloadError, A] = Left(
      PayloadError(PayloadPath.root, "error", "error")
    )
    def writeToArray[A](codec: Codec[A], value: A): Array[Byte] = Array.empty
  }

  val stringsAndBlobs =
    CodecAPI.nativeStringsAndBlob(dummy)

  private def compilePartial[A](
      schema: Schema[A]
  ): (HttpMediaType, A => Array[Byte], Array[Byte] => Either[Throwable, A]) = {
    val transform = smithy4s.internals.ToPartialSchema(
      _.instance.hints.has(smithy.api.HttpPayload),
      payload = true
    )
    transform(schema) match {
      case Wedge.Left(partialSchema) =>
        val codec = stringsAndBlobs.compileCodec(partialSchema)
        val write = (input: A) =>
          stringsAndBlobs.writeToArray(codec, PartialData.Total(input))
        val read = (array: Array[Byte]) =>
          stringsAndBlobs
            .decodeFromByteArray(codec, array)
            .map(PartialData.unsafeReconcile(_))
        val mediaType = stringsAndBlobs.mediaType(codec)
        (mediaType, write, read)
      case Wedge.Right(totalSchema) =>
        val codec = stringsAndBlobs.compileCodec(totalSchema)
        val write = (input: A) => stringsAndBlobs.writeToArray(codec, input)
        val read = (array: Array[Byte]) =>
          stringsAndBlobs
            .decodeFromByteArray(codec, array)
        val mediaType = stringsAndBlobs.mediaType(codec)
        (mediaType, write, read)
      case Wedge.Empty => fail("Could not compile a partial schema")
    }
  }

  def check[A: Schema](
      input: A,
      expectedBodyString: String,
      expectedMediaType: String
  ): Unit = {
    val (mediaType, write, read) = compilePartial(implicitly[Schema[A]])
    val result = write(input)
    val roundTripped = read(result)
    expect(result.sameElements(expectedBodyString.getBytes()))
    expect.same(Right(input), roundTripped)
    expect.same(HttpMediaType(expectedMediaType), mediaType)
  }

  test("Strings") {
    check(StringBody("hello"), "hello", "text/plain")
  }

  test("Strings (custom media-type)") {
    check(CSVBody(CSV("hello")), "hello", "text/csv")
  }

  test("Blobs") {
    check(
      BlobBody(ByteArray("hello".getBytes())),
      "hello",
      "application/octet-stream"
    )
  }

  test("Blobs (custom media-type)") {
    check(
      PNGBody(PNG(ByteArray("hello".getBytes()))),
      "hello",
      "image/png"
    )
  }

  test("Delegates to some other codec when neither strings not bytes") {
    val input = 1
    val codec =
      stringsAndBlobs.compileCodec(Schema.int)
    val result = stringsAndBlobs.writeToArray(codec, input)
    val roundTripped = stringsAndBlobs.decodeFromByteArray(codec, result)
    val mediaType = stringsAndBlobs.mediaType(codec)
    expect(result.isEmpty)
    expect.same(
      Left(PayloadError(PayloadPath.root, "error", "error")),
      roundTripped
    )
    expect.same(HttpMediaType("foo/bar"), mediaType)
  }

}
