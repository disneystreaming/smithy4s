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
    union(payload, metadata) {
      case _: HttpPayloadError => 0
      case _: MetadataError    => 1
    }
  }

}

case class HttpPayloadError private (
    path: PayloadPath,
    expected: String,
    message: String
) extends HttpContractError {
  def withPath(value: PayloadPath): HttpPayloadError = {
    copy(path = value)
  }

  def withExpected(value: String): HttpPayloadError = {
    copy(expected = value)
  }

  def withMessage(value: String): HttpPayloadError = {
    copy(message = value)
  }
  override def toString(): String =
    s"HttpPayloadError($path, expected = $expected, message=$message)"
  override def getMessage(): String = s"$message (path: $path)"
}

object HttpPayloadError {
  @scala.annotation.nowarn(
    "msg=private method unapply in object HttpPayloadError is never used"
  )
  private def unapply(c: HttpPayloadError): Option[HttpPayloadError] = Some(c)
  def apply(
      path: PayloadPath,
      expected: String,
      message: String
  ): HttpPayloadError = {
    new HttpPayloadError(path, expected, message)
  }

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
    case nf: NotFound =>
      s"${nf.location.show} was not found (field ${nf.field})"
    case wt: WrongType =>
      s"""String "${wt.value}", found in ${wt.location.show}, does not fit field ${wt.field} (${wt.expectedType})"""
    case ae: ArityError =>
      s"Field ${ae.field} expects a single value to be found at ${ae.location.show}"
    case fc: FailedConstraint =>
      s"Field ${fc.field}, found in ${fc.location.show}, failed constraint checks with message: ${fc.message}"
    case id: ImpossibleDecoding =>
      id.message
  }
}

object MetadataError {

  case class NotFound private (field: String, location: HttpBinding)
      extends MetadataError {
    def withField(value: String): NotFound = {
      copy(field = value)
    }

    def withLocation(value: HttpBinding): NotFound = {
      copy(location = value)
    }

  }

  object NotFound {
    @scala.annotation.nowarn(
      "msg=private method unapply in object NotFound is never used"
    )
    private def unapply(c: NotFound): Option[NotFound] = Some(c)
    def apply(field: String, location: HttpBinding): NotFound = {
      new NotFound(field, location)
    }

    val schema: Schema[NotFound] = struct(
      string.required[NotFound]("field", _.field),
      HttpBinding.schema.required[NotFound]("location", _.location)
    )(NotFound.apply)
  }

  case class WrongType private (
      field: String,
      location: HttpBinding,
      expectedType: String,
      value: String
  ) extends MetadataError {
    def withField(value: String): WrongType = {
      copy(field = value)
    }

    def withLocation(value: HttpBinding): WrongType = {
      copy(location = value)
    }

    def withExpectedType(value: String): WrongType = {
      copy(expectedType = value)
    }

    def withValue(value: String): WrongType = {
      copy(value = value)
    }

  }

  object WrongType {
    @scala.annotation.nowarn(
      "msg=private method unapply in object WrongType is never used"
    )
    private def unapply(c: WrongType): Option[WrongType] = Some(c)
    def apply(
        field: String,
        location: HttpBinding,
        expectedType: String,
        value: String
    ): WrongType = {
      new WrongType(field, location, expectedType, value)
    }

    val schema = struct(
      string.required[WrongType]("field", _.field),
      HttpBinding.schema.required[WrongType]("location", _.location),
      string.required[WrongType]("expectedType", _.expectedType),
      string.required[WrongType]("value", _.value)
    )(WrongType.apply)
  }

  case class ArityError private (
      field: String,
      location: HttpBinding
  ) extends MetadataError {
    def withField(value: String): ArityError = {
      copy(field = value)
    }

    def withLocation(value: HttpBinding): ArityError = {
      copy(location = value)
    }

  }

  object ArityError {
    @scala.annotation.nowarn(
      "msg=private method unapply in object ArityError is never used"
    )
    private def unapply(c: ArityError): Option[ArityError] = Some(c)
    def apply(field: String, location: HttpBinding): ArityError = {
      new ArityError(field, location)
    }

    val schema = struct(
      string.required[ArityError]("field", _.field),
      HttpBinding.schema.required[ArityError]("location", _.location)
    )(ArityError.apply)
  }

  case class FailedConstraint private (
      field: String,
      location: HttpBinding,
      message: String
  ) extends MetadataError {
    def withField(value: String): FailedConstraint = {
      copy(field = value)
    }

    def withLocation(value: HttpBinding): FailedConstraint = {
      copy(location = value)
    }

    def withMessage(value: String): FailedConstraint = {
      copy(message = value)
    }

  }

  object FailedConstraint {
    @scala.annotation.nowarn(
      "msg=private method unapply in object FailedConstraint is never used"
    )
    private def unapply(c: FailedConstraint): Option[FailedConstraint] = Some(c)
    def apply(
        field: String,
        location: HttpBinding,
        message: String
    ): FailedConstraint = {
      new FailedConstraint(field, location, message)
    }

    val schema = struct(
      string.required[FailedConstraint]("field", _.field),
      HttpBinding.schema.required[FailedConstraint]("location", _.location),
      string.required[FailedConstraint]("message", _.message)
    )(FailedConstraint.apply)
  }

  case class ImpossibleDecoding private (
      message: String
  ) extends MetadataError {
    def withMessage(value: String): ImpossibleDecoding = {
      copy(message = value)
    }

  }

  object ImpossibleDecoding {
    @scala.annotation.nowarn(
      "msg=private method unapply in object ImpossibleDecoding is never used"
    )
    private def unapply(c: ImpossibleDecoding): Option[ImpossibleDecoding] =
      Some(c)
    def apply(message: String): ImpossibleDecoding = {
      new ImpossibleDecoding(message)
    }
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
