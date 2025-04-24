/*
 *  Copyright 2021-2025 Disney Streaming
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
