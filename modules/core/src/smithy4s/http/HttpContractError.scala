/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.PayloadError
import smithy4s.codecs.PayloadPath
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Schema._
import smithy4s.schema._

sealed trait HttpContractError
    extends Throwable
    with scala.util.control.NoStackTrace

object HttpContractError {

  def fromPayloadError(payloadError: PayloadError): HttpContractError =
    HttpPayloadError(
      payloadError.path,
      payloadError.expected,
      payloadError.message
    )

  def fromPayloadErrorK[F[_]: MonadThrowLike]: PolyFunction[F, F] =
    MonadThrowLike.mapErrorK[F] { case e: PayloadError => fromPayloadError(e) }

  val schema: Schema[HttpContractError] = {
    val payload = HttpPayloadError.schema.oneOf[HttpContractError]("payload")
    val metadata = MetadataError.schema.oneOf[HttpContractError]("metadata")
    val upstreamServiceError = UpstreamServiceError.schema.oneOf[HttpContractError]("upstreamServiceError")
    union(payload, metadata, upstreamServiceError) {
      case _: HttpPayloadError => 0
      case _: MetadataError    => 1
      case _: UpstreamServiceError => 2
    }
  }

}

case class HttpPayloadError(
    path: PayloadPath,
    expected: String,
    message: String
) extends HttpContractError {
  override def toString(): String =
    s"HttpPayloadError($path, expected = $expected, message=$message)"
  override def getMessage(): String = s"$message (path: $path)"
}

object HttpPayloadError {
  val schema: Schema[HttpPayloadError] = {
    val path = PayloadPath.schema.required[HttpPayloadError]("path", _.path)
    val expected = string.required[HttpPayloadError]("expected", _.expected)
    val message = string.required[HttpPayloadError]("message", _.message)
    struct(path, expected, message)(HttpPayloadError.apply)
  }
}

sealed trait MetadataError extends HttpContractError {
  import MetadataError._

  override def getMessage(): String = this match {
    case NotFound(field, location) =>
      s"${location.show} was not found (field $field)"
    case WrongType(field, location, expectedType, value) =>
      s"""String "$value", found in ${location.show}, does not fit field $field ($expectedType)"""
    case ArityError(field, location) =>
      s"Field $field expects a single value to be found at ${location.show}"
    case FailedConstraint(field, location, message) =>
      s"Field $field, found in ${location.show}, failed constraint checks with message: $message"
    case ImpossibleDecoding(message) =>
      message
  }
}

object MetadataError {

  case class NotFound(field: String, location: HttpBinding)
      extends MetadataError

  object NotFound {
    val schema: Schema[NotFound] = struct(
      string.required[NotFound]("field", _.field),
      HttpBinding.schema.required[NotFound]("location", _.location)
    )(NotFound.apply)
  }

  case class WrongType(
      field: String,
      location: HttpBinding,
      expectedType: String,
      value: String
  ) extends MetadataError

  object WrongType {
    val schema = struct(
      string.required[WrongType]("field", _.field),
      HttpBinding.schema.required[WrongType]("location", _.location),
      string.required[WrongType]("expectedType", _.expectedType),
      string.required[WrongType]("value", _.value)
    )(WrongType.apply)
  }

  case class ArityError(
      field: String,
      location: HttpBinding
  ) extends MetadataError

  object ArityError {
    val schema = struct(
      string.required[ArityError]("field", _.field),
      HttpBinding.schema.required[ArityError]("location", _.location)
    )(ArityError.apply)
  }

  case class FailedConstraint(
      field: String,
      location: HttpBinding,
      message: String
  ) extends MetadataError

  object FailedConstraint {
    val schema = struct(
      string.required[FailedConstraint]("field", _.field),
      HttpBinding.schema.required[FailedConstraint]("location", _.location),
      string.required[FailedConstraint]("message", _.message)
    )(FailedConstraint.apply)
  }

  case class ImpossibleDecoding(
      message: String
  ) extends MetadataError

  object ImpossibleDecoding {
    val schema = struct(
      string.required[ImpossibleDecoding]("message", _.message)
    )(ImpossibleDecoding.apply)
  }

  val schema: Schema[MetadataError] = {
    val notFound = NotFound.schema.oneOf[MetadataError]("notFound")
    val wrongType = WrongType.schema.oneOf[MetadataError]("wrongType")
    val arityError = ArityError.schema.oneOf[MetadataError]("arity")
    val failedConstraint =
      FailedConstraint.schema.oneOf[MetadataError]("failedConstraint")
    val impossibleDecoding =
      ImpossibleDecoding.schema.oneOf[MetadataError]("impossibleDecoding")

    union(
      notFound,
      wrongType,
      arityError,
      failedConstraint,
      impossibleDecoding
    ) {
      case _: NotFound           => 0
      case _: WrongType          => 1
      case _: ArityError         => 2
      case _: FailedConstraint   => 3
      case _: ImpossibleDecoding => 4
    }
  }

}

case class UpstreamServiceError(message: String) extends HttpContractError {
  override def toString: String =
    s"UpstreamServiceError(message=$message)"


  override def getMessage: String = message
}

object UpstreamServiceError {
  val schema: Schema[UpstreamServiceError] = {
    val message = string.required[UpstreamServiceError]("message", _.message)
    struct(message)(UpstreamServiceError.apply)
  }
}
