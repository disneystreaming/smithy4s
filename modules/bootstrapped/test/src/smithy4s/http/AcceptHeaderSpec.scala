/*
 *  Copyright 2021-2025 Disney Streaming
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

import munit._
import smithy4s._
import smithy4s.capability.MonadThrowLike
import smithy4s.example.accept._
import cats.Id
import smithy4s.client.UnaryClientCodecs

final class AcceptHeaderSpec extends FunSuite {

  implicit val F: MonadThrowLike[Id] = new MonadThrowLike[Id] {
    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)
    def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    def raiseError[A](e: Throwable): Id[A] = throw e
    def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] =
      try fa
      catch { case e: Throwable => f(e) }
    def pure[A](a: A): Id[A] = a
    def zipMapAll[A](seq: IndexedSeq[Id[Any]])(f: IndexedSeq[Any] => A): Id[A] =
      f(seq)
  }

  def baseBuilder: HttpUnaryClientCodecs.Builder[Id, HttpRequest[
    Blob
  ], HttpResponse[Blob]] =
    HttpUnaryClientCodecs
      .builder[Id]
      .withBaseRequest(_ =>
        HttpRequest(
          HttpMethod.POST,
          HttpUri.fromURI(new java.net.URI("/")),
          Map.empty,
          Blob.empty
        )
      )

  val codecWithRawStringsAndBlobsPayloads
      : UnaryClientCodecs.Make[Id, HttpRequest[Blob], HttpResponse[Blob]] =
    baseBuilder.withRawStringsAndBlobsPayloads.build()

  def extractAcceptHeader(request: HttpRequest[Blob]): Option[String] = {
    request.headers
      .get(CaseInsensitive("Accept"))
      .flatMap(_.headOption)
  }

  test(
    "Accept header uses provided acceptMediaType when rawStringsAndBlobPayloads is false"
  ) {
    val codecsMake = baseBuilder
      .withAcceptMediaType("application/json")
      .build()

    val codec = codecsMake.apply[
      DefaultAcceptHeaderInput,
      Nothing,
      DefaultAcceptHeaderOutput,
      Nothing,
      Nothing
    ](
      AcceptHeaderTestServiceOperation.DefaultAcceptHeader.schema
    )
    val request = codec.inputEncoder(DefaultAcceptHeaderInput())

    val acceptHeader = extractAcceptHeader(request)
    assertEquals(acceptHeader, Some("application/json"))
  }

  test(
    "Accept header uses default acceptMediaType (*/*) when no specific media type is set"
  ) {
    val codecsMake = baseBuilder.build()

    val codec = codecsMake.apply[
      DefaultAcceptHeaderInput,
      Nothing,
      DefaultAcceptHeaderOutput,
      Nothing,
      Nothing
    ](
      AcceptHeaderTestServiceOperation.DefaultAcceptHeader.schema
    )
    val request = codec.inputEncoder(DefaultAcceptHeaderInput())

    val acceptHeader = extractAcceptHeader(request)
    assertEquals(acceptHeader, Some("*/*"))
  }

  test(
    "Accept header derives from output schema and uses the value from @mediaType, when rawStringsAndBlobPayloads is true and output has @mediaType applied"
  ) {

    val codec = codecWithRawStringsAndBlobsPayloads
      .apply[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing](
        AcceptHeaderTestServiceOperation.XmlOutput.schema
      )
    val request = codec.inputEncoder(XmlOutputInput())

    val acceptHeader = extractAcceptHeader(request)
    assertEquals(acceptHeader, Some("application/xml"))
  }

  test(
    "Accept header uses the default media type provided by the protocol , when rawStringsAndBlobPayloads is true but output has no @mediaType"
  ) {
    val codecsMake = codecWithRawStringsAndBlobsPayloads

    val codec = codecsMake.apply[
      BlobOutputNoMediaTypeInput,
      Nothing,
      BlobOutputNoMediaTypeOutput,
      Nothing,
      Nothing
    ](
      AcceptHeaderTestServiceOperation.BlobOutputNoMediaType.schema
    )
    val request = codec.inputEncoder(BlobOutputNoMediaTypeInput())

    val acceptHeader = extractAcceptHeader(request)
    // Blob without @mediaType defaults to "application/octet-stream"
    assertEquals(acceptHeader, Some("application/octet-stream"))
  }

  test(
    "Accept header correctly derives from output when input and output have different media types"
  ) {

    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      JsonInputXmlOutputInput,
      Nothing,
      JsonInputXmlOutputOutput,
      Nothing,
      Nothing
    ](
      AcceptHeaderTestServiceOperation.JsonInputXmlOutput.schema
    )
    val request =
      codec.inputEncoder(JsonInputXmlOutputInput(Some(JsonPayload("test"))))

    val acceptHeader = extractAcceptHeader(request)
    // Accept header should be derived from output (XmlPayload), not input (JsonPayload)
    assertEquals(acceptHeader, Some("application/xml"))
  }

  test("Accept header for Blob output with @mediaType") {

    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      BlobOutputWithMediaTypeInput,
      Nothing,
      BlobOutputWithMediaTypeOutput,
      Nothing,
      Nothing
    ](
      AcceptHeaderTestServiceOperation.BlobOutputWithMediaType.schema
    )
    val request = codec.inputEncoder(BlobOutputWithMediaTypeInput())

    val acceptHeader = extractAcceptHeader(request)
    assertEquals(acceptHeader, Some("image/png"))
  }
}
