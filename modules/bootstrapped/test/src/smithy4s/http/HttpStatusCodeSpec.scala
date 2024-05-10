package smithy4s.http

import munit._
import smithy4s.schema.Schema
import smithy4s._
import smithy4s.Schema._

final class HttpStatusCodeSpec extends FunSuite {

  case class StaticError(message: String)

  object StaticError extends ShapeTag.Companion[StaticError] {
    implicit val schema: Schema[StaticError] =
      Schema
        .struct[StaticError](
          string
            .required[StaticError]("message", _.message)
        )(StaticError.apply)
        .withId("", "StaticError")
        .addHints(smithy.api.HttpError(503))

    val id: ShapeId = ShapeId("", "StaticError")
  }

  test("HttpStatusCode works on static error code") {
    val encoder = HttpStatusCode.fromSchema(StaticError.schema)
    assert(encoder.code(StaticError("error"), 500) == 503)
  }

  case class DynamicError(message: String, code: Int)

  object DynamicError extends ShapeTag.Companion[DynamicError] {
    implicit val schema: Schema[DynamicError] =
      Schema
        .struct[DynamicError](
          string
            .required[DynamicError]("message", _.message),
          int
            .required[DynamicError]("code", _.code)
            .addHints(smithy.api.HttpResponseCode())
        )(DynamicError.apply)
        .withId("", "DynamicError")

    val id: ShapeId = ShapeId("", "DynamicError")
  }

  test("HttpStatusCode works on dynamic error code") {
    val encoder = HttpStatusCode.fromSchema(DynamicError.schema)
    assert(encoder.code(DynamicError("error", 402), 503) == 402)
  }

}
