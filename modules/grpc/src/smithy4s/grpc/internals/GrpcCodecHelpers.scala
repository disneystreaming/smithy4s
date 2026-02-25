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

package smithy4s.grpc.internals

import alloy.proto.GrpcStatusCode
import cats.effect.Concurrent
import smithy4s.Blob
import smithy4s.grpc._

private[grpc] object GrpcCodecHelpers {

  val successTrailers: GrpcMetadata =
    GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, GrpcStatusCode.OK.intValue.toString)

  val internalErrorTrailers: GrpcMetadata =
    GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, GrpcStatusCode.INTERNAL.intValue.toString)

  def encodeUnaryFrame(payload: Blob): Blob =
    Blob(GrpcFrame.encode(GrpcFrame(compressed = false, payload)).toArray)

  def buildTrailers(
      status: Int,
      message: Option[String],
      payload: Option[ErrorPayload],
      encodePayload: ErrorPayload => Blob
  ): GrpcMetadata = {
    val base = GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, status.toString)
    val withMessage = message.filter(_.nonEmpty) match {
      case Some(value) => base.addText(GrpcConstants.grpcMessageHeader, GrpcMessageEncoding.encode(value))
      case None        => base
    }
    payload match {
      case Some(details) => withMessage.addBinary(GrpcConstants.grpcStatusDetailsHeader, encodePayload(details))
      case None          => withMessage
    }
  }

  def ensureTrailers[F[_]](
      response: GrpcResponse[Blob],
      strictTrailers: Boolean
  )(implicit F: Concurrent[F]): F[Unit] =
    F.raiseWhen(strictTrailers && response.trailers.getText(GrpcConstants.grpcStatusHeader).isEmpty)(
      MissingTrailers
    )
}
