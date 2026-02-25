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

package smithy4s.grpc.http4s.internals

import cats.effect.Concurrent
import cats.syntax.all._
import fs2.{Chunk, Stream}
import org.http4s.{Header, HttpVersion, Method, Request, Response, Uri}
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import smithy4s.Blob
import smithy4s.client.UnaryLowLevelClient
import smithy4s.grpc._
import smithy4s.grpc.internals.GrpcConstants
import smithy4s.interopcats._

private[http4s] final case class GrpcHttp4sLowLevelClient[F[_]](
    client: Client[F],
    baseUri: Uri,
    requireHttp2: Boolean
)(implicit F: Concurrent[F])
    extends UnaryLowLevelClient[F, GrpcRequest[Blob], GrpcResponse[Blob]] {

  def run[Output](request: GrpcRequest[Blob])(
      cb: GrpcResponse[Blob] => F[Output]
  ): F[Output] = {
    val httpRequest = toHttpRequest(request)
    client.run(httpRequest).use { response =>
      fromHttpResponse(response).flatMap(cb)
    }
  }

  private def toHttpRequest(request: GrpcRequest[Blob]): Request[F] = {
    val uri = baseUri.withPath(Uri.Path.unsafeFromString(request.path.render))
    val metadataHeaders = GrpcHttp4sMetadata.toHeaders(request.headers)
    val timeoutHeader = request.timeout
      .map(t => Header.Raw(org.typelevel.ci.CIString(GrpcConstants.grpcTimeoutHeader), t.toHeader))
    val acceptEncodingHeader = request.acceptedCompressions match {
      case Nil => None
      case values =>
        Some(
          Header.Raw(
            org.typelevel.ci.CIString(GrpcConstants.grpcAcceptEncodingHeader),
            values.map(_.name).mkString(",")
          )
        )
    }
    val headers = acceptEncodingHeader match {
      case Some(h) => metadataHeaders.put(h)
      case None    => metadataHeaders
    }
    val headersWithTimeout = timeoutHeader match {
      case Some(h) => headers.put(h)
      case None    => headers
    }

    val framedBody = Stream
      .chunk(Chunk.array(request.message.toArray))

    Request[F](
      method = Method.POST,
      uri = uri,
      httpVersion = if (requireHttp2) HttpVersion.`HTTP/2` else HttpVersion.`HTTP/1.1`,
      headers = headersWithTimeout
    )
      .withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
      .putHeaders(Header.Raw(org.typelevel.ci.CIString(GrpcConstants.grpcEncodingHeader), request.compression.name))
      .putHeaders(Header.Raw(org.typelevel.ci.CIString("TE"), "trailers"))
      .withBodyStream(framedBody)
  }

  private def fromHttpResponse(response: Response[F]): F[GrpcResponse[Blob]] =
    if (requireHttp2 && response.httpVersion != HttpVersion.`HTTP/2`) {
      F.raiseError(GrpcFailure.Http2Required)
    } else {
      for {
        headers <- GrpcHttp4sMetadata.fromApplicationHeaders[F](response.headers)
        rawHeaders <- GrpcHttp4sMetadata.fromTrailers[F](response.headers)
        encodingHeader = response.headers
          .get(org.typelevel.ci.CIString(GrpcConstants.grpcEncodingHeader))
          .map(_.head.value)
          .getOrElse("identity")
        parsedEncoding = GrpcCompression.parseLenient(encodingHeader)
        bytes <- response.body.compile.toVector
        trailers <- response.trailerHeaders
        trailerMetadata <- GrpcHttp4sMetadata.fromTrailers[F](trailers)
        effectiveTrailers = {
          // gRPC allows Trailers-Only responses (single HEADERS with END_STREAM), which ember
          // surfaces as Response.headers while completing trailerHeaders as empty.
          // See https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md and
          // https://github.com/http4s/http4s/blob/v0.23.33/ember-core/shared/src/main/scala/org/http4s/ember/core/h2/H2Stream.scala#L226-L244
          // https://github.com/http4s/http4s/blob/v0.23.33/core/shared/src/main/scala/org/http4s/Message.scala#L200-L210
          if (trailerMetadata.nonEmpty) trailerMetadata
          else if (rawHeaders.getText(GrpcConstants.grpcStatusHeader).nonEmpty) {
            val grpcPrefix = smithy4s.http.CaseInsensitive("grpc-")
            val excluded = Set(
              smithy4s.http.CaseInsensitive(GrpcConstants.grpcEncodingHeader),
              smithy4s.http.CaseInsensitive(GrpcConstants.grpcAcceptEncodingHeader),
              smithy4s.http.CaseInsensitive(GrpcConstants.grpcTimeoutHeader)
            )
            rawHeaders.keys
              .filter(key => key.startsWith(grpcPrefix) && !excluded.contains(key))
              .foldLeft(GrpcMetadata.empty) { (acc, key) =>
                rawHeaders.get(key.toString).foldLeft(acc) { (acc2, value) =>
                  value match {
                    case GrpcMetadata.Value.Text(text) => acc2.addText(key.toString, text)
                    case GrpcMetadata.Value.Binary(bin) => acc2.addBinary(key.toString, bin)
                  }
                }
              }
          } else trailerMetadata
        }
      } yield GrpcResponse(
        message = Blob(bytes.toArray),
        headers = headers,
        trailers = effectiveTrailers,
        compression = parsedEncoding
      )
    }
}
