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

package smithy4s.grpc

import alloy.proto.GrpcStatusCode
import scala.util.control.NoStackTrace

sealed abstract class GrpcFailure(val status: GrpcStatusCode, val message: String)
  extends Throwable(message)
    with NoStackTrace

object GrpcFailure {

  final case class InvalidFrame(errorMessage: String)
      extends GrpcFailure(GrpcStatusCode.INVALID_ARGUMENT, errorMessage)

  final case class UnsupportedCompression(encoding: String)
      extends GrpcFailure(GrpcStatusCode.UNIMPLEMENTED, s"Unsupported gRPC compression: ${encoding}")

  case object MissingCompressionEncoding
      extends GrpcFailure(GrpcStatusCode.INVALID_ARGUMENT, "Missing gRPC compression encoding")

  case object Http2Required extends GrpcFailure(
    GrpcStatusCode.UNIMPLEMENTED,
    "gRPC requires HTTP/2"
  )

  final case class Unimplemented(override val message: String) extends GrpcFailure(GrpcStatusCode.UNIMPLEMENTED, message)
}

final case class InvalidStatus(value: String) extends Throwable(s"Received invalid gRPC status ${value}") with NoStackTrace

case object MissingTrailers extends Throwable("Response is missing gRPC trailers") with NoStackTrace

final case class StatusCodeMismatch(grpcStatus: Int, detailsStatus: Int) 
  extends Throwable(s"Status code mismatch grpc-status=${grpcStatus}; grpc-status-details-bin = ${detailsStatus}")
  with NoStackTrace
