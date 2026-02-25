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

import alloy.proto.ProtoIndex
import smithy4s.Blob
import smithy4s.Schema
import smithy4s.schema.Schema.{blob, int, list, string, struct}

// Wire-compatible with google.protobuf.Any.
final case class StatusDetails(
    typeUrl: String,
    value: Blob
)

object StatusDetails {
  implicit val schema: Schema[StatusDetails] = {
    val typeUrl = string.required[StatusDetails]("typeUrl", _.typeUrl)
      .addHints(ProtoIndex(1))
    val value = blob.required[StatusDetails]("value", _.value)
      .addHints(ProtoIndex(2))
    struct(typeUrl, value)(StatusDetails.apply)
  }
}

// Wire-compatible with google.rpc.Status.
final case class ErrorPayload(
    code: Option[Int],
    message: Option[String],
    details: Option[List[StatusDetails]]
)

object ErrorPayload {
  implicit val schema: Schema[ErrorPayload] = {
    val code = int.optional[ErrorPayload]("code", _.code)
      .addHints(ProtoIndex(1))
    val message = string.optional[ErrorPayload]("message", _.message)
      .addHints(ProtoIndex(2))
    val details = list(StatusDetails.schema)
      .optional[ErrorPayload]("details", _.details)
      .addHints(ProtoIndex(3))
    struct(code, message, details)(ErrorPayload.apply)
  }
}
