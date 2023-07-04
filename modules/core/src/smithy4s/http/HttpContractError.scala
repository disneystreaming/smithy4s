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

import smithy4s.schema._
import smithy4s.schema.Schema._
import smithy4s.codecs.PayloadError

sealed trait HttpContractError
    extends Throwable
    with scala.util.control.NoStackTrace

object HttpContractError {

  val schema: Schema[HttpContractError] = {
    val payload = HttpPayloadError.schema.oneOf[HttpContractError]("payload")
    val metadata = MetadataError.schema.oneOf[HttpContractError]("metadata")
    union(payload, metadata) {
      case n: HttpPayloadError => payload(n)
      case w: MetadataError    => metadata(w)
    }
  }

}

case class HttpPayloadError(
    payloadError: PayloadError
) extends HttpContractError {
  import payloadError._
  override def toString(): String =
    s"HttpPayloadError($path, expected = $expected, message=$message)"
  override def getMessage(): String = s"$message (path: $path)"
}

object HttpPayloadError {
  val schema: Schema[HttpPayloadError] = PayloadError.schema
    .biject(
      HttpPayloadError(_),
      (_: HttpPayloadError).payloadError
    )
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
    union(notFound, wrongType, arityError, failedConstraint) {
      case n: NotFound           => notFound(n)
      case w: WrongType          => wrongType(w)
      case a: ArityError         => arityError(a)
      case f: FailedConstraint   => failedConstraint(f)
      case i: ImpossibleDecoding => impossibleDecoding(i)
    }
  }

}
