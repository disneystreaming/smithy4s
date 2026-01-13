/*
 *  Copyright 2021-2026 Disney Streaming
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
import smithy4s.example.content._
import cats.Id
import smithy4s.client.UnaryClientCodecs

final class ContentHeaderSpec extends FunSuite {

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
      .withMetadataEncoders(Metadata.Encoder)
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

  def extractContentTypeHeader(request: HttpRequest[Blob]): Option[String] = {
    request.headers
      .get(CaseInsensitive("Content-Type"))
      .flatMap(_.headOption)
  }

  test(
    "Content-Type header ignores provided requestMediaType when rawStringsAndBlobPayloads is true, rather header is derived from the input schema"
  ) {
    val codecsMake = baseBuilder
      .withRequestMediaType("application/json")
      .withRawStringsAndBlobsPayloads
      .build()

    val codec = codecsMake.apply[
      DefaultContentHeaderInput,
      Nothing,
      DefaultContentHeaderOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.DefaultContentHeader.schema
    )
    val request = codec.inputEncoder(DefaultContentHeaderInput("test data"))

    val contentTypeHeader = extractContentTypeHeader(request)

    assertEquals(contentTypeHeader, Some("text/plain"))
  }

  test(
    "Content-Type header is NOT set when body encoders are not configured (body is empty)"
  ) {
    val codecsMake = baseBuilder.build()

    val codec = codecsMake.apply[
      DefaultContentHeaderInput,
      Nothing,
      DefaultContentHeaderOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.DefaultContentHeader.schema
    )
    val request = codec.inputEncoder(DefaultContentHeaderInput("hello"))

    val contentTypeHeader = extractContentTypeHeader(request)
    // Without body encoders, the body stays empty, so no Content-Type
    assertEquals(contentTypeHeader, None)
  }

  test(
    "Content-Type header derives from input schema and uses the value from @mediaType, when rawStringsAndBlobPayloads is true and input has @mediaType applied"
  ) {

    val codec = codecWithRawStringsAndBlobsPayloads
      .apply[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing](
        ContentHeaderTestServiceOperation.XmlInput.schema
      )
    val request = codec.inputEncoder(XmlInputInput(XmlPayload("test")))

    val contentTypeHeader = extractContentTypeHeader(request)
    assertEquals(contentTypeHeader, Some("application/xml"))
  }

  test(
    "Content-Type header uses the default media type provided by the protocol, when rawStringsAndBlobPayloads is true but input has no @mediaType"
  ) {
    val codecsMake = codecWithRawStringsAndBlobsPayloads

    val codec = codecsMake.apply[
      BlobInputNoMediaTypeInput,
      Nothing,
      BlobInputNoMediaTypeOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.BlobInputNoMediaType.schema
    )
    val request =
      codec.inputEncoder(BlobInputNoMediaTypeInput(Blob("some data")))

    val contentTypeHeader = extractContentTypeHeader(request)
    // Blob without @mediaType defaults to "application/octet-stream"
    assertEquals(contentTypeHeader, Some("application/octet-stream"))
  }

  test(
    "Content-Type header correctly derives from input when input and output have different media types"
  ) {

    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      XmlInputJsonOutputInput,
      Nothing,
      XmlInputJsonOutputOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.XmlInputJsonOutput.schema
    )
    val request =
      codec.inputEncoder(XmlInputJsonOutputInput(XmlPayload("test")))

    val contentTypeHeader = extractContentTypeHeader(request)
    // Content-Type header should be derived from input (XmlPayload), not output (JsonPayload)
    assertEquals(contentTypeHeader, Some("application/xml"))
  }

  test("Content-Type header for Blob input with @mediaType") {

    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      BlobInputWithMediaTypeInput,
      Nothing,
      BlobInputWithMediaTypeOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.BlobInputWithMediaType.schema
    )
    val request =
      codec.inputEncoder(
        BlobInputWithMediaTypeInput(PngImage(Blob("fake png data")))
      )

    val contentTypeHeader = extractContentTypeHeader(request)
    assertEquals(contentTypeHeader, Some("image/png"))
  }

  test(
    "Content-Type header is NOT set for Blob input with @mediaType when body is empty"
  ) {
    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      BlobInputWithMediaTypeInput,
      Nothing,
      BlobInputWithMediaTypeOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.BlobInputWithMediaType.schema
    )
    val request =
      codec.inputEncoder(BlobInputWithMediaTypeInput(PngImage(Blob.empty)))

    val contentTypeHeader = extractContentTypeHeader(request)
    // Even though schema specifies @mediaType("image/png"), no Content-Type when body is empty
    assertEquals(contentTypeHeader, None)
  }

  test(
    "Content-Type header respects explicit @httpHeader(\"Content-Type\") over defaults"
  ) {
    val codec = codecWithRawStringsAndBlobsPayloads.apply[
      ExplicitContentTypeHeaderInput,
      Nothing,
      ExplicitContentTypeHeaderOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.ExplicitContentTypeHeader.schema
    )
    val request = codec.inputEncoder(
      ExplicitContentTypeHeaderInput(
        contentType = Some("application/custom-type"),
        data = Blob("test data")
      )
    )

    val contentTypeHeader = extractContentTypeHeader(request)
    // Explicit Content-Type from @httpHeader should override default behavior
    assertEquals(contentTypeHeader, Some("application/custom-type"))
  }

  test(
    "Content-Type header is not set when there is no body content (only metadata)"
  ) {
    val codecsMake = baseBuilder.withRawStringsAndBlobsPayloads.build()

    val codec = codecsMake.apply[
      NoBodyOperationInput,
      Nothing,
      NoBodyOperationOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.NoBodyOperation.schema
    )
    val request = codec.inputEncoder(NoBodyOperationInput(Some("test query")))

    val contentTypeHeader = extractContentTypeHeader(request)
    // No body content, so no Content-Type header should be set
    assertEquals(contentTypeHeader, None)
  }

  test(
    "Content-Type header is NOT set for empty struct with writeEmptyStructs=true but no JSON encoders"
  ) {
    val codecsMake =
      baseBuilder.withRawStringsAndBlobsPayloads // Provides String/Blob encoders, but not struct encoders
        .withWriteEmptyStructs(_ => true) // Enable writing empty structs
        .build()

    val codec = codecsMake.apply[
      EmptyStructOperationInput,
      Nothing,
      EmptyStructOperationOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.EmptyStructOperation.schema
    )
    val request = codec.inputEncoder(EmptyStructOperationInput())

    val contentTypeHeader = extractContentTypeHeader(request)
    // Without JSON encoders, empty struct writes empty Blob (not {}), so Content-Type is NOT set
    assertEquals(contentTypeHeader, None)
  }

  test(
    "Content-Type header is NOT set for empty struct when writeEmptyStructs is false"
  ) {
    val codecsMake =
      baseBuilder.withRawStringsAndBlobsPayloads // Same encoders as the true case
        .withWriteEmptyStructs(_ =>
          false
        ) // Explicitly disable (though this is the default)
        .build()

    val codec = codecsMake.apply[
      EmptyStructOperationInput,
      Nothing,
      EmptyStructOperationOutput,
      Nothing,
      Nothing
    ](
      ContentHeaderTestServiceOperation.EmptyStructOperation.schema
    )
    val request = codec.inputEncoder(EmptyStructOperationInput())

    val contentTypeHeader = extractContentTypeHeader(request)
    // Empty struct will NOT be serialized (no body), so Content-Type should NOT be set
    assertEquals(contentTypeHeader, None)
  }
}
