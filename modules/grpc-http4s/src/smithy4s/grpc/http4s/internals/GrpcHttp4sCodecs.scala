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

package smithy4s
package grpc
package http4s
package internals

import cats.effect.Concurrent
import cats.implicits._
import fs2.Stream
import org.http4s.{Header, MediaType, Request, Response, Status}
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import smithy4s.Blob
import smithy4s.grpc._
import smithy4s.grpc.internals.{ GrpcConstants }
import smithy4s.interopcats._

private[http4s] object GrpcHttp4sCodecs {

  private[http4s] val grpcMediaType: MediaType =
    MediaType.unsafeParse("application/grpc")
  private[http4s] val grpcProtoMediaType: MediaType =
    MediaType.unsafeParse("application/grpc+proto")
  private[http4s] val grpcContentTypes: Set[MediaType] =
    Set(grpcMediaType, grpcProtoMediaType)

  def toGrpcRequest[F[_]](
      request: Request[F],
      path: GrpcMethodPath,
      maxMessageSize: Int
  )(implicit F: Concurrent[F]): F[GrpcRequest[Blob]] = {
    val timeout = request.headers.get(CIString(GrpcConstants.grpcTimeoutHeader))
      .map(_.head.value)
      .flatMap(h => GrpcTimeout.parse(h).toOption)

    val encoding = request.headers.get(CIString(GrpcConstants.grpcEncodingHeader))
      .map(_.head.value)
      .getOrElse("identity")

    val acceptEncoding = request.headers
      .get(CIString(GrpcConstants.grpcAcceptEncodingHeader))
      .map(_.head.value)
      .toList
      .flatMap(_.split(',').toList.map(_.trim).filter(_.nonEmpty))

    val accepted = acceptEncoding.map(GrpcCompression.parseLenient)

    // We read at most 5 + maxMessageSize + 1 bytes: the gRPC framing header (5 bytes),
    // the largest valid payload, and one extra byte to detect truncation vs overflow
    // without buffering unbounded data into memory.
    val grpcHeaderSizeBytes = 5L
    val overflowProbeBytes  = 1L
    val maxBytesToRead      = grpcHeaderSizeBytes + maxMessageSize.toLong + overflowProbeBytes
    for {
      metadata <- GrpcHttp4sMetadata.fromApplicationHeaders[F](request.headers)
      bytes    <- request.body.take(maxBytesToRead).compile.toVector
    } yield GrpcRequest(
      message              = Blob(bytes.toArray),
      headers              = metadata,
      timeout              = timeout,
      compression          = GrpcCompression.parseLenient(encoding),
      acceptedCompressions = accepted,
      path                 = path
    )
  }

  def grpcResponseToHttp4s[F[_]](response: GrpcResponse[Blob])(implicit F: Concurrent[F]): F[Response[F]] = {
    val headers  = GrpcHttp4sMetadata.toHeaders(response.headers)
    val body     = Stream.chunk(fs2.Chunk.array(response.message.toArray)).covary[F]
    val trailers = GrpcHttp4sMetadata.toHeaders(response.trailers)
    val base = Response[F](status = Status.Ok, headers = headers)
      .withContentType(`Content-Type`(grpcProtoMediaType))
      .withBodyStream(body)
      .withTrailerHeaders(F.pure(trailers))
      .putHeaders(Header.Raw(CIString(GrpcConstants.grpcAcceptEncodingHeader), GrpcCompression.Identity.name))
    F.pure(base)
  }
}
